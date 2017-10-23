package org.oucho.musicplayer.utils;

import android.content.Context;
import android.media.AudioManager;

import org.oucho.musicplayer.MainActivity;


public class GetAudioFocusTask implements Runnable {
    private final MainActivity mContext;

    public GetAudioFocusTask(MainActivity mContext) {
        this.mContext = mContext;
    }

    public void run() {
        ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        MainActivity.stopTimer();
    }
}
