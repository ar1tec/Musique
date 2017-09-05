package org.oucho.musicplayer;


import android.net.Uri;

public interface MusiqueKeys {

    String FILTER = "filter";

    int PERMISSIONS_REQUEST_READ_PHONE_STATE = 2;
    int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    String APP_RADIO = "org.oucho.radio2";

    String INTENT_QUIT = "org.oucho.musicplayer.QUIT";
    String INTENT_STATE = "org.oucho.musicplayer.STATE";
    String INTENT_QUEUEVIEW = "org.oucho.musicplayer.QUEUEVIEW";
    String INTENT_LAYOUTVIEW = "org.oucho.musicplayer.LAYOUTVIEW";
    String INTENT_TOOLBAR_SHADOW = "org.oucho.musicplayer.SHADOW";

    String ACTION_NEXT = "org.oucho.musicplayer.ACTION_NEXT";
    String ACTION_STOP = "org.oucho.musicplayer.ACTION_STOP";
    String ACTION_PAUSE = "org.oucho.musicplayer.ACTION_PAUSE";
    String ACTION_TOGGLE = "org.oucho.musicplayer.ACTION_TOGGLE";
    String ACTION_PREVIOUS = "org.oucho.musicplayer.ACTION_PREVIOUS";
    String ACTION_CHOOSE_SONG = "org.oucho.musicplayer.ACTION_CHOOSE_SONG";

    String ITEM_ADDED = "org.oucho.musicplayer.ITEM_ADDED";
    String EXTRA_POSITION = "org.oucho.musicplayer.POSITION";
    String PREF_AUTO_PAUSE = "org.oucho.musicplayer.AUTO_PAUSE";

    String META_CHANGED = "org.oucho.musicplayer.META_CHANGED";
    String QUEUE_CHANGED = "org.oucho.musicplayer.QUEUE_CHANGED";
    String ORDER_CHANGED = "org.oucho.musicplayer.ORDER_CHANGED";
    String POSITION_CHANGED = "org.oucho.musicplayer.POSITION_CHANGED";
    String PLAYSTATE_CHANGED = "org.oucho.musicplayer.PLAYSTATE_CHANGED";
    String REPEAT_MODE_CHANGED = "org.oucho.musicplayer.REPEAT_MODE_CHANGED";

    String FICHIER_PREFS = "org.oucho.musicplayer_preferences";

    String STATE_PREFS_NAME = "PlayerState";

    Uri ARTWORK_URI = Uri.parse("content://media/external/audio/albumart");
}
