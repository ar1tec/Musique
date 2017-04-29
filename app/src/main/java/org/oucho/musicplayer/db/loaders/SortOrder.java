package org.oucho.musicplayer.db.loaders;

import android.provider.MediaStore;


public final class SortOrder {

    // DESC = ordre inverse

    public interface ArtistSortOrder {
        String ARTIST_A_Z = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER;

        String ARTIST_NUMBER_OF_SONGS = MediaStore.Audio.Artists.NUMBER_OF_TRACKS + " DESC";

        String ARTIST_NUMBER_OF_ALBUMS = MediaStore.Audio.Artists.NUMBER_OF_ALBUMS + " DESC";
    }


    public interface AlbumSortOrder {

        //titre
        String ALBUM_A_Z = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;

        //String ALBUM_ARTIST = MediaStore.Audio.Albums.ARTIST;
        //Ne pas prendre en compte "The" lors du tri des albums par artiste
        String ALBUM_ARTIST = "REPLACE ('<BEGIN>' || " + MediaStore.Audio.Albums.ARTIST +", '<BEGIN>The ', '<BEGIN>')";

        String ALBUM_YEAR = MediaStore.Audio.Albums.FIRST_YEAR + " DESC";

    }


    public interface SongSortOrder {
        // titre
        String SONG_A_Z = MediaStore.Audio.Media.TITLE;

        String SONG_ARTIST = "REPLACE ('<BEGIN>' || " + MediaStore.Audio.Media.ARTIST +", '<BEGIN>The ', '<BEGIN>')";

        //String SONG_ARTIST = MediaStore.Audio.Media.ARTIST;

        String SONG_ALBUM = MediaStore.Audio.Media.ALBUM;

        String SONG_YEAR = MediaStore.Audio.Media.YEAR + " DESC";

    }

}
