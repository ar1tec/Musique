package org.oucho.musicplayer;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.oucho.musicplayer.audiofx.AudioEffectsReceiver;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.utils.Notification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.oucho.musicplayer.utils.Permissions;
import org.oucho.musicplayer.db.QueueDbHelper;


public class PlayerService extends Service implements
        MusiqueKeys,
        OnPreparedListener,
        OnErrorListener,
        OnCompletionListener {


    private static final String TAG = "PlayerService";


    public static final int NO_REPEAT = 20;
    public static final int REPEAT_ALL = 21;
    public static final int REPEAT_CURRENT = 22;

    private static final int IDLE_DELAY = 60000;

    private final PlaybackBinder mBinder = new PlaybackBinder();

    private static MediaPlayer mMediaPlayer;
    private static MediaSessionCompat mMediaSession;

    private List<Song> mOriginalSongList = new ArrayList<>();
    private static final List<Song> mPlayList = new ArrayList<>();

    private static boolean mShuffle = false;
    private static boolean mIsPaused = false;
    private static boolean mIsPlaying = false;
    private static boolean mHasPlaylist = false;

    private boolean mBound = false;
    private boolean mPausedByFocusLoss;
    private boolean mAutoPause = false;
    private boolean mPlayImmediately = false;


    private int mStartId;
    private int mCurrentPosition;
    private static int mRepeatMode = NO_REPEAT;

    private Boolean start = false;
    private static Song mCurrentSong;
    private AudioManager mAudioManager;
    private SharedPreferences mStatePrefs;
    private TelephonyManager mTelephonyManager;



    @Override
    public void onCreate() {
        super.onCreate();
        mStatePrefs = getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        Intent i = new Intent(this, AudioEffectsReceiver.class);
        i.setAction(AudioEffectsReceiver.ACTION_OPEN_AUDIO_EFFECT_SESSION);
        i.putExtra(AudioEffectsReceiver.EXTRA_AUDIO_SESSION_ID, mMediaPlayer.getAudioSessionId());
        sendBroadcast(i);

        IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadsetStateReceiver, receiverFilter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAutoPause = prefs.getBoolean(PREF_AUTO_PAUSE, false);

        restoreState();
        initTelephony();
        setupMediaSession();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (mPlayList.size() == 0 || action.equals(ACTION_CHOOSE_SONG)) {

                    Intent dialogIntent = new Intent(this, MainActivity.class);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogIntent);

                } else if (action.equals(ACTION_TOGGLE)) {
                    toggle();
                } else if (action.equals(ACTION_PAUSE)) {
                    pause();
                } else if (action.equals(ACTION_STOP)) {

                    if (!mBound)
                        stopSelf(mStartId);

                } else if (action.equals(ACTION_NEXT)) {
                    playNext(true);
                } else if (action.equals(ACTION_PREVIOUS)) {
                    playPrev();
                }
            }
        }
        //return START_STICKY;
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {

        if (mMediaSession != null) {
            mMediaSession.release();
        }

        unregisterReceiver(mHeadsetStateReceiver);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);

        mMediaPlayer.stop();
        Intent i = new Intent(this, AudioEffectsReceiver.class);
        i.setAction(AudioEffectsReceiver.ACTION_CLOSE_AUDIO_EFFECT_SESSION);
        sendBroadcast(i);
        mMediaPlayer.release();

        super.onDestroy();
    }

    private void restoreState() {

        if (Permissions.checkPermission(this) && mStatePrefs.getBoolean("stateSaved", false)) {

            int position = mStatePrefs.getInt("currentPosition", 0);

            QueueDbHelper dbHelper = new QueueDbHelper(this);
            List<Song> playList = dbHelper.readAll();
            dbHelper.close();

            mRepeatMode = mStatePrefs.getInt("repeatMode", mRepeatMode);
            mShuffle = mStatePrefs.getBoolean("shuffle", mShuffle);

            setPlayListInternal(playList);
            setPosition(position, false);

            open();
        }
    }

    private void initTelephony() {
        if (mAutoPause) {
            mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (mTelephonyManager != null) {
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
    }

    private void setupMediaSession() {
        mMediaSession = new MediaSessionCompat(this, TAG);
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                playNext(true);
            }

            @Override
            public void onSkipToPrevious() {
                playPrev();
            }

            @Override
            public void onStop() {
                pause();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int) pos);
            }
        });
    }





    public void setAutoPauseEnabled() {
        if (!mAutoPause) {
            mAutoPause = true;

            initTelephony();
        }
    }


    public static List<Song> getPlayList() {
        return mPlayList;
    }

    public void setPlayList(List<Song> songList, int position, boolean play) {

        setPlayListInternal(songList);

        setPosition(position, play);
        if (mShuffle) {
            shuffle();
        }
        notifyChange(QUEUE_CHANGED);
    }

    private void setPlayListInternal(List<Song> songList) {
        if (songList == null || songList.size() <= 0) {
            return;
        }

        mOriginalSongList = songList;
        mPlayList.clear();
        mPlayList.addAll(mOriginalSongList);
        mHasPlaylist = true;
    }

    public void addToQueue(Song song) {
        mOriginalSongList.add(song);
        mPlayList.add(song);
        notifyChange(ITEM_ADDED);
    }

    public void notifyChange(String what) {
            updateMediaSession(what);

        boolean saveQueue = (QUEUE_CHANGED.equals(what) || ITEM_ADDED.equals(what) || ORDER_CHANGED.equals(what));

        if (mPlayList.size() > 0) {
            SharedPreferences.Editor editor = mStatePrefs.edit();
            editor.putBoolean("stateSaved", true);

            if (saveQueue) {
                QueueDbHelper dbHelper = new QueueDbHelper(this);
                dbHelper.removeAll();
                dbHelper.add(mPlayList);
                dbHelper.close();
            }

            editor.putInt("currentPosition", mCurrentPosition);
            editor.putInt("repeatMode", mRepeatMode);
            editor.putBoolean("shuffle", mShuffle);
            editor.apply();
        }

        if (PLAYSTATE_CHANGED.equals(what) || META_CHANGED.equals(what)) {
                Notification.updateNotification(this);

            if (isPlaying()) {
                Intent intent = new Intent();
                intent.setAction("org.oucho.radio2.STOP");
		        intent.putExtra("halt", "stop");
                sendBroadcast(intent);
            }

        }

        sendBroadcast(what, null);
    }



    private void updateMediaSession(String what) {

        if (!mMediaSession.isActive()) {
            mMediaSession.setActive(true);
        }

        if (what.equals(PLAYSTATE_CHANGED)) {

            int playState = isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
            mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(playState, getPlayerPosition(), 1.0F)
                    .setActions(PlaybackStateCompat.ACTION_PLAY
                            | PlaybackStateCompat.ACTION_PAUSE
                            | PlaybackStateCompat.ACTION_PLAY_PAUSE
                            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    .build());
        }

        if (what.equals(META_CHANGED)) {
            int largeArtSize = (int) getResources().getDimension(R.dimen.art_size);
            Bitmap artwork = ArtworkCache.getInstance().getCachedBitmap(getAlbumId(), largeArtSize, largeArtSize);

            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getArtistName())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getAlbumName())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getSongTitle())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getTrackDuration())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork);
            mMediaSession.setMetadata(builder.build());
        }
    }

    private void sendBroadcast(String action, Bundle data) {

        Intent i = new Intent(action);
        if (data != null) {
            i.putExtras(data);
        }
        sendBroadcast(i);
    }



    private void play() {


        int result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);

        // previent crash lors du premier lancement à vide de l'application
        // et qu' aucun titre n'a été sélectionné
        try {

            if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mMediaPlayer.start();
                mIsPlaying = true;
                mIsPaused = false;

                notifyChange(PLAYSTATE_CHANGED);

            }

        } catch (NullPointerException ignored) {}



    }

    private void pause() {
        mMediaPlayer.pause();
        mIsPlaying = false;
        mIsPaused = true;
        notifyChange(PLAYSTATE_CHANGED);
    }

    private void resume() {
        play();
    }

    public void toggle() {
        if (mMediaPlayer.isPlaying()) {
            pause();
        } else {
            resume();
        }
    }


