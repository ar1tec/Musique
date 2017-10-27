package org.oucho.musicplayer.fragments.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import org.oucho.musicplayer.MusiqueApplication;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.utils.Permissions;

import java.util.ArrayList;
import java.util.List;


public class PlaylistLoader extends BaseLoader<List<Song>> {
    private static final String[] sProjection = {
            MediaStore.Audio.Playlists.Members.AUDIO_ID,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA};

    private static final String[] genresProjection = {
            MediaStore.Audio.Genres.NAME,
            MediaStore.Audio.Genres._ID
    };

    private final long mPlaylistId;

    public PlaylistLoader(Context context, long playlistId) {
        super(context);
        mPlaylistId = playlistId;
    }

    @Override
    public List<Song> loadInBackground() {

        List<Song> playlist = new ArrayList<>();

        Cursor cursor = getPlaylistCursor();

        Cursor genresCursor;

        if (cursor != null && cursor.moveToFirst()) {
            int idCol = cursor
                    .getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
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
            int pathCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);


            do {
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);

                String artist = cursor.getString(artistCol);

                String album = cursor.getString(albumCol);

                long albumId = cursor.getLong(albumIdCol);

                int track = cursor.getInt(trackCol);

                int duration = cursor.getInt(trackDur);

                int year = cursor.getInt(yearCol);

                String path = cursor.getString(pathCol);


                // genre
                int musicId = Integer.parseInt(cursor.getString(idCol));

                Uri uri;
                String genre = "unknow";
                int genreCol;

                try {
                    uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", musicId);
                    genresCursor = MusiqueApplication.getInstance().getContentResolver().query(uri, genresProjection, null, null, null);

                    if (genresCursor != null) {
                        genreCol = genresCursor.getColumnIndex(MediaStore.Audio.Genres.NAME);

                        if (genresCursor.moveToFirst()) {
                            do {
                                genre = genresCursor.getString(genreCol);
                            } while (genresCursor.moveToNext());
                        }
                    }


                    if (genresCursor != null)
                        genresCursor.close();

                } catch (Exception ignore) {}


                playlist.add(new Song(id, title, artist, album, albumId, track, 0, duration, year, null, path));

            } while (cursor.moveToNext());

        }

        if (cursor != null)
            cursor.close();



        return playlist;
    }

    private Cursor getPlaylistCursor() {
        if (!Permissions.checkPermission(getContext())) {
            return null;
        }
        Uri musicUri = MediaStore.Audio.Playlists.Members.getContentUri( "external", mPlaylistId);

        return getContext().getContentResolver().query(musicUri,
                sProjection, getSelectionString(), getSelectionArgs(),
                MediaStore.Audio.Playlists.Members.PLAY_ORDER);
    }
}
