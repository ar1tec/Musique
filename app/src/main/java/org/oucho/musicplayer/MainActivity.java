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
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.widget.TimePicker;
import android.widget.Toast;


import org.oucho.musicplayer.PlayerService.PlaybackBinder;
import org.oucho.musicplayer.audiofx.AudioEffects;
import org.oucho.musicplayer.fragments.AlbumFragment;
import org.oucho.musicplayer.fragments.ArtistFragment;
import org.oucho.musicplayer.fragments.BaseFragment;
import org.oucho.musicplayer.fragments.LibraryFragment;
import org.oucho.musicplayer.images.ArtistImageCache;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.model.Album;
import org.oucho.musicplayer.model.Artist;
import org.oucho.musicplayer.model.Song;
import org.oucho.musicplayer.utils.GetAudioFocusTask;
import org.oucho.musicplayer.utils.NavigationUtils;
import org.oucho.musicplayer.utils.Notification;
import org.oucho.musicplayer.utils.PrefUtils;
import org.oucho.musicplayer.widgets.ProgressBar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
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
    public static final String ACTION_ADD_TO_QUEUE = "add_to_queue";
    private static final String ACTION_SET_AS_NEXT_TRACK = "set_as_next_track";

    private static final int SEARCH_ACTIVITY = 234;

    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE = 2;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    private final Handler mHandler = new Handler();
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;

    private Intent mOnActivityResultIntent;

    private PlayerService mPlaybackService;

    private boolean mServiceBound = false;
    private ProgressBar mProgressBar;

    private PlaybackRequests mPlaybackRequests;

    private static ScheduledFuture mTask;
    private static boolean running;


    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

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


        PrefUtils.init(this);
        ArtworkCache.init(this);
        ArtistImageCache.init(this);
        AudioEffects.init(this);

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
            case R.id.action_equalizer:
                NavigationUtils.showEqualizer(MainActivity.this);
                break;
            case R.id.action_radio:
                radio();
                return true;

            case R.id.action_sleep_timer:
                if (! running) {
                    showDatePicker();
                } else {
                    showTimerInfo();
                }
                break;
            case R.id.nav_help:
                about();
                return true;
            case R.id.nav_exit:
                mPlaybackService.stop();
                killNotif();
                clearCache();
                //finish();
                System.exit(0);
                break;
            default: //do nothing
                break;
        }
        return true;
    }



    /* **************************
     * Lance l'application radio
     * **************************/

    private void radio() {

        Context context = getApplicationContext();

        PackageManager pm = context.getPackageManager();
        Intent appStartIntent = pm.getLaunchIntentForPackage("org.oucho.radio");
        context.startActivity(appStartIntent);
        killNotif();
        clearCache();
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
                default: //do nothing
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
            default: //do nothing
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    /* ************************
     * Affiche la bibliothèque
     * ************************/

    private void showLibrary() {
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, LibraryFragment.newInstance()).commit();
    }


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
                assert quickButton != null;
                quickButton.setImageResource(R.drawable.musicplayer_pause);
            } else {
                assert quickButton != null;
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



    /******************************************************
     * Mise à jour des informations de la barre de lecture
     ******************************************************/

    @SuppressLint("PrivateResource")
    private void updateTrackInfo() {
        View trackInfoLayout = findViewById(R.id.track_info);

        if (mPlaybackService != null && mPlaybackService.hasPlaylist()) {

            assert trackInfoLayout != null;
            if (trackInfoLayout.getVisibility() != View.VISIBLE) {
                trackInfoLayout.setVisibility(View.VISIBLE);
                trackInfoLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.abc_grow_fade_in_from_bottom));
            }

            String title = mPlaybackService.getSongTitle();
            String artist = mPlaybackService.getArtistName();

            if (title != null) {
                //noinspection ConstantConditions
                ((TextView) findViewById(R.id.song_title)).setText(title);

            }

            if (artist != null) {
                //noinspection ConstantConditions
                ((TextView) findViewById(R.id.song_artist)).setText(artist);
            }

            int duration = mPlaybackService.getTrackDuration();

            if (duration != -1) {
                mProgressBar.setMax(duration);

                updateProgressBar();
            }

        } else {
            assert trackInfoLayout != null;
            trackInfoLayout.setVisibility(View.GONE);
        }
    }



    /***********************
     * Barre de progression
     ***********************/

    private void updateProgressBar() {
        if (mPlaybackService != null) {
            int position = mPlaybackService.getPlayerPosition();
            mProgressBar.setProgress(position);
        }
    }



    /***********************************************************************************************
     * Sleep Timer
     **********************************************************************************************/

    public void showDatePicker() {

        final String start = getString(R.string.start);
        final String cancel = getString(R.string.cancel);

        View view = getLayoutInflater().inflate(R.layout.date_picker_dialog, null);

        final TimePicker picker = (TimePicker) view.findViewById(R.id.time_picker);
        final Calendar cal = Calendar.getInstance();

        picker.setIs24HourView(true);

        picker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(start, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int hours;
                int mins;

                int hour = picker.getCurrentHour();
                int minute = picker.getCurrentMinute();
                int curHour = cal.get(Calendar.HOUR_OF_DAY);
                int curMin = cal.get(Calendar.MINUTE);

                if (hour < curHour) hours =  (24 - curHour) + (hour);
                else hours = hour - curHour;

                if (minute < curMin) {
                    hours--;
                    mins = (60 - curMin) + (minute);
                } else mins = minute - curMin;

                startTimer(hours, mins);
            }
        });

        builder.setNegativeButton(cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // This constructor is intentionally empty, pourquoi ? parce que !
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showTimerInfo() {

        final String continuer = getString(R.string.continuer);
        final String cancelTimer = getString(R.string.cancel_timer);


        if (mTask.getDelay(TimeUnit.MILLISECONDS) < 0) {
            stopTimer();
            return;
        }

        View view = getLayoutInflater().inflate(R.layout.timer_info_dialog, null);
        final TextView timeLeft = ((TextView) view.findViewById(R.id.time_left));

        final AlertDialog dialog = new AlertDialog.Builder(this).setPositiveButton(continuer, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setNegativeButton(cancelTimer, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopTimer();
            }
        }).setView(view).create();

        new CountDownTimer(mTask.getDelay(TimeUnit.MILLISECONDS), 1000) {
            @Override
            public void onTick(long seconds) {

                long secondes = seconds;

                secondes = secondes / 1000;
                timeLeft.setText(String.format(getString(R.string.timer_info), (secondes / 3600), ((secondes % 3600) / 60), ((secondes % 3600) % 60)));
            }

            @Override
            public void onFinish() {
                dialog.dismiss();
            }
        }.start();

        dialog.show();
    }

    public void startTimer(final int hours, final int minutes) {

        final String impossible = getString(R.string.impossible);
        final String activeTimer = getString(R.string.active_timer);


        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final int delay = ((hours * 3600) + (minutes * 60)) * 1000;

        if (delay == 0) {
            Toast.makeText(this, impossible, Toast.LENGTH_LONG).show();
            return;
        }

            mTask = scheduler.schedule(new GetAudioFocusTask(this), delay, TimeUnit.MILLISECONDS);
            Toast.makeText(this, activeTimer, Toast.LENGTH_LONG).show();

            running = true;
    }

    public static void stopTimer() {
        if (running) mTask.cancel(true);

        running = false;

    }



    /***********************************************************************************************
     * About dialog
     **********************************************************************************************/

    private void about() {

        String title = getString(R.string.about);
        AlertDialog.Builder about = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();

        @SuppressLint("InflateParams") View dialoglayout = inflater.inflate(R.layout.alertdialog_main_noshadow, null);
        Toolbar toolbar = (Toolbar) dialoglayout.findViewById(R.id.dialog_toolbar_noshadow);
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(0xffffffff);

        final TextView text = (TextView) dialoglayout.findViewById(R.id.showrules_dialog);
        text.setText(getString(R.string.about_message));

        about.setView(dialoglayout);

        AlertDialog dialog = about.create();
        dialog.show();
    }



    /***********************************************************************************************
     * Touche retour
     **********************************************************************************************/

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return true;
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

    /***********************************************************************************************
     * Purge du cache des images
     **********************************************************************************************/
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
            default: //do nothing
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

    public static class DialogUtils {

        public static void showPermissionDialog(Context context, String message, DialogInterface.OnClickListener listener) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.permission)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, listener)
                    .show();
        }


    }
}
