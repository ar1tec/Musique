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
import android.os.Build;
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
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.oucho.musicplayer.PlayerService.PlaybackBinder;
import org.oucho.musicplayer.audiofx.AudioEffects;
import org.oucho.musicplayer.images.blurview.BlurView;
import org.oucho.musicplayer.images.blurview.RenderScriptBlur;
import org.oucho.musicplayer.dialog.AboutDialog;
import org.oucho.musicplayer.dialog.HelpDialog;
import org.oucho.musicplayer.fragments.AlbumFragment;
import org.oucho.musicplayer.fragments.ArtistFragment;
import org.oucho.musicplayer.fragments.BaseFragment;
import org.oucho.musicplayer.fragments.LibraryFragment;
import org.oucho.musicplayer.images.ArtistImageCache;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Artist;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.update.CheckUpdate;
import org.oucho.musicplayer.utils.GetAudioFocusTask;
import org.oucho.musicplayer.utils.MusiqueKeys;
import org.oucho.musicplayer.utils.NavigationUtils;
import org.oucho.musicplayer.utils.Notification;
import org.oucho.musicplayer.utils.PrefUtils;
import org.oucho.musicplayer.utils.SeekArc;
import org.oucho.musicplayer.widgets.ProgressBar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.oucho.musicplayer.R.id.drawer_layout;

