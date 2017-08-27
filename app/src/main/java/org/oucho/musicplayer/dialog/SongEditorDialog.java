package org.oucho.musicplayer.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.oucho.musicplayer.utils.TagEdit;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Song;

import java.util.HashMap;


public class SongEditorDialog extends DialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_ARTIST = "artist";
    private static final String ARG_ALBUM = "album";
    private static final String ARG_ALBUM_ID = "album_id";
    private static final String ARG_TRACK_NUMBER = "track_number";
    private static final String ARG_TRACK_DURATION = "duration";
    private static final String ARG_YEAR = "year";


    private Song mSong;

    private EditText mTitleEditText;
    private EditText mArtistEditText;
    private EditText mAlbumEditText;
    private EditText mTrackEditText;
    private EditText mGenreEditText;
    private OnTagsEditionSuccessListener mListener;

    private Context context;

    public static SongEditorDialog newInstance(Song song) {
        SongEditorDialog fragment = new SongEditorDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, song.getId());
        args.putString(ARG_TITLE, song.getTitle());
        args.putString(ARG_ARTIST, song.getArtist());
        args.putString(ARG_ALBUM, song.getAlbum());
        args.putLong(ARG_ALBUM_ID, song.getAlbumId());
        args.putInt(ARG_TRACK_NUMBER, song.getTrackNumber());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        context = getContext();

        if (args != null) {

            long id = args.getLong(ARG_ID);
            String title = args.getString(ARG_TITLE);
            String artist = args.getString(ARG_ARTIST);
            String album = args.getString(ARG_ALBUM);
            long albumId = args.getLong(ARG_ALBUM_ID);
            int trackNumber = args.getInt(ARG_TRACK_NUMBER);
            int trackDur = args.getInt(ARG_TRACK_DURATION);
            int yearCol = args.getInt(ARG_YEAR);

            mSong = new Song(id, title, artist, album, albumId, trackNumber, trackDur, yearCol);
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.edit_tags);

        mSong.setGenre(TagEdit.getSongGenre(getActivity(), mSong.getId()));
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


        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                final Activity activity = getActivity();
                dismiss();

                new AsyncTask<Object,Object,Boolean>(){

                    @Override
                    protected Boolean doInBackground(Object... params) {
                        return saveTags(activity);
                    }

                    @Override
                    protected void onPostExecute(Boolean b) {
                        super.onPostExecute(b);
                        if (b) {
                            if(mListener != null) {
                                mListener.onTagsEditionSuccess();
                            }
                        }
                        else {
                            Toast.makeText(context,R.string.tags_edition_failed,Toast.LENGTH_SHORT).show();

                        }
                    }
                }.execute();


            }
        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());


        return builder.create();
    }

    private boolean saveTags(Context context) {


        HashMap<String,String> tags = new HashMap<>();

        tags.put(TagEdit.TITLE, mTitleEditText.getText().toString());
        tags.put(TagEdit.ARTIST, mArtistEditText.getText().toString());
        tags.put(TagEdit.ALBUM, mAlbumEditText.getText().toString());
        tags.put(TagEdit.TRACK, mTrackEditText.getText().toString());
        tags.put(TagEdit.GENRE, mGenreEditText.getText().toString());

        return TagEdit.editSongTags(context, mSong, tags);

    }


    public void setOnTagsEditionSuccessListener(OnTagsEditionSuccessListener listener)
    {
        mListener = listener;
    }


    public interface OnTagsEditionSuccessListener {


        void onTagsEditionSuccess();
    }

}
