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
import android.support.v4.app.FragmentTransaction;
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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.oucho.musicplayer.PlayerService.PlaybackBinder;
import org.oucho.musicplayer.audiofx.AudioEffects;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Artist;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.dialog.AboutDialog;
import org.oucho.musicplayer.dialog.HelpDialog;
import org.oucho.musicplayer.fragments.AlbumFragment;
import org.oucho.musicplayer.fragments.ArtistFragment;
import org.oucho.musicplayer.fragments.BaseFragment;
import org.oucho.musicplayer.fragments.LibraryFragment;
import org.oucho.musicplayer.adapters.BaseAdapter;
import org.oucho.musicplayer.adapters.QueueAdapter;
import org.oucho.musicplayer.fragments.PlayerFragment;
import org.oucho.musicplayer.images.ArtistImageCache;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.images.blurview.BlurView;
import org.oucho.musicplayer.images.blurview.RenderScriptBlur;
import org.oucho.musicplayer.update.CheckUpdate;
import org.oucho.musicplayer.utils.CustomLayoutManager;
import org.oucho.musicplayer.utils.GetAudioFocusTask;
import org.oucho.musicplayer.utils.NavigationUtils;
import org.oucho.musicplayer.utils.Notification;
import org.oucho.musicplayer.utils.PrefUtils;
import org.oucho.musicplayer.utils.SeekArc;
import org.oucho.musicplayer.utils.VolumeTimer;
import org.oucho.musicplayer.widgets.DragRecyclerView;
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

    private Context mContext;

    private View mQueueLayout;
    private List<Song> mQueue;
    private BlurView topBlurView;
    private TextView timeAfficheur;
    private RelativeLayout layoutA;
    private RelativeLayout layoutB;
    private BlurView bottomBlurView;
    private ProgressBar mProgressBar;
    private DrawerLayout mDrawerLayout;
    private ImageButton forwardButton;
    private ImageButton previousButton;
    private ImageButton forwardButton0;
    private ImageButton previousButton0;
    private DragRecyclerView mQueueView;
    private CountDownTimer minuteurVolume;
    private Intent mOnActivityResultIntent;
    private NavigationView mNavigationView;
    private PlaybackRequests mPlaybackRequests;
    private VolumeTimer volume = new VolumeTimer();

    private static Menu menu;
    private static ScheduledFuture mTask;
    private static QueueAdapter mQueueAdapter;
    private static PlayerService mPlayerService;

    private final Handler mHandler = new Handler();

    private boolean mServiceBound = false;
    private boolean autoScrollQueue = false;

    static int viewID;

    private static boolean running;
    private static boolean playBarLayout = false;
    private static boolean queueLayout = false;
    private static boolean playlistFragmentState = false;



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

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mPlaybackRequests = new PlaybackRequests();

        mQueueLayout = findViewById(R.id.queue_layout);
        mQueueView = (DragRecyclerView) findViewById(R.id.queue_view);
        mQueueView.setLayoutManager(new CustomLayoutManager(this));
        mQueueAdapter = new QueueAdapter(mContext, mQueueView);
        mQueueView.setOnItemMovedListener(new DragRecyclerView.OnItemMovedListener() {
            @Override
            public void onItemMoved(int oldPosition, int newPosition) {
                mQueueAdapter.moveItem(oldPosition, newPosition);
                mPlayerService.notifyChange(QUEUE_CHANGED);
            }
        });

        mQueueAdapter.setOnItemClickListener(new BaseAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int oldPosition, View newPosition) {

                updateQueue();
                mQueueAdapter.notifyDataSetChanged();
                mPlayerService.notifyChange(QUEUE_CHANGED);
            }
        });

        mQueueView.setAdapter(mQueueAdapter);



        findViewById(R.id.repeat0).setOnClickListener(mOnClickListener);
        findViewById(R.id.shuffle0).setOnClickListener(mOnClickListener);

        findViewById(R.id.track_info).setOnClickListener(mOnClickListener);
        findViewById(R.id.track_info0).setOnClickListener(mOnClickListener);

        findViewById(R.id.quick_prev).setOnClickListener(mOnClickListener);
        findViewById(R.id.quick_next).setOnClickListener(mOnClickListener);
        findViewById(R.id.quick_prev0).setOnClickListener(mOnClickListener);
        findViewById(R.id.quick_next0).setOnClickListener(mOnClickListener);

        findViewById(R.id.quick_prev).setOnLongClickListener(mOnLongClickListener);
        findViewById(R.id.quick_next).setOnLongClickListener(mOnLongClickListener);
        findViewById(R.id.quick_prev0).setOnLongClickListener(mOnLongClickListener);
        findViewById(R.id.quick_next0).setOnLongClickListener(mOnLongClickListener);

        findViewById(R.id.play_pause_toggle).setOnClickListener(mOnClickListener);
        findViewById(R.id.play_pause_toggle0).setOnClickListener(mOnClickListener);


        layoutA = (RelativeLayout) findViewById(R.id.track_info);
        layoutB = (RelativeLayout) findViewById(R.id.track_info0);

        previousButton = (ImageButton) findViewById(R.id.quick_prev);
        forwardButton = (ImageButton) findViewById(R.id.quick_next);
        previousButton0 = (ImageButton) findViewById(R.id.quick_prev0);
        forwardButton0 = (ImageButton) findViewById(R.id.quick_next0);

        timeAfficheur = ((TextView) findViewById(R.id.zZz));

        topBlurView = (BlurView) findViewById(R.id.topBlurView);
        bottomBlurView = (BlurView) findViewById(R.id.bottomBlurView);

        mDrawerLayout = (DrawerLayout) findViewById(drawer_layout);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);


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


    private void setupBlurView() {

        final float radius = 5f;

        final View decorView = getWindow().getDecorView();
        //Activity's root View. Can also be root View of your layout (preferably)
        final ViewGroup rootView = (ViewGroup) decorView.findViewById(R.id.drawer_layout);

        topBlurView.setupWith(rootView)
                .blurAlgorithm(new RenderScriptBlur(mContext, true))
                .blurRadius(radius);

        bottomBlurView.setupWith(rootView)
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

        mHandler.postDelayed(new Runnable() {
            public void run() {
                mDrawerLayout.closeDrawers();
            }
        }, 300);

        switch (menuItem.getItemId()) {
            case R.id.action_equalizer:
                NavigationUtils.showEqualizer(this);
                break;

            case R.id.action_search:
                NavigationUtils.showSearchActivity(this);
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
                case R.id.play_pause_toggle0:
                case R.id.play_pause_toggle:
                    mPlayerService.toggle();

                    Intent intentPl = new Intent(INTENT_STATE);
                    intentPl.putExtra("state", "play");
                    sendBroadcast(intentPl);

                    break;

                case R.id.quick_prev:
                case R.id.quick_prev0:
                    autoScrollQueue = true;
                    mPlayerService.playPrev();
                    Intent intentP = new Intent(INTENT_STATE);
                    intentP.putExtra("state", "prev");
                    sendBroadcast(intentP);

                    updateQueue();
                    mQueueAdapter.notifyDataSetChanged();

                    break;

                case R.id.quick_next:
                case R.id.quick_next0:
                    autoScrollQueue = true;

                    mPlayerService.playNext(true);

                    Intent intentN = new Intent(INTENT_STATE);
                    intentN.putExtra("state", "next");
                    sendBroadcast(intentN);

                    updateQueue();
                    mQueueAdapter.notifyDataSetChanged();

                    break;

                case R.id.shuffle0:
                    boolean shuffle = PlayerService.isShuffleEnabled();
                    mPlayerService.setShuffleEnabled(!shuffle);
                    updateShuffleButton();

                    updateQueue();
                    mPlayerService.notifyChange(QUEUE_CHANGED);
                    break;

                case R.id.repeat0:
                    int mode = mPlayerService.getNextRepeatMode();
                    mPlayerService.setRepeatMode(mode);
                    updateRepeatButton();

                    updateQueue();
                    mPlayerService.notifyChange(QUEUE_CHANGED);


                    break;

                case R.id.track_info:

                    File file = new File(getApplicationInfo().dataDir, "/databases/Queue.db");

                    if (file.exists()) {

                        Fragment fragment = PlayerFragment.newInstance();
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_in_bottom);

                        if ( viewID == R.id.fragment_album_list_layout ){
                            ft.replace(viewID, fragment);

                            Log.d("Main Activity", "R.id.fragment_album_list_layout");

                        } else if ( viewID == R.id.fragment_song_layout ){
                            ft.replace(viewID, fragment);

                            Log.d("Main Activity", "R.id.fragment_song_layout");

                        } else if ( viewID == R.id.fragment_playlist_list ) {
                            ft.replace(viewID, fragment);

                            Log.d("Main Activity", "R.id.fragment_playlist_list");
                        } else if ( viewID == R.id.fragment_playlist ) {
                            ft.replace(viewID, fragment);
                            Log.d("Main Activity", "R.id.fragment_playlist");
                        }

                        ft.commit();

                        if (menu == null)
                            return;

                        menu.setGroupVisible(R.id.main_menu_group, false);


                    } else {
                        Toast.makeText(mContext, "Vous devez d'abord sélectionner un titre", Toast.LENGTH_LONG).show();
                    }

                    break;

                default:
                    break;
            }
        }
    };


    private final View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {

            if (mPlayerService == null) {
                return false;
            }
            switch (v.getId()) {

                case R.id.quick_prev:
                case R.id.quick_prev0:

                    mHandler.postDelayed(fRewind, 300);

                    break;

                case R.id.quick_next:
                case R.id.quick_next0:

                    mHandler.postDelayed(fForward, 300);

                    break;

                default:
                    break;
            }

            return true;
        }
    };





    private void updateShuffleButton() {
        boolean shuffle = PlayerService.isShuffleEnabled();
        ImageView shuffleButton = (ImageView) findViewById(R.id.shuffle0);
        if (shuffle) {
            assert shuffleButton != null;
            shuffleButton.setImageResource(R.drawable.ic_shuffle_grey_600_24dp);

        } else {
            assert shuffleButton != null;
            shuffleButton.setImageResource(R.drawable.ic_shuffle_grey_400_24dp);

        }
    }

    private void updateRepeatButton() {
        ImageView repeatButton = (ImageView) findViewById(R.id.repeat0);
        int mode = PlayerService.getRepeatMode();
        if (mode == PlayerService.NO_REPEAT) {
            assert repeatButton != null;
            repeatButton.setImageResource(R.drawable.ic_repeat_grey_400_24dp);
        } else if (mode == PlayerService.REPEAT_ALL) {
            assert repeatButton != null;
            repeatButton.setImageResource(R.drawable.ic_repeat_grey_600_24dp);
        } else if (mode == PlayerService.REPEAT_CURRENT) {
            assert repeatButton != null;
            repeatButton.setImageResource(R.drawable.ic_repeat_one_grey_600_24dp);

        }
    }

    /* *********************************************************************************************
     * Menu
     * ********************************************************************************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity, menu);

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

            case R.id.action_view_queue:
                autoScrollQueue = true;

                updateQueue();
                toggleQueue();

                queueLayout = true;
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



    private void toggleQueue() {
        if (mQueueLayout.getVisibility() != View.VISIBLE) {
            mQueueLayout.setVisibility(View.VISIBLE);
            topBlurView.setVisibility(View.VISIBLE);
            queueLayout = true;
        } else {
            mQueueLayout.setVisibility(View.GONE);
            topBlurView.setVisibility(View.GONE);
            queueLayout = false;
        }
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
            filter.addAction(INTENT_BLURVIEW);
            filter.addAction(INTENT_QUIT);
            filter.addAction(INTENT_QUEUEVIEW);
            filter.addAction(INTENT_LAYOUTVIEW);


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

            mProgressBar.setProgress(PlayerService.getPlayerPosition());

            mHandler.postDelayed(mUpdateProgressBar, 1000);
        }
    };

    private final Runnable fForward = new Runnable() {
        @Override
        public void run() {
            if (forwardButton.isPressed() || forwardButton0.isPressed()) {
                int position = PlayerService.getPlayerPosition();
                position += 8000;
                PlayerService.seekTo(position);
                mHandler.postDelayed(fForward, 200);
            }
        }
    };

    private final Runnable fRewind = new Runnable() {
        @Override
        public void run() {
            if (previousButton.isPressed() || previousButton0.isPressed()) {
                int position = PlayerService.getPlayerPosition();
                position -= 8000;
                PlayerService.seekTo(position);
                mHandler.postDelayed(fRewind, 200);
            }
        }
    };


    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            Log.d("INTENT_LAYOUTVIEW", receiveIntent);


            if (mPlayerService == null) {
                return;
            }

            if (receiveIntent.equals(INTENT_QUEUEVIEW)) {

                mQueueLayout.setVisibility(View.GONE);
                topBlurView.setVisibility(View.GONE);

                queueLayout = false;
            }

            if (receiveIntent.equals(INTENT_LAYOUTVIEW)) {


                final float tailleBarre = getResources().getDimension(R.dimen.barre_lecture);


                if ("layout0".equals(intent.getStringExtra("vue"))) {

                    Log.d("INTENT_LAYOUTVIEW", "layout0");


                    TranslateAnimation animate = new TranslateAnimation(0, 0, tailleBarre, 0);
                    animate.setDuration(400);
                    animate.setFillAfter(true);
                    layoutB.startAnimation(animate);
                    layoutB.setVisibility(View.VISIBLE);

                    TranslateAnimation animate2 = new TranslateAnimation(0, 0, 0, -tailleBarre);
                    animate2.setDuration(400);
                    animate2.setFillAfter(true);
                    layoutA.startAnimation(animate2);
                    layoutA.setVisibility(View.GONE);


                } else if ("playBarLayout".equals(intent.getStringExtra("vue"))) {

                    Log.d("INTENT_LAYOUTVIEW", "playBarLayout");

                    TranslateAnimation animate = new TranslateAnimation(0, 0, tailleBarre, 0);
                    animate.setDuration(400);
                    animate.setFillAfter(true);
                    layoutB.startAnimation(animate);
                    layoutB.setVisibility(View.VISIBLE);

                    TranslateAnimation animate2 = new TranslateAnimation(0, 0, 0, -tailleBarre);
                    animate2.setDuration(400);
                    animate2.setFillAfter(true);
                    layoutA.startAnimation(animate2);
                    layoutA.setVisibility(View.GONE);


                    Animation fadeOut = new AlphaAnimation(1, 0);
                    fadeOut.setInterpolator(new AccelerateInterpolator());
                    fadeOut.setDuration(400);
                    mProgressBar.setAnimation(fadeOut);
                    mProgressBar.setVisibility(View.GONE);

                    playBarLayout = true;

                } else {

                    Log.d("INTENT_LAYOUTVIEW", "else");

                    // TranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta)

                    TranslateAnimation animate2 = new TranslateAnimation(0, 0, -tailleBarre, 0);
                    animate2.setDuration(400);
                    animate2.setFillAfter(true);
                    layoutA.startAnimation(animate2);
                    layoutA.setVisibility(View.VISIBLE);


                    TranslateAnimation animate = new TranslateAnimation(0, 0, 0, tailleBarre);
                    animate.setDuration(400);
                    animate.setFillAfter(true);
                    layoutB.startAnimation(animate);

                    // bug GONE, reste actif si pas de clearAnimation
                    mHandler.postDelayed(new Runnable() {

                        public void run() {
                            layoutB.clearAnimation();
                            layoutB.setVisibility(View.GONE);
                        }
                    }, 400);


                    if (playBarLayout) {
                        Animation fadeIn = new AlphaAnimation(0, 1);
                        fadeIn.setInterpolator(new AccelerateInterpolator());
                        fadeIn.setDuration(400);
                        mProgressBar.setAnimation(fadeIn);
                        mProgressBar.setVisibility(View.VISIBLE);

                        playBarLayout = false;
                    } else {
                        mProgressBar.setVisibility(View.VISIBLE);

                    }
                }

                refresh();

                updateRepeatButton();
                updateShuffleButton();

            }

            if (receiveIntent.equals(PlayerService.PLAYSTATE_CHANGED)) {
                if (PlayerService.isPlaying()) {
                    mHandler.post(mUpdateProgressBar);
                } else {
                    mHandler.removeCallbacks(mUpdateProgressBar);
                }
            }

            if (receiveIntent.equals(PlayerService.PLAYSTATE_CHANGED)) {
                setButtonDrawable();
                if (PlayerService.isPlaying()) {
                    mHandler.post(mUpdateProgressBar);
                } else {
                    mHandler.removeCallbacks(mUpdateProgressBar);
                }
            }

            if (receiveIntent.equals(PlayerService.POSITION_CHANGED)) {
                autoScrollQueue = true;
                updateQueue();
            }

            if (receiveIntent.equals(PlayerService.META_CHANGED)) {
                updateTrackInfo();
            }

            if (receiveIntent.equals(INTENT_BLURVIEW))
                setupBlurView();

            if (receiveIntent.equals(INTENT_QUIT) && "exit".equals(intent.getStringExtra("halt")))
                exit();

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

            updateQueue();

            updateTrackInfo();
            setButtonDrawable();
            if (PlayerService.isPlaying()) {
                mHandler.post(mUpdateProgressBar);
            }
        }
    }

    private void updateQueue() {
        if (mPlayerService == null) {
            return;
        }

        List<Song> queue = PlayerService.getPlayList();
        if (!queue.equals(mQueue)) {
            mQueue = queue;
            mQueueAdapter.setQueue(mQueue);
        }

        mQueueAdapter.notifyDataSetChanged();

        setQueueSelection(PlayerService.getPositionWithinPlayList());
    }

    private void setQueueSelection(final int position) {

        mQueueAdapter.setSelection(position);

        if (autoScrollQueue) {

            mHandler.postDelayed(new Runnable() {

                public void run() {

                    mQueueView.smoothScrollToPosition(position);
                    autoScrollQueue = false;
                }
            }, 100);

        }

    }

    /***********************************************************************************************
     * Barre de lecture
     **********************************************************************************************/

    private void setButtonDrawable() {
        if (mPlayerService != null) {
            ImageButton quickButton = (ImageButton) findViewById(R.id.play_pause_toggle);
            ImageButton quickButton0 = (ImageButton) findViewById(R.id.play_pause_toggle0);

            if (PlayerService.isPlaying()) {
                assert quickButton != null;
                quickButton.setImageResource(R.drawable.ic_pause_circle_filled_grey_600_48dp);
                quickButton0.setImageResource(R.drawable.ic_pause_circle_filled_grey_600_48dp);

            } else {
                assert quickButton != null;
                quickButton.setImageResource(R.drawable.ic_play_circle_filled_grey_600_48dp);
                quickButton0.setImageResource(R.drawable.ic_play_circle_filled_grey_600_48dp);
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
            mProgressBar.setProgress(PlayerService.getPlayerPosition());
        }
    }



    /***********************************************************************************************
     * Sleep Timer
     **********************************************************************************************/

    private void showTimePicker() {

        final String start = getString(R.string.start);
        final String cancel = getString(R.string.cancel);

        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.dialog_date_picker, null);

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

        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.dialog_timer_info, null);
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
        menu.getItem(1).setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_timer_indigo_400_24dp));
        timeAfficheur.setVisibility(View.VISIBLE);

        Notification.setState(true);
        Notification.updateNotification(mPlayerService);

        showTimeEcran();

        volume.baisser(mContext, mTask, delay);
        //baisseVolume(delay);
    }


    public static void stopTimer(Context context) {
        if (running)
            mTask.cancel(true);

        Notification.setState(false);

        running = false;
        menu.getItem(1).setIcon(ContextCompat.getDrawable(context, R.drawable.ic_timer_grey_600_24dp));
    }

    private  void annulTimer() {

        if (running) {

            mTask.cancel(true);

            minuteurVolume.cancel();
            minuteurVolume = null;


            volume.getMinuteur().cancel();
            volume.setVolume(1.0f);

        }

        running = false;
        menu.getItem(1).setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_timer_grey_600_24dp));
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
        mHandler.postDelayed(new Runnable() {

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

    public static PlayerService getPlayerService() {
        return mPlayerService;
    }

    public static QueueAdapter getQueueAdapter() {
        return mQueueAdapter;
    }

// instance variables here

    public QueueAdapter run() throws Exception
    {
        // put your code here
        return mQueueAdapter;
    }

    public static boolean getQueueLayout() {
        return queueLayout;
    }

    public static void setMenu(Boolean value) {
        menu.setGroupVisible(R.id.main_menu_group, value);
    }

    public static boolean getPlaylistFragmentState() {
        return playlistFragmentState;
    }

    public static void setPlaylistFragmentState(Boolean value) {
        playlistFragmentState = value;
    }

    public static void setViewID(int id) {
        viewID = id;
    }

    public static int getViewID() {
        return viewID;
    }

}
