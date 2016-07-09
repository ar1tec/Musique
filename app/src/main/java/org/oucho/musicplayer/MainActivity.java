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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.oucho.musicplayer.PlayerService.PlaybackBinder;
import org.oucho.musicplayer.audiofx.AudioEffects;
import org.oucho.musicplayer.dialog.AboutDialog;
import org.oucho.musicplayer.fragments.AlbumFragment;
import org.oucho.musicplayer.fragments.ArtistFragment;
import org.oucho.musicplayer.fragments.BaseFragment;
import org.oucho.musicplayer.fragments.LibraryFragment;
import org.oucho.musicplayer.images.ArtistImageCache;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.model.Album;
import org.oucho.musicplayer.model.Artist;
import org.oucho.musicplayer.model.Song;
import org.oucho.musicplayer.update.CheckUpdate;
import org.oucho.musicplayer.utils.GetAudioFocusTask;
import org.oucho.musicplayer.utils.NavigationUtils;
import org.oucho.musicplayer.utils.Notification;
import org.oucho.musicplayer.utils.PrefUtils;
import org.oucho.musicplayer.utils.SeekArc;
import org.oucho.musicplayer.widgets.ProgressBar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        OnNavigationItemSelectedListener {

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
    public static final String SONG_ALBUM = "song_album";
    public static final String SONG_ARTIST = "song_artist";
    public static final String SONG_ALBUM_ID = "song_album_id";
    public static final String SONG_DURATION = "song_duration";
    public static final String SONG_TRACK_NUMBER = "song_track_number";

    public static final String ACTION_REFRESH = "resfresh";
    public static final String ACTION_PLAY_SONG = "play_song";
    public static final String ACTION_SHOW_ALBUM = "show_album";
    public static final String ACTION_SHOW_ARTIST = "show_artist";
    public static final String ACTION_ADD_TO_QUEUE = "add_to_queue";
    public static final String ACTION_SET_AS_NEXT_TRACK = "set_as_next_track";

    public static final int SEARCH_ACTIVITY = 234;

    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE = 2;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    private final Handler mHandler = new Handler();
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;

    private Intent mOnActivityResultIntent;

    private PlayerService mPlayerService;

    private boolean mServiceBound = false;
    private ProgressBar mProgressBar;

    private PlaybackRequests mPlaybackRequests;

    private static ScheduledFuture mTask;
    private static boolean running;

    private final Handler handler = new Handler();


    private static final String intent_state = "org.oucho.musicplayer.STATE";

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

        CheckUpdate.onStart(this);

    }

    /* *********************************************************************************************
     * Navigation Drawer
     * ********************************************************************************************/

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {

        handler.postDelayed(new Runnable() {
            public void run() {
                mDrawerLayout.closeDrawers();
            }
        }, 300);

        switch (menuItem.getItemId()) {
            case R.id.action_equalizer:
                NavigationUtils.showEqualizer(MainActivity.this);
                break;

            case R.id.action_radio:
                radio();
                break;


            case R.id.nav_update:
                CheckUpdate.withInfo(this);
                break;

            case R.id.action_sleep_timer:
                if (! running) {
                    showTimePicker();
                } else {
                    showTimerInfo();
                }
                break;

            case R.id.nav_help:
                showAboutDialog();
                break;

            case R.id.nav_exit:
                exit();
                break;

            default:
                break;
        }
        return true;
    }

    private void exit() {

        if (running)
            annulTimer();

        if (mPlayerService.isPlaying())
            mPlayerService.toggle();

        killNotif();
        clearCache();
        finish();
    }


    /* **************************
     * Lance l'application radio
     * **************************/

    private void radio() {

        Context context = getApplicationContext();

        PackageManager pm = context.getPackageManager();
        Intent appStartIntent = pm.getLaunchIntentForPackage("org.oucho.radio2");
        context.startActivity(appStartIntent);
        killNotif();
    }


    /**************
     * About dialog
     **************/

    private void showAboutDialog(){
        AboutDialog dialog = new AboutDialog();
        dialog.show(getSupportFragmentManager(), "about");
    }



    /* *********************************************************************************************
     * Click listener activity_main layout
     * ********************************************************************************************/

    private final OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            if (mPlayerService == null) {
                return;
            }
            switch (v.getId()) {
                case R.id.play_pause_toggle:
                case R.id.quick_play_pause_toggle:
                    mPlayerService.toggle();

                    Intent intentPl = new Intent(intent_state);
                    intentPl.putExtra("state", "play");
                    sendBroadcast(intentPl);

                    break;

                case R.id.quick_prev:
                case R.id.prev:
                    mPlayerService.playPrev();

                    Intent intentP = new Intent(intent_state);
                    intentP.putExtra("state", "prev");
                    sendBroadcast(intentP);

                    break;

                case R.id.quick_next:
                case R.id.next:
                    mPlayerService.playNext(true);

                    Intent intentN = new Intent(intent_state);
                    intentN.putExtra("state", "next");
                    sendBroadcast(intentN);

                    break;

                case R.id.action_equalizer:
                    NavigationUtils.showEqualizer(MainActivity.this);
                    break;

                case R.id.track_info:
                    NavigationUtils.showPlaybackActivity(MainActivity.this);
                    break;

                default:
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
                NavigationUtils.showSearchActivity(this);
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    /* ************************
     * Affiche la bibliothèque
     * ************************/

    private void showLibrary() {
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, LibraryFragment.newInstance())
                .commit();
    }


    /* *********************************************************************************************
     * Pause, resume etc.
     * ********************************************************************************************/

    @Override
    protected void onPause() {
        super.onPause();

        if (!mPlayerService.isPlaying())
            killNotif();

        if (mServiceBound) {
            mPlayerService = null;

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
        int duration = bundle.getInt(SONG_DURATION);

        return new Song(id, title, artist, album, albumId, trackNumber, duration);
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
            if (mPlayerService == null) {
                return;
            }
            String action = intent.getAction();

            if (action.equals(PlayerService.PLAYSTATE_CHANGED)) {
                setButtonDrawable();
                if (mPlayerService.isPlaying()) {
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
            mPlayerService = binder.getService();
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
        if (mPlayerService == null) {
            return;
        }
        mPlayerService.setPlayList(songList, position, true);
        // mPlayerService.play();
    }

    public void addToQueue(Song song) {
        if (mPlayerService != null) {
            mPlayerService.addToQueue(song);
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
        if (mPlayerService != null) {
            updateTrackInfo();
            setButtonDrawable();
            if (mPlayerService.isPlaying()) {
                mHandler.post(mUpdateProgressBar);
            }
        }
    }



    /***********************************************************************************************
     * Barre de lecture
     **********************************************************************************************/

    private void setButtonDrawable() {
        if (mPlayerService != null) {
            ImageButton quickButton = (ImageButton) findViewById(R.id.quick_play_pause_toggle);
            if (mPlayerService.isPlaying()) {
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
            if (mPlayerService != null) {
                mPlayerService.setPlayList(playList, 0, true);
            } else {
                mPlayList = playList;
                mIndex = 0;
                mAutoPlay = true;
            }
        }

        public void requestAddToQueue(Song song) {
            if (mPlayerService != null) {
                mPlayerService.addToQueue(song);
            } else {
                mAddToQueue = song;
            }
        }

        public void requestAsNextTrack(Song song) {
            if (mPlayerService != null) {
                mPlayerService.setAsNextTrack(song);
            } else {
                mNextTrack = song;
            }
        }

        public void sendRequests() {
            if (mPlayerService == null) {
                return;
            }

            if (mPlayList != null) {
                mPlayerService.setPlayList(mPlayList, mIndex, mAutoPlay);
                mPlayList = null;
            }

            if (mAddToQueue != null) {
                mPlayerService.addToQueue(mAddToQueue);
                mAddToQueue = null;
            }

            if (mNextTrack != null) {
                mPlayerService.setAsNextTrack(mNextTrack);
                mNextTrack = null;
            }
        }
    }



    /******************************************************
     * Mise à jour des informations de la barre de lecture
     ******************************************************/

    @SuppressLint("PrivateResource")
    private void updateTrackInfo() {

        String title = mPlayerService.getSongTitle();
        String artist = mPlayerService.getArtistName();


        if (title != null) {
            //noinspection ConstantConditions
            ((TextView) findViewById(R.id.song_title)).setText(title);
        }

        if (artist != null) {
            //noinspection ConstantConditions
            ((TextView) findViewById(R.id.song_artist)).setText(artist);
        }

        int duration = mPlayerService.getTrackDuration();

        if (duration != -1) {
            mProgressBar.setMax(duration);

            updateProgressBar();
        }

    }



    /***********************
     * Barre de progression
     ***********************/

    private void updateProgressBar() {
        if (mPlayerService != null) {
            int position = mPlayerService.getPlayerPosition();
            mProgressBar.setProgress(position);
        }
    }



    /***********************************************************************************************
     * Sleep Timer
     **********************************************************************************************/

    private void showTimePicker() {

        final String start = getString(R.string.start);
        final String cancel = getString(R.string.cancel);

        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.date_picker_dialog, null);

        final SeekArc mSeekArc;
        final TextView mSeekArcProgress;

        mSeekArc = (SeekArc) view.findViewById(R.id.seekArc);
        mSeekArcProgress = (TextView) view.findViewById(R.id.seekArcProgress);


        mSeekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {

            @Override
            public void onStopTrackingTouch() {
                // vide, obligatoire
            }

            @Override
            public void onStartTrackingTouch() {
                // vide, obligatoire
            }

            @Override
            public void onProgressChanged(int progress) {

                String minute;

                if (progress <= 1){
                    minute = "minute";
                } else {
                    minute = "minutes";
                }

                String temps = String.valueOf(progress) + " " + minute;

                mSeekArcProgress.setText(temps);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(start, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int mins = mSeekArc.getProgress();

                startTimer(mins);
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

    private void showTimerInfo() {

        final String continuer = getString(R.string.continuer);
        final String cancelTimer = getString(R.string.cancel_timer);


        if (mTask.getDelay(TimeUnit.MILLISECONDS) < 0) {
            annulTimer();
            return;
        }
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.timer_info_dialog, null);
        final TextView timeLeft = ((TextView) view.findViewById(R.id.time_left));


        final String stopTimer = getString(R.string.stop_timer);

        final AlertDialog dialog = new AlertDialog.Builder(this).setPositiveButton(continuer, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setNegativeButton(cancelTimer, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                annulTimer();

                Context context = getApplicationContext();

                Toast.makeText(context, stopTimer, Toast.LENGTH_LONG).show();
            }
        }).setView(view).create();

        new CountDownTimer(mTask.getDelay(TimeUnit.MILLISECONDS), 1000) {
            @SuppressLint("StringFormatInvalid")
            @Override
            public void onTick(long seconds) {

                long secondes = seconds;

                secondes = secondes / 1000;
                timeLeft.setText(String.format(getString(R.string.timer_info), ((secondes % 3600) / 60), ((secondes % 3600) % 60)));
            }

            @Override
            public void onFinish() {
                dialog.dismiss();
            }
        }.start();

        dialog.show();
    }

    private void startTimer(final int minutes) {

        final String impossible = getString(R.string.impossible);


        final String minuteSingulier = getString(R.string.minute_singulier);
        final String minutePluriel = getString(R.string.minute_pluriel);

        final String arret = getString(R.string.arret);

        final String minuteTxt;

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final int delay = (minutes * 60) * 1000;

        if (delay == 0) {
            Toast.makeText(this, impossible, Toast.LENGTH_LONG).show();
            return;
        }

        if (minutes == 1) {
            minuteTxt = minuteSingulier;
        } else {
            minuteTxt = minutePluriel;
        }

        mTask = scheduler.schedule(new GetAudioFocusTask(this), delay, TimeUnit.MILLISECONDS);


        Toast.makeText(this, arret + " " + minutes + " " + minuteTxt, Toast.LENGTH_LONG).show();


        running = true;

        Notification.setState(true);

        Notification.updateNotification(mPlayerService);



        baisseVolume(delay);

    }

    public static void stopTimer() {
        if (running) mTask.cancel(true);

        Notification.setState(false);

        running = false;

    }

    private  void annulTimer() {

        if (running) mTask.cancel(true);

        if (running) {

            mTask.cancel(true);

            minuteurVolume.cancel();
            mPlayerService.setVolume(1.0f);
        }

        running = false;

        Notification.setState(false);

        Notification.updateNotification(mPlayerService);


    }


   /* ********************************
    * Réduction progressive du volume
    * ********************************/

    private CountDownTimer minuteurVolume;

    private void baisseVolume(final int delay) {

        // définir si le delay est supérieur ou inférieur à 10mn

        final short minutes = (short) ( ( (delay / 1000) % 3600) / 60);

        final boolean tempsMinuterie = minutes > 10;

        int cycle;

        if (tempsMinuterie) {
            cycle = 60000;
        } else {
            cycle = 1000;
        }

        minuteurVolume = new CountDownTimer(delay, cycle) {
            @Override
            public void onTick(long mseconds) {

                long temps1 = ((mTask.getDelay(TimeUnit.MILLISECONDS) / 1000) % 3600) / 60 ;

                long temps2 = mTask.getDelay(TimeUnit.MILLISECONDS) / 1000;

                if (tempsMinuterie) {

                    if (temps1 < 1) {
                        mPlayerService.setVolume(0.1f);
                    } else if (temps1 < 2) {
                        mPlayerService.setVolume(0.2f);
                    } else if (temps1 < 3) {
                        mPlayerService.setVolume(0.3f);
                    } else if (temps1 < 4) {
                        mPlayerService.setVolume(0.4f);
                    } else if (temps1 < 5) {
                        mPlayerService.setVolume(0.5f);
                    } else if (temps1 < 6) {
                        mPlayerService.setVolume(0.6f);
                    } else if (temps1 < 7) {
                        mPlayerService.setVolume(0.7f);
                    } else if (temps1 < 8) {
                        mPlayerService.setVolume(0.8f);
                    } else if (temps1 < 9) {
                        mPlayerService.setVolume(0.9f);
                    } else if (temps1 < 10) {
                        mPlayerService.setVolume(1.0f);
                    }

                } else {

                    if (temps2 < 6) {
                        mPlayerService.setVolume(0.1f);
                    } else if (temps2 < 12) {
                        mPlayerService.setVolume(0.2f);
                    } else if (temps2 < 18) {
                        mPlayerService.setVolume(0.3f);
                    } else if (temps2 < 24) {
                        mPlayerService.setVolume(0.4f);
                    } else if (temps2 < 30) {
                        mPlayerService.setVolume(0.5f);
                    } else if (temps2 < 36) {
                        mPlayerService.setVolume(0.6f);
                    } else if (temps2 < 42) {
                        mPlayerService.setVolume(0.7f);
                    } else if (temps2 < 48) {
                        mPlayerService.setVolume(0.8f);
                    } else if (temps2 < 54) {
                        mPlayerService.setVolume(0.9f);
                    } else if (temps2 < 60) {
                        mPlayerService.setVolume(1.0f);
                    }
                }
            }

            @Override
            public void onFinish() {
                exit();
            }

        }.start();
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
        handler.postDelayed(new Runnable() {


            @SuppressLint("SetTextI18n")
            public void run() {
                NotificationManager notificationManager;
                notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancel(Notification.NOTIFY_ID);

            }
        }, 500);
    }


    /***********************************************************************************************
     * Purge du cache
     **********************************************************************************************/
    private void clearCache() {
        ArtistImageCache.getInstance().clear();
        ArtworkCache.getInstance().clear();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        clearCache();
    }

    /* *********************************************************************************************
    * Gestion des permissions (Android >= 6.0)
    * *********************************************************************************************/

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String permissions[],
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_PHONE_STATE:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(PlayerService.PREF_AUTO_PAUSE, true);
                if (mPlayerService != null) {
                    mPlayerService.setAutoPauseEnabled();
                }
                editor.apply();
                break;
            default: //do nothing
                break;
        }
    }


    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                DialogUtils.showPermissionDialog(this, getString(R.string.permission_write_external_storage),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    }
                });

            } else {


                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {

                DialogUtils.showPermissionDialog(this, getString(R.string.permission_read_phone_state),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_REQUEST_READ_PHONE_STATE);
                    }
                });

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_REQUEST_READ_PHONE_STATE);
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
