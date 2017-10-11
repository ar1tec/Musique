package org.oucho.musicplayer;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.media.audiofx.BassBoost;
import android.text.TextPaint;
import android.view.Display;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.squareup.leakcanary.LeakCanary;

import org.oucho.musicplayer.equalizer.AudioEffects;
import org.oucho.musicplayer.utils.PrefUtils;


public class MusiqueApplication extends Application {

    private static MusiqueApplication sInstance;



    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        setInstance(this);

        PrefUtils.init(this);
        AudioEffects.init(this);
    }

    public static synchronized MusiqueApplication getInstance() {
        return sInstance;
    }

    private static void setInstance(MusiqueApplication value) {
        sInstance = value;
    }

 }
