package org.oucho.musicplayer.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.oucho.musicplayer.model.Song;

import java.util.ArrayList;
import java.util.List;


public class QueueDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Queue.db";

    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + QueueEntry.TABLE_NAME + " (" +
                    QueueEntry._ID + " INTEGER PRIMARY KEY," +
                    QueueEntry.COLUMN_NAME_SONG_ID + " INTEGER UNIQUE" + COMMA_SEP +
                    QueueEntry.COLUMN_NAME_TITLE + " TEXT" + COMMA_SEP +
                    QueueEntry.COLUMN_NAME_ARTIST + " TEXT" + COMMA_SEP +
                    QueueEntry.COLUMN_NAME_ALBUM + " TEXT" + COMMA_SEP +
                    QueueEntry.COLUMN_NAME_TRACK_NUMBER + " INTEGER" + COMMA_SEP +
                    QueueEntry.COLUMN_NAME_TRACK_DURATION + " INTEGER" + COMMA_SEP +
                    QueueEntry.COLUMN_NAME_ALBUM_ID + " INTEGER" + COMMA_SEP +
                    QueueEntry.COLUMN_NAME_GENRE + " TEXT" +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + QueueEntry.TABLE_NAME;

    private static final String[] sProjection = new String[]
            {
                    QueueEntry._ID, //0
                    QueueEntry.COLUMN_NAME_SONG_ID,
                    QueueEntry.COLUMN_NAME_TITLE,
                    QueueEntry.COLUMN_NAME_ARTIST,
                    QueueEntry.COLUMN_NAME_ALBUM,
                    QueueEntry.COLUMN_NAME_TRACK_NUMBER,
                    QueueEntry.COLUMN_NAME_TRACK_DURATION,
                    QueueEntry.COLUMN_NAME_ALBUM_ID,
                    QueueEntry.COLUMN_NAME_GENRE,
            };

    public QueueDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    private void addInternal(SQLiteDatabase db, Song song) {

        ContentValues values = new ContentValues();
        values.put(QueueEntry.COLUMN_NAME_SONG_ID, song.getId());
        values.put(QueueEntry.COLUMN_NAME_TITLE, song.getTitle());
        values.put(QueueEntry.COLUMN_NAME_ARTIST, song.getArtist());
        values.put(QueueEntry.COLUMN_NAME_ALBUM, song.getAlbum());
        values.put(QueueEntry.COLUMN_NAME_TRACK_NUMBER, song.getTrackNumber());
        values.put(QueueEntry.COLUMN_NAME_TRACK_DURATION, song.getDuration());
        values.put(QueueEntry.COLUMN_NAME_ALBUM_ID, song.getAlbumId());
        values.put(QueueEntry.COLUMN_NAME_GENRE, song.getGenre());

        db.insert(QueueEntry.TABLE_NAME, null, values);

    }

    public void removeAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(QueueEntry.TABLE_NAME, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public void add(List<Song> songList) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            for(Song song:songList) {
                addInternal(db, song);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        db.close();
    }


    public List<Song> readAll() {
        return read(-1);
    }

    private List<Song> read(int limit) {
        SQLiteDatabase db = getReadableDatabase();

        List<Song> list = new ArrayList<>();

        Cursor cursor;
        if (limit < 0) {
            cursor = db.query(QueueEntry.TABLE_NAME, sProjection, null, null, null, null, null);

        } else {
            cursor = db.query(QueueEntry.TABLE_NAME, sProjection, null, null, null, null, null, String.valueOf(limit));
        }
        if (cursor != null && cursor.moveToFirst()) {

            int idCol = cursor.getColumnIndex(QueueEntry.COLUMN_NAME_SONG_ID);

            int titleCol = cursor.getColumnIndex(QueueEntry.COLUMN_NAME_TITLE);
            int artistCol = cursor.getColumnIndex(QueueEntry.COLUMN_NAME_ARTIST);
            int albumCol = cursor.getColumnIndex(QueueEntry.COLUMN_NAME_ALBUM);
            int albumIdCol = cursor.getColumnIndex(QueueEntry.COLUMN_NAME_ALBUM_ID);
            int trackCol = cursor.getColumnIndex(QueueEntry.COLUMN_NAME_TRACK_NUMBER);
            int trackDur = cursor.getColumnIndex(QueueEntry.COLUMN_NAME_TRACK_DURATION);

            do {
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);

                String artist = cursor.getString(artistCol);

                String album = cursor.getString(albumCol);

                long albumId = cursor.getLong(albumIdCol);

                int track = cursor.getInt(trackCol);

                int duration = cursor.getInt(trackDur);

                list.add(new Song(id, title, artist, album, albumId, track, duration));
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        db.close();

        return list;
    }

    private static class QueueEntry implements SongListColumns {
        public static final String TABLE_NAME = "queue";
    }

}