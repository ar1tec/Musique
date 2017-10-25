package org.oucho.musicplayer.dialog;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.fragments.loaders.SongLoader;

import java.util.ArrayList;
import java.util.List;

import static org.oucho.musicplayer.MusiqueKeys.ALBUM_TAG;


public class AlbumEditorDialog extends DialogFragment {

    private static final String TAG = "AlbumEditorDialog";

    private static Album mAlbum;

    private EditText mAlbumEditText;
    private EditText mArtistEditText;
    private EditText mGenreEditText;
    private EditText mYearEditText;

    public static AlbumEditorDialog newInstance(Album album) {
        AlbumEditorDialog fragment = new AlbumEditorDialog();

        Bundle args = new Bundle();

        args.putParcelable("album", album);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle != null) {

            mAlbum = bundle.getParcelable("album");

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

            Intent intent = new Intent();
            intent.setAction(ALBUM_TAG);

            intent.putExtra("albumName", mAlbumEditText.getText().toString());
            intent.putExtra("artistName", mArtistEditText.getText().toString());
            intent.putExtra("year", mYearEditText.getText().toString());
            intent.putExtra("genre", mGenreEditText.getText().toString());

            intent.putExtra("album", mAlbum);
            getContext().sendBroadcast(intent);

        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());


        return builder.create();
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

            mGenreEditText.setText(songList.get(0).getGenre());
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            //  Auto-generated method stub
        }
    };

}
