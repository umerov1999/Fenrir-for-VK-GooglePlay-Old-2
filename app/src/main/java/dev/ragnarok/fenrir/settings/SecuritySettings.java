package dev.ragnarok.fenrir.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.ragnarok.fenrir.crypt.KeyLocationPolicy;
import dev.ragnarok.fenrir.util.AeSimpleSHA1;
import dev.ragnarok.fenrir.util.Objects;
import dev.ragnarok.fenrir.util.Utils;

public class SecuritySettings implements ISettings.ISecuritySettings {

    public static final String KEY_USE_PIN_FOR_SECURITY = "use_pin_for_security";
    public static final String KEY_CHANGE_PIN = "change_pin";
    public static final String KEY_DELETE_KEYS = "delete_all_encryption_keys";
    private static final String PREFS_NAME = "security_prefs";
    private static final String KEY_PIN_HASH = "app_pin";
    private static final String KEY_PIN_ENTER_HISTORY = "pin_enter_history";
    private static final String KEY_USE_PIN_FOR_ENTRANCE = "use_pin_for_entrance";
    private static final String KEY_ENCRYPTION_POLICY_ACCEPTED = "encryption_policy_accepted";
    private static final int PIN_HISTORY_DEPTH = 3;
    private final SharedPreferences mPrefs;
    private final Context mApplication;
    private final List<Long> mPinEnterHistory;
    private String mPinHash;
    private boolean mKeyEncryptionPolicyAccepted;
    private boolean isShowHiddenDialogs;

    SecuritySettings(Context context) {
        mApplication = context.getApplicationContext();
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mPinHash = mPrefs.getString(KEY_PIN_HASH, null);
        mPinEnterHistory = extractPinEnterHistrory(mPrefs);
        mKeyEncryptionPolicyAccepted = mPrefs.getBoolean(KEY_ENCRYPTION_POLICY_ACCEPTED, false);
    }

    @NonNull
    private static ArrayList<Long> extractPinEnterHistrory(@NonNull SharedPreferences preferences) {
        Set<String> set = preferences.getStringSet(KEY_PIN_ENTER_HISTORY, null);
        ArrayList<Long> result = new ArrayList<>(Utils.safeCountOf(set));
        if (set != null) {
            int index = 0;
            for (String value : set) {
                result.add(index, Long.parseLong(value));
                index++;
            }
        }

        Collections.sort(result);
        return result;
    }

    private static String encryptionKeyFor(int accountId, int peerId) {
        return "encryptionkeypolicy" + accountId + "_" + peerId;
    }

