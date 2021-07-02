package dev.ragnarok.fenrir.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.ragnarok.fenrir.BuildConfig;
import dev.ragnarok.fenrir.Constants;
import dev.ragnarok.fenrir.api.model.LocalServerSettings;
import dev.ragnarok.fenrir.api.model.PlayerCoverBackgroundSettings;
import dev.ragnarok.fenrir.model.Lang;
import dev.ragnarok.fenrir.util.Objects;
import dev.ragnarok.fenrir.util.Utils;

class OtherSettings implements ISettings.IOtherSettings {

    private static final String KEY_JSON_STATE = "json_list_state";

    private static final String KEY_DONATE = "donates";

    private final Context app;

    OtherSettings(Context context) {
        app = context.getApplicationContext();
    }

    @Override
    public String getFeedSourceIds(int accountId) {
        return PreferenceManager.getDefaultSharedPreferences(app)
                .getString("source_ids" + accountId, null);
    }

    @Override
    public void setFeedSourceIds(int accountId, String sourceIds) {
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putString("source_ids" + accountId, sourceIds)
                .apply();
    }

    @Override
    public void storeFeedScrollState(int accountId, String state) {
        if (Objects.nonNull(state)) {
            PreferenceManager
                    .getDefaultSharedPreferences(app)
                    .edit()
                    .putString(KEY_JSON_STATE + accountId, state)
                    .apply();
        } else {
            PreferenceManager
                    .getDefaultSharedPreferences(app)
                    .edit()
                    .remove(KEY_JSON_STATE + accountId)
                    .apply();
        }
    }

    @Override
    public String restoreFeedScrollState(int accountId) {
        return PreferenceManager.getDefaultSharedPreferences(app).getString(KEY_JSON_STATE + accountId, null);
    }

    @Override
    public String restoreFeedNextFrom(int accountId) {
        return PreferenceManager
                .getDefaultSharedPreferences(app)
                .getString("next_from" + accountId, null);
    }

    @Override
    public void storeFeedNextFrom(int accountId, String nextFrom) {
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putString("next_from" + accountId, nextFrom)
                .apply();
    }

