package org.oucho.musicplayer.fragments.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.database.DatabaseUtilsCompat;
import android.util.Log;

import org.oucho.musicplayer.MusiqueApplication;
import org.oucho.musicplayer.db.model.Song;

import java.util.ArrayList;
import java.util.List;


public class SongLoader extends BaseLoader<List<Song>> {

    private static final String TAG_LOG = "SongLoader";

    private static final String[] sProjection = {MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.DATA};

    public SongLoader(Context context) {
        super(context);

    }

    @Override
    public List<Song> loadInBackground() {
        List<Song> mSongList = new ArrayList<>();

        Cursor cursor = getSongCursor();

        if (cursor != null && cursor.moveToFirst()) {
            int idCol = cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
            if (idCol == -1) {
                idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            }

            int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);

            int trackDur = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

            int yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR);

            int mimeCol  = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);
            int pathCol  = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);


            do {

                Log.i(TAG_LOG, "do");

                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);

                String artist = cursor.getString(artistCol);

                String album = cursor.getString(albumCol);

                long albumId = cursor.getLong(albumIdCol);

                int track = cursor.getInt(trackCol);

                int duration = cursor.getInt(trackDur);

                int year = cursor.getInt(yearCol);

                String mimeType = cursor.getString(mimeCol);
                String path = cursor.getString(pathCol);


                // ne pas integrer recher genre, trop long à charger
                mSongList.add(new Song(id, title, artist, album, albumId, track, duration, year, "unknow", mimeType, path));
            } while (cursor.moveToNext());

        }

        if (cursor != null)
            cursor.close();

        return mSongList;
    }


    private Cursor getSongCursor() {

        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String selection = getSelectionString();
        String[] selectionArgs = getSelectionArgs();

        selection = DatabaseUtilsCompat.concatenateWhere(selection, MediaStore.Audio.Media.IS_MUSIC+" = 1");

        String fieldName = MediaStore.Audio.Media.TITLE;
        String filter = getFilter();
        return getCursor(musicUri, sProjection, selection, selectionArgs, fieldName, filter);
    }

}