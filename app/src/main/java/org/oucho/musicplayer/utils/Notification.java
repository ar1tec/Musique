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
import org.oucho.musicplayer.images.BitmapCache;

public class Notification {

    public static final int NOTIFY_ID = 32;

    private static boolean sIsServiceForeground = false;

    public static void updateNotification(@NonNull final PlayerService playbackService) {

        if (!playbackService.hasPlaylist()) {
            removeNotification(playbackService);
            return; // no need to go further since there is nothing to display
        }

        PendingIntent togglePlayIntent = PendingIntent.getService(playbackService, 0,
                new Intent(playbackService, PlayerService.class)
                        .setAction(PlayerService.ACTION_TOGGLE), 0);



        PendingIntent nextIntent = PendingIntent.getService(playbackService, 0, new Intent(playbackService, PlayerService.class)
                        .setAction(PlayerService.ACTION_NEXT), 0);

        PendingIntent previousIntent = PendingIntent.getService(playbackService, 0,
                new Intent(playbackService, PlayerService.class)
                        .setAction(PlayerService.ACTION_PREVIOUS), 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(playbackService);


        builder.setContentTitle(playbackService.getSongTitle())
                .setContentText(playbackService.getArtistName());

        int toggleResId = playbackService.isPlaying() ? R.drawable.notification_pause : R.drawable.notification_play;

        builder.addAction(R.drawable.notification_previous, "", previousIntent)
                .addAction(toggleResId, "", togglePlayIntent)
                .addAction(R.drawable.notification_next, "", nextIntent)

                .setVisibility(android.app.Notification.VISIBILITY_PUBLIC);


        Intent intent = new Intent(playbackService, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendInt = PendingIntent.getActivity(playbackService, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.ic_stat_note)
                .setShowWhen(false)
                .setColor(ContextCompat.getColor(playbackService, R.color.controls_bg_dark));


        Resources res = playbackService.getResources();

         @SuppressLint("PrivateResource")
         int height = (int) res.getDimension(R.dimen.notification_large_icon_height);

        @SuppressLint("PrivateResource")
        final int width = (int) res.getDimension(R.dimen.notification_large_icon_width);

        ArtworkCache artworkCache = ArtworkCache.getInstance();
        Bitmap b = artworkCache.getCachedBitmap(playbackService.getAlbumId(), width, height);
        if (b != null) {
            setBitmapAndBuild(b, playbackService, builder);

        } else {
            ArtworkCache.getInstance().loadBitmap(playbackService.getAlbumId(), width, height, new BitmapCache.Callback() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap) {
                    setBitmapAndBuild(bitmap, playbackService, builder);
                }
            });
        }
    }

    private static void setBitmapAndBuild(Bitmap bitmap, @NonNull PlayerService playbackService, NotificationCompat.Builder builder) {

        Bitmap image = bitmap;

        if (image == null) {
            BitmapDrawable d = ((BitmapDrawable) ContextCompat.getDrawable(playbackService, R.drawable.ic_stat_note));
            image = d.getBitmap();
        }
        builder.setLargeIcon(image);


        builder.setStyle(new NotificationCompat.MediaStyle()
                .setMediaSession(playbackService.getMediaSession().getSessionToken())
                .setShowActionsInCompactView(0, 1, 2));


        android.app.Notification notification = builder.build();

        boolean startForeground = playbackService.isPlaying();
        if (startForeground) {
            playbackService.startForeground(NOTIFY_ID, notification);
        } else {
            if (sIsServiceForeground) {
                playbackService.stopForeground(false);
            }
            NotificationManager notificationManager = (NotificationManager) playbackService.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFY_ID, notification);
        }

        sIsServiceForeground = startForeground;

    }


    private static void removeNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFY_ID);
    }
}