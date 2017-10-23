package org.oucho.musicplayer.dialog;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.utils.StorageHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AlbumEditorDialog extends DialogFragment {

    private static List<Song> mSongList = new ArrayList<>();

    private static final String TAG = "AlbumEditorDialog";

    private static final String ARG_ID = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_ARTIST = "artist";
    private static final String ARG_YEAR = "year";
    private static final String ARG_TRACK_COUNT = "track_count";

    private static Album mAlbum;

    private EditText mAlbumEditText;
    private EditText mArtistEditText;
    private EditText mGenreEditText;
    private EditText mYearEditText;

    public static AlbumEditorDialog newInstance(Album album) {
        AlbumEditorDialog fragment = new AlbumEditorDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, album.getId());
        args.putString(ARG_NAME, album.getAlbumName());
        args.putString(ARG_ARTIST, album.getArtistName());
        args.putInt(ARG_YEAR, album.getYear());
        args.putInt(ARG_TRACK_COUNT, album.getTrackCount());

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        if (args != null) {

            long id = args.getLong(ARG_ID);
            String album = args.getString(ARG_NAME);
            String artist = args.getString(ARG_ARTIST);
            int year = args.getInt(ARG_YEAR);
            int trackCount = args.getInt(ARG_TRACK_COUNT);

            mAlbum = new Album(id,album,artist, year,trackCount);

            getLoaderManager().initLoader(0, null, mLoaderCallbacks);
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.edit_tags);

        @SuppressLint("InflateParams")
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_album_tag_editor, null);
        builder.setView(dialogView);

        mAlbumEditText = dialogView.findViewById(R.id.album);
        mArtistEditText = dialogView.findViewById(R.id.artist);
        mGenreEditText = dialogView.findViewById(R.id.genre);
        mYearEditText = dialogView.findViewById(R.id.year);

        mAlbumEditText.setText(mAlbum.getAlbumName());
        mArtistEditText.setText(mAlbum.getArtistName());
        mYearEditText.setText(String.valueOf(mAlbum.getYear()));


        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {

            dismiss();

            String list[] = {mAlbumEditText.getText().toString(), mArtistEditText.getText().toString(), mGenreEditText.getText().toString(), mYearEditText.getText().toString()};
            Toast.makeText(MusiqueApplication.getInstance(), R.string.tags_edition, Toast.LENGTH_SHORT).show();

            new tag(list).execute();


        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());


        return builder.create();
    }

    private static class tag extends AsyncTask<Object,Object,Boolean> {

        final String album;
        final String artist;
        final String genre;
        final String year;

        tag(String[] value) {
            super();

            this.album = value[0];
            this.artist = value[1];
            this.genre = value[2];
            this.year = value[3];
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            return saveTags(album, artist, genre, year);
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

    private static boolean saveTags(String album, String artist, String genre, String year) {

        boolean success = false;

        for (int i = 0; i < mSongList.size(); i++) {


            try {
                File song = new File(mSongList.get(i).getPath());

                if (StorageHelper.isWritable(song)) {

                    AudioFile audioFile = AudioFileIO.read(song);
                    Tag tag = audioFile.getTag();
                    tag.setField(FieldKey.ARTIST, artist);
                    tag.setField(FieldKey.ALBUM, album);
                    tag.setField(FieldKey.GENRE, genre);
                    tag.setField(FieldKey.YEAR, year);

                    audioFile.commit();

                    success = true;

                } else {

                    // TODO VERSION.SDK_INT >= 26
                    //AudioFileIO.writeAs(audioFile, MusiqueApplication.getInstance().getCacheDir().getPath() + "/temp");

                    String filename = new File(mSongList.get(i).getPath()).getName();
                    String pathCache = MusiqueApplication.getInstance().getCacheDir().getPath() + "/";
                    String pathSong = new File(mSongList.get(i).getPath()).getParent();

                    File file = new File(pathCache + filename);

                    if (file.exists())
                        StorageHelper.deleteFile(file);

                    StorageHelper.copyFile(song, MusiqueApplication.getInstance().getCacheDir(), false);

                    AudioFile audioFile = AudioFileIO.read(file);
                    Tag tag = audioFile.getTag();

                    tag.setField(FieldKey.ARTIST, artist);
                    tag.setField(FieldKey.ALBUM, album);
                    tag.setField(FieldKey.GENRE, genre);
                    tag.setField(FieldKey.YEAR, year);

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
        }

        return success;
    }



    private final LoaderManager.LoaderCallbacks<List<Song>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(getActivity());

            loader.setSelection(MediaStore.Audio.Media.ALBUM_ID + " = ?", new String[]{String.valueOf(mAlbum.getId())});
            loader.setSortOrder(MediaStore.Audio.Media.TRACK);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> songList) {

            mSongList = songList;

            mGenreEditText.setText(mSongList.get(0).getGenre());
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            //  Auto-generated method stub
        }
    };

}
