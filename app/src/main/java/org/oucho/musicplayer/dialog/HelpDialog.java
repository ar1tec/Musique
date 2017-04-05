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

import org.oucho.musicplayer.R;

public class HelpDialog extends DialogFragment {

    private final ViewGroup nullParent = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String title = getString(R.string.help);

        AlertDialog.Builder about = new AlertDialog.Builder(getActivity());

        View dialoglayout = getActivity().getLayoutInflater().inflate(R.layout.alertdialog_main_noshadow, nullParent);
        Toolbar toolbar = (Toolbar) dialoglayout.findViewById(R.id.dialog_toolbar_noshadow);
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(0xffffffff);

        final TextView text = (TextView) dialoglayout.findViewById(R.id.showrules_dialog);
        text.setText(getString(R.string.help_message));

        about.setView(dialoglayout);


        return about.create();
    }


}
