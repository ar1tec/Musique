package org.oucho.musicplayer.db.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

public class Album implements Parcelable {

    private long id;
    private String albumName;
    private String artistName;
    private int year;
    private int trackCount;

    public Album() { }

    public Album(long id, String albumName, String artistName, int year, int trackCount) {
        super();
        this.id = id;
        this.albumName = albumName == null ? MediaStore.UNKNOWN_STRING : albumName;
        this.artistName = artistName == null ? MediaStore.UNKNOWN_STRING : artistName;
        this.year = year;
        this.trackCount = trackCount;
    }

    public long getId() {
        return id;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getArtistName() {
        return artistName;
    }

    public int getYear() {
        return year;
    }

    public int getTrackCount() {
        return trackCount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.albumName);
        dest.writeString(this.artistName);
        dest.writeInt(this.year);
        dest.writeInt(this.trackCount);
    }

    public Album(Parcel in) {
        this.id = in.readLong();
        this.albumName = in.readString();
        this.artistName = in.readString();
        this.year = in.readInt();
        this.trackCount = in.readInt();
    }

    public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<Album>() {
        @Override
        public Album createFromParcel(Parcel source) {
            return new Album(source);
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };
}
