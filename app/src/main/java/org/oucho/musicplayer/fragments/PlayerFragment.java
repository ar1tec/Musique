package org.oucho.musicplayer.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.QueueDbHelper;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.images.ArtworkCache;

import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class PlayerFragment extends BaseFragment
        implements MusiqueKeys,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG_LOG = "Player Fragment";

    private Context mContext;

    private View rootView;
    private SeekBar mSeekBar;
    private TextView nbTrack;
    private SharedPreferences préférences;
    private PlayerService mPlayerService = MainActivity.getPlayerService();

    private final Handler mHandler = new Handler();

    private int track = -1;
    private int mArtworkSize;
    private int total_track = -1;

    private boolean mServiceBound;
    private boolean first_run = true;


    public static PlayerFragment newInstance() {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void load() {

    }

    /* *********************************************************************************************
 * Création du fragment
 * ********************************************************************************************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();

        préférences = mContext.getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE);
        préférences.registerOnSharedPreferenceChangeListener(this);

        total_track = getSizeQueue();
        track = préférences.getInt("currentPosition", 0) + 1;

        mHandler.postDelayed(new Runnable() {

            public void run() {

                String artist = PlayerService.getAlbumName();
                getActivity().setTitle(artist);

            }
        }, 300);

        Intent intent = new Intent();
        intent.setAction(INTENT_LAYOUTVIEW);
        intent.putExtra("vue", "playBarLayout");
        mContext.sendBroadcast(intent);
    }



    /* *********************************************************************************************
     * Création du visuel
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_player, container, false);

        mArtworkSize = getResources().getDimensionPixelSize(R.dimen.playback_activity_art_size);

        SharedPreferences préférences = mContext.getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE);
        track = préférences.getInt("currentPosition", 0) + 1;

        nbTrack = (TextView) rootView.findViewById(R.id.nombre_titre);
        nbTrack.setText(track + "/" + total_track);

        mSeekBar = (SeekBar) rootView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        //mSeekBar.getThumb().mutate().setAlpha(0);

        LinearLayout linearLayout = (LinearLayout) rootView.findViewById(R.id.root);
        linearLayout.setOnClickListener(mOnClickListener);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        updateAll();

        return rootView;
    }


        // empêche les clicks vers le layout du dessous
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            if (mPlayerService == null) {
                return;
            }
            switch (v.getId()) {
                case R.id.root:
                    //mPlayerService.toggle();
                    break;


                default:
                    break;
            }
        }
    };


    private int getSizeQueue() {
        QueueDbHelper dbHelper = new QueueDbHelper(mContext);
        List<Song> playList = dbHelper.readAll();
        dbHelper.close();

        return playList.size();
    }

    private final Runnable mUpdateSeekBarRunnable = new Runnable() {

        @Override
        public void run() {

            updateSeekBar();

            mHandler.postDelayed(mUpdateSeekBarRunnable, 250);
        }
    };

    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser
                    && mPlayerService != null
                    && (PlayerService.isPlaying() || PlayerService.isPaused())) {
                PlayerService.seekTo(seekBar.getProgress());
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mHandler.removeCallbacks(mUpdateSeekBarRunnable);

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mPlayerService != null && PlayerService.isPlaying()) {
                mHandler.post(mUpdateSeekBarRunnable);
            }

        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if ("currentPosition".equals(key)) {



            track = préférences.getInt("currentPosition", 0) + 1;

            nbTrack.setText(track + "/" + total_track);


        }
    }


    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i(TAG_LOG, "onReceive");
            //updateTrackInfo();

            if (mPlayerService == null) {
                Log.i(TAG_LOG, "if (mPlayerService == null)");

                return;
            }


            String action = intent.getAction();

            Log.i(TAG_LOG, action);


            switch (action) {

                case PlayerService.PLAYSTATE_CHANGED:

                    if (PlayerService.isPlaying()) {
                        mHandler.post(mUpdateSeekBarRunnable);
                    } else {
                        mHandler.removeCallbacks(mUpdateSeekBarRunnable);
                    }
                    break;

                case PlayerService.META_CHANGED:
                    Log.i(TAG_LOG, "mServiceListener, case PlayerService.META_CHANGED:");

                    updateTrackInfo();
                    break;

                case PlayerService.QUEUE_CHANGED:
                case PlayerService.POSITION_CHANGED:
                case PlayerService.ITEM_ADDED:
                case PlayerService.ORDER_CHANGED:

                    break;
                default:
                    break;
            }
        }

    };



    @Override
    public void onPause() {
        super.onPause();

        mContext.unregisterReceiver(mServiceListener);

        if (mServiceBound) {
            mServiceBound = false;
        }
        mHandler.removeCallbacks(mUpdateSeekBarRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mServiceBound) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(PlayerService.META_CHANGED);
            filter.addAction(PlayerService.PLAYSTATE_CHANGED);
            filter.addAction(PlayerService.POSITION_CHANGED);
            filter.addAction(PlayerService.ITEM_ADDED);
            filter.addAction(PlayerService.ORDER_CHANGED);
            mContext.registerReceiver(mServiceListener, filter);
        } else {
            updateAll();
        }


        // Active la touche back
        if (getView() == null) {
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {

                    LibraryFragment.setLock(false);

                    int viewID = MainActivity.getViewID();

                    if (MainActivity.getQueueLayout()) {

                        Intent intent = new Intent();
                        intent.setAction(INTENT_QUEUEVIEW);
                        mContext.sendBroadcast(intent);

                        return true;


                    } else if (getFragmentManager().findFragmentById(viewID) != null) {

                        Intent intent0 = new Intent();
                        intent0.setAction(INTENT_LAYOUTVIEW);
                        intent0.putExtra("vue", "layoutx");
                        mContext.sendBroadcast(intent0);

                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                        ft.remove(getFragmentManager().findFragmentById(viewID));
                        ft.commit();
                        Intent intent = new Intent();
                        intent.setAction("reload");
                        mContext.sendBroadcast(intent);

                        if (!MainActivity.getPlaylistFragmentState())
                            MainActivity.setMenu(true);

                        // rustine blurview
                        mHandler.postDelayed(new Runnable() {

                            public void run() {

                                Intent intent0 = new Intent();
                                intent0.setAction(INTENT_BLURVIEW);
                                mContext.sendBroadcast(intent0);
                            }
                        }, 1000);

                        return true;
                    }
                    return false;
                }
                return false;
            }
        });

    }


    private void updateAll() {
        if (mPlayerService != null) {

            Log.i(TAG_LOG, "updateAll(), if (mPlayerService != null)");

            updateTrackInfo();

            if (PlayerService.isPlaying()) {
                mHandler.post(mUpdateSeekBarRunnable);
            }
        }
    }


    private void updateTrackInfo() {

        Log.i(TAG_LOG, "updateTrackInfo");


        if (mPlayerService != null) {

            Log.i(TAG_LOG, "updateTrackInfo, if (mPlayerService != null)");


            String title = PlayerService.getSongTitle();
            String artist = PlayerService.getArtistName();

            final String album = PlayerService.getAlbumName();

            if (album != null) {

                if (first_run) {

                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            getActivity().setTitle(album);
                            first_run = false;
                        }
                    }, 300);

                }  else {
                    getActivity().setTitle(album);
                }
            }



            if (title != null) {
                //noinspection ConstantConditions
                ((TextView) rootView.findViewById(R.id.song_title)).setText(title);
            }

            if (artist != null) {
                //noinspection ConstantConditions
                ((TextView) rootView.findViewById(R.id.song_artist)).setText(artist);
            }

            ImageView artworkView = (ImageView) rootView.findViewById(R.id.artwork);
            ArtworkCache.getInstance().loadBitmap(PlayerService.getAlbumId(), artworkView, mArtworkSize, mArtworkSize);


            final int duration = PlayerService.getTrackDuration();

            if (duration != -1) {

                Log.i(TAG_LOG, "updateTrackInfo, mHandler.postDelayed(new Runnable()");

                ((TextView) rootView.findViewById(R.id.track_duration)).setText(msToText(duration));

                mSeekBar.setMax(duration);

                updateSeekBar();
            }
        }
    }


    private String msToText(int msec) {
        return String.format(Locale.getDefault(), "%d:%02d", msec / 60000,
                (msec % 60000) / 1000);
    }

    private void updateSeekBar() {
        if (mPlayerService != null) {
            int position = PlayerService.getPlayerPosition();
            mSeekBar.setProgress(position);

            //noinspection ConstantConditions
            ((TextView) rootView.findViewById(R.id.current_position)).setText(msToText(position));
        }
    }



}
