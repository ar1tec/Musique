package org.oucho.musicplayer.dialog;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.activities.FilePickerActivity;
import org.oucho.musicplayer.filepicker.FilePicker;
import org.oucho.musicplayer.filepicker.FilePickerParcelObject;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.images.FilePickCache;
import org.oucho.musicplayer.model.Album;
import org.oucho.musicplayer.audiotag.TagEdit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;


public class AlbumEditorDialog extends DialogFragment implements
        View.OnClickListener {


    private static final String ARG_ID = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_ARTIST = "artist";
    private static final String ARG_YEAR = "year";
    private static final String ARG_TRACK_COUNT = "track_count";


    private Album mAlbum;


    private EditText mTitleEditText;
    private EditText mArtistEditText;
    private EditText mYearEditText;
    private OnEditionSuccessListener mListener;


    private ImageView artworkView;
    private String cheminImage = "";


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
            String title = args.getString(ARG_NAME);
            String artist = args.getString(ARG_ARTIST);
            int year = args.getInt(ARG_YEAR);
            int trackCount = args.getInt(ARG_TRACK_COUNT);

            mAlbum = new Album(id,title,artist,year,trackCount);

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

        mTitleEditText = (EditText) dialogView.findViewById(R.id.title);
        mArtistEditText = (EditText) dialogView.findViewById(R.id.artist);
        mYearEditText = (EditText) dialogView.findViewById(R.id.year);

        mTitleEditText.setText(mAlbum.getAlbumName());
        mArtistEditText.setText(mAlbum.getArtistName());
        mYearEditText.setText(String.valueOf(mAlbum.getYear()));


        int mArtworkSize = getResources().getDimensionPixelSize(R.dimen.art_size);

        artworkView = (ImageView) dialogView.findViewById(R.id.artwork);
        ArtworkCache.getInstance().loadBitmap(mAlbum.getId(), artworkView, mArtworkSize, mArtworkSize);

        dialogView.findViewById(R.id.artwork).setOnClickListener(this);



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

        data.put(TagEdit.ALBUM_NAME, mTitleEditText.getText().toString());
        data.put(TagEdit.ARTIST_NAME, mArtistEditText.getText().toString());
        data.put(TagEdit.YEAR, mYearEditText.getText().toString());

        data.put(TagEdit.COVER, cheminImage);

        return TagEdit.editAlbumData(context, mAlbum, data);

    }


    public void setOnEditionSuccessListener(OnEditionSuccessListener listener)
    {
        mListener = listener;
    }


    public interface OnEditionSuccessListener {


        void onEditionSuccess();

    }



    public void filePicker() {

        Intent intent = new Intent(getContext(), FilePickerActivity.class);

        intent.putExtra(FilePicker.SET_FILTER_LISTED, new String[] { "jpg", "jpeg", "png"});
        intent.putExtra(FilePicker.DISABLE_SORT_BUTTON, true);
        intent.putExtra(FilePicker.SET_ONLY_ONE_ITEM, true);

        startActivityForResult(intent, 0);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (resultCode != 0) {


            StringBuffer buffer = new StringBuffer();

            FilePickerParcelObject object =  data.getParcelableExtra(FilePickerParcelObject.class.getCanonicalName());

            if (object.count > 0) {

                for (int i = 0; i < object.count; i++) {
                    buffer.append(object.names.get(i));
                    if (i < object.count - 1) buffer.append(", ");
                }


                Log.d("Résultat", "Count: " + object.count + "\n" + "Path: " + object.path + "\n" + "Selected: " + buffer.toString());


                String image = object.path + buffer.toString();

                artworkView.setImageBitmap(FilePickCache.getInstance().getBitmapFromMemCache(image));

                Log.d("TEST", String.valueOf(getImageContentUri(getContext(), new File(image))));

                cheminImage = String.valueOf(getImageContentUri(getContext(), new File(image)));

            }

        }
    }



    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID },
                MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }



    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.artwork:
                filePicker();
                break;


            default:
                break;
        }
    }
}
