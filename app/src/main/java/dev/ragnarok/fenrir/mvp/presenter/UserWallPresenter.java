package dev.ragnarok.fenrir.mvp.presenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.ragnarok.fenrir.Injection;
import dev.ragnarok.fenrir.R;
import dev.ragnarok.fenrir.api.model.VKApiUser;
import dev.ragnarok.fenrir.domain.IAccountsInteractor;
import dev.ragnarok.fenrir.domain.IFaveInteractor;
import dev.ragnarok.fenrir.domain.IOwnersRepository;
import dev.ragnarok.fenrir.domain.IPhotosInteractor;
import dev.ragnarok.fenrir.domain.IRelationshipInteractor;
import dev.ragnarok.fenrir.domain.IWallsRepository;
import dev.ragnarok.fenrir.domain.InteractorFactory;
import dev.ragnarok.fenrir.domain.Repository;
import dev.ragnarok.fenrir.fragment.friends.FriendsTabsFragment;
import dev.ragnarok.fenrir.model.FriendsCounters;
import dev.ragnarok.fenrir.model.LocalPhoto;
import dev.ragnarok.fenrir.model.LocalVideo;
import dev.ragnarok.fenrir.model.Peer;
import dev.ragnarok.fenrir.model.Photo;
import dev.ragnarok.fenrir.model.Post;
import dev.ragnarok.fenrir.model.PostFilter;
import dev.ragnarok.fenrir.model.User;
import dev.ragnarok.fenrir.model.UserDetails;
import dev.ragnarok.fenrir.model.criteria.WallCriteria;
import dev.ragnarok.fenrir.mvp.reflect.OnGuiCreated;
import dev.ragnarok.fenrir.mvp.view.IUserWallView;
import dev.ragnarok.fenrir.mvp.view.IWallView;
import dev.ragnarok.fenrir.place.PlaceFactory;
import dev.ragnarok.fenrir.settings.Settings;
import dev.ragnarok.fenrir.upload.IUploadManager;
import dev.ragnarok.fenrir.upload.MessageMethod;
import dev.ragnarok.fenrir.upload.Method;
import dev.ragnarok.fenrir.upload.Upload;
import dev.ragnarok.fenrir.upload.UploadDestination;
import dev.ragnarok.fenrir.upload.UploadIntent;
import dev.ragnarok.fenrir.upload.UploadResult;
import dev.ragnarok.fenrir.upload.UploadUtils;
import dev.ragnarok.fenrir.util.Pair;
import dev.ragnarok.fenrir.util.RxUtils;
import dev.ragnarok.fenrir.util.Utils;

import static dev.ragnarok.fenrir.util.Objects.nonNull;
import static dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime;
import static dev.ragnarok.fenrir.util.Utils.nonEmpty;

public class UserWallPresenter extends AbsWallPresenter<IUserWallView> {

    private final List<PostFilter> filters;
    private final IOwnersRepository ownersRepository;
    private final IRelationshipInteractor relationshipInteractor;
    private final IAccountsInteractor accountInteractor;
    private final IPhotosInteractor photosInteractor;
    private final IFaveInteractor faveInteractor;
    private final IWallsRepository wallsRepository;
    private final IUploadManager uploadManager;
    private final Context context;
    private User user;
    private UserDetails details;
    private boolean loadingAvatarPhotosNow;

