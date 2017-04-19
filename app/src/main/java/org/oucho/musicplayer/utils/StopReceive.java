package org.oucho.musicplayer.utils;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class StopReceive  extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        String etat = intent.getAction();
        String halt = intent.getStringExtra("halt");

        if ("org.oucho.musicplayer.STOP".equals(etat) && "stop".equals(halt)) {

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {

                @SuppressLint("SetTextI18n")
                public void run() {
                    NotificationManager notificationManager;
                    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(Notification.NOTIFY_ID);

                }
            }, 500);
        }
    }
}
