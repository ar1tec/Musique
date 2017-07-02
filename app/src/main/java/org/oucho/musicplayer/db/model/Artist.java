package org.oucho.musicplayer.db.model;

import android.provider.MediaStore;

public class Artist {

    private final long id;
    private final String name;
    private final int albumCount;
    private final int trackCount;

    public Artist(long id, String name, int albumCount, int trackCount) {
        super();
        this.id = id;
        this.name = name == null ? MediaStore.UNKNOWN_STRING : name;
        this.albumCount = albumCount;
        this.trackCount = trackCount;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public int getAlbumCount() {
        return albumCount;
    }

    public int getTrackCount() {
        return trackCount;
    }

}
