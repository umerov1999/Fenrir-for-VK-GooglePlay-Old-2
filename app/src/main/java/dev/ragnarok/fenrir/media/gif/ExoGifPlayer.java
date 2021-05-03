package dev.ragnarok.fenrir.media.gif;

import android.view.SurfaceHolder;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.video.VideoListener;

import java.util.ArrayList;
import java.util.List;

import dev.ragnarok.fenrir.Account_Types;
import dev.ragnarok.fenrir.App;
import dev.ragnarok.fenrir.Constants;
import dev.ragnarok.fenrir.model.ProxyConfig;
import dev.ragnarok.fenrir.model.VideoSize;
import dev.ragnarok.fenrir.util.AssertUtils;
import dev.ragnarok.fenrir.util.Logger;
import dev.ragnarok.fenrir.util.Utils;

import static dev.ragnarok.fenrir.util.Objects.nonNull;

public class ExoGifPlayer implements IGifPlayer {

    private final String url;
    private final ProxyConfig proxyConfig;
    private final List<IVideoSizeChangeListener> videoSizeChangeListeners = new ArrayList<>(1);
    private final List<IStatusChangeListener> statusChangeListeners = new ArrayList<>(1);
    private final boolean isRepeat;
    private int status;
    private VideoSize size;
    private final VideoListener videoListener = new VideoListener() {
        @Override
        public void onVideoSizeChanged(int i, int i1, int i2, float v) {
            size = new VideoSize(i, i1);
            ExoGifPlayer.this.onVideoSizeChanged();
        }

        @Override
        public void onRenderedFirstFrame() {

        }
    };
    private SimpleExoPlayer internalPlayer;
    private boolean supposedToBePlaying;

    public ExoGifPlayer(String url, ProxyConfig proxyConfig, boolean isRepeat) {
        this.isRepeat = isRepeat;
        this.url = url;
        this.proxyConfig = proxyConfig;
        status = IStatus.INIT;
    }

    private static void pausePlayer(SimpleExoPlayer internalPlayer) {
        internalPlayer.setPlayWhenReady(false);
        internalPlayer.getPlaybackState();
    }

    private static void startPlayer(SimpleExoPlayer internalPlayer) {
        internalPlayer.setPlayWhenReady(true);
        internalPlayer.getPlaybackState();
    }

    @Override
    public VideoSize getVideoSize() {
        return size;
    }

    @Override
    public void play() {
        if (supposedToBePlaying) return;

        supposedToBePlaying = true;

        switch (status) {
            case IStatus.PREPARED:
                AssertUtils.requireNonNull(internalPlayer);
                startPlayer(internalPlayer);
                break;
            case IStatus.INIT:
                preparePlayer();
                break;
            case IStatus.PREPARING:
                //do nothing
                break;
        }
    }

    private void preparePlayer() {
        setStatus(IStatus.PREPARING);
        internalPlayer = new SimpleExoPlayer.Builder(App.getInstance()).build();


        String userAgent = Constants.USER_AGENT(Account_Types.BY_TYPE);

        // This is the MediaSource representing the media to be played:
        // FOR SD CARD SOURCE:
        // MediaSource videoSource = new ExtractorMediaSource(mp4VideoUri, dataSourceFactory, extractorsFactory, null, null);
        // FOR LIVESTREAM LINK:

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(Utils.getExoPlayerFactory(userAgent, proxyConfig)).createMediaSource(Utils.makeMediaItem((url)));
        internalPlayer.setRepeatMode(isRepeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        internalPlayer.addListener(new Player.EventListener() {
            @Override
            public void onPlaybackStateChanged(@Player.State int state) {
                Logger.d("FenrirExo", "onPlaybackStateChanged, state: " + state);
                onInternalPlayerStateChanged(state);
            }
        });

        internalPlayer.addVideoListener(videoListener);
        internalPlayer.setPlayWhenReady(true);
        internalPlayer.setMediaSource(mediaSource);
        internalPlayer.prepare();
    }

    private void onInternalPlayerStateChanged(@Player.State int state) {
        if (state == Player.STATE_READY) {
            setStatus(IStatus.PREPARED);
        }
    }

    private void onVideoSizeChanged() {
        for (IVideoSizeChangeListener listener : videoSizeChangeListeners) {
            listener.onVideoSizeChanged(this, size);
        }
    }

    @Override
    public void pause() {
        if (!supposedToBePlaying) return;

        supposedToBePlaying = false;

        if (nonNull(internalPlayer)) {
            try {
                pausePlayer(internalPlayer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setDisplay(SurfaceHolder holder) {
        if (nonNull(internalPlayer)) {
            internalPlayer.setVideoSurfaceHolder(holder);
        }
    }

    @Override
    public void release() {
        if (nonNull(internalPlayer)) {
            try {
                internalPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setStatus(int newStatus) {
        int oldStatus = status;

        if (status == newStatus) {
            return;
        }

        status = newStatus;
        for (IStatusChangeListener listener : statusChangeListeners) {
            listener.onPlayerStatusChange(this, oldStatus, newStatus);
        }
    }

    @Override
    public void addVideoSizeChangeListener(IVideoSizeChangeListener listener) {
        videoSizeChangeListeners.add(listener);
    }

    @Override
    public void addStatusChangeListener(IStatusChangeListener listener) {
        statusChangeListeners.add(listener);
    }

    @Override
    public void removeVideoSizeChangeListener(IVideoSizeChangeListener listener) {
        videoSizeChangeListeners.remove(listener);
    }

    @Override
    public void removeStatusChangeListener(IStatusChangeListener listener) {
        statusChangeListeners.remove(listener);
    }

    @Override
    public int getPlayerStatus() {
        return status;
    }
}