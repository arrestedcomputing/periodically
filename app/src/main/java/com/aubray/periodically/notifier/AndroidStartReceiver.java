package com.aubray.periodically.notifier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.aubray.periodically.activities.PeriodicalsActivity;

public class AndroidStartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        startNotifier(context, PeriodicalNotificationService.class);
        startNotifier(context, InvitationNotificationService.class);
    }

    public static void startNotifier(Context context, Class<?> serviceClass) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, serviceClass);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        am.cancel(pi);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60*1000,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);

        // Also, immediately update notifications
        try {
            pi.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public static void start(Context context) {
        new AndroidStartReceiver().onReceive(context, null);
    }
}