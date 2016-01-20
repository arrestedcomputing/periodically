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

    @Override
    protected void onHandleIntent(Intent intent) {
        Optional<User> user = localStore.getUser();
        if (!user.isPresent()) {
            return;
        }

        cloudStore = new FirebaseCloudStore(this);

        cloudStore.addPeriodicalsListener(user.get(), new Callback<List<Periodical>>() {
            @Override
            public void receive(List<Periodical> periodicals) {
                // find due periodicals
                for (Periodical p : periodicals) {
                    if (Periodicals.isDue(p, Instant.now())) {
                        notifyDue(p);
                    }
                }
            }
        });
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
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(TAG, periodical.getId().hashCode(), mBuilder.build());
    }
}
