package org.oucho.musicplayer;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;


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
    }

    public static synchronized MusiqueApplication getInstance() {
        return sInstance;
    }

    private static void setInstance(MusiqueApplication value) {
        sInstance = value;
    }
}
