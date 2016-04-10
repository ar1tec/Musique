package org.oucho.musicplayer.loaders;

import android.content.Context;

public class GenreSongLoader extends SongLoader {
    private final long mGenreId;

    public GenreSongLoader(Context context, long genreId) {
        super(context);
        mGenreId = genreId;
    }

/*    @Override
    public  Uri getContentUri() {
        return MediaStore.Audio.Genres.Members.getContentUri(
                "external", mGenreId);
    }*/
}
