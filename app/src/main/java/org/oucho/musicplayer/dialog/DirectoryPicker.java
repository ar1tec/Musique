package org.oucho.musicplayer.dialog;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.utils.ExtensionsFilter;
import org.oucho.musicplayer.utils.StorageHelper;
import org.oucho.musicplayer.view.CustomLayoutManager;
import org.oucho.musicplayer.view.fastscroll.FastScrollRecyclerView;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DirectoryPicker extends BottomSheetDialogFragment {


    private ArrayList<File> folders;

    private BottomSheetAlbumsAdapter adapter;

    private TextView currentFolderPath;

    private OnImageSelected onImageSelected;

    private FragmentManager fragmentManager;

    private Spinner spinner;

    private View rootView;

    private String sdCardPath = null;

    private boolean canGoBack = false;

    private final int INTERNAL_STORAGE = 0;


    public static DirectoryPicker with(FragmentManager manager) {
        DirectoryPicker fragment = new DirectoryPicker();
        fragment.fragmentManager = manager;
        return fragment;
    }


    public DirectoryPicker onImageSelected(OnImageSelected callback) {
        onImageSelected = callback;
        return this;
    }

    public void show() {
        show(fragmentManager, getTag());
    }


    @Override
    public void setupDialog(Dialog dialog, int style) {

        rootView = View.inflate(getContext(), R.layout.dialog_directory_picker, null);

        final FastScrollRecyclerView mRecyclerView = rootView.findViewById(R.id.folders);
        spinner = rootView.findViewById(R.id.storage_spinner);

        sdCardPath = StorageHelper.getSdcardPath(getContext());

        currentFolderPath = rootView.findViewById(R.id.current_path_txt);

        mRecyclerView.setLayoutManager(new CustomLayoutManager(getContext()));
        adapter = new BottomSheetAlbumsAdapter();
        mRecyclerView.setAdapter(adapter);

        initSpinner();
        displayContentFolder(Environment.getExternalStorageDirectory());

        rootView.findViewById(R.id.create_new_folder).setOnClickListener(ClickListener);

        dialog.setContentView(rootView);

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) ((View) rootView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = layoutParams.getBehavior();

        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }

        adapter.notifyDataSetChanged();
    }


    private void initSpinner() {

        spinner.setAdapter(new VolumeSpinnerAdapter(rootView.getContext()));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                switch(pos){
                    case INTERNAL_STORAGE:
                        displayContentFolder(Environment.getExternalStorageDirectory());
                        break;
                    default:

                        if (StorageHelper.getSdcardPath(getContext()) != null)
                            displayContentFolder(new File(StorageHelper.getSdcardPath(getContext() ) ) );
                        else
                            displayContentFolder(Environment.getExternalStorageDirectory());
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private boolean canGoBack() {
        return canGoBack;
    }

    public interface OnImageSelected {
        void imageSelected(String path);
    }

    private final View.OnClickListener ClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId()) {

                case R.id.create_new_folder:
                    final EditText editText = new EditText(getContext());

                    AlertDialog insertTextDialog = getInsertTextDialog(getActivity(), editText, R.string.new_folder);
                    insertTextDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok_action).toUpperCase(), (dialogInterface, i) -> {

                        File folderPath = new File(currentFolderPath.getText().toString() + File.separator + editText.getText().toString());

                        if (folderPath.mkdir()) {
                            displayContentFolder(folderPath);
                        } else {
                            StorageHelper.getDocumentFile(getContext(), folderPath, true, true);
                            displayContentFolder(folderPath);
                        }
                    });
                    insertTextDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel).toUpperCase(), (dialogInterface, i) -> {});
                    insertTextDialog.show();
                    break;

                default:
                    break;
            }
        }
    };


    private void displayContentFolder(File dir) {
        canGoBack = false;
        if(dir.canRead()) {
            folders = new ArrayList<>();
            File parent = dir.getParentFile();
            if (parent.canRead()) {
                canGoBack = true;
                folders.add(0, parent);
            }

            ExtensionsFilter imageFilter = new ExtensionsFilter(new String[] {".png", ".jpg", ".bmp"});
            File[] images = dir.listFiles(imageFilter);
            File[] files = dir.listFiles();

            if (files != null && files.length > 0) {
                folders.addAll(new ArrayList<>(Arrays.asList(files)));
                folders.addAll(new ArrayList<>(Arrays.asList(images)));
            }

            // remove duplicates
            Set<File> hs = new HashSet<>();
            hs.addAll(folders);
            folders.clear();
            folders.addAll(hs);

            sort();

            currentFolderPath.setText(dir.getAbsolutePath());
            adapter.notifyDataSetChanged();
        }
    }

    private void sort() {
        Collections.sort(folders, (file1, file2) -> {

            boolean isDirectory1 = file1.isDirectory();
            boolean isDirectory2 = file2.isDirectory();

            if (isDirectory1 && !isDirectory2)
                return -1;

            if (!isDirectory1 && isDirectory2)
                return 1;

            return file1.getName().toLowerCase(Locale.getDefault()).compareTo(file2.getName().toLowerCase(Locale.getDefault()));
        });
    }

    private final BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN)
                dismiss();
        }
        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
    };


    private class VolumeSpinnerAdapter extends ArrayAdapter<String> {

        VolumeSpinnerAdapter(Context context) {
            super(context, R.layout.dialog_directory_picker_spinner, R.id.spinner_volume_name);
            insert(getString(R.string.internal_storage), INTERNAL_STORAGE);
            if(sdCardPath != null)
                add(getString(R.string.extrnal_storage));
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            Drawable icon;

            switch (position){
                case INTERNAL_STORAGE:
                    icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_storage_grey_600_24dp);
                    break;
                default:
                    icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_sd_storage_grey_600_24dp);
                    break;
            }

            ((ImageView)view.findViewById(R.id.volume_image)).setImageDrawable(icon);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            Drawable icon;

            switch (position){
                case INTERNAL_STORAGE:
                    icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_storage_grey_600_24dp);
                break;
                default:
                    icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_sd_storage_grey_600_24dp);
                    break;
            }
            ((ImageView) view.findViewById(R.id.volume_image)).setImageDrawable(icon);
            return view;
        }
    }

    private class BottomSheetAlbumsAdapter extends RecyclerView.Adapter<BottomSheetAlbumsAdapter.ViewHolder> {

        BottomSheetAlbumsAdapter() { }

        public BottomSheetAlbumsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_directory_picker_item, parent, false);
            v.setOnClickListener(viewHolderClickListener);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final BottomSheetAlbumsAdapter.ViewHolder holder, final int position) {

            File f = folders.get(position);

            if (f.isFile()) {

                Picasso.with(getContext())
                        .load(f)
                        .config(Bitmap.Config.RGB_565)
                        .resize(100, 100)
                        .centerCrop()
                        .into(holder.imgFolder);

                holder.imgFolder.setTag(f.getPath());

            } else {
                holder.imgFolder.setImageResource(R.drawable.ic_folder_grey_600_24dp);
            }

            holder.folderName.setText(f.getName());
            holder.folderName.setTag(f.getPath());

            if(canGoBack() && position == 0) // go to parent folder
                holder.folderName.setText("..");

        }

        public int getItemCount() {
            return folders.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            final TextView folderName;
            final ImageView imgFolder;
            final LinearLayout llItemBackground;

            ViewHolder(View itemView) {
                super(itemView);

                folderName = itemView.findViewById(R.id.name_folder);
                imgFolder = itemView.findViewById(R.id.folder_icon_bottom_sheet_item);
                llItemBackground = itemView.findViewById(R.id.folder_item);
            }
        }

        private final View.OnClickListener viewHolderClickListener = view -> {

            String path = view.findViewById(R.id.name_folder).getTag().toString();

            File file = new File(path);

            if (!file.isFile()) {
                displayContentFolder(new File(path));
            } else {
                dismiss();
                onImageSelected.imageSelected(view.findViewById(R.id.folder_icon_bottom_sheet_item).getTag().toString());
            }
        };

    }

    public static  AlertDialog getInsertTextDialog(Activity activity, EditText editText, @StringRes int title) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        @SuppressLint("InflateParams") View dialogLayout = activity.getLayoutInflater().inflate(R.layout.dialog_directory_picker_insert_text, null);
        TextView textViewTitle = dialogLayout.findViewById(R.id.rename_title);

        textViewTitle.setText(title);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(layoutParams);
        editText.setSingleLine(true);

        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(editText, null);
        } catch (Exception ignored) { }

        ((RelativeLayout) dialogLayout.findViewById(R.id.container_edit_text)).addView(editText);

        dialogBuilder.setView(dialogLayout);
        return dialogBuilder.create();
    }

}

