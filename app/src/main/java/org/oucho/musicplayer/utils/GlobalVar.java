package org.oucho.musicplayer.utils;


import android.app.Application;


public class GlobalVar extends Application {


    private static long currentSongPlay;


    public static long getCurrentSongPlay() {
        return currentSongPlay;
    }

    public void setCurrentSongPlay(long id) {
        this.currentSongPlay = id;
    }


}
