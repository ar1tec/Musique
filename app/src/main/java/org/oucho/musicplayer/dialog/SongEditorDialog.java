package org.oucho.musicplayer.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Song;


public class SongEditorDialog extends DialogFragment implements MusiqueKeys {

    private static final String TAG = "SongEditorDialog";



    private static Song mSong;

    private EditText mTitleEditText;
    private EditText mArtistEditText;
    private EditText mAlbumEditText;
    private EditText mTrackEditText;
    private EditText mGenreEditText;


    public static SongEditorDialog newInstance(Song song) {
        SongEditorDialog fragment = new SongEditorDialog();

        Bundle args = new Bundle();

        args.putParcelable("song", song);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle != null) {
            mSong = bundle.getParcelable("song");
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

            Intent intent = new Intent();
            intent.setAction(SONG_TAG);

            intent.putExtra("title", mTitleEditText.getText().toString());
            intent.putExtra("albumName", mAlbumEditText.getText().toString());
            intent.putExtra("artistName", mArtistEditText.getText().toString());
            intent.putExtra("track", mTrackEditText.getText().toString());
            intent.putExtra("genre", mGenreEditText.getText().toString());

            intent.putExtra("song", mSong);
            getContext().sendBroadcast(intent);


        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());


        return builder.create();
    }

}