    @Override
    public boolean isAudioBroadcastActive() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("broadcast", false);
    }

    @Override
    public boolean isCommentsDesc() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("comments_desc", true);
    }

    @Override
    public boolean toggleCommentsDirection() {
        boolean descNow = isCommentsDesc();

        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putBoolean("comments_desc", !descNow)
                .apply();

        return !descNow;
    }

    @Override
    public boolean isKeepLongpoll() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("keep_longpoll", false);
    }

    @Override
    public void setDisableErrorFCM(boolean en) {
        PreferenceManager.getDefaultSharedPreferences(app).edit().putBoolean("disable_error_fcm", en).apply();
    }

    @Override
    public boolean isDisabledErrorFCM() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_error_fcm", false);
    }

    @Override
    public boolean isSettings_no_push() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("settings_no_push", false);
    }

    @Override
    public int getMaxBitmapResolution() {
        try {
            return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(app).getString("max_bitmap_resolution", "4000").trim());
        } catch (Exception e) {
            return 4000;
        }
    }

    @Override
    public int getFFmpegPlugin() {
        try {
            return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(app).getString("ffmpeg_audio_codecs", "1").trim());
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    public int getMusicLifecycle() {
        try {
            int v = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(app).getString("lifecycle_music_service", String.valueOf(Constants.AUDIO_PLAYER_SERVICE_IDLE)).trim());
            if (v < 60000) {
                PreferenceManager.getDefaultSharedPreferences(app).edit().putString("lifecycle_music_service", "60000").apply();
                v = 60000;
            }
            return v;
        } catch (Exception e) {
            return Constants.AUDIO_PLAYER_SERVICE_IDLE;
        }
    }

    @Override
    public boolean isAutoplay_gif() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("autoplay_gif", true);
    }

    @Override
    public boolean isStrip_news_repost() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("strip_news_repost", false);
    }

    @Override
    public String get_Api_Domain() {
        return PreferenceManager.getDefaultSharedPreferences(app).getString("vk_api_domain", "api.vk.com").trim();
    }

    @Override
    public String get_Auth_Domain() {
        return PreferenceManager.getDefaultSharedPreferences(app).getString("vk_auth_domain", "oauth.vk.com").trim();
    }

    @Override
    public boolean isDeveloper_mode() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("developer_mode", false);
    }

    @Override
    public boolean isForce_cache() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("force_cache", false);
    }

    @Override
    public boolean isUse_hls_downloader() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("use_hls_downloader", false);
    }

    @Override
    public boolean isDisable_history() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_history", false);
    }

    @Override
    public boolean isShow_wall_cover() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_wall_cover", true);
    }

    @Override
    public int getColorChat() {
        return PreferenceManager.getDefaultSharedPreferences(app).getInt("custom_chat_color", Color.argb(255, 255, 255, 255));
    }

    @Override
    public int getSecondColorChat() {
        return PreferenceManager.getDefaultSharedPreferences(app).getInt("custom_chat_color_second", Color.argb(255, 255, 255, 255));
    }

    @Override
    public boolean isCustom_chat_color() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("custom_chat_color_usage", false);
    }

    @Override
    public int getColorMyMessage() {
        return PreferenceManager.getDefaultSharedPreferences(app).getInt("custom_message_color", Color.parseColor("#CBD438FF"));
    }

    @Override
    public int getSecondColorMyMessage() {
        return PreferenceManager.getDefaultSharedPreferences(app).getInt("custom_second_message_color", Color.parseColor("#BF6539DF"));
    }

    @Override
    public boolean isCustom_MyMessage() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("custom_message_color_usage", false);
    }

    @Override
    public boolean isInfo_reading() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("info_reading", true);
    }

    @Override
    public boolean isAuto_read() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("auto_read", false);
    }

    @Override
    public boolean isNot_update_dialogs() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("not_update_dialogs", false);
    }

    @Override
    public boolean isBe_online() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("be_online", false);
    }

    @Override
    public int getDonate_anim_set() {
        try {
            return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(app).getString("donate_anim_set", "1").trim());
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    public boolean isUse_stop_audio() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("use_stop_audio", false);
    }

    @Override
    public boolean isBlur_for_player() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("blur_for_player", true);
    }

    @Override
    public boolean isShow_mini_player() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_mini_player", true);
    }

    @Override
    public boolean isEnable_last_read() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("enable_last_read", true);
    }

    @Override
    public boolean isNot_read_show() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("not_read_show", true);
    }

    @Override
    public boolean isEnable_show_recent_dialogs() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_recent_dialogs", true);
    }

    @Override
    public boolean is_side_navigation() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("is_side_navigation", false);
    }

    @Override
    public boolean is_side_no_stroke() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("is_side_no_stroke", false);
    }

    @Override
    public boolean is_notification_force_link() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("notification_force_link", false);
    }

    @Override
    public boolean isEnable_show_audio_top() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_audio_top", false);
    }

    @Override
    public boolean isUse_internal_downloader() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("use_internal_downloader", true);
    }

    @Override
    public boolean appStoredVersionEqual() {
        boolean ret = PreferenceManager.getDefaultSharedPreferences(app).getInt("app_stored_versuion", 0) == BuildConfig.VERSION_CODE;
        if (!ret) {
            PreferenceManager.getDefaultSharedPreferences(app).edit().putInt("app_stored_versuion", BuildConfig.VERSION_CODE).apply();
        }
        return ret;
    }

    @Override
    public String getMusicDir() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("music_dir", null);
        if (Utils.isEmpty(ret) || !new File(ret).exists()) {
            ret = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString("music_dir", ret).apply();
        }
        return ret;
    }

    @Override
    public String getPhotoDir() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("photo_dir", null);
        if (Utils.isEmpty(ret) || !new File(ret).exists()) {
            ret = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Fenrir";
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString("photo_dir", ret).apply();
        }
        return ret;
    }

    @Override
    public String getVideoDir() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("video_dir", null);
        if (Utils.isEmpty(ret) || !new File(ret).exists()) {
            ret = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/Fenrir";
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString("video_dir", ret).apply();
        }
        return ret;
    }

    @Override
    public String getDocDir() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("docs_dir", null);
        if (Utils.isEmpty(ret) || !new File(ret).exists()) {
            ret = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Fenrir";
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString("docs_dir", ret).apply();
        }
        return ret;
    }

    @Override
    public String getStickerDir() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("sticker_dir", null);
        if (Utils.isEmpty(ret) || !new File(ret).exists()) {
            ret = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Fenrir_Stickers";
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString("sticker_dir", ret).apply();
        }
        return ret;
    }

    @Override
    public boolean isPhoto_to_user_dir() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("photo_to_user_dir", true);
    }

    @Override
    public boolean isDelete_cache_images() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("delete_cache_images", false);
    }

    @Override
    public boolean isDisabled_encryption() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_encryption", false);
    }

    @Override
    public boolean isDownload_photo_tap() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("download_photo_tap", true);
    }

    @Override
    public boolean isAudio_save_mode_button() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("audio_save_mode_button", true);
    }

    @Override
    public boolean isShow_mutual_count() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_mutual_count", false);
    }

    @Override
    public boolean isNot_friend_show() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("not_friend_show", false);
    }

    @Override
    public boolean isDo_zoom_photo() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("do_zoom_photo", true);
    }

    @Override
    public boolean isChange_upload_size() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("change_upload_size", false);
    }

    @Override
    public boolean isShow_photos_line() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_photos_line", true);
    }

    @Override
    public boolean isDo_auto_play_video() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("do_auto_play_video", false);
    }

    @Override
    public boolean isVideo_controller_to_decor() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("video_controller_to_decor", false);
    }

    @Override
    public boolean isVideo_swipes() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("video_swipes", true);
    }

    @Override
    public boolean isDisable_likes() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_likes", false);
    }

    @Override
    public void setDisable_likes(boolean disabled) {
        PreferenceManager.getDefaultSharedPreferences(app).edit().putBoolean("disable_likes", disabled).apply();
    }

    @Override
    public boolean isDisable_notifications() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_notifications", false);
    }

    @Override
    public void setDisable_notifications(boolean disabled) {
        PreferenceManager.getDefaultSharedPreferences(app).edit().putBoolean("disable_notifications", disabled).apply();
    }

    @Override
    public boolean isNative_parcel() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("native_parcel_enable", false);
    }

    @Override
    public boolean isExtra_debug() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("extra_debug", false);
    }

    @Override
    public boolean isHint_stickers() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("hint_stickers", true);
    }

    @Override
    public boolean isEnable_native() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("enable_native", true);
    }

    @Override
    public boolean isEnable_cache_ui_anim() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("enable_cache_ui_anim", false);
    }

    @Override
    public boolean isRecording_to_opus() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("recording_to_opus", false);
    }

    @Override
    public boolean isDisable_sensored_voice() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_sensored_voice", false);
    }

    @Override
    public boolean isInvertPhotoRev() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("invert_photo_rev", false);
    }

    @Override
    public void setInvertPhotoRev(boolean rev) {
        PreferenceManager.getDefaultSharedPreferences(app).edit().putBoolean("invert_photo_rev", rev).apply();
    }

    @Override
    public @NonNull
    LocalServerSettings getLocalServer() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("local_media_server", null);
        if (ret == null) {
            return new LocalServerSettings();
        } else {
            return new Gson().fromJson(ret, LocalServerSettings.class);
        }
    }

    @Override
    public void setLocalServer(@NonNull LocalServerSettings settings) {
        PreferenceManager.getDefaultSharedPreferences(app).edit().putString("local_media_server", new Gson().toJson(settings)).apply();
    }

    @Override
    public void registerDonatesId(List<Integer> Ids) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        Set<String> uids = new HashSet<>(Ids.size());
        for (int i : Ids) {
            uids.add(String.valueOf(i));
        }
        preferences.edit().putStringSet(KEY_DONATE, uids).apply();
    }

    @NonNull
    @Override
    public List<Integer> getDonates() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        Set<String> uids = preferences.getStringSet(KEY_DONATE, new HashSet<>(0));

        List<Integer> ids = new ArrayList<>(uids.size());
        for (String stringuid : uids) {
            int uid = Integer.parseInt(stringuid);
            ids.add(uid);
        }

        return ids;
    }

    @Override
    public @NonNull
    PlayerCoverBackgroundSettings getPlayerCoverBackgroundSettings() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("player_background_json", null);
        if (ret == null) {
            return new PlayerCoverBackgroundSettings().set_default();
        } else {
            return new Gson().fromJson(ret, PlayerCoverBackgroundSettings.class);
        }
    }

    @Override
    public void setPlayerCoverBackgroundSettings(@NonNull PlayerCoverBackgroundSettings settings) {
        PreferenceManager.getDefaultSharedPreferences(app).edit().putString("player_background_json", new Gson().toJson(settings)).apply();
    }

    @Override
    public int getPaganSymbol() {
        try {
            return Integer.parseInt(java.util.Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(app).getString("pagan_symbol", "1")));
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    public @NonNull
    String getKateGMSToken() {
        String res = PreferenceManager.getDefaultSharedPreferences(app).getString("kate_gms_token", Constants.KATE_RECEIPT_GMS_TOKEN).trim();
        if (res.isEmpty()) {
            res = Constants.KATE_RECEIPT_GMS_TOKEN;
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString("kate_gms_token", Constants.KATE_RECEIPT_GMS_TOKEN).apply();
        }
        return res.replaceAll("[\\w%\\-]+:", ":");
    }

    @Lang
    @Override
    public int getLanguage() {
        return Integer.parseInt(java.util.Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(app).getString("language_ui", "0")));
    }

    @Override
    public int getEndListAnimation() {
        return Integer.parseInt(java.util.Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(app).getString("end_list_anim", "0")));
    }

    @Override
    public boolean isRunes_show() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("runes_show", true);
    }
}
