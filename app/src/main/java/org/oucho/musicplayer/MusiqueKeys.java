package org.oucho.musicplayer;


public interface MusiqueKeys {


    int SEARCH_ACTIVITY = 42;

    String FILTER = "filter";


    int PERMISSIONS_REQUEST_READ_PHONE_STATE = 2;
    int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    String APP_RADIO = "org.oucho.radio2";

    String INTENT_QUIT = "org.oucho.musicplayer.QUIT";
    String INTENT_STATE = "org.oucho.musicplayer.STATE";
    String INTENT_QUEUEVIEW = "org.oucho.musicplayer.QUEUEVIEW";
    String INTENT_LAYOUTVIEW = "org.oucho.musicplayer.LAYOUTVIEW";
    String INTENT_TOOLBAR8_SHADOW = "org.oucho.musicplayer.SHADOW";

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


    String fichier_préférence = "org.oucho.musicplayer_preferences";

    String STATE_PREFS_NAME = "PlayerState";

    String ARTIST_ARTIST_ID = "artist_id";
    String ARTIST_ARTIST_NAME = "artist_name";
    String ARTIST_ALBUM_COUNT = "album_count";
    String ARTIST_TRACK_COUNT = "track_count";

    String ALBUM_ID = "id";
    String ALBUM_NAME = "name";
    String ALBUM_ARTIST = "artist";
    String ALBUM_YEAR = "year";
    String ALBUM_TRACK_COUNT = "track_count";

    String SONG_ID = "song_id";
    String SONG_YEAR = "song_year";
    String SONG_TITLE = "song_title";
    String SONG_ALBUM = "song_album";
    String SONG_ARTIST = "song_artist";
    String SONG_ALBUM_ID = "song_album_id";
    String SONG_DURATION = "song_duration";
    String SONG_TRACK_NUMBER = "song_track_number";

    String ACTION_REFRESH = "resfresh";
    String ACTION_PLAY_SONG = "play_song";
    String ACTION_SHOW_ALBUM = "show_album";
    String ACTION_SHOW_ARTIST = "show_artist";
    String ACTION_ADD_TO_QUEUE = "add_to_queue";

    String ACTION_SET_AS_NEXT_TRACK = "set_as_next_track";
}
