package org.oucho.musicplayer.utils;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.images.ArtworkCache;

public class Notification {

    public static final int NOTIFY_ID = 32;

    private static boolean sIsServiceForeground = false;

    private static boolean timer = false;

    public static void setState(boolean onOff){
        timer = onOff;
    }

    public static void updateNotification(@NonNull final PlayerService playerbackService) {

        if (!PlayerService.hasPlaylist()) {
            removeNotification(playerbackService);
            return; // no need to go further since there is nothing to display
        }

        PendingIntent togglePlayIntent = PendingIntent.getService(playerbackService, 0,
                new Intent(playerbackService, PlayerService.class).setAction(PlayerService.ACTION_TOGGLE), 0);

        PendingIntent nextIntent = PendingIntent.getService(playerbackService, 0,
                new Intent(playerbackService, PlayerService.class).setAction(PlayerService.ACTION_NEXT), 0);

        PendingIntent previousIntent = PendingIntent.getService(playerbackService, 0,
                new Intent(playerbackService, PlayerService.class).setAction(PlayerService.ACTION_PREVIOUS), 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(playerbackService);


        builder.setContentTitle(PlayerService.getSongTitle())
                .setContentText(PlayerService.getArtistName());

        int toggleResId = PlayerService.isPlaying() ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;

        builder.addAction(R.drawable.ic_fast_rewind_white_24dp, "", previousIntent)
                .addAction(toggleResId, "", togglePlayIntent)
                .addAction(R.drawable.ic_fast_forward_white_24dp, "", nextIntent)

                .setVisibility(android.app.Notification.VISIBILITY_PUBLIC);


        Intent intent = new Intent(playerbackService, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendInt = PendingIntent.getActivity(playerbackService, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendInt);

        if (!timer) {
            builder.setSmallIcon(R.drawable.ic_audiotrack_white_24dp);
        } else {
            builder.setSmallIcon(R.drawable.ic_timer2_white_24dp);
        }

        builder.setShowWhen(false);
        builder.setColor(ContextCompat.getColor(playerbackService, R.color.grey_900));


        Resources res = playerbackService.getResources();

         @SuppressLint("PrivateResource")
         int height = (int) res.getDimension(R.dimen.notification_large_icon_height);

        @SuppressLint("PrivateResource")
        final int width = (int) res.getDimension(R.dimen.notification_large_icon_width);

        ArtworkCache artworkCache = ArtworkCache.getInstance();
        Bitmap b = artworkCache.getCachedBitmap(PlayerService.getAlbumId(), width, height);
        if (b != null) {
            setBitmapAndBuild(b, playerbackService, builder);

        } else {
            ArtworkCache.getInstance().loadBitmap(PlayerService.getAlbumId(), width, height, bitmap -> setBitmapAndBuild(bitmap, playerbackService, builder));
        }
    }

    private static void setBitmapAndBuild(Bitmap bitmap, @NonNull PlayerService playerbackService, NotificationCompat.Builder builder) {

        Bitmap image = bitmap;

        if (image == null) {
            BitmapDrawable d = ((BitmapDrawable) ContextCompat.getDrawable(playerbackService, R.drawable.ic_audiotrack_white_24dp));
            image = d.getBitmap();
        }
        builder.setLargeIcon(image);


        builder.setStyle(new NotificationCompat.MediaStyle()
                .setMediaSession(PlayerService.getMediaSession().getSessionToken())
                .setShowActionsInCompactView(0, 1, 2));


        android.app.Notification notification = builder.build();

        boolean startForeground = PlayerService.isPlaying();
        if (startForeground) {
            playerbackService.startForeground(NOTIFY_ID, notification);
        } else {
            if (sIsServiceForeground) {
                playerbackService.stopForeground(false);
            }
            NotificationManager notificationManager = (NotificationManager) playerbackService.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFY_ID, notification);
        }

        sIsServiceForeground = startForeground;

    }


    private static void removeNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFY_ID);
    }
}