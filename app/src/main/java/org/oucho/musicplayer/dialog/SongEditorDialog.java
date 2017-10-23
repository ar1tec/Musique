package org.oucho.musicplayer.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.oucho.musicplayer.MusiqueApplication;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.utils.StorageHelper;

import java.io.File;


public class SongEditorDialog extends DialogFragment {

    private static final String TAG = "SongEditorDialog";

    private static final String ARG_ID = "id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_ARTIST = "artist";
    private static final String ARG_ALBUM = "album";
    private static final String ARG_ALBUM_ID = "album_id";
    private static final String ARG_TRACK_NUMBER = "track_number";
    private static final String ARG_TRACK_DURATION = "duration";
    private static final String ARG_YEAR = "year";
    private static final String ARG_GENRE = "genre";
    private static final String ARG_MIME_TYPE = "mime_type";
    private static final String ARG_PATH = "path";


    private static Song mSong;

    private EditText mTitleEditText;
    private EditText mArtistEditText;
    private EditText mAlbumEditText;
    private EditText mTrackEditText;
    private EditText mGenreEditText;


    public static SongEditorDialog newInstance(Song song) {
        SongEditorDialog fragment = new SongEditorDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, song.getId());
        args.putString(ARG_TITLE, song.getTitle());
        args.putString(ARG_ARTIST, song.getArtist());
        args.putString(ARG_ALBUM, song.getAlbum());
        args.putLong(ARG_ALBUM_ID, song.getAlbumId());
        args.putInt(ARG_TRACK_NUMBER, song.getTrackNumber());
        args.putString(ARG_GENRE, song.getGenre());
        args.putString(ARG_MIME_TYPE, song.getMimeType());
        args.putString(ARG_PATH, song.getPath());

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        if (args != null) {

            long id = args.getLong(ARG_ID);
            String title = args.getString(ARG_TITLE);
            String artist = args.getString(ARG_ARTIST);
            String album = args.getString(ARG_ALBUM);
            long albumId = args.getLong(ARG_ALBUM_ID);
            int trackNumber = args.getInt(ARG_TRACK_NUMBER);
            int trackDur = args.getInt(ARG_TRACK_DURATION);
            int yearCol = args.getInt(ARG_YEAR);
            String genre = args.getString(ARG_GENRE);
            String mimeType = args.getString(ARG_MIME_TYPE);
            String path = args.getString(ARG_PATH);

            mSong = new Song(id, title, artist, album, albumId, trackNumber, trackDur, yearCol, genre, mimeType, path);
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.edit_tags);

        @SuppressLint("InflateParams")
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_song_tag_editor, null);
        builder.setView(dialogView);

        mTitleEditText = dialogView.findViewById(R.id.title);
        mArtistEditText = dialogView.findViewById(R.id.artist);
        mAlbumEditText = dialogView.findViewById(R.id.album);
        mTrackEditText = dialogView.findViewById(R.id.track_number);
        mGenreEditText = dialogView.findViewById(R.id.genre);

        mTitleEditText.setText(mSong.getTitle());
        mArtistEditText.setText(mSong.getArtist());
        mAlbumEditText.setText(mSong.getAlbum());
        mTrackEditText.setText(String.valueOf(mSong.getTrackNumber()));
        mGenreEditText.setText(mSong.getGenre());


        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {

            dismiss();

            String list[] = {
                    mTitleEditText.getText().toString(),
                    mArtistEditText.getText().toString(),
                    mAlbumEditText.getText().toString(),
                    mTrackEditText.getText().toString(),
                    mGenreEditText.getText().toString()
            };

            Toast.makeText(MusiqueApplication.getInstance(), R.string.tags_edition, Toast.LENGTH_SHORT).show();

            new tag(list).execute();


        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());


        return builder.create();
    }

    private static class tag extends AsyncTask<Object,Object,Boolean> {

        final String title;
        final String artist;
        final String album;
        final String track;
        final String genre;

        tag(String[] value) {
            super();

            this.title = value[0];
            this.artist = value[1];
            this.album = value[2];
            this.track = value[3];
            this.genre = value[4];
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                return saveTags(title, artist, album, track, genre);
            } catch (Exception ignore) {}

            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if (aBoolean) {
                Toast.makeText(MusiqueApplication.getInstance(), R.string.tags_edition_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MusiqueApplication.getInstance(), R.string.tags_edition_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }



    private static boolean saveTags(String title, String artist, String album, String track, String genre) {

        boolean success = false;

        try {
            File song = new File(mSong.getPath());

            if (StorageHelper.isWritable(song)) {

                AudioFile audioFile = AudioFileIO.read(song);
                Tag tag = audioFile.getTag();
                tag.setField(FieldKey.TITLE, title);
                tag.setField(FieldKey.ARTIST, artist);
                tag.setField(FieldKey.ALBUM, album);
                tag.setField(FieldKey.TRACK, track);
                tag.setField(FieldKey.GENRE, genre);
                audioFile.commit();

                success = true;

            } else {

                // TODO VERSION.SDK_INT >= 26
                //AudioFileIO.writeAs(audioFile, MusiqueApplication.getInstance().getCacheDir().getPath() + "/temp");

                String filename = new File(mSong.getPath()).getName();
                String pathCache = MusiqueApplication.getInstance().getCacheDir().getPath() + "/";
                String pathSong = new File(mSong.getPath()).getParent();

                File file = new File(pathCache + filename);

                if (file.exists())
                    StorageHelper.deleteFile(file);

                StorageHelper.copyFile(song, MusiqueApplication.getInstance().getCacheDir(), false);

                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();

                tag.setField(FieldKey.TITLE, title);
                tag.setField(FieldKey.ARTIST, artist);
                tag.setField(FieldKey.ALBUM, album);
                tag.setField(FieldKey.TRACK, track);
                tag.setField(FieldKey.GENRE, genre);
                audioFile.commit();

                File target = new File(pathSong);

                if (StorageHelper.copyFile(file, target, true)) {
                    success = true;
                    StorageHelper.deleteFile(file);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }


        return success;

    }

}
