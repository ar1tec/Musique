package org.oucho.musicplayer.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.activities.EqualizerActivity;
import org.oucho.musicplayer.activities.SearchActivity;

public class NavigationUtils {

    private static final String TAG_LOG = "NavigationUtils";

    public static void showSearchActivity(Activity activity) {

        Log.i(TAG_LOG, "showSearchActivity");

        Intent i = new Intent(activity, SearchActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivityForResult(i, MainActivity.SEARCH_ACTIVITY);
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_in);
    }

    public static void showEqualizer(Activity activity) {

        Log.i(TAG_LOG, "showSearchActivity");

        Intent i = new Intent(activity, EqualizerActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(i);
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_in);
    }

    public static void showMainActivity(Activity activity) {

        Log.i(TAG_LOG, "showSearchActivity");

        Intent i = new Intent(activity, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(i);
        activity.overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom);
    }
}
