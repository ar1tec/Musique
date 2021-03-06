package org.oucho.musicplayer.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.oucho.musicplayer.MusiqueApplication;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.angelo.Angelo;
import org.oucho.musicplayer.utils.StorageHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class SaveTagProgressDialog extends DialogFragment implements MusiqueKeys{

    private static final String TAG = "DeleteProgressDialog";

    private TextView file_name;

    private boolean isCancel = false;
    private boolean isAlbum;

    private ProgressBar progressBar;

    private Song mSong;
    private Album mAlbum;
    private List<Song> mSongList = new ArrayList<>();

    private AlertDialog controlDialog;
    private AsyncTask<String, Integer, Boolean> deleteTask;

    private String disc;
    private String year;
    private String cover;
    private String genre;
    private String title;
    private String track;
    private String albumName;
    private String artistName;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        String type = bundle.getString("type");

        if (type != null && type.equals("album")) {

            isAlbum = true;

            mAlbum = bundle.getParcelable("album");
            albumName = bundle.getString("albumName");
            artistName = bundle.getString("artistName");
            genre = bundle.getString("genre");
            year = bundle.getString("year");
            cover = bundle.getString("cover");

            getLoaderManager().initLoader(0, null, mLoaderSongs);

        } else if (type != null && type.equals("song")) {

            isAlbum = false;

            mSong = bundle.getParcelable("song");
            title = bundle.getString("title");
            albumName = bundle.getString("albumName");
            artistName = bundle.getString("artistName");
            track = bundle.getString("track");
            disc = bundle.getString("disc");
            genre = bundle.getString("genre");

        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_progressbar, null, false);

        file_name = view.findViewById(R.id.file_name);

        progressBar = view.findViewById(R.id.progress_bar);

        progressBar.setMax(100);
        progressBar.setProgress(0);

        builder.setView(view);
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> isCancel = true);

        controlDialog = builder.create();

        return controlDialog;
    }


    @Override
    public void onStart() {
        super.onStart();

        if (!isAlbum)
            deleteTask = new Delete().execute();
    }


    @Override
    public void onStop() {

        Log.w(TAG, "stop");
        if (deleteTask.getStatus() == AsyncTask.Status.FINISHED) {

            Intent refresh = new Intent();
            refresh.setAction(REFRESH_TAG);
            // don't touch error on break != getContext().sendBroadcast(refresh);
            MusiqueApplication.getInstance().sendBroadcast(refresh);
            controlDialog.cancel();
        } else {
            isCancel = true;
            new Handler().postDelayed(this::onStop, 500);
        }
        super.onStop();
    }


    @SuppressLint("StaticFieldLeak")
    class Delete extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(String... arg0) {
            boolean success;

            if (isAlbum) {

                success = saveTagsAlbum();


            } else {
                success = saveTags();
            }

            return success;
        }

        private  boolean saveTags() {

            boolean success = false;

            try {
                // TODO VERSION.SDK_INT >= 26
                //AudioFileIO.writeAs(audioFile, MusiqueApplication.getInstance().getCacheDir().getPath() + "/temp");

                File song = new File(mSong.getPath());

                try {
                    getActivity().runOnUiThread(() -> file_name.setText(mSong.getPath()));
                } catch (NullPointerException ignored) {}

                String filename = new File(mSong.getPath()).getName();
                String pathCache = MusiqueApplication.getInstance().getCacheDir().getPath() + "/";
                String pathSong = new File(mSong.getPath()).getParent();

                File file = new File(pathCache + filename);

                if (file.exists())
                    StorageHelper.deleteFile(file);

                StorageHelper.copyFile(song, MusiqueApplication.getInstance().getCacheDir(), false);

                try {
                    getActivity().runOnUiThread(() -> progressBar.setProgress(50));
                } catch (NullPointerException ignored) {}

                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();

                tag.setField(FieldKey.TITLE, title);
                tag.setField(FieldKey.ARTIST, artistName);
                tag.setField(FieldKey.ALBUM, albumName);
                tag.setField(FieldKey.TRACK, track);
                tag.setField(FieldKey.GENRE, genre);

                if (disc.equals("0") || disc.equals(""))
                    tag.deleteField(FieldKey.DISC_NO);
                else
                    tag.setField(FieldKey.DISC_NO, disc);

                if (cover != null) {
                    File art = new File(cover);
                    Artwork cover = ArtworkFactory.createArtworkFromFile(art);
                    tag.setField(cover);
                }

                audioFile.commit();

                File target = new File(pathSong);

                if (StorageHelper.copyFile(file, target, true)) {
                    success = true;
                    StorageHelper.deleteFile(file);
                }

                try {
                    getActivity().runOnUiThread(() -> progressBar.setProgress(100));
                } catch (NullPointerException ignored) {}


            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            return success;
        }

        private boolean saveTagsAlbum() {

            boolean success = false;

            float step = 100/mSongList.size();
            int totalCount = 0;

            for (int i = 0; i < mSongList.size(); i++) {

                if (isCancel)
                    break;

                totalCount += 1;

                int j = i;

                try {

                    // TODO VERSION.SDK_INT >= 26
                    //AudioFileIO.writeAs(audioFile, MusiqueApplication.getInstance().getCacheDir().getPath() + "/temp");

                    File song = new File(mSongList.get(i).getPath());

                    try {
                        getActivity().runOnUiThread(() -> file_name.setText(mSongList.get(j).getPath()));
                    } catch (NullPointerException ignored) {}


                    String filename = new File(mSongList.get(i).getPath()).getName();
                    String pathCache = MusiqueApplication.getInstance().getCacheDir().getPath() + "/";
                    String pathSong = new File(mSongList.get(i).getPath()).getParent();

                    File file = new File(pathCache + filename);

                    if (file.exists())
                        StorageHelper.deleteFile(file);

                    StorageHelper.copyFile(song, MusiqueApplication.getInstance().getCacheDir(), false);

                    float tc1 = (step * totalCount) - (step/2);

                    try {
                        getActivity().runOnUiThread(() -> progressBar.setProgress((int) tc1));
                    } catch (NullPointerException ignored) {}


                    AudioFile audioFile = AudioFileIO.read(file);
                    Tag tag = audioFile.getTag();

                    tag.setField(FieldKey.ARTIST, artistName);
                    tag.setField(FieldKey.ALBUM, albumName);
                    tag.setField(FieldKey.GENRE, genre);
                    tag.setField(FieldKey.YEAR, year);

                    if (cover != null) {
                        File art = new File(cover);
                        Artwork cover = ArtworkFactory.createArtworkFromFile(art);
                        tag.setField(cover);
                    }

                    audioFile.commit();


                    File target = new File(pathSong);

                    if (StorageHelper.copyFile(file, target, true)) {
                        success = true;
                        StorageHelper.deleteFile(file);
                    }

                    float tc2 = (step * totalCount);

                    try {
                        getActivity().runOnUiThread(() -> progressBar.setProgress((int) tc2));
                    } catch (NullPointerException ignored) {}


                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }

            }

            if (cover != null) {
                getContext().getContentResolver().delete(ContentUris.withAppendedId(ARTWORK_URI, mAlbum.getId()), null, null);

                Uri uri = ContentUris.withAppendedId(ARTWORK_URI, mAlbum.getId());
                Angelo.with(getActivity()).invalidate(uri);
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "onPostExecute()");

            onStop();
        }
    }


    private final LoaderManager.LoaderCallbacks<List<Song>> mLoaderSongs = new LoaderManager.LoaderCallbacks<List<Song>>() {

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
            deleteTask = new Delete().execute();
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {}
    };

}
