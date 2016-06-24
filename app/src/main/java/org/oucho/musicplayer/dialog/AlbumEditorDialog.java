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

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.audiotag.TagEdit;
import org.oucho.musicplayer.model.Album;

import java.util.HashMap;


public class AlbumEditorDialog extends DialogFragment {


    private static final String ARG_ID = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_ARTIST = "artist";
    private static final String ARG_YEAR = "year";
    private static final String ARG_TRACK_COUNT = "track_count";

    private Album mAlbum;

    private EditText mAlbumEditText;
    private EditText mArtistEditText;
    private EditText mYearEditText;
    private OnEditionSuccessListener mListener;

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

            mAlbum = new Album(id,album,artist,year,trackCount);

        }
    }



    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Context context = getContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.edit_tags);

        @SuppressLint("InflateParams")
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_album_tag_editor, null);
        builder.setView(dialogView);

        mAlbumEditText = (EditText) dialogView.findViewById(R.id.album);
        mArtistEditText = (EditText) dialogView.findViewById(R.id.artist);
        mYearEditText = (EditText) dialogView.findViewById(R.id.year);

        mAlbumEditText.setText(mAlbum.getAlbumName());
        mArtistEditText.setText(mAlbum.getArtistName());
        mYearEditText.setText(String.valueOf(mAlbum.getYear()));


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
                                mListener.onEditionSuccess();
                            }
                        } else {
                            Toast.makeText(context, R.string.tags_edition_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                }.execute();


            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });


        return builder.create();
    }

    private boolean saveTags(Context context) {

        HashMap<String,String> data = new HashMap<>();

        data.put(TagEdit.ALBUM_NAME, mAlbumEditText.getText().toString());
        data.put(TagEdit.ARTIST_NAME, mArtistEditText.getText().toString());
        data.put(TagEdit.YEAR, mYearEditText.getText().toString());

        return TagEdit.editAlbumData(context, mAlbum, data);
    }


    public void setOnEditionSuccessListener(OnEditionSuccessListener listener)
    {
        mListener = listener;
    }


    public interface OnEditionSuccessListener {


        void onEditionSuccess();

    }

}
