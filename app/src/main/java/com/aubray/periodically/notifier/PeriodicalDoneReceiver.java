package com.aubray.periodically.notifier;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;
import com.aubray.periodically.util.Callback;
import com.google.common.base.Optional;

/**
 * Marks a periodical as done.
 */
public class PeriodicalDoneReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        final CloudStore cloudStore = new FirebaseCloudStore(context);
        final LocalStore localStore = new PreferencesLocalStore(context);

        String periodicalId = intent.getStringExtra("PERIODICAL_ID");

        cloudStore.lookUpPeriodical(periodicalId, new Callback<Periodical>() {
            @Override
            public void receive(Periodical periodical) {
                markPeriodicalDone(context, periodical, cloudStore, localStore.getUser());
            }
        });
    }

    public void markPeriodicalDone(Context context, Periodical periodical, CloudStore cloudStore, Optional<User> user) {
        if (user.isPresent()) {
            periodical.didIt(user.get(), System.currentTimeMillis());
            cloudStore.savePeriodical(periodical);

            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.cancel(PeriodicalNotificationService.TAG, periodical.getId().hashCode());

            Toast.makeText(context, "completed " + periodical.getName(),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "not logged in",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
