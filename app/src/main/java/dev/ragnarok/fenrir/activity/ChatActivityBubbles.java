package dev.ragnarok.fenrir.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrListener;

import dev.ragnarok.fenrir.Extra;
import dev.ragnarok.fenrir.R;
import dev.ragnarok.fenrir.fragment.AudioPlayerFragment;
import dev.ragnarok.fenrir.fragment.ChatFragment;
import dev.ragnarok.fenrir.fragment.GifPagerFragment;
import dev.ragnarok.fenrir.fragment.PhotoPagerFragment;
import dev.ragnarok.fenrir.fragment.SinglePhotoFragment;
import dev.ragnarok.fenrir.fragment.StoryPagerFragment;
import dev.ragnarok.fenrir.listener.AppStyleable;
import dev.ragnarok.fenrir.longpoll.NotificationHelper;
import dev.ragnarok.fenrir.model.Peer;
import dev.ragnarok.fenrir.place.Place;
import dev.ragnarok.fenrir.place.PlaceFactory;
import dev.ragnarok.fenrir.place.PlaceProvider;
import dev.ragnarok.fenrir.settings.CurrentTheme;
import dev.ragnarok.fenrir.util.AssertUtils;
import dev.ragnarok.fenrir.util.Objects;
import dev.ragnarok.fenrir.util.Utils;
import dev.ragnarok.fenrir.util.ViewUtils;

public class ChatActivityBubbles extends NoMainActivity implements PlaceProvider, AppStyleable {

    public static final String ACTION_OPEN_PLACE = "dev.ragnarok.fenrir.activity.ChatActivityBubbles.openPlace";
    //resolveToolbarNavigationIcon();
    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = this::keyboardHide;

    public static Intent forStart(Context context, int accountId, @NonNull Peer peer) {
        Intent intent = new Intent(context, ChatActivityBubbles.class);
        intent.setAction(ACTION_OPEN_PLACE);
        intent.putExtra(Extra.PLACE, PlaceFactory.getChatPlace(accountId, accountId, peer));
        intent.putExtra(Extra.ACCOUNT_ID, accountId);
        intent.putExtra(Extra.OWNER_ID, peer.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Slidr.attach(this, new SlidrConfig.Builder().listener(new SlidrListener() {
            @Override
            public void onSlideStateChanged(int state) {

            }

            @Override
            public void onSlideChange(float percent) {

            }

            @Override
            public void onSlideOpened() {

            }

            @Override
            public boolean onSlideClosed() {
                NotificationHelper.resetBubbleOpened(ChatActivityBubbles.this, false);
                finish();
                return true;
            }
        }).scrimColor(CurrentTheme.getColorBackground(this)).build());
        super.onCreate(savedInstanceState);
        if (Objects.isNull(savedInstanceState)) {
            handleIntent(getIntent());
            getSupportFragmentManager().addOnBackStackChangedListener(mOnBackStackChangedListener);
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }
        String action = intent.getAction();
        if (ACTION_OPEN_PLACE.equals(action)) {
            Place place = intent.getParcelableExtra(Extra.PLACE);
            if (Objects.isNull(place)) {
                finish();
                return;
            }
            openPlace(place);
        }
    }

    public void keyboardHide() {
        try {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(getWindow().getDecorView().getRootView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void openPlace(Place place) {
        Bundle args = place.getArgs();
        switch (place.getType()) {
            case Place.CHAT:
                Peer peer = args.getParcelable(Extra.PEER);
                AssertUtils.requireNonNull(peer);
                ChatFragment chatFragment = ChatFragment.Companion.newInstance(args.getInt(Extra.ACCOUNT_ID), args.getInt(Extra.OWNER_ID), peer);
                attachToFront(chatFragment);
                break;

            case Place.VK_PHOTO_ALBUM_GALLERY:
            case Place.FAVE_PHOTOS_GALLERY:
            case Place.SIMPLE_PHOTO_GALLERY:
            case Place.VK_PHOTO_TMP_SOURCE:
            case Place.VK_PHOTO_ALBUM_GALLERY_SAVED:
                attachToFront(PhotoPagerFragment.newInstance(place.getType(), args));
                break;

            case Place.SINGLE_PHOTO:
                attachToFront(SinglePhotoFragment.newInstance(args));
                break;

            case Place.GIF_PAGER:
                attachToFront(GifPagerFragment.newInstance(args));
                break;
            case Place.STORY_PLAYER:
                attachToFront(StoryPagerFragment.newInstance(args));
                break;
            case Place.PLAYER:
                Fragment player = getSupportFragmentManager().findFragmentByTag("audio_player");
                if (player instanceof AudioPlayerFragment)
                    ((AudioPlayerFragment) player).dismiss();
                AudioPlayerFragment.newInstance(args).show(getSupportFragmentManager(), "audio_player");
                break;
            default:
                Intent intent = new Intent(this, SwipebleActivity.class);
                intent.setAction(MainActivity.ACTION_OPEN_PLACE);
                intent.putExtra(Extra.PLACE, place);
                SwipebleActivity.start(this, intent);
                break;
        }
    }

    private void attachToFront(Fragment fragment) {
        attachToFront(fragment, true);
    }

    private void attachToFront(Fragment fragment, boolean animate) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (animate)
            fragmentTransaction.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit);

        fragmentTransaction
                .replace(R.id.fragment, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public void onPause() {
        ViewUtils.keyboardHide(this);
        super.onPause();
    }

    public void onResume() {
        Intent data = getIntent();
        if (Objects.nonNull(data) && Objects.nonNull(data.getExtras())) {
            NotificationHelper.setBubbleOpened(data.getExtras().getInt(Extra.ACCOUNT_ID), data.getExtras().getInt(Extra.OWNER_ID));
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        NotificationHelper.resetBubbleOpened(this, true);
        getSupportFragmentManager().removeOnBackStackChangedListener(mOnBackStackChangedListener);
        ViewUtils.keyboardHide(this);
        super.onDestroy();
    }

    @Override
    public void hideMenu(boolean hide) {

    }

    @Override
    public void openMenu(boolean open) {

    }

    @Override
    public void setStatusbarColored(boolean colored, boolean invertIcons) {
        int statusbarNonColored = CurrentTheme.getStatusBarNonColored(this);
        int statusbarColored = CurrentTheme.getStatusBarColor(this);


        Window w = getWindow();
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(colored ? statusbarColored : statusbarNonColored);
        @ColorInt
        int navigationColor = colored ? CurrentTheme.getNavigationBarColor(this) : Color.BLACK;
        w.setNavigationBarColor(navigationColor);

        if (Utils.hasMarshmallow()) {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            if (invertIcons) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }

        if (Utils.hasOreo()) {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            if (invertIcons) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                w.getDecorView().setSystemUiVisibility(flags);
                w.setNavigationBarColor(Color.WHITE);
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                w.getDecorView().setSystemUiVisibility(flags);
            }
        }
    }
}
