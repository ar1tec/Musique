package org.oucho.musicplayer.audiotag;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;

import org.oucho.musicplayer.model.Album;
import org.oucho.musicplayer.model.Song;

import java.util.HashMap;
import java.util.Map;


public class TagEdit {

    public static final String TITLE = "title";
    public static final String ARTIST = "artist";
    public static final String ALBUM = "album";
    public static final String TRACK = "track";
    public static final String GENRE = "genre";
    public static final String ALBUM_NAME = "album_name";
    public static final String ARTIST_NAME = "artist_name";
    public static final String YEAR = "year";

    public static final String COVER = "cover";

    public static final String TAG = "TagEdit";



    private static Map<Long, String> getGenres(Context context) {

        Log.d(TAG, "Map<Long, String> getGenres(Context context)");


        HashMap<Long, String> genreIdMap = new HashMap<>();

        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME},
                null, null, null);

        if (c != null && c.moveToFirst()) {
            do {
                genreIdMap.put(c.getLong(0), c.getString(1));
            } while (c.moveToNext());
        }


        if (c != null) {
            c.close();
        }
        return genreIdMap;
    }

    private static long getGenreId(Context context, String genreName) {

        Log.d(TAG, "getGenreId(Context context, String genreName)");


        long id = -1;
        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME},
                null, null, null);

        if (c != null && c.moveToFirst()) {
            do {
                if (genreName.equals(c.getString(1))) {
                    id = c.getLong(0);
                    break;
                }
            } while (c.moveToNext());
        }


        if (c != null) {
            c.close();
        }

        return id;
    }


    public static String getSongGenre(Context context, long songId) {

        Log.d(TAG, "getSongGenre(Context context, long songId)");

        Map<Long, String> genreIdMap = getGenres(context);

        String genre = "";

        for (Long genreId : genreIdMap.keySet()) {

            boolean found = false;

            Cursor c = context.getContentResolver().query(
                    MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                    new String[]{MediaStore.Audio.Genres.Members.AUDIO_ID},
                    MediaStore.Audio.Genres.Members.AUDIO_ID + " = " + songId,
                    null, null);


            if ((c != null ? c.getCount() : 0) != 0) {

                genre = genreIdMap.get(genreId);
                found = true;
            }

            assert c != null;
            c.close();

            if (found) {
                break;
            }
        }


        return genre;
    }


    private static long getSongGenreId(Context context, long songId) {

        Log.d(TAG, "getSongGenreId(Context context, long songId)");


        Map<Long, String> genreIdMap = getGenres(context);

        long genreId = -1;
        for (Long l : genreIdMap.keySet()) {
            Cursor c = context.getContentResolver().query(
                    MediaStore.Audio.Genres.Members.getContentUri(
                            "external", l),
                    new String[]{MediaStore.Audio.Genres.Members.AUDIO_ID},
                    MediaStore.Audio.Genres.Members.AUDIO_ID + " = " + songId,
                    null, null);


            if ((c != null ? c.getCount() : 0) != 0) {
                genreId = l;
            }

            assert c != null;
            c.close();
        }

        return genreId;
    }


    public static boolean editSongTags(Context context, Song song, Map<String, String> tags) {

        String newTitle = tags.get(TITLE) == null ? song.getTitle() : tags.get(TITLE);
        String newArtist = tags.get(ARTIST) == null ? song.getArtist() : tags.get(ARTIST);
        String newAlbum = tags.get(ALBUM) == null ? song.getAlbum() : tags.get(ALBUM);
        String newTrackNumber = tags.get(TRACK) == null ? String.valueOf(song.getTrackNumber()) : tags.get(TRACK);
        String newGenre = tags.get(GENRE) == null ? song.getGenre() : tags.get(GENRE);


        Log.d(TAG, ", titre: " + newTitle + ", artist: " + newArtist + ", album: " + newAlbum + ", track: " + newTrackNumber + ", genre: " + newGenre);


        ContentValues values = new ContentValues();

        if (!song.getTitle().equals(newTitle)) {

            values.put(MediaStore.Audio.Media.TITLE, newTitle);
        }


        if (!song.getTitle().equals(newTitle)) {

            values.put(MediaStore.Audio.Media.TITLE, newTitle);
        }

        if (!song.getArtist().equals(newArtist)) {

            values.put(MediaStore.Audio.Media.ARTIST, newArtist);
        }

        if (!song.getAlbum().equals(newAlbum)) {

            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{
                            BaseColumns._ID,
                            MediaStore.Audio.AlbumColumns.ALBUM,
                            MediaStore.Audio.AlbumColumns.ALBUM_KEY,
                            MediaStore.Audio.AlbumColumns.ARTIST
                    },
                    MediaStore.Audio.AlbumColumns.ALBUM + " = ?", new String[]{newAlbum}, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);


            if (cursor != null && cursor.moveToFirst()) {

                long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

                values.put(MediaStore.Audio.Media.ALBUM_ID, id);


            } else {

                values.put(MediaStore.Audio.Media.ALBUM, newAlbum);
            }


            if (cursor != null) {
                cursor.close();
            }

        }
        if (!String.valueOf(song.getTrackNumber()).equals(newTrackNumber)) {

            values.put(MediaStore.Audio.Media.TRACK, newTrackNumber);
        }

        if (!song.getGenre().equals(newGenre)) {
            editSongGenre(context, song, newGenre);
        }

        if (values.size() > 0) {

            context.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values, MediaStore.Audio.Media._ID + "=" + song.getId(), null);
        }

        return true;

    }


    private static void editSongGenre(Context context, Song song, String newGenre) {

        Log.d(TAG, "editSongGenre(Context context, Song song, String newGenre)");


        long genreId = getSongGenreId(context, song.getId());

        if (genreId != -1)//si la chanson se trouve dans une des tables Genres.Members on supprime l'entrée correspondante
        {
            context.getContentResolver().delete(MediaStore.Audio.Genres.Members.getContentUri(
                    "external", genreId), MediaStore.Audio.Genres.Members.AUDIO_ID + " = " + song.getId(), null);
        }


        genreId = getGenreId(context, newGenre);
        ContentValues values = new ContentValues();

        if (genreId != -1)//si le nouveau genre existe dans la bdd
        {
            values.put(MediaStore.Audio.Genres.Members.AUDIO_ID, song.getId());
            values.put(MediaStore.Audio.Genres.Members.GENRE_ID, genreId);

            context.getContentResolver().insert(MediaStore.Audio.Genres.Members.getContentUri(
                    "external", genreId), values);
        } else {
            values.put(MediaStore.Audio.Genres.NAME, newGenre);
            context.getContentResolver().insert(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, values);
            genreId = getGenreId(context, newGenre);

            if (genreId != -1) {
                values.clear();
                values.put(MediaStore.Audio.Genres.Members.AUDIO_ID, song.getId());
                values.put(MediaStore.Audio.Genres.Members.GENRE_ID, genreId);

                context.getContentResolver().insert(MediaStore.Audio.Genres.Members.getContentUri(
                        "external", genreId), values);
            }

        }
    }

    public static boolean editAlbumData(Context context, Album mAlbum, HashMap<String, String> data) {

        Log.d(TAG, "editAlbumData(Context context, Album mAlbum, HashMap<String, String> data)");


        ContentValues values = new ContentValues();

        ContentValues valuesCover = new ContentValues();

        String newName = data.get(ALBUM_NAME) == null ? mAlbum.getAlbumName() : data.get(ALBUM_NAME);
        String newArtistName = data.get(ARTIST_NAME) == null ? mAlbum.getArtistName() : data.get(ARTIST_NAME);
        String newYear = data.get(YEAR) == null ? String.valueOf(mAlbum.getYear()) : data.get(YEAR);

        String newCover = data.get(COVER);

        if (!mAlbum.getAlbumName().equals(newName)) {
            values.put(MediaStore.Audio.Media.ALBUM, newName);
        }

        if (!mAlbum.getArtistName().equals(newArtistName)) {
            values.put(MediaStore.Audio.Media.ARTIST, newArtistName);
        }

        if (!String.valueOf(mAlbum.getYear()).equals(newYear)) {
            values.put(MediaStore.Audio.Media.YEAR, newYear);
        }

/*        if (!newCover.equals("")) {

            // supprimer l'image existante
            Uri sArtworkUri = Uri.parse(newCover);
            context.getContentResolver().delete(ContentUris.withAppendedId(sArtworkUri, mAlbum.getId()), null, null);

            //injecter la nouvelle
            //valuesCover.put(MediaStore.Audio.Albums.ALBUM_ART, newCover );
            //context.getContentResolver().update(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, valuesCover, MediaStore.Audio.Albums.ALBUM_ID + "=" + mAlbum.getId(), null);

            valuesCover.put("album_id", mAlbum.getId());
            valuesCover.put("_data", newCover);
            context.getContentResolver().insert(sArtworkUri, valuesCover);

            context.getContentResolver().update(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, valuesCover, MediaStore.Audio.Albums.ALBUM_ID + "=" + mAlbum.getId(), null);
        }*/


        if (values.size() > 0) {
            context.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values, MediaStore.Audio.Media.ALBUM_ID + "=" + mAlbum.getId(), null);
            return true;
        }
        return true;

    }

