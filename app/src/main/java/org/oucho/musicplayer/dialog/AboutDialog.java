package org.oucho.musicplayer.dialog;


import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oucho.musicplayer.BuildConfig;
import org.oucho.musicplayer.R;

public class AboutDialog extends DialogFragment {

    private final ViewGroup nullParent = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String title = getString(R.string.about);

        AlertDialog.Builder about = new AlertDialog.Builder(getActivity());

        View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_main_noshadow, nullParent);
        Toolbar toolbar = rootView.findViewById(R.id.dialog_toolbar_noshadow);
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(0xffffffff);


        String versionName = BuildConfig.VERSION_NAME;

        String version = getContext().getResources().getString(R.string.about_message, versionName);


        final TextView text = rootView.findViewById(R.id.showrules_dialog);
        text.setText(version);

        about.setView(rootView);


        return about.create();
    }


}
