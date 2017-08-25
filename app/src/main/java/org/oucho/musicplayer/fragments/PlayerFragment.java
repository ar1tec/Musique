package org.oucho.musicplayer.fragments;


import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
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

import com.squareup.picasso.Picasso;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.QueueDbHelper;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.widgets.LockableViewPager;

import java.io.IOException;
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
    private final PlayerService mPlayerService = MainActivity.getPlayerService();

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

        Intent intent = new Intent();
        intent.setAction(INTENT_LAYOUTVIEW);
        intent.putExtra("vue", "playBarLayout");
        mContext.sendBroadcast(intent);


        mHandler.postDelayed(new Runnable() {

            public void run() {

                String artist = PlayerService.getAlbumName();
                getActivity().setTitle(artist);

                Intent intent = new Intent();
                intent.setAction(INTENT_TOOLBAR_SHADOW);
                intent.putExtra("boolean", false);
                mContext.sendBroadcast(intent);

            }
        }, 300);

    }



    /* *********************************************************************************************
     * Création du visuel
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_player, container, false);

        mArtworkSize = getResources().getDimensionPixelSize(R.dimen.fragment_player_art_size);

        SharedPreferences préférences = mContext.getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE);
        track = préférences.getInt("currentPosition", 0) + 1;

        nbTrack = rootView.findViewById(R.id.nombre_titre);
        nbTrack.setText(track + "/" + total_track);

        TextView bitrate = rootView.findViewById(R.id.bitrate);
        bitrate.setText(getBitrate());

        mSeekBar = rootView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        LinearLayout linearLayout = rootView.findViewById(R.id.root);
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

            if (mPlayerService == null) {
                Log.i(TAG_LOG, "if (mPlayerService == null)");

                return;
            }

            String action = intent.getAction();

            switch (action) {

                case PlayerService.PLAYSTATE_CHANGED:

                    if (PlayerService.isPlaying()) {
                        mHandler.post(mUpdateSeekBarRunnable);
                    } else {
                        mHandler.removeCallbacks(mUpdateSeekBarRunnable);
                    }
                    break;

                case PlayerService.META_CHANGED:
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

        LockableViewPager.setSwipeLocked(true);

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

                    int viewID = MainActivity.getViewID();

                    if( ! MainActivity.getPlaylistFragmentState() && ! MainActivity.getAlbumFragmentState())
                    LockableViewPager.setSwipeLocked(false);


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

                        if (!MainActivity.getAlbumFragmentState()) {
                            Intent shadow = new Intent();
                            shadow.setAction(INTENT_TOOLBAR_SHADOW);
                            shadow.putExtra("boolean", true);
                            mContext.sendBroadcast(shadow);
                        }

                        if (!MainActivity.getPlaylistFragmentState() && !MainActivity.getAlbumFragmentState())
                            MainActivity.setMenu(true);

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

            updateTrackInfo();

            if (PlayerService.isPlaying()) {
                mHandler.post(mUpdateSeekBarRunnable);
            }
        }
    }


    private void updateTrackInfo() {

        if (mPlayerService != null) {

            String title = PlayerService.getSongTitle();
            final String artist = PlayerService.getArtistName();
            String album = PlayerService.getAlbumName();

            if (artist != null) {

                if (first_run) {

                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            getActivity().setTitle(artist);
                            first_run = false;
                        }
                    }, 300);

                }  else {
                    getActivity().setTitle(artist);
                }
            }

            if (title != null) {
                //noinspection ConstantConditions
                ((TextView) rootView.findViewById(R.id.song_title)).setText(title);
            }

            if (album != null) {
                //noinspection ConstantConditions
                ((TextView) rootView.findViewById(R.id.song_album)).setText(album);
            }

            ImageView artworkView = rootView.findViewById(R.id.artwork);

            Uri uri = ContentUris.withAppendedId(ARTWORK_URI, PlayerService.getAlbumId());

            Picasso.with(mContext).load(uri).resize(mArtworkSize, mArtworkSize).into(artworkView);


            final int duration = PlayerService.getTrackDuration();

            if (duration != -1) {
                ((TextView) rootView.findViewById(R.id.track_duration)).setText(msToText(duration));

                mSeekBar.setMax(duration);

                updateSeekBar();
            }
        }
    }


    private String msToText(int msec) {
        return String.format(Locale.getDefault(), "%d:%02d", msec / 60000, (msec % 60000) / 1000);
    }

    private void updateSeekBar() {
        if (mPlayerService != null) {
            int position = PlayerService.getPlayerPosition();
            mSeekBar.setProgress(position);

            //noinspection ConstantConditions
            ((TextView) rootView.findViewById(R.id.current_position)).setText(msToText(position));
        }
    }


    private String getBitrate() {
        MediaExtractor mex = new MediaExtractor();
        try {
            mex.setDataSource(getContext(), PlayerService.getSongPath(), null);// the adresss location of the sound on sdcard.
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaFormat mf = mex.getTrackFormat(0);

        Log.d(TAG_LOG, "mf = " + mf);

        String mime = mf.getString(MediaFormat.KEY_MIME);
        Log.d(TAG_LOG, "mime = " + mime);

        if (mime.equals("audio/mp4a-latm")) {
            mime = "aac";
        } else if (mime.equals("audio/mpeg")) {
            mime = "mp3";
        }


        String bitRate = "";
        try {
            bitRate = String.valueOf((mf.getInteger("bit-rate") / 1000) + "kb/s - ");
        } catch (NullPointerException ignore) {

        }

        try {
            bitRate = String.valueOf(mf.getInteger(MediaFormat.KEY_BIT_RATE));
        } catch (NullPointerException ignore) {

        }

        int sampleRate = -1;
        try {
            sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        } catch (NullPointerException ignore) {}

        Log.d(TAG_LOG, "getBitRate : " + bitRate + " " + sampleRate + " ");

        return bitRate + mime + " - " + sampleRate + "Hz";
    }


}