/*    public void stop() {
        mMediaPlayer.stop();

        mIsPlaying = false;

        notifyChange(PLAYSTATE_CHANGED);
    }*/


    public void playPrev() {
        int position = getPreviousPosition(true);

        if (position >= 0 && position < mPlayList.size()) {
            mCurrentPosition = position;
            mCurrentSong = mPlayList.get(position);
            openAndPlay();
        }
    }

    private int getPreviousPosition(boolean force) {

        updateCurrentPosition();
        int position = mCurrentPosition;


        if ((mRepeatMode == REPEAT_CURRENT && !force) || (isPlaying() && getPlayerPosition() >= 1500)) {
            return position;
        }


        if (position - 1 < 0) {
            if (mRepeatMode == REPEAT_ALL) {
                return mPlayList.size() - 1;
            }
            return -1;// NO_REPEAT;
        }

        return position - 1;
    }


    public void playNext(boolean force) {
        int position = getNextPosition(force);

        if (position >= 0 && position < mPlayList.size()) {
            mCurrentPosition = position;
            mCurrentSong = mPlayList.get(position);
            openAndPlay();

            Intent intentN = new Intent(INTENT_STATE);
            intentN.putExtra("state", "next");
            sendBroadcast(intentN);

        }
    }

    private int getNextPosition(boolean force) {

        updateCurrentPosition();
        int position = mCurrentPosition;
        if (mRepeatMode == REPEAT_CURRENT && !force) {
            return position;
        }


        if (position + 1 >= mPlayList.size()) {
            if (mRepeatMode == REPEAT_ALL) {
                return 0;
            }
            return -1;
        }
        return position + 1;
    }



    public void setAsNextTrack(Song song) {
        mOriginalSongList.add(song);
        int currentPos = mCurrentPosition;
        mPlayList.add(currentPos + 1, song);
        notifyChange(ITEM_ADDED);
    }

    public int getNextRepeatMode() {
        switch (mRepeatMode) {

            case NO_REPEAT:
                return REPEAT_ALL;

            case REPEAT_ALL:
                return REPEAT_CURRENT;

            case REPEAT_CURRENT:
                return NO_REPEAT;

            default:
                break;
        }
        return NO_REPEAT;
    }

    public void setShuffleEnabled(boolean enable) {

        if (mShuffle != enable) {

            mShuffle = enable;
            if (enable) {
                shuffle();
            } else {
                mPlayList.clear();
                mPlayList.addAll(mOriginalSongList);
            }

            //on met à jour la position
            updateCurrentPosition();
            notifyChange(ORDER_CHANGED);
        }
    }

    private void shuffle() {
        boolean b = mPlayList.remove(mCurrentSong);
        Collections.shuffle(mPlayList);
        if (b) {
            mPlayList.add(0, mCurrentSong);
        }
        setPosition(0, false);
    }

    public void setPosition(int position, boolean play) {
        if (position >= mPlayList.size()) {
            return;
        }
        mCurrentPosition = position;
        Song song = mPlayList.get(position);
        if (!song.equals(mCurrentSong)) {
            mCurrentSong = song;
            if (play) {
                openAndPlay();
            } else {
                open();
            }
        } else if (play) {
            play();
        }
    }


    private void updateCurrentPosition() {
        int pos = mPlayList.indexOf(mCurrentSong);
        if (pos != -1) {
            mCurrentPosition = pos;
        }
    }

    private void openAndPlay() {

        mPlayImmediately = true;

        open();
    }

    private void open() {

        Bundle extras = new Bundle();
        extras.putInt(EXTRA_POSITION, getPositionWithinPlayList());
        sendBroadcast(POSITION_CHANGED, extras);

        mMediaPlayer.reset();

        Uri songUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                mCurrentSong.getId());

        try {
            mMediaPlayer.setDataSource(getApplicationContext(), songUri);
            mMediaPlayer.prepareAsync();

        } catch (IllegalArgumentException
                | SecurityException
                | IllegalStateException
                | IOException e) {
            Log.e("open() ee", "ee", e);
        }

    }


    private final BroadcastReceiver mHeadsetStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG) && isPlaying()) {
                boolean plugged = intent.getIntExtra("state", 0) == 1;
                if (!plugged) {
                    pause();
                }
            }
        }
    };

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    pause();
                    break;
                default:
                    break;
            }
        }
    };


    @SuppressLint("HandlerLeak")
    private final Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isPlaying() || mBound) {
                return;
            }

            stopSelf(mStartId);
        }
    };


    private final OnAudioFocusChangeListener mAudioFocusChangeListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (isPlaying()) {
                        pause();
                        mPausedByFocusLoss = true;
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (!isPlaying() && mPausedByFocusLoss) {
                        resume();
                        mPausedByFocusLoss = false;
                    }
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (isPlaying()) {
                        mMediaPlayer.setVolume(0.1f, 0.1f);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
                    pause();
                    mPausedByFocusLoss = false;
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    public void onCompletion(MediaPlayer mp) {

        playNext(false);

        if (mCurrentPosition+1 == mPlayList.size()) {
            mIsPlaying = false;
            mIsPaused = true;
            notifyChange(PLAYSTATE_CHANGED);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError " + String.valueOf(what) + " " + String.valueOf(extra));

        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // évite de charger la notification au démarrage de l'application
        if (!start) {
            start = true;
        } else {
            notifyChange(META_CHANGED);

        }

        if (mPlayImmediately) {
            play();
            mPlayImmediately = false;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBound = false;
        if (isPlaying()) {
            return true;
        }

        if (mPlayList.size() > 0) {
            Message msg = mDelayedStopHandler.obtainMessage();
            mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
            return true;
        }

        stopSelf(mStartId);
        return true;
    }



    public static void seekTo(int msec) {
        mMediaPlayer.seekTo(msec);
    }

    public static boolean isPlaying() {
        return mIsPlaying;
    }


    public static boolean isPaused() {
        return mIsPaused;
    }

    public static boolean hasPlaylist() {
        return mHasPlaylist;
    }

    public static boolean isShuffleEnabled() {
        return mShuffle;
    }

    public static void setVolume(float vol) {
        mMediaPlayer.setVolume(vol, vol);
    }

    public void setRepeatMode(int mode) {
        mRepeatMode = mode;
        notifyChange(REPEAT_MODE_CHANGED);
    }

    public static int getRepeatMode() {
        return mRepeatMode;
    }


    public static String getSongTitle() {
        if (mCurrentSong != null) {
            return mCurrentSong.getTitle();
        }
        return null;
    }

    public static String getArtistName() {
        if (mCurrentSong != null) {
            return mCurrentSong.getArtist();
        }
        return null;
    }

    public static String getAlbumName() {
        if (mCurrentSong != null) {
            return mCurrentSong.getAlbum();
        }
        return null;
    }

    public static long getAlbumId() {
        if (mCurrentSong != null) {
            return mCurrentSong.getAlbumId();
        }
        return -1;
    }

    public static long getSongID() {
        if (mCurrentSong != null) {
            return mCurrentSong.getId();
        }
        return -1;
    }

    public static int getTrackDuration() {
        return mMediaPlayer.getDuration();
    }

    public static int getPlayerPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public static int getPositionWithinPlayList() {
        return mPlayList.indexOf(mCurrentSong);
    }

    public static MediaSessionCompat getMediaSession() {
        return mMediaSession;
    }

    public class PlaybackBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

}