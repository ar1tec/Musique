package org.oucho.musicplayer.utils;

import android.content.Context;
import android.media.AudioManager;

import org.oucho.musicplayer.MainActivity;


public class GetAudioFocusTask implements Runnable {
	public MainActivity context;

	public GetAudioFocusTask(MainActivity context) {
		this.context = context;
	}

	public void run() {
		((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		MainActivity.stopTimer();
	}
}