public class MainActivity extends AppCompatActivity implements
        MusiqueKeys,
        OnNavigationItemSelectedListener {

    public static final int SEARCH_ACTIVITY = 234;

    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE = 2;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    private final Handler mHandler = new Handler();
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;

    private BlurView blurView;

    private Intent mOnActivityResultIntent;

    private PlayerService mPlayerService;

    private boolean mServiceBound = false;
    private ProgressBar mProgressBar;

    private PlaybackRequests mPlaybackRequests;

    private static ScheduledFuture mTask;
    private static boolean running;

    private final Handler handler = new Handler();

    private static Menu menu;
    private TextView timeAfficheur;

    private activationReceiver blurActivationReceiver;
    private boolean isRegistered = false;

    private Context mContext;



    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

            getWindow().getDecorView().setSystemUiVisibility(mUIFlag);

            getWindow().setStatusBarColor(ContextCompat.getColor(mContext, R.color.blanc));
        }

        if(android.os.Build.VERSION.SDK_INT >= 23)
        {
            checkPermissions();
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mPlaybackRequests = new PlaybackRequests();

        findViewById(R.id.quick_play_pause_toggle).setOnClickListener(mOnClickListener);
        findViewById(R.id.track_info).setOnClickListener(mOnClickListener);
        findViewById(R.id.quick_prev).setOnClickListener(mOnClickListener);
        findViewById(R.id.quick_next).setOnClickListener(mOnClickListener);

        timeAfficheur = ((TextView) findViewById(R.id.zZz));

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mDrawerLayout = (DrawerLayout) findViewById(drawer_layout);

        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        mNavigationView.setNavigationItemSelectedListener(this);

        blurView = (BlurView) findViewById(R.id.bottomBlurView);

        PrefUtils.init(this);
        ArtworkCache.init(this);
        ArtistImageCache.init(this);
        AudioEffects.init(this);

        if (savedInstanceState == null) {
            showLibrary();
        }


        blurActivationReceiver = new activationReceiver();
        IntentFilter filter = new IntentFilter(INTENT_BLURVIEW);

        registerReceiver(blurActivationReceiver, filter);
        isRegistered = true;

        CheckUpdate.onStart(this);

    }


    private void setupBlurView() {

        final float radius = 5f;

        final View decorView = getWindow().getDecorView();
        //Activity's root View. Can also be root View of your layout (preferably)
        final ViewGroup rootView = (ViewGroup) decorView.findViewById(R.id.drawer_layout);

        blurView.setupWith(rootView)
                .blurAlgorithm(new RenderScriptBlur(mContext, true))
                .blurRadius(radius);
    }

    /* *********************************************************************************************
     * Navigation Drawer
     * ********************************************************************************************/

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        handler.postDelayed(new Runnable() {
            public void run() {
                mDrawerLayout.closeDrawers();
            }
        }, 300);

        switch (menuItem.getItemId()) {
            case R.id.action_equalizer:
                NavigationUtils.showEqualizer(this);
                break;

            case R.id.nav_update:
                CheckUpdate.withInfo(this);
                break;

            case R.id.nav_about:
                showAboutDialog();
                break;

            case R.id.nav_help:
                showHelpDialog();
                break;

            case R.id.nav_exit:
                exit();
                break;

            default:
                break;
        }
        return true;
    }


    /**************
     * About dialog
     **************/

    private void showAboutDialog(){
        AboutDialog dialog = new AboutDialog();
        dialog.show(getSupportFragmentManager(), "about");
    }

    private void showHelpDialog(){
        HelpDialog dialog = new HelpDialog();
        dialog.show(getSupportFragmentManager(), "help");
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

                    Intent intentPl = new Intent(INTENT_STATE);
                    intentPl.putExtra("state", "play");
                    sendBroadcast(intentPl);

                    break;

                case R.id.quick_prev:
                case R.id.prev:
                    mPlayerService.playPrev();

                    Intent intentP = new Intent(INTENT_STATE);
                    intentP.putExtra("state", "prev");
                    sendBroadcast(intentP);

                    break;

                case R.id.quick_next:
                case R.id.next:
                    mPlayerService.playNext(true);

                    Intent intentN = new Intent(INTENT_STATE);
                    intentN.putExtra("state", "next");
                    sendBroadcast(intentN);

                    break;

                case R.id.action_equalizer:
                    NavigationUtils.showEqualizer(MainActivity.this);
                    break;

                case R.id.track_info:

                    File file = new File(getApplicationInfo().dataDir, "/databases/Queue.db");

                    if (file.exists()) {

                        NavigationUtils.showPlaybackActivity(MainActivity.this);


                    } else {
                        Toast.makeText(mContext, "Vous devez d'abord sélectionner un titre", Toast.LENGTH_LONG).show();
                    }

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

        MainActivity.menu = menu;

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
            case R.id.action_timer:
                if (! running) {
                    showTimePicker();
                } else {
                    showTimerInfo();
                }
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

        if (!PlayerService.isPlaying())
            killNotif();

        if (isRegistered) {
            unregisterReceiver(blurActivationReceiver);
            isRegistered = false;
        }

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

        if (!isRegistered) {
            IntentFilter filter = new IntentFilter(INTENT_BLURVIEW);
            registerReceiver(blurActivationReceiver, filter);
            isRegistered = true;
        }

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


    private class activationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            if (INTENT_BLURVIEW.equals(receiveIntent)) {

                Log.d("MainActivity", "setupBlurView();");

                setupBlurView();
            }
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

    @SuppressWarnings("RestrictedApi")
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
                if (PlayerService.isPlaying()) {
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
            if (PlayerService.isPlaying()) {
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
            if (PlayerService.isPlaying()) {
                assert quickButton != null;
                quickButton.setImageResource(R.drawable.ic_pause_circle_filled_grey_600_48dp);
            } else {
                assert quickButton != null;
                quickButton.setImageResource(R.drawable.ic_play_circle_filled_grey_600_48dp);
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

        void requestAddToQueue(Song song) {
            if (mPlayerService != null) {
                mPlayerService.addToQueue(song);
            } else {
                mAddToQueue = song;
            }
        }

        void requestAsNextTrack(Song song) {
            if (mPlayerService != null) {
                mPlayerService.setAsNextTrack(song);
            } else {
                mNextTrack = song;
            }
        }

        void sendRequests() {
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

        String title = PlayerService.getSongTitle();
        String artist = PlayerService.getArtistName();


        if (title != null) {
            //noinspection ConstantConditions
            ((TextView) findViewById(R.id.song_title)).setText(title);
        }

        if (artist != null) {
            //noinspection ConstantConditions
            ((TextView) findViewById(R.id.song_artist)).setText(artist + ", "  + PlayerService.getAlbumName());
        }

        int duration = PlayerService.getTrackDuration();

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
            int position = PlayerService.getPlayerPosition();
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

                Toast.makeText(mContext, stopTimer, Toast.LENGTH_LONG).show();
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
        menu.getItem(0).setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_timer_indigo_400_24dp));
        timeAfficheur.setVisibility(View.VISIBLE);

        Notification.setState(true);
        Notification.updateNotification(mPlayerService);

        showTimeEcran();
        baisseVolume(delay);
    }


    public static void stopTimer(Context context) {
        if (running)
            mTask.cancel(true);

        Notification.setState(false);

        running = false;
        menu.getItem(0).setIcon(ContextCompat.getDrawable(context, R.drawable.ic_timer_grey_600_24dp));
    }

    private  void annulTimer() {

        if (running) {

            mTask.cancel(true);

            minuteurVolume.cancel();
            minuteurVolume = null;


            PlayerService.setVolume(1.0f);
        }

        running = false;
        menu.getItem(0).setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_timer_grey_600_24dp));
        timeAfficheur.setVisibility(View.GONE);

        Notification.setState(false);

        Notification.updateNotification(mPlayerService);
    }

       /* ********************************
    * Afficher temps restant à l'écran
    * ********************************/

    private void showTimeEcran() {



        assert timeAfficheur != null;
        timeAfficheur.setVisibility(View.VISIBLE);

        minuteurVolume = new CountDownTimer(mTask.getDelay(TimeUnit.MILLISECONDS), 1000) {
            @Override
            public void onTick(long seconds) {

                long secondes = seconds;

                secondes = secondes / 1000;

                String textTemps = "zZz " + String.format(getString(R.string.timer_info), ((secondes % 3600) / 60), ((secondes % 3600) % 60));

                timeAfficheur.setText(textTemps);
            }

            @Override
            public void onFinish() {
                timeAfficheur.setVisibility(View.GONE);
            }

        }.start();
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
                        PlayerService.setVolume(0.1f);
                    } else if (temps1 < 2) {
                        PlayerService.setVolume(0.2f);
                    } else if (temps1 < 3) {
                        PlayerService.setVolume(0.3f);
                    } else if (temps1 < 4) {
                        PlayerService.setVolume(0.4f);
                    } else if (temps1 < 5) {
                        PlayerService.setVolume(0.5f);
                    } else if (temps1 < 6) {
                        PlayerService.setVolume(0.6f);
                    } else if (temps1 < 7) {
                        PlayerService.setVolume(0.7f);
                    } else if (temps1 < 8) {
                        PlayerService.setVolume(0.8f);
                    } else if (temps1 < 9) {
                        PlayerService.setVolume(0.9f);
                    } else if (temps1 < 10) {
                        PlayerService.setVolume(1.0f);
                    }

                } else {

                    if (temps2 < 6) {
                        PlayerService.setVolume(0.1f);
                    } else if (temps2 < 12) {
                        PlayerService.setVolume(0.2f);
                    } else if (temps2 < 18) {
                        PlayerService.setVolume(0.3f);
                    } else if (temps2 < 24) {
                        PlayerService.setVolume(0.4f);
                    } else if (temps2 < 30) {
                        PlayerService.setVolume(0.5f);
                    } else if (temps2 < 36) {
                        PlayerService.setVolume(0.6f);
                    } else if (temps2 < 42) {
                        PlayerService.setVolume(0.7f);
                    } else if (temps2 < 48) {
                        PlayerService.setVolume(0.8f);
                    } else if (temps2 < 54) {
                        PlayerService.setVolume(0.9f);
                    } else if (temps2 < 60) {
                        PlayerService.setVolume(1.0f);
                    }
                }
            }

            @Override
            public void onFinish() {
                exit();
            }

        }.start();
    }

    private void exit() {

        if (running)
            annulTimer();

        if (PlayerService.isPlaying())
            mPlayerService.toggle();

        PlayerService.setVolume(1.0f);

        killNotif();
        clearCache();
        finish();
    }

    /***********************************************************************************************
     * Touche retour
     **********************************************************************************************/

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {

        DrawerLayout drawer = (DrawerLayout) findViewById(drawer_layout);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {

            case PERMISSIONS_REQUEST_READ_PHONE_STATE:

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(PlayerService.PREF_AUTO_PAUSE, true);
                    if (mPlayerService != null) {
                        mPlayerService.setAutoPauseEnabled();
                    }
                    editor.apply();
                }

                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
                break;

            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                refresh();
                break;

            default:
                break;
        }
    }


    private void checkPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {

                DialogUtils.showPermissionDialog(this, getString(R.string.permission_read_phone_state),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_REQUEST_READ_PHONE_STATE);
                            }
                        });
            }
        }
    }

    private static class DialogUtils {

        private static void showPermissionDialog(Context context, String message, DialogInterface.OnClickListener listener) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.permission)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, listener)
                    .show();
        }
    }
}
