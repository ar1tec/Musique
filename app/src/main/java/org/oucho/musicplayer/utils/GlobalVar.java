package org.oucho.musicplayer.utils;


import android.app.Application;


public class GlobalVar extends Application {


    private static long currentSongID;
    private static String currentSongTitle;
    private static String currentSongArtist;


    public static long getCurrentSongID() {
        return currentSongID;
    }

    public void setCurrentSongID(long id) {
        currentSongID = id;
    }


    public static String getCurrentSongTitle() {
        return currentSongTitle;
    }

    public void setCurrentSongTitle(String id) {
        currentSongTitle = id;
    }


    public static String getCurrentSongArtist() {
        return currentSongArtist;
    }

    public void setCurrentSongArtist(String id) {
        currentSongArtist = id;
    }

}