    public UserWallPresenter(int accountId, int ownerId, @Nullable User owner, Context context, @Nullable Bundle savedInstanceState) {
        super(accountId, ownerId, savedInstanceState);
        this.context = context;

        ownersRepository = Repository.INSTANCE.getOwners();
        relationshipInteractor = InteractorFactory.createRelationshipInteractor();
        accountInteractor = InteractorFactory.createAccountInteractor();
        photosInteractor = InteractorFactory.createPhotosInteractor();
        faveInteractor = InteractorFactory.createFaveInteractor();
        wallsRepository = Repository.INSTANCE.getWalls();
        uploadManager = Injection.provideUploadManager();

        filters = new ArrayList<>();
        filters.addAll(createPostFilters());

        user = nonNull(owner) ? owner : new User(ownerId);
        details = new UserDetails();

        syncFiltersWithSelectedMode();
        syncFilterCountersWithDetails();

        refreshUserDetails();

        appendDisposable(uploadManager.observeResults()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onUploadFinished, RxUtils.ignore()));
    }

    public User getUser() {
        return user;
    }

    @Override
    protected void onRefresh() {
        requestActualFullInfo();
    }

    public void firePhotosSelected(ArrayList<LocalPhoto> localPhotos, String file, LocalVideo video) {
        if (nonEmpty(file))
            doUploadFile(file);
        else if (nonEmpty(localPhotos)) {
            doUploadPhotos(localPhotos);
        } else if (video != null) {
            doUploadVideo(video.getData().toString());
        }
    }

    private void doUploadFile(String file) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.select)
                .setNegativeButton(R.string.video, (dialog, which) -> doUploadFile(file, 0, true))
                .setPositiveButton(R.string.photo, (dialog, which) -> getView().doEditPhoto(Uri.fromFile(new File(file))))
                .create().show();
    }

    public void doUploadFile(String file, int size, boolean isVideo) {
        List<UploadIntent> intents;
        if (isVideo) {
            intents = UploadUtils.createIntents(getAccountId(), UploadDestination.forStory(MessageMethod.VIDEO), file, size, true);
        } else {
            intents = UploadUtils.createIntents(getAccountId(), UploadDestination.forStory(MessageMethod.PHOTO), file, size, true);
        }
        uploadManager.enqueue(intents);
    }

    private void doUploadVideo(String file) {
        List<UploadIntent> intents = UploadUtils.createVideoIntents(getAccountId(), UploadDestination.forStory(MessageMethod.VIDEO), file, true);
        uploadManager.enqueue(intents);
    }

    private void doUploadPhotos(List<LocalPhoto> photos) {
        if (photos.size() == 1) {
            Uri to_up = photos.get(0).getFullImageUri();
            if (new File(to_up.getPath()).isFile()) {
                to_up = Uri.fromFile(new File(to_up.getPath()));
            }
            getView().doEditPhoto(to_up);
            return;
        }
        List<UploadIntent> intents = UploadUtils.createIntents(getAccountId(), UploadDestination.forStory(MessageMethod.PHOTO), photos, Upload.IMAGE_SIZE_FULL, true);
        uploadManager.enqueue(intents);
    }

    private void onUploadFinished(Pair<Upload, UploadResult<?>> pair) {
        UploadDestination destination = pair.getFirst().getDestination();
        if (destination.getMethod() == Method.PHOTO_TO_PROFILE && destination.getOwnerId() == ownerId) {
            requestActualFullInfo();

            if (isGuiResumed()) {
                Post post = (Post) pair.getSecond().getResult();
                getView().showAvatarUploadedMessage(getAccountId(), post);
            }
        } else if (destination.getMethod() == Method.STORY && Settings.get().accounts().getCurrent() == ownerId) {
            fireRefresh();
        }
    }

    @OnGuiCreated
    private void resolveCounters() {
        if (isGuiReady()) {
            getView().displayCounters(details.getFriendsCount(),
                    details.getMutualFriendsCount(),
                    details.getFollowersCount(),
                    details.getGroupsCount(),
                    details.getPhotosCount(),
                    details.getAudiosCount(),
                    details.getVideosCount(),
                    details.getArticlesCount(),
                    details.getProductsCount(),
                    details.getGiftCount());
        }
    }

    @OnGuiCreated
    private void resolveBaseUserInfoViews() {
        if (isGuiReady()) {
            getView().displayBaseUserInfo(user);
        }
    }

    private void refreshUserDetails() {
        int accountId = getAccountId();
        appendDisposable(ownersRepository.getFullUserInfo(accountId, ownerId, IOwnersRepository.MODE_CACHE)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(pair -> {
                    onFullInfoReceived(pair.getFirst(), pair.getSecond());
                    requestActualFullInfo();
                }, RxUtils.ignore()));
    }

    private void requestActualFullInfo() {
        int accountId = getAccountId();
        appendDisposable(ownersRepository.getFullUserInfo(accountId, ownerId, IOwnersRepository.MODE_NET)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(pair -> onFullInfoReceived(pair.getFirst(), pair.getSecond()), this::onDetailsGetError));
    }

    private void onFullInfoReceived(User user, UserDetails details) {
        if (nonNull(user)) {
            this.user = user;
            onUserInfoUpdated();
        }

        if (nonNull(details)) {
            this.details = details;
            onUserDetalsUpdated();
        }

        resolveStatusView();
        resolveMenu();
    }

    private void onUserDetalsUpdated() {
        syncFilterCountersWithDetails();
        callView(IUserWallView::notifyWallFiltersChanged);

        resolvePrimaryActionButton();
        resolveCounters();
    }

    private void onUserInfoUpdated() {
        resolveBaseUserInfoViews();
    }

    private void onDetailsGetError(Throwable t) {
        showError(getView(), getCauseIfRuntime(t));
    }

    private void syncFiltersWithSelectedMode() {
        for (PostFilter filter : filters) {
            filter.setActive(filter.getMode() == getWallFilter());
        }
    }

    private void syncFilterCountersWithDetails() {
        for (PostFilter filter : filters) {
            switch (filter.getMode()) {
                case WallCriteria.MODE_ALL:
                    filter.setCount(details.getAllWallCount());
                    break;
                case WallCriteria.MODE_OWNER:
                    filter.setCount(details.getOwnWallCount());
                    break;
                case WallCriteria.MODE_SCHEDULED:
                    filter.setCount(details.getPostponedWallCount());
                    break;
            }
        }
    }

    @Override
    public void onGuiCreated(@NonNull IUserWallView viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayWallFilters(filters);
    }

    private List<PostFilter> createPostFilters() {
        List<PostFilter> filters = new ArrayList<>();
        filters.add(new PostFilter(WallCriteria.MODE_ALL, getString(R.string.all_posts)));
        filters.add(new PostFilter(WallCriteria.MODE_OWNER, getString(R.string.owner_s_posts)));

        if (isMyWall()) {
            filters.add(new PostFilter(WallCriteria.MODE_SCHEDULED, getString(R.string.scheduled)));
        }

        return filters;
    }

    public void fireStatusClick() {
        if (nonNull(details) && nonNull(details.getStatusAudio())) {
            getView().playAudioList(getAccountId(), 0, Utils.singletonArrayList(details.getStatusAudio()));
        }
    }

    public void fireMoreInfoClick() {
        getView().openUserDetails(getAccountId(), user, details);
    }

    public void fireFilterClick(PostFilter entry) {
        if (changeWallFilter(entry.getMode())) {
            syncFiltersWithSelectedMode();

            getView().notifyWallFiltersChanged();
        }
    }

    public void fireHeaderPhotosClick() {
        getView().openPhotoAlbums(getAccountId(), ownerId, user);
    }

    public void fireHeaderAudiosClick() {
        getView().openAudios(getAccountId(), ownerId, user);
    }

    public void fireHeaderArticlesClick() {
        getView().openArticles(getAccountId(), ownerId, user);
    }

    public void fireHeaderProductsClick() {
        getView().openProducts(getAccountId(), ownerId, user);
    }

    public void fireHeaderGiftsClick() {
        getView().openGifts(getAccountId(), ownerId, user);
    }

    public void fireHeaderFriendsClick() {
        getView().openFriends(getAccountId(), ownerId, FriendsTabsFragment.TAB_ALL_FRIENDS, getFriendsCounters());
    }

    private FriendsCounters getFriendsCounters() {
        return new FriendsCounters(
                details.getFriendsCount(),
                details.getOnlineFriendsCount(),
                details.getFollowersCount(),
                details.getMutualFriendsCount()
        );
    }

    public void fireHeaderGroupsClick() {
        getView().openGroups(getAccountId(), ownerId, user);
    }

    public void fireHeaderVideosClick() {
        getView().openVideosLibrary(getAccountId(), ownerId, user);
    }

    @SuppressLint("ResourceType")
    @OnGuiCreated
    private void resolvePrimaryActionButton() {
        if (!isGuiReady()) return;

        @StringRes
        Integer title = null;
        if (getAccountId() == ownerId) {
            title = R.string.edit_status;
        } else {
            switch (user.getFriendStatus()) {
                case VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND:
                    title = R.string.add_to_friends;
                    break;
                case VKApiUser.FRIEND_STATUS_REQUEST_SENT:
                    title = R.string.cancel_request;
                    break;
                case VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST:
                    title = R.string.accept_request;
                    break;
                case VKApiUser.FRIEND_STATUS_IS_FRIEDND:
                    title = R.string.delete_from_friends;
                    break;
            }
            if (user.getBlacklisted_by_me()) {
                title = R.string.is_to_blacklist;
            }
        }

        getView().setupPrimaryActionButton(title);
    }

    public void firePrimaryActionsClick() {
        if (getAccountId() == ownerId) {
            getView().showEditStatusDialog(user.getStatus());
            return;
        }

        if (user.getBlacklisted_by_me()) {
            getView().showUnbanMessageDialog();
            return;
        }

        switch (user.getFriendStatus()) {
            case VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND:
                getView().showAddToFriendsMessageDialog();
                break;

            case VKApiUser.FRIEND_STATUS_REQUEST_SENT:
                fireDeleteFromFriends();
                break;
            case VKApiUser.FRIEND_STATUS_IS_FRIEDND:
                getView().showDeleteFromFriendsMessageDialog();
                break;

            case VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST:
                executeAddToFriendsRequest(null, false);
                break;
        }
    }

    private void DisplayUserProfileAlbum(List<Photo> photos) {
        setLoadingAvatarPhotosNow(false);

        if (photos.isEmpty()) {
            callView(view -> view.showSnackbar(R.string.no_photos_found, true));
            return;
        }
        Integer currentAvatarPhotoId = nonNull(details) && nonNull(details.getPhotoId()) ? details.getPhotoId().getId() : null;
        Integer currentAvatarOwner_id = nonNull(details) && nonNull(details.getPhotoId()) ? details.getPhotoId().getOwnerId() : null;
        int sel = 0;
        if (currentAvatarPhotoId != null && currentAvatarOwner_id != null) {
            int ut = 0;
            for (Photo i : photos) {
                if (i.getOwnerId() == currentAvatarOwner_id && i.getId() == currentAvatarPhotoId) {
                    sel = ut;
                    break;
                }
                ut++;
            }
        }
        int curr = sel;
        callView(view -> view.openPhotoAlbum(getAccountId(), ownerId, -6, new ArrayList<>(photos), curr));
    }

    private void onAddFriendResult(int resultCode) {
        Integer strRes = null;
        Integer newFriendStatus = null;

        switch (resultCode) {
            case IRelationshipInteractor.FRIEND_ADD_REQUEST_SENT:
                strRes = R.string.friend_request_sent;
                newFriendStatus = VKApiUser.FRIEND_STATUS_REQUEST_SENT;
                break;

            case IRelationshipInteractor.FRIEND_ADD_REQUEST_FROM_USER_APPROVED:
                strRes = R.string.friend_request_from_user_approved;
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_FRIEDND;
                break;

            case IRelationshipInteractor.FRIEND_ADD_RESENDING:
                strRes = R.string.request_resending;
                newFriendStatus = VKApiUser.FRIEND_STATUS_REQUEST_SENT;
                break;
        }

        if (nonNull(newFriendStatus)) {
            user.setFriendStatus(newFriendStatus);
        }

        if (nonNull(strRes) && isGuiReady()) {
            getView().showSnackbar(strRes, true);
        }

        resolvePrimaryActionButton();
    }

    public void fireDeleteFromFriends() {
        int accountId = getAccountId();
        appendDisposable(relationshipInteractor.deleteFriends(accountId, ownerId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onFriendsDeleteResult, t -> showError(getView(), getCauseIfRuntime(t))));
    }

    public void fireNewStatusEntered(String newValue) {
        int accountId = getAccountId();
        appendDisposable(accountInteractor.changeStatus(accountId, newValue)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> onStatusChanged(newValue), t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onStatusChanged(String status) {
        user.setStatus(status);

        callView(view -> view.showSnackbar(R.string.status_was_changed, true));
        resolveStatusView();
    }

    @OnGuiCreated
    private void resolveStatusView() {
        if (isGuiReady()) {
            String statusText;
            if (nonNull(details.getStatusAudio())) {
                statusText = details.getStatusAudio().getArtistAndTitle();
            } else {
                statusText = user.getStatus();
            }

            getView().displayUserStatus(statusText, nonNull(details.getStatusAudio()));
        }
    }

    @OnGuiCreated
    private void resolveMenu() {
        if (isGuiReady()) {
            getView().InvalidateOptionsMenu();
        }
    }

    public void fireAddToFrindsClick(String message) {
        executeAddToFriendsRequest(message, false);
    }

    public void fireAddToBookmarks() {
        int accountId = getAccountId();
        appendDisposable(faveInteractor.addPage(accountId, ownerId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(this::onExecuteComplete, this::onExecuteError));
    }

    public void fireRemoveFromBookmarks() {
        int accountId = getAccountId();
        appendDisposable(faveInteractor.removePage(accountId, ownerId, true)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(this::onExecuteComplete, this::onExecuteError));
    }

    public void fireSubscribe() {
        int accountId = getAccountId();
        appendDisposable(wallsRepository.subscribe(accountId, ownerId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(t -> onExecuteComplete(), this::onExecuteError));
    }

    public void fireUnSubscribe() {
        int accountId = getAccountId();
        appendDisposable(wallsRepository.unsubscribe(accountId, ownerId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(t -> onExecuteComplete(), this::onExecuteError));
    }

    private void executeAddToFriendsRequest(String text, boolean follow) {
        int accountId = getAccountId();

        appendDisposable(relationshipInteractor.addFriend(accountId, ownerId, text, follow)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onAddFriendResult, t -> showError(getView(), getCauseIfRuntime(t))));
    }

    private void onFriendsDeleteResult(int responseCode) {
        Integer strRes = null;
        Integer newFriendStatus = null;

        switch (responseCode) {
            case IRelationshipInteractor.DeletedCodes.FRIEND_DELETED:
                newFriendStatus = VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST;
                strRes = R.string.friend_deleted;
                break;

            case IRelationshipInteractor.DeletedCodes.OUT_REQUEST_DELETED:
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND;
                strRes = R.string.out_request_deleted;
                break;

            case IRelationshipInteractor.DeletedCodes.IN_REQUEST_DELETED:
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND;
                strRes = R.string.in_request_deleted;
                break;

            case IRelationshipInteractor.DeletedCodes.SUGGESTION_DELETED:
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND;
                strRes = R.string.suggestion_deleted;
                break;
        }

        if (newFriendStatus != null) {
            user.setFriendStatus(newFriendStatus);
        }

        if (nonNull(strRes) && isGuiReady()) {
            getView().showSnackbar(strRes, true);

            resolvePrimaryActionButton();
        }
    }

    private void prepareUserAvatarsAndShow() {
        setLoadingAvatarPhotosNow(true);

        int accountId = getAccountId();

        appendDisposable(photosInteractor.get(accountId, ownerId, -6, 100, 0, true)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::DisplayUserProfileAlbum, this::onAvatarAlbumPrepareFailed));
    }

    private void onAvatarAlbumPrepareFailed(Throwable t) {
        setLoadingAvatarPhotosNow(false);
        showError(getView(), getCauseIfRuntime(t));
    }

    @OnGuiCreated
    private void resolveProgressDialogView() {
        if (isGuiReady()) {
            if (loadingAvatarPhotosNow) {
                getView().displayProgressDialog(R.string.please_wait, R.string.loading_owner_photo_album, false);
            } else {
                getView().dismissProgressDialog();
            }
        }
    }

    private void setLoadingAvatarPhotosNow(boolean loadingAvatarPhotosNow) {
        this.loadingAvatarPhotosNow = loadingAvatarPhotosNow;
        resolveProgressDialogView();
    }

    public void fireAvatarClick() {
        getView().showAvatarContextMenu(isMyWall());
    }

    public void fireOpenAvatarsPhotoAlbum() {
        prepareUserAvatarsAndShow();
    }

    public void fireAddToBlacklistClick() {
        int accountId = getAccountId();

        appendDisposable(InteractorFactory.createAccountInteractor()
                .banUsers(accountId, Collections.singletonList(user))
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(this::onExecuteComplete, this::onExecuteError));
    }

    public void fireMentions() {
        PlaceFactory.getMentionsPlace(getAccountId(), getOwnerId()).tryOpenWith(context);
    }

    @Override
    public void fireOptionViewCreated(IWallView.IOptionView view) {
        super.fireOptionViewCreated(view);
        view.setIsBlacklistedByMe(user.getBlacklisted_by_me());
        view.setIsFavorite(details.isSetFavorite());
        view.setIsSubscribed(details.isSetSubscribed());
    }

    public void fireGetRegistrationDate() {
        Utils.getRegistrationDate(context, getOwnerId());
    }

    public void fireReport() {
        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(context);
        alert.setTitle(R.string.report);
        CharSequence[] values = {"porn", "spam", "insult", "advertisement"};
        CharSequence[] items = {"Порнография", "Спам, Мошенничество", "Оскорбительное поведение", "Рекламная страница"};
        alert.setItems(items, (dialog, item) -> {
            String report = values[item].toString();
            appendDisposable(ownersRepository.report(getAccountId(), getOwnerId(), report, null)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(p -> {
                        if (p == 1)
                            getView().getCustomToast().showToast(R.string.success);
                        else
                            getView().getCustomToast().showToast(R.string.error);
                    }, t -> showError(getView(), getCauseIfRuntime(t))));
            dialog.dismiss();
        });
        alert.show();
    }

    public void fireRemoveBlacklistClick() {
        int accountId = getAccountId();

        appendDisposable(InteractorFactory.createAccountInteractor()
                .unbanUser(accountId, user.getId())
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(this::onExecuteComplete, this::onExecuteError));
    }

    private void onExecuteError(Throwable t) {
        showError(getView(), getCauseIfRuntime(t));
    }

    private void onExecuteComplete() {
        onRefresh();
        getView().getCustomToast().showToast(R.string.success);
    }

    public void fireChatClick() {
        int accountId = getAccountId();
        Peer peer = new Peer(Peer.fromUserId(user.getId()))
                .setAvaUrl(user.getMaxSquareAvatar())
                .setTitle(user.getFullName());

        getView().openChatWith(accountId, accountId, peer);
    }

    public void fireNewAvatarPhotoSelected(LocalPhoto photo) {
        UploadIntent intent = new UploadIntent(getAccountId(), UploadDestination.forProfilePhoto(ownerId))
                .setAutoCommit(true)
                .setFileId(photo.getImageId())
                .setFileUri(photo.getFullImageUri())
                .setSize(Upload.IMAGE_SIZE_FULL);

        uploadManager.enqueue(Collections.singletonList(intent));
    }

    public void fireNewAvatarPhotoSelected(String file) {
        UploadIntent intent = new UploadIntent(getAccountId(), UploadDestination.forProfilePhoto(ownerId))
                .setAutoCommit(true)
                .setFileUri(Uri.parse(file))
                .setSize(Upload.IMAGE_SIZE_FULL);

        uploadManager.enqueue(Collections.singletonList(intent));
    }

    @Override
    public void fireAddToNewsClick() {
        appendDisposable(InteractorFactory.createFeedInteractor().saveList(getAccountId(), user.getShortFullName(), Collections.singleton(user.getOwnerId()))
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(i -> getView().showSnackbar(R.string.success, true), t -> showError(getView(), t)));
    }

    @Override
    public void searchStory(boolean ByName) {
        appendDisposable(ownersRepository.searchStory(getAccountId(), ByName ? user.getFullName() : null, ByName ? null : ownerId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> {
                    if (!Utils.isEmpty(data)) {
                        stories.clear();
                        stories.addAll(data);
                        getView().updateStory(stories);
                    }
                }, t -> {
                }));
    }
}
