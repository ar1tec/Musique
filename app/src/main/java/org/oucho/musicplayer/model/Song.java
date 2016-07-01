package org.oucho.musicplayer.model;

import android.provider.MediaStore;

public class Song {
    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final int trackNumber;
    private final long albumId;
    private String genre;
    private final int duration;

    public Song(long id, String title, String artist, String album, long albumId, int trackNumber, int duration) {
        super();
        this.id = id;
        this.title = title == null ? MediaStore.UNKNOWN_STRING : title;
        this.artist = artist == null ? MediaStore.UNKNOWN_STRING : artist;
        this.album = album == null ? MediaStore.UNKNOWN_STRING : album;
        this.albumId = albumId;
        this.trackNumber = trackNumber;
        this.duration = duration;

    }

    public long getId() {
        return id;
    }

    public String getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public long getAlbumId() {
        return albumId;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public String getGenre() {
        return genre;
    }

    public int getDuration() {
        return duration;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }
}
