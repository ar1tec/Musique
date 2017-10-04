package org.oucho.musicplayer.bugDroid;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

// bug android memory leaks inputMethodManager (api 21, 22, 23, 24, 25; +?)

public class DummyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Handler().postDelayed(this::finish, 500);
    }
}
