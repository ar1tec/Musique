package org.oucho.musicplayer.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import org.oucho.musicplayer.R;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme();
        super.onCreate(savedInstanceState);
    }

    public static final int original_green = 1;
    public static final int red = 2;
    public static final int orange = 3;
    public static final int purple = 4;
    public static final int navy = 5;
    public static final int blue = 6;
    public static final int sky = 7;
    public static final int seagreen = 8;
    public static final int cyan = 9;
    public static final int pink = 10;


    private void setTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int theme = prefs.getInt("pref_theme", 0);

        switch (theme) {
            case original_green:
                    setTheme(R.style.AppThemeOGreenLight);
                break;
            case red:
                    setTheme(R.style.AppThemeRedLight);
                break;
            case orange:
                setTheme(R.style.AppThemeOrangeLight);
                break;
            case purple:
                setTheme(R.style.AppThemePurpleLight);
                break;
            case navy:
                setTheme(R.style.AppThemeNavyLight);
                break;
            case blue:
                setTheme(R.style.AppThemeBlueLight);
                break;
            case sky:
                setTheme(R.style.AppThemeSkyLight);
                break;
            case seagreen:
                setTheme(R.style.AppThemeSeagreenLight);
                break;
            case cyan:
                setTheme(R.style.AppThemeCyanLight);
                break;
            case pink:
                setTheme(R.style.AppThemePinkLight);
                break;
        }
    }

    static String getColor(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int theme = prefs.getInt("pref_theme", 0);

        String Couleur = "";
        switch (theme) {
            case original_green:
                Couleur = "14b68e";
                break;
            case red:
                Couleur = "a50916";
                break;
            case orange:
                Couleur = "fd7c08";
                break;
            case purple:
                Couleur = "5b1588";
                break;
            case navy:
                Couleur = "303aa6";
                break;
            case blue:
                Couleur = "175fc9";
                break;
            case sky:
                Couleur = "19729a";
                break;
            case seagreen:
                Couleur = "239388";
                break;
            case cyan:
                Couleur = "138d3a";
                break;
            case pink:
                Couleur = "ff4381";
                break;
        }

        return Couleur;
    }

}
