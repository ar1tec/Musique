package org.oucho.musicplayer.update;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.update.enums.Display;
import org.oucho.musicplayer.update.enums.Duration;
import org.oucho.musicplayer.update.objects.Update;

public class AppUpdater {
    private final Context context;
    private final LibraryPreferences libraryPreferences;
    private Display display;
    private String xmlUrl;
    private final Integer showEvery;
    private Boolean showAppUpdated;
    private final String titleUpdate;
    private final String btnDismiss;
    private final String btnUpdate;
    private final String titleNoUpdate;

    public AppUpdater(Context context) {
        this.context = context;
        this.libraryPreferences = new LibraryPreferences(context);
        this.display = Display.DIALOG;
        this.showEvery = 1;
        this.showAppUpdated = false;

        // Dialog
        this.titleUpdate = context.getResources().getString(R.string.appupdater_update_available);
        this.titleNoUpdate = context.getResources().getString(R.string.appupdater_update_not_available);
        this.btnUpdate = context.getResources().getString(R.string.appupdater_btn_update);
        this.btnDismiss = context.getResources().getString(R.string.appupdater_btn_dismiss);
    }

    public AppUpdater setDisplay(Display display) {
        this.display = display;
        return this;
    }

    @SuppressWarnings("SameParameterValue")
    public AppUpdater setUpdateXML(@NonNull String xmlUrl) {
        this.xmlUrl = xmlUrl;
        return this;
    }

    public AppUpdater showAppUpdated() {
        this.showAppUpdated = true;
        return this;
    }

    @SuppressWarnings("unused")
    public AppUpdater init() {
        start();
        return this;
    }

    /**
     * Execute AppUpdater in background.
     */
    public void start() {
        UtilsAsync.LatestAppVersion latestAppVersion = new UtilsAsync.LatestAppVersion(context, false, xmlUrl, new LibraryListener() {
            @Override
            public void onSuccess(Update update) {
                if (UtilsLibrary.isUpdateAvailable(UtilsLibrary.getAppInstalledVersion(context), update.getLatestVersion())) {
                    Integer successfulChecks = libraryPreferences.getSuccessfulChecks();
                    if (UtilsLibrary.isAbleToShow(successfulChecks, showEvery)) {
                        switch (display) {
                            case DIALOG:
                                UtilsDisplay.showUpdateAvailableDialog(context, titleUpdate, getDescriptionUpdate(context, update, Display.DIALOG), btnDismiss, btnUpdate, update.getUrlToDownload());
                                break;
                            case SNACKBAR:
                                UtilsDisplay.showUpdateAvailableSnackbar(context, getDescriptionUpdate(context, update, Display.SNACKBAR), UtilsLibrary.getDurationEnumToBoolean(Duration.NORMAL), update.getUrlToDownload());
                                break;
                        }
                    }
                    libraryPreferences.setSuccessfulChecks(successfulChecks + 1);
                } else if (showAppUpdated) {
                    switch (display) {
                        case DIALOG:
                            UtilsDisplay.showUpdateNotAvailableDialog(context, titleNoUpdate, getDescriptionNoUpdate(context));
                            break;
                        case SNACKBAR:
                            UtilsDisplay.showUpdateNotAvailableSnackbar(context, getDescriptionNoUpdate(context), UtilsLibrary.getDurationEnumToBoolean(Duration.NORMAL));
                            break;
                    }
                }
            }

            @Override
            public void onFailed() {

                    throw new IllegalArgumentException("XML file is not valid!");
            }
        });

        latestAppVersion.execute();
    }

    interface LibraryListener {
        void onSuccess(Update update);

        void onFailed();
    }

    private String getDescriptionUpdate(Context context, Update update, Display display) {

            switch (display) {
                case DIALOG:
                    if (!TextUtils.isEmpty(update.getReleaseNotes()))
                        return String.format(context.getResources().getString(R.string.appupdater_update_available_description_dialog_before_release_notes), update.getLatestVersion(), update.getReleaseNotes());
                    else
                        return String.format(context.getResources().getString(R.string.appupdater_update_available_description_dialog), update.getLatestVersion(), UtilsLibrary.getAppName(context));

                case SNACKBAR:
                    return String.format(context.getResources().getString(R.string.appupdater_update_available_description_snackbar), update.getLatestVersion());
            }

        return null;
    }

    private String getDescriptionNoUpdate(Context context) {

            return String.format(context.getResources().getString(R.string.appupdater_update_not_available_description), UtilsLibrary.getAppName(context));
    }

}
