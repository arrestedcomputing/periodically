package com.aubray.periodically.notifier;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.aubray.periodically.R;
import com.aubray.periodically.activities.InvitationsActivity;
import com.aubray.periodically.model.Invitation;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;
import com.aubray.periodically.util.Callback;
import com.google.common.base.Optional;

import java.util.List;

/**
 * An {@link IntentService} for notifications
 */
public class InvitationNotificationService extends IntentService {
    public static final String TAG = "periodically_invite";

    public InvitationNotificationService() {
        super("InvitationNotificationService");
    }

    CloudStore cloudStore;
    LocalStore localStore = new PreferencesLocalStore(this);
    NotificationManager notificationManager;

    @Override
    protected void onHandleIntent(final Intent intent) {
        cloudStore = new FirebaseCloudStore(this);
        notificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final Optional<User> user = localStore.getUser();
        if (!user.isPresent()) {
            return;
        }

        cloudStore.lookupInvitations(user.get(), new Callback<List<Invitation>>() {
            @Override
            public void receive(List<Invitation> invitations) {
                // todo cancel notifications for pids not in list

                for (final Invitation invitation : invitations) {
                    cloudStore.lookUpPeriodical(invitation.getPeriodicalId(), new Callback<Periodical>() {
                        @Override
                        public void receive(Periodical p) {
                            notifyInvited(p);
                        }
                    });
                }
            }
        });
    }

    private void notifyInvited(Periodical periodical) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("You've been invited to " + periodical.getName());

        Intent resultIntent = new Intent(this, InvitationsActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(InvitationsActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        notificationManager.notify(TAG, periodical.getId().hashCode(), mBuilder.build());
    }
}
