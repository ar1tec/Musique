package org.oucho.musicplayer.dialog;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.utils.BitmapHelper;

import java.io.File;
import java.util.List;

import static org.oucho.musicplayer.MusiqueKeys.ALBUM_TAG;


public class AlbumEditorDialog extends DialogFragment {

    private static final String TAG = "AlbumEditorDialog";

    private static Album mAlbum;

    private ImageView mArtwork;
    private EditText mAlbumEditText;
    private EditText mArtistEditText;
    private EditText mGenreEditText;
    private EditText mYearEditText;

    private String newArtwork = null;

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
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_tag_album_editor, null);
        builder.setView(dialogView);

        mArtwork = dialogView.findViewById(R.id.artwork);
        mArtwork.setOnClickListener(mOnClickListener);
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
            intent.putExtra("cover", newArtwork);

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

            try {
                File file = new File(songList.get(0).getPath());

                AudioFile f = AudioFileIO.read(file);

                Tag tag = f.getTag();

                mArtwork.setImageBitmap(BitmapHelper.byteToBitmap(tag.getFirstArtwork().getBinaryData()));

            } catch (Exception ignore) {}


            mGenreEditText.setText(songList.get(0).getGenre());

        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            //  Auto-generated method stub
        }
    };


    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {


            switch (v.getId()) {
                case R.id.artwork:
                    FilePickerDialog.with(getFragmentManager())
                            .onImageSelected(path -> {

                                Log.d(TAG, "result = " + path);

                                File imgFile = new  File(path);

                                if(imgFile.exists()){

                                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                                    mArtwork.setImageBitmap(myBitmap);

                                    newArtwork = path;
                                }

                            }).show();
                    break;

                default:
                    break;
            }
        }
    };

}
