package com.aubray.periodically.notifier;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.aubray.periodically.R;
import com.aubray.periodically.activities.PeriodicalsActivity;
import com.aubray.periodically.logic.Periodicals;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.Subscription;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;
import com.aubray.periodically.util.Callback;
import com.google.common.base.Optional;

import org.joda.time.Instant;

import java.util.List;

import static com.aubray.periodically.ui.PeriodicalFormatter.printDueDate;

/**
 * An {@link IntentService} for notifications
 */
public class PeriodicalNotificationService extends IntentService {
    public static final String TAG = "periodically";

    public PeriodicalNotificationService() {
        super("PeriodicalNotificationService");
    }

    CloudStore cloudStore;
    LocalStore localStore = new PreferencesLocalStore(this);
    NotificationManager notificationManager;

    @Override
    protected void onHandleIntent(Intent intent) {
        cloudStore = new FirebaseCloudStore(this);
        notificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final Optional<User> user = localStore.getUser();
        if (!user.isPresent()) {
            return;
        }

        cloudStore.lookUpPeriodicals(user.get(), new Callback<List<String>>() {
            @Override
            public void receive(List<String> pids) {
                // todo cancel notifications for pids not in list

                for (String pid : pids) {
                    cloudStore.lookUpPeriodical(pid, new Callback<Periodical>() {
                        @Override
                        public void receive(Periodical p) {
                            Optional<Subscription> sub = p.getSubscriptionFor(user.get().getUid());
                            if (sub.isPresent() && !sub.get().isMuted() &&
                                    Periodicals.isDue(p, Instant.now())) {
                                notifyDue(p);
                            } else {
                                cancelNotifications(p);
                            }
                        }
                    });
                }
            }
        });
    }

    private void cancelNotifications(Periodical periodical) {
        notificationManager.cancel(TAG, periodical.getId().hashCode());
    }

    private void notifyDue(Periodical periodical) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(periodical.getName() + " is due!")
                        .setContentText("Came due: " + printDueDate(periodical));

        Intent resultIntent = new Intent(this, PeriodicalsActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(PeriodicalsActivity.class);
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