    private static String calculateHash(String value) {
        try {
            return AeSimpleSHA1.sha1(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean getShowHiddenDialogs() {
        return isShowHiddenDialogs;
    }

    @Override
    public void setShowHiddenDialogs(boolean showHiddenDialogs) {
        isShowHiddenDialogs = showHiddenDialogs;
    }

    @Override
    public boolean IsShow_hidden_accounts() {
        return PreferenceManager.getDefaultSharedPreferences(mApplication)
                .getBoolean("show_hidden_accounts", true);
    }

    private String getPinHash() {
        return mPinHash;
    }

    private void setPinHash(String pinHash) {
        mPinHash = pinHash;
        if (Objects.isNull(pinHash)) {
            mPrefs.edit().remove(KEY_PIN_HASH).apply();
        } else {
            mPrefs.edit().putString(KEY_PIN_HASH, pinHash).apply();
        }
    }

    @NonNull
    public List<Long> getPinEnterHistory() {
        return mPinEnterHistory;
    }

    private void storePinHistory() {
        Set<String> target = new HashSet<>(mPinEnterHistory.size());
        for (long value : mPinEnterHistory) {
            target.add(String.valueOf(value));
        }

        mPrefs.edit().putStringSet(KEY_PIN_ENTER_HISTORY, target).apply();
    }

    @Override
    public void clearPinHistory() {
        mPinEnterHistory.clear();
        mPrefs.edit().remove(KEY_PIN_ENTER_HISTORY).apply();
    }

    @Override
    public void firePinAttemptNow() {
        long now = System.currentTimeMillis();
        mPinEnterHistory.add(now);
        if (mPinEnterHistory.size() > PIN_HISTORY_DEPTH) {
            mPinEnterHistory.remove(0);
        }

        storePinHistory();
    }

    @Override
    public void enableMessageEncryption(int accountId, int peerId, @KeyLocationPolicy int policy) {
        mPrefs.edit()
                .putInt(encryptionKeyFor(accountId, peerId), policy)
                .apply();
    }

    @Override
    public boolean isMessageEncryptionEnabled(int accountId, int peerId) {
        return mPrefs.contains(encryptionKeyFor(accountId, peerId));
    }

    @Override
    public void disableMessageEncryption(int accountId, int peerId) {
        mPrefs.edit()
                .remove(encryptionKeyFor(accountId, peerId))
                .apply();
    }

    @Override
    @KeyLocationPolicy
    public int getEncryptionLocationPolicy(int accountId, int peerId) {
        @KeyLocationPolicy
        int result = mPrefs.getInt(encryptionKeyFor(accountId, peerId), KeyLocationPolicy.PERSIST);
        return result;
    }

    @Override
    public boolean hasPinHash() {
        return !TextUtils.isEmpty(mPinHash);
    }

    @Override
    public int getPinHistoryDepth() {
        return PIN_HISTORY_DEPTH;
    }

    @Override
    public boolean needHideMessagesBodyForNotif() {
        return PreferenceManager.getDefaultSharedPreferences(mApplication)
                .getBoolean("hide_notif_message_body", false);
    }

    @Override
    public boolean isUsePinForSecurity() {
        return hasPinHash() && PreferenceManager.getDefaultSharedPreferences(mApplication)
                .getBoolean(KEY_USE_PIN_FOR_SECURITY, false);
    }

    @Override
    public boolean isEntranceByFingerprintAllowed() {
        return PreferenceManager.getDefaultSharedPreferences(mApplication).getBoolean("allow_fingerprint", false);
    }

    @Override
    public boolean isUsePinForEntrance() {
        return hasPinHash() && PreferenceManager.getDefaultSharedPreferences(mApplication)
                .getBoolean(KEY_USE_PIN_FOR_ENTRANCE, false);
    }

    @Override
    public void setPin(@Nullable int[] pin) {
        setPinHash(Objects.isNull(pin) ? null : calculatePinHash(pin));
    }

    private String calculatePinHash(@NonNull int[] values) {
        StringBuilder builder = new StringBuilder();
        for (int value : values) {
            builder.append(value);
        }

        return calculateHash(builder.toString());
    }

    @Override
    public boolean isPinValid(@NonNull int[] values) {
        String hash = calculatePinHash(values);
        return hash.equals(getPinHash());
    }

    @Override
    public boolean isKeyEncryptionPolicyAccepted() {
        return mKeyEncryptionPolicyAccepted;
    }

    @Override
    public void setKeyEncryptionPolicyAccepted(boolean accepted) {
        mKeyEncryptionPolicyAccepted = accepted;
        mPrefs.edit()
                .putBoolean(KEY_ENCRYPTION_POLICY_ACCEPTED, accepted)
                .apply();
    }

    private boolean saveSet(Set<Integer> array, String arrayName) {
        SharedPreferences prefs = mApplication.getSharedPreferences("security_other", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(arrayName + "_size", array.size());
        int s = 0;
        for (Integer i : array) {
            editor.putInt(arrayName + "_" + s, i);
            s++;
        }
        return editor.commit();
    }

    @Override
    public boolean AddValueToSet(int value, String arrayName) {
        Set<Integer> itr = loadSet(arrayName);
        itr.add(value);
        return saveSet(itr, arrayName);
    }

    @Override
    public boolean ContainsValueInSet(int value, String arrayName) {
        Set<Integer> itr = loadSet(arrayName);
        return itr.contains(value);
    }

    @Override
    public boolean ContainsValuesInSet(int[] values, String arrayName) {
        Set<Integer> itr = loadSet(arrayName);
        for (Integer i : values) {
            if (!itr.contains(i))
                return false;
        }
        return true;
    }

    @Override
    public boolean RemoveValueFromSet(int value, String arrayName) {
        Set<Integer> itr = loadSet(arrayName);
        itr.remove(value);
        return saveSet(itr, arrayName);
    }

    @Override
    public int getSetSize(String arrayName) {
        SharedPreferences prefs = mApplication.getSharedPreferences("security_other", 0);
        return prefs.getInt(arrayName + "_size", 0);
    }

    @Override
    public Set<Integer> loadSet(String arrayName) {
        SharedPreferences prefs = mApplication.getSharedPreferences("security_other", 0);
        int size = prefs.getInt(arrayName + "_size", 0);
        Set<Integer> array = new HashSet<>(size);
        for (int i = 0; i < size; i++)
            array.add(prefs.getInt(arrayName + "_" + i, 0));
        return array;
    }
}
