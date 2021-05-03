package dev.ragnarok.fenrir.mvp.presenter.conversations;

import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import dev.ragnarok.fenrir.R;
import dev.ragnarok.fenrir.api.Apis;
import dev.ragnarok.fenrir.api.model.VKApiPhoto;
import dev.ragnarok.fenrir.api.model.response.AttachmentsHistoryResponse;
import dev.ragnarok.fenrir.db.Stores;
import dev.ragnarok.fenrir.db.serialize.Serializers;
import dev.ragnarok.fenrir.domain.mappers.Dto2Model;
import dev.ragnarok.fenrir.model.Photo;
import dev.ragnarok.fenrir.model.TmpSource;
import dev.ragnarok.fenrir.mvp.reflect.OnGuiCreated;
import dev.ragnarok.fenrir.mvp.view.conversations.IChatAttachmentPhotosView;
import dev.ragnarok.fenrir.util.Analytics;
import dev.ragnarok.fenrir.util.DisposableHolder;
import dev.ragnarok.fenrir.util.Pair;
import dev.ragnarok.fenrir.util.RxUtils;
import io.reactivex.rxjava3.core.Single;

import static dev.ragnarok.fenrir.util.Objects.nonNull;
import static dev.ragnarok.fenrir.util.Utils.safeCountOf;

public class ChatAttachmentPhotoPresenter extends BaseChatAttachmentsPresenter<Photo, IChatAttachmentPhotosView> {

    private final DisposableHolder<Void> openGalleryDisposableHolder = new DisposableHolder<>();

    public ChatAttachmentPhotoPresenter(int peerId, int accountId, @Nullable Bundle savedInstanceState) {
        super(peerId, accountId, savedInstanceState);
    }

    @Override
    Single<Pair<String, List<Photo>>> requestAttachments(int peerId, String nextFrom) {
        return Apis.get().vkDefault(getAccountId())
                .messages()
                .getHistoryAttachments(peerId, "photo", nextFrom, 50, null)
                .map(response -> {
                    List<Photo> photos = new ArrayList<>();

                    for (AttachmentsHistoryResponse.One one : response.items) {
                        if (nonNull(one) && nonNull(one.entry) && one.entry.attachment instanceof VKApiPhoto) {
                            VKApiPhoto dto = (VKApiPhoto) one.entry.attachment;
                            photos.add(Dto2Model.transform(dto).setMsgId(one.messageId).setMsgPeerId(peerId));
                        }
                    }

                    return Pair.Companion.create(response.next_from, photos);
                });
    }

    @Override
    void onDataChanged() {
        super.onDataChanged();
        resolveToolbar();
    }

    @OnGuiCreated
    private void resolveToolbar() {
        if (isGuiReady()) {
            getView().setToolbarTitle(getString(R.string.attachments_in_chat));
            getView().setToolbarSubtitle(getString(R.string.photos_count, safeCountOf(data)));
        }
    }

    @Override
    public void onDestroyed() {
        openGalleryDisposableHolder.dispose();
        super.onDestroyed();
    }

    @SuppressWarnings("unused")
    public void firePhotoClick(int position, Photo photo) {
        List<Photo> photos = data;

        TmpSource source = new TmpSource(getInstanceId(), 0);

        fireTempDataUsage();

        openGalleryDisposableHolder.append(Stores.getInstance()
                .tempStore()
                .put(source.getOwnerId(), source.getSourceId(), data, Serializers.PHOTOS_SERIALIZER)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> onPhotosSavedToTmpStore(position, source), Analytics::logUnexpectedError));
    }

    private void onPhotosSavedToTmpStore(int index, TmpSource source) {
        callView(view -> view.goToTempPhotosGallery(getAccountId(), source, index));
    }
}