/*    private void putCover(Context context) {

        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
        if (uri != null) {
            InputStream in = null;
            try {
                in = res.openInputStream(uri);
                return BitmapFactory.decodeStream(in, null, sBitmapOptions);
            } catch (FileNotFoundException ex) {
                // The album art thumbnail does not actually exist. Maybe the user deleted it, or
                // maybe it never existed to begin with.
                Bitmap bm = getArtworkFromFile(context, null, album_id);
                if (bm != null) {
                    // Put the newly found artwork in the database.
                    // Note that this shouldn't be done for the "unknown" album,
                    // but if this method is called correctly, that won't happen.

                    // first write it somewhere
                    String file = Environment.getExternalStorageDirectory()
                            + "/albumthumbs/" + String.valueOf(System.currentTimeMillis());
                    if (ensureFileExists(file)) {
                        try {
                            OutputStream outstream = new FileOutputStream(file);
                            if (bm.getConfig() == null) {
                                bm = bm.copy(Bitmap.Config.RGB_565, false);
                                if (bm == null) {
                                    return getDefaultArtwork(context);
                                }
                            }
                            boolean success = bm.compress(Bitmap.CompressFormat.JPEG, 75, outstream);
                            outstream.close();
                            if (success) {
                                ContentValues values = new ContentValues();
                                values.put("album_id", album_id);
                                values.put("_data", file);
                                Uri newuri = res.insert(sArtworkUri, values);
                                if (newuri == null) {
                                    // Failed to insert in to the database. The most likely
                                    // cause of this is that the item already existed in the
                                    // database, and the most likely cause of that is that
                                    // the album was scanned before, but the user deleted the
                                    // album art from the sd card.
                                    // We can ignore that case here, since the media provider
                                    // will regenerate the album art for those entries when
                                    // it detects this.
                                    success = false;
                                }
                            }
                            if (!success) {
                                File f = new File(file);
                                f.delete();
                            }
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "error creating file", e);
                        } catch (IOException e) {
                            Log.e(TAG, "error creating file", e);
                        }
                    }
                } else {
                    bm = getDefaultArtwork(context);
                }
                return bm;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                }
            }
        }
    }*/

/*    private static String getRealPathFromUri(Context context, Uri contentUri) {

        Log.d(TAG, "getRealPathFromUri(Context context, Uri contentUri)");


        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor != null ? cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) : 0;
            assert cursor != null;
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }*/

}
