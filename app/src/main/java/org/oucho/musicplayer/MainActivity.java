package org.oucho.musicplayer;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.codetroopers.betterpickers.hmspicker.HmsPickerDialogFragment;

import org.oucho.musicplayer.PlayerService.PlaybackBinder;
import org.oucho.musicplayer.activities.BaseActivity;
import org.oucho.musicplayer.fragments.AlbumFragment;
import org.oucho.musicplayer.fragments.ArtistFragment;
import org.oucho.musicplayer.fragments.BaseFragment;
import org.oucho.musicplayer.fragments.LibraryFragment;
import org.oucho.musicplayer.fragments.PlaylistFragment;
import org.oucho.musicplayer.images.ArtistImageCache;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.model.Album;
import org.oucho.musicplayer.model.Artist;
import org.oucho.musicplayer.model.Song;
import org.oucho.musicplayer.utils.DialogUtils;
import org.oucho.musicplayer.utils.NavigationUtils;
import org.oucho.musicplayer.utils.Notification;
import org.oucho.musicplayer.utils.SleepTimer;
import org.oucho.musicplayer.widgets.ProgressBar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        NavigationView.OnNavigationItemSelectedListener {

    public static final String ALBUM_ID = "id";
    public static final String ALBUM_NAME = "name";
    public static final String ALBUM_ARTIST = "artist";
    public static final String ALBUM_YEAR = "year";
    public static final String ALBUM_TRACK_COUNT = "track_count";
    public static final String ARTIST_ARTIST_ID = "artist_id";
    public static final String ARTIST_ARTIST_NAME = "artist_name";
    public static final String ARTIST_ALBUM_COUNT = "album_count";
    public static final String ARTIST_TRACK_COUNT = "track_count";
    public static final String SONG_ID = "song_id";
    public static final String SONG_TITLE = "song_title";
    public static final String SONG_ARTIST = "song_artist";
    public static final String SONG_ALBUM = "song_album";
    public static final String SONG_ALBUM_ID = "song_album_id";
    public static final String SONG_TRACK_NUMBER = "song_track_number";
    private static final String ACTION_REFRESH = "resfresh";
    public static final String ACTION_SHOW_ALBUM = "show_album";
    public static final String ACTION_SHOW_ARTIST = "show_artist";
    public static final String ACTION_PLAY_SONG = "play_song";
    private static final String ACTION_ADD_TO_QUEUE = "add_to_queue";
    private static final String ACTION_SET_AS_NEXT_TRACK = "set_as_next_track";

    private static final int SEARCH_ACTIVITY = 234;

    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE = 2;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    private final Handler mHandler = new Handler();
    private final String fichier_préférence = "org.oucho.musicplayer_preferences";
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private boolean favorite = false;
    private SharedPreferences préférences = null;

    private Intent mOnActivityResultIntent;

    private PlayerService mPlaybackService;



    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mPlaybackRequests = new PlaybackRequests();

        findViewById(R.id.quick_play_pause_toggle).setOnClickListener(mOnClickListener);
        findViewById(R.id.track_info).setOnClickListener(mOnClickListener);
        findViewById(R.id.quick_prev).setOnClickListener(mOnClickListener);
        findViewById(R.id.quick_next).setOnClickListener(mOnClickListener);


        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        mNavigationView.setNavigationItemSelectedListener(this);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        if (savedInstanceState == null) {
            showLibrary();
        }


    }

    /* *********************************************************************************************
     * Navigation Drawer
     * ********************************************************************************************/

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        mDrawerLayout.closeDrawers();
        switch (menuItem.getItemId()) {
            case R.id.action_library:
                showLibrary();
                break;
/*            case R.id.action_favorites:
                showFavorites();
                break;*/
            case R.id.action_equalizer:
                NavigationUtils.showEqualizer(MainActivity.this);
                break;
            case R.id.action_radio:
                Radio();
                return true;
            case R.id.action_theme:
                NavigationUtils.showTheme(MainActivity.this);
                break;
            case R.id.nav_help:
                About();
                return true;
            case R.id.nav_exit:
                mPlaybackService.stop();
                killNotif();
                System.exit(0);
                break;
        }
        return true;
    }



    /* **************************
     * Lance l'application radio
     * **************************/

    private void Radio() {
        Context context = getApplicationContext();
        PackageManager pm = context.getPackageManager();
        Intent appStartIntent = pm.getLaunchIntentForPackage("org.oucho.radio");
        context.startActivity(appStartIntent);
        clearCache();
        killNotif();
        finish();
    }



    /* *********************************************************************************************
     * Click listener activity_main layout
     * ********************************************************************************************/

    private final OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            if (mPlaybackService == null) {
                return;
            }
            switch (v.getId()) {
                case R.id.play_pause_toggle:
                case R.id.quick_play_pause_toggle:
                    mPlaybackService.toggle();
                    break;
                case R.id.quick_prev:
                case R.id.prev:
                    mPlaybackService.playPrev(true);
                    break;
                case R.id.quick_next:
                case R.id.next:
                    mPlaybackService.playNext(true);
                    break;
                case R.id.action_equalizer:
                    NavigationUtils.showEqualizer(MainActivity.this);
                    break;
                case R.id.track_info:
                    NavigationUtils.showPlaybackActivity(MainActivity.this, true);
                    break;
            }
        }
    };



    /* *********************************************************************************************
     * Menu
     * ********************************************************************************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    showLibrary();
                }
                return true;
            case R.id.action_search:
                NavigationUtils.showSearchActivity(this, SEARCH_ACTIVITY);
                return true;
            case R.id.action_equalizer:
                NavigationUtils.showEqualizer(this);
                return true;
            case R.id.action_sleep_timer:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if (SleepTimer.isTimerSet(prefs)) {
                    DialogUtils.showSleepTimerDialog(this, mSleepTimerDialogListener);
                } else {
                    DialogUtils.showSleepHmsPicker(this, mHmsPickerHandler);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    /* ************************
     * Affiche la bibliothèque
     * ************************/

    private void showLibrary() {
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        mNavigationView.getMenu().findItem(R.id.action_library);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, LibraryFragment.newInstance()).commit();
    }



    /* *********************
     * Afficher les favoris
     * *********************/

