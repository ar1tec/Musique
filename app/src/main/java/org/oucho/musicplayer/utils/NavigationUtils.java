package org.oucho.musicplayer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.activities.EqualizerActivity;
import org.oucho.musicplayer.activities.PlayerActivity;
import org.oucho.musicplayer.activities.SearchActivity;


public class NavigationUtils {
    public static void showSearchActivity(Activity activity) {
        Intent i = new Intent(activity, SearchActivity.class);
        activity.startActivityForResult(i, MainActivity.SEARCH_ACTIVITY);
    }

    public static void showEqualizer(Context context) {
        Intent i = new Intent(context, EqualizerActivity.class);
        context.startActivity(i);
    }

    public static void showMainActivity(Activity activity) {
        Intent i = new Intent(activity, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(i);
        activity.overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom);
    }

    public static void showPlaybackActivity(Activity activity) {
        Intent i = new Intent(activity, PlayerActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(i);
        activity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
    }
}
