package dev.ragnarok.fenrir.push.message;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.List;

import dev.ragnarok.fenrir.Extra;
import dev.ragnarok.fenrir.R;
import dev.ragnarok.fenrir.activity.MainActivity;
import dev.ragnarok.fenrir.domain.IOwnersRepository;
import dev.ragnarok.fenrir.domain.Repository;
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels;
import dev.ragnarok.fenrir.longpoll.NotificationHelper;
import dev.ragnarok.fenrir.model.Owner;
import dev.ragnarok.fenrir.place.PlaceFactory;
import dev.ragnarok.fenrir.push.NotificationScheduler;
import dev.ragnarok.fenrir.push.NotificationUtils;
import dev.ragnarok.fenrir.settings.Settings;
import dev.ragnarok.fenrir.util.Objects;
import dev.ragnarok.fenrir.util.Utils;

import static dev.ragnarok.fenrir.push.NotificationUtils.configOtherPushNotification;
import static dev.ragnarok.fenrir.util.Utils.isEmpty;

public class BirthdayFCMMessage {

    //Bundle[{type=birthday, uids=20924995, _genSrv=605120, sandbox=0, collapse_key=birthday}]

    //key: google.sent_time, value: 1481879102336, class: class java.lang.Long
    //key: type, value: birthday, class: class java.lang.String
    //key: uids, value: 61354506,8056682, class: class java.lang.String
    //key: google.message_id, value: 0:1481879102338566%8c76e97a3fbd627d, class: class java.lang.String
    //key: no_sound, value: 1, class: class java.lang.String
    //key: _genSrv, value: 605119, class: class java.lang.String
    //key: sandbox, value: 0, class: class java.lang.String
    //key: collapse_key, value: birthday, class: class java.lang.String

    private ArrayList<Integer> uids;

    public static BirthdayFCMMessage fromRemoteMessage(@NonNull RemoteMessage remote) {
        BirthdayFCMMessage message = new BirthdayFCMMessage();

        String uidsString = remote.getData().get("uids");
        String[] uidsStringArray = TextUtils.isEmpty(uidsString) ? null : uidsString.split(",");

        if (uidsStringArray != null) {
            message.uids = new ArrayList<>(uidsStringArray.length);
            for (String anUidsStringArray : uidsStringArray) {
                try {
                    message.uids.add(Integer.parseInt(anUidsStringArray));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return message;
    }

    public void notify(Context context, int accountId) {
        if (isEmpty(uids)) {
            return;
        }

        if (!Settings.get()
                .notifications()
                .isBirtdayNotifEnabled()) {
            return;
        }

        Repository.INSTANCE.getOwners()
                .findBaseOwnersDataAsList(accountId, uids, IOwnersRepository.MODE_ANY)
                .subscribeOn(NotificationScheduler.INSTANCE)
                .subscribe(owners -> onOwnersDataReceived(context, accountId, owners), t -> {
                });
    }

    private void onOwnersDataReceived(Context context, int accountId, List<Owner> owners) {
        for (Owner owner : owners) {
            NotificationUtils.loadRoundedImageRx(context, owner.get100photoOrSmaller(), R.drawable.ic_avatar_unknown)
                    .subscribeOn(NotificationScheduler.INSTANCE)
                    .subscribe(bitmap -> onUsersDataReceived(context, accountId, owner, bitmap), t -> {
                    });
        }
    }

    private void onUsersDataReceived(Context context, int accountId, Owner owner, Bitmap bitmap) {
        int ownerId = owner.getOwnerId();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Objects.isNull(manager)) {
            return;
        }

        if (Utils.hasOreo()) {
            manager.createNotificationChannel(AppNotificationChannels.getBirthdaysChannel(context));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AppNotificationChannels.BIRTHDAYS_CHANNEL_ID)
                .setSmallIcon(R.drawable.cake)
                .setLargeIcon(bitmap)
                .setContentTitle(context.getString(R.string.birthday))
                .setContentText(owner.getFullName())
                .setAutoCancel(true);

        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(Extra.PLACE, PlaceFactory.getOwnerWallPlace(accountId, ownerId, owner));
        intent.setAction(MainActivity.ACTION_OPEN_PLACE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(context, ownerId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();

        configOtherPushNotification(notification);
        manager.notify(String.valueOf(ownerId), NotificationHelper.NOTIFICATION_BIRTHDAY, notification);
    }
}