/*    private void showFavorites() {
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        mNavigationView.getMenu().findItem(R.id.action_favorites);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, PlaylistFragment.newFavoritesFragment()).commit();
        setTitle("Favoris");

        favorite = true;
    }*/


    /* *********************************************************************************************
     * Pause, resume etc.
     * ********************************************************************************************/

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onPause() {
        super.onPause();

        killNotif();
        clearCache();

        if (mServiceBound) {
            mPlaybackService = null;

            unregisterReceiver(mServiceListener);

            unbindService(mServiceConnection);
            mServiceBound = false;
        }
        mHandler.removeCallbacks(mUpdateProgressBar);

        préférences = getSharedPreferences(fichier_préférence, MODE_PRIVATE);

        SharedPreferences.Editor editor = préférences.edit();

        editor.putBoolean("favorite_state", favorite);
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mServiceBound) {
            Intent mServiceIntent = new Intent(this, PlayerService.class);
            bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            startService(mServiceIntent);
            IntentFilter filter = new IntentFilter();
            filter.addAction(PlayerService.META_CHANGED);
            filter.addAction(PlayerService.PLAYSTATE_CHANGED);
            filter.addAction(PlayerService.POSITION_CHANGED);
            filter.addAction(PlayerService.ITEM_ADDED);
            filter.addAction(PlayerService.ORDER_CHANGED);
            registerReceiver(mServiceListener, filter);
        } else {
            updateAll();
        }

        préférences = getSharedPreferences(fichier_préférence, MODE_PRIVATE);
        favorite = préférences.getBoolean("favorite_state", false);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mOnActivityResultIntent != null) {
            Bundle bundle = mOnActivityResultIntent.getExtras();
            if (mOnActivityResultIntent.getAction().equals(ACTION_REFRESH)) {
                refresh();
            } else if (mOnActivityResultIntent.getAction().equals(ACTION_SHOW_ALBUM)) {
                Album album = getAlbumFromBundle(bundle);
                AlbumFragment fragment = AlbumFragment.newInstance(album);
                setFragment(fragment);
            } else if (mOnActivityResultIntent.getAction().equals(ACTION_SHOW_ARTIST)) {
                Artist artist = getArtistFromBundle(bundle);
                ArtistFragment fragment = ArtistFragment.newInstance(artist);
                setFragment(fragment);
            } else {


                Song song = getSongFromBundle(bundle);

                if (mOnActivityResultIntent.getAction().equals(ACTION_PLAY_SONG)) {
                    ArrayList<Song> songList = new ArrayList<>();
                    songList.add(song);
                    mPlaybackRequests.requestPlayList(songList);
                } else if (mOnActivityResultIntent.getAction().equals(ACTION_ADD_TO_QUEUE)) {
                    mPlaybackRequests.requestAddToQueue(song);
                } else if (mOnActivityResultIntent.getAction().equals(ACTION_SET_AS_NEXT_TRACK)) {
                    mPlaybackRequests.requestAsNextTrack(song);
                }
            }
            mOnActivityResultIntent = null;
        }
    }

    public void setFragment(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, f).addToBackStack(null).commit();
    }

    private Album getAlbumFromBundle(Bundle bundle) {
        long id = bundle.getLong(ALBUM_ID);
        String title = bundle.getString(ALBUM_NAME);
        String artist = bundle.getString(ALBUM_ARTIST);
        int year = bundle.getInt(ALBUM_YEAR);
        int trackCount = bundle.getInt(ALBUM_TRACK_COUNT);

        return new Album(id, title, artist, year, trackCount);
    }



    private Artist getArtistFromBundle(Bundle bundle) {
        long id = bundle.getLong(ARTIST_ARTIST_ID);
        String name = bundle.getString(ARTIST_ARTIST_NAME);
        int albumCount = bundle.getInt(ARTIST_ALBUM_COUNT);
        int trackCount = bundle.getInt(ARTIST_TRACK_COUNT);
        return new Artist(id, name, albumCount, trackCount);
    }

    private Song getSongFromBundle(Bundle bundle) {
        long id = bundle.getLong(SONG_ID);
        String title = bundle.getString(SONG_TITLE);
        String artist = bundle.getString(SONG_ARTIST);
        String album = bundle.getString(SONG_ALBUM);
        long albumId = bundle.getLong(SONG_ALBUM_ID);
        int trackNumber = bundle.getInt(SONG_TRACK_NUMBER);

        return new Song(id, title, artist, album, albumId, trackNumber);
    }

    public void refresh() {
        for (Fragment f : getSupportFragmentManager().getFragments()) {
            if (f != null) {
                ((BaseFragment) f).load();
            }
        }
    }



    /***********************************************************************************************
     * Lecture
     **********************************************************************************************/

    private boolean mServiceBound = false;
    private ProgressBar mProgressBar;

    private final Runnable mUpdateProgressBar = new Runnable() {

        @Override
        public void run() {

            updateProgressBar();

            mHandler.postDelayed(mUpdateProgressBar, 1000);

        }
    };

    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPlaybackService == null) {
                return;
            }
            String action = intent.getAction();

            if (action.equals(PlayerService.PLAYSTATE_CHANGED)) {
                setButtonDrawable();
                if (mPlaybackService.isPlaying()) {
                    mHandler.post(mUpdateProgressBar);
                } else {
                    mHandler.removeCallbacks(mUpdateProgressBar);
                }


            } else if (action.equals(PlayerService.META_CHANGED)) {
                updateTrackInfo();
            }
        }
    };

    private PlaybackRequests mPlaybackRequests;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            PlayerService.PlaybackBinder binder = (PlaybackBinder) service;
            mPlaybackService = binder.getService();
            mServiceBound = true;

            mPlaybackRequests.sendRequests();

            updateAll();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;

        }
    };

    public void onSongSelected(List<Song> songList, int position) {
        if (mPlaybackService == null) {
            return;
        }
        mPlaybackService.setPlayList(songList, position, true);
        // mPlaybackService.play();
    }

    public void onShuffleRequested(List<Song> songList, boolean play) {
        if (mPlaybackService == null) {
            return;
        }
        mPlaybackService.setPlayListAndShuffle(songList, play);


    }

    public void addToQueue(Song song) {
        if (mPlaybackService != null) {
            mPlaybackService.addToQueue(song);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SEARCH_ACTIVITY && resultCode == RESULT_OK) {
            mOnActivityResultIntent = data;
        }
    }


    private void updateAll() {
        if (mPlaybackService != null) {
            updateTrackInfo();
            setButtonDrawable();
            if (mPlaybackService.isPlaying()) {
                mHandler.post(mUpdateProgressBar);
            }
        }
    }



    /***********************************************************************************************
     * Barre de lecture
     **********************************************************************************************/

    private void setButtonDrawable() {
        if (mPlaybackService != null) {
            ImageButton quickButton = (ImageButton) findViewById(R.id.quick_play_pause_toggle);
            if (mPlaybackService.isPlaying()) {
                quickButton.setImageResource(R.drawable.musicplayer_pause);
            } else {
                quickButton.setImageResource(R.drawable.musicplayer_play);
            }
        }
    }


    private class PlaybackRequests {

        private List<Song> mPlayList;
        private int mIndex;
        private boolean mAutoPlay;

        private Song mNextTrack;

        private Song mAddToQueue;

        private void requestPlayList(List<Song> playList) {
            if (mPlaybackService != null) {
                mPlaybackService.setPlayList(playList, 0, true);
            } else {
                mPlayList = playList;
                mIndex = 0;
                mAutoPlay = true;
            }
        }

        public void requestAddToQueue(Song song) {
            if (mPlaybackService != null) {
                mPlaybackService.addToQueue(song);
            } else {
                mAddToQueue = song;
            }
        }

        public void requestAsNextTrack(Song song) {
            if (mPlaybackService != null) {
                mPlaybackService.setAsNextTrack(song);
            } else {
                mNextTrack = song;
            }
        }

        public void sendRequests() {
            if (mPlaybackService == null) {
                return;
            }

            if (mPlayList != null) {
                mPlaybackService.setPlayList(mPlayList, mIndex, mAutoPlay);
                mPlayList = null;
            }

            if (mAddToQueue != null) {
                mPlaybackService.addToQueue(mAddToQueue);
                mAddToQueue = null;
            }

            if (mNextTrack != null) {
                mPlaybackService.setAsNextTrack(mNextTrack);
                mNextTrack = null;
            }
        }
    }


    @SuppressLint("PrivateResource")
    private void updateTrackInfo() {
        View trackInfoLayout = findViewById(R.id.track_info);

        if (mPlaybackService != null && mPlaybackService.hasPlaylist()) {

            if (trackInfoLayout.getVisibility() != View.VISIBLE) {
                trackInfoLayout.setVisibility(View.VISIBLE);
                trackInfoLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.abc_grow_fade_in_from_bottom));
            }

            String title = mPlaybackService.getSongTitle();
            String artist = mPlaybackService.getArtistName();

            if (title != null) {
                ((TextView) findViewById(R.id.song_title)).setText(title);

            }

            if (artist != null) {
                ((TextView) findViewById(R.id.song_artist)).setText(artist);
            }

            int duration = mPlaybackService.getTrackDuration();

            if (duration != -1) {
                mProgressBar.setMax(duration);

                updateProgressBar();
            }

        } else {
            trackInfoLayout.setVisibility(View.GONE);
        }
    }

    private void updateProgressBar() {
        if (mPlaybackService != null) {
            int position = mPlaybackService.getPlayerPosition();
            mProgressBar.setProgress(position);
        }
    }



    /***********************************************************************************************
     * Sleep Timer
     **********************************************************************************************/

    private final HmsPickerDialogFragment.HmsPickerDialogHandler mHmsPickerHandler
            = new HmsPickerDialogFragment.HmsPickerDialogHandler() {
        @Override
        public void onDialogHmsSet(int reference, int hours, int minutes, int seconds) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            SleepTimer.setTimer(MainActivity.this, prefs, hours * 3600 + minutes * 60 + seconds);
        }
    };

    private final DialogInterface.OnClickListener mSleepTimerDialogListener
            = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    DialogUtils.showSleepHmsPicker(MainActivity.this, mHmsPickerHandler);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    SleepTimer.cancelTimer(MainActivity.this, prefs);
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    break;
            }
        }
    };



    /***********************************************************************************************
     * About dialog
     **********************************************************************************************/

    private void About() {

        String title = getString(R.string.about);
        AlertDialog.Builder About = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();

        @SuppressLint("InflateParams") View dialoglayout = inflater.inflate(R.layout.alertdialog_main_noshadow, null);
        Toolbar toolbar = (Toolbar) dialoglayout.findViewById(R.id.dialog_toolbar_noshadow);
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(0xffffffff);

        final TextView text = (TextView) dialoglayout.findViewById(R.id.showrules_dialog);
        text.setText(getString(R.string.about_message));

        About.setView(dialoglayout);

        AlertDialog dialog = About.create();
        dialog.show();
    }



    /***********************************************************************************************
     * Touche retour
     **********************************************************************************************/

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }


        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (favorite) {
                favorite = false;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, LibraryFragment.newInstance())
                        .commit();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }



    /***********************************************************************************************
     * Fermeture notification
     **********************************************************************************************/

    private void killNotif() {
        NotificationManager notificationManager;

        try {
            if (!mPlaybackService.isPlaying()) {
                notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancel(Notification.NOTIFY_ID);
            }
        } catch (RuntimeException ignore){}
    }

    private void clearCache() {
        ArtistImageCache.getInstance().clear();
        ArtworkCache.getInstance().clear();
    }



    /* *********************************************************************************************
    * Gestion des permissions (Android >= 6.0)
    * *********************************************************************************************/

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String permissions[],
            @NonNull int[] grantResults)
    {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_PHONE_STATE:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(PlayerService.PREF_AUTO_PAUSE, true);
                if (mPlaybackService != null) {
                    mPlaybackService.setAutoPauseEnabled(true);
                }
                editor.commit();
                break;
        }
    }


    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                DialogUtils.showPermissionDialog(this, getString(R.string.permission_read_external_storage), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });

            } else {


                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                DialogUtils.showPermissionDialog(this, getString(R.string.permission_write_external_storage), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    }
                });

            } else {


                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {

                DialogUtils.showPermissionDialog(this, getString(R.string.permission_read_phone_state), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_PHONE_STATE},
                                PERMISSIONS_REQUEST_READ_PHONE_STATE);
                    }
                });

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        PERMISSIONS_REQUEST_READ_PHONE_STATE);
            }
        }
    }



    /* *********************************************************************************************
     * Thème
     * ********************************************************************************************/

    // recharge pour appliquer la nouvelle couleur de thème
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        recreate();
    }

    private void setTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int theme = prefs.getInt(BaseActivity.KEY_PREF_THEME, BaseActivity.DEFAULT_THEME);

        switch (theme) {
            case BaseActivity.original_green:
                setTheme(R.style.MainActivityOGreenLight);
                break;
            case BaseActivity.red:
                setTheme(R.style.MainActivityRedLight);
                break;
            case BaseActivity.orange:
                setTheme(R.style.MainActivityOrangeLight);
                break;
            case BaseActivity.purple:
                setTheme(R.style.MainActivityPurpleLight);
                break;
            case BaseActivity.navy:
                setTheme(R.style.MainActivityNavyLight);
                break;
            case BaseActivity.blue:
                setTheme(R.style.MainActivityBlueLight);
                break;
            case BaseActivity.sky:
                setTheme(R.style.MainActivitySkyLight);
                break;
            case BaseActivity.seagreen:
                setTheme(R.style.MainActivitySeagreenLight);
                break;
            case BaseActivity.cyan:
                setTheme(R.style.MainActivityCyanLight);
                break;
            case BaseActivity.pink:
                setTheme(R.style.MainActivityPinkLight);
                break;
        }
    }

}
