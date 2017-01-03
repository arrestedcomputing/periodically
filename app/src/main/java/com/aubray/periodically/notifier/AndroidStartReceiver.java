package com.aubray.periodically.notifier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class AndroidStartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        startNotifier(context);
    }

    public static void startNotifier(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, PeriodicalNotificationService.class);
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
}