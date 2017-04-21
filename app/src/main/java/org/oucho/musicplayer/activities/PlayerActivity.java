package org.oucho.musicplayer.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.oucho.musicplayer.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.images.blurview.BlurView;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.db.QueueDbHelper;
import org.oucho.musicplayer.utils.CustomLayoutManager;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.utils.NavigationUtils;
import org.oucho.musicplayer.widgets.DragRecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity
        implements MusiqueKeys,
        SharedPreferences.OnSharedPreferenceChangeListener {


    private SeekBar mSeekBar;
    private View mQueueLayout;
    private BlurView mBlurView;
    private ActionBar actionBar;
    private FrameLayout mLayout;
    private DragRecyclerView mQueueView;
    private PlayerService mPlayerService;

    private final Handler mHandler = new Handler();
    private final PlaybackRequests mPlaybackRequests = new PlaybackRequests();

    private List<Song> mQueue;
    private QueueAdapter mQueueAdapter = new QueueAdapter();

    private boolean mServiceBound;

    private int total_track = -1;
    private int track = -1;
    private int mArtworkSize;
    private int couleurSousTitre;


    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

            getWindow().getDecorView().setSystemUiVisibility(mUIFlag);

            Context context = getApplicationContext();

            getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.blanc));
        }

        SharedPreferences préférences = getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE);
        préférences.registerOnSharedPreferenceChangeListener(this);

        total_track = getSizeQueue();

        track = préférences.getInt("currentPosition", 0) + 1;

        Context context = getApplicationContext();
        int couleurTitre = ContextCompat.getColor(context, R.color.colorAccent);
        couleurSousTitre = ContextCompat.getColor(context, R.color.secondary_text);

        actionBar = getSupportActionBar();
        assert actionBar != null;

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            actionBar.setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>En cours de lecture</font>", Html.FROM_HTML_MODE_LEGACY));
            actionBar.setSubtitle(Html.fromHtml("<small><font color='" + couleurSousTitre + "'>" + track + "/" + total_track + "</font><small>", Html.FROM_HTML_MODE_LEGACY));

        } else {
            //noinspection deprecation
            actionBar.setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>En cours de lecture</font>"));
            //noinspection deprecation
            actionBar.setSubtitle(Html.fromHtml("<small><font color='" + couleurSousTitre + "'>" + track + "/" + total_track + "</font><small>"));

        }
        actionBar.setElevation(0);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.activity_player);

        mArtworkSize = getResources().getDimensionPixelSize(R.dimen.playback_activity_art_size);

        ImageView button_next = (ImageView) findViewById(R.id.next);
        ImageView button_prev = (ImageView) findViewById(R.id.prev);

        mBlurView = (BlurView) findViewById(R.id.blurView);

        mLayout = (FrameLayout) findViewById(R.id.root);

        setupBlurView();

        assert button_next != null;
        button_next.setColorFilter(couleurTitre, PorterDuff.Mode.SRC_ATOP);
        assert button_prev != null;
        button_prev.setColorFilter(couleurTitre, PorterDuff.Mode.SRC_ATOP);

        final Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_downward_grey_600_24dp);
        //noinspection ConstantConditions
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        mQueueLayout = findViewById(R.id.queue_layout);
        mQueueView = (DragRecyclerView) findViewById(R.id.queue_view);
        mQueueView.setLayoutManager(new CustomLayoutManager(this));
        mQueueAdapter = new QueueAdapter();
        mQueueView.setOnItemMovedListener(new DragRecyclerView.OnItemMovedListener() {
            @Override
            public void onItemMoved(int oldPosition, int newPosition) {
                mQueueAdapter.moveItem(oldPosition, newPosition);
            }
        });
        mQueueView.setAdapter(mQueueAdapter);

        findViewById(R.id.prev).setOnClickListener(mOnClickListener);
        findViewById(R.id.next).setOnClickListener(mOnClickListener);
        findViewById(R.id.play_pause_toggle).setOnClickListener(mOnClickListener);
        findViewById(R.id.shuffle).setOnClickListener(mOnClickListener);
        findViewById(R.id.repeat).setOnClickListener(mOnClickListener);

        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        //mSeekBar.getThumb().mutate().setAlpha(0);

    }



    private void setupBlurView() {
        final float radius = 5f;

        //set background, if your root layout doesn't have one
        final Drawable windowBackground = getWindow().getDecorView().getBackground();

        /// mDrawerLayout pour indiquer la racine du layout
        mBlurView.setupWith(mLayout)
                .windowBackground(windowBackground)
                .blurRadius(radius);
    }


    private int getSizeQueue() {
        QueueDbHelper dbHelper = new QueueDbHelper(this);
        List<Song> playList = dbHelper.readAll();
        dbHelper.close();

        return playList.size();
    }

    private final Runnable mUpdateSeekBarRunnable = new Runnable() {

        @Override
        public void run() {

            updateSeekBar();

            mHandler.postDelayed(mUpdateSeekBarRunnable, 1000);
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
            total_track = getSizeQueue();

            SharedPreferences préférences = getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE);
            track = préférences.getInt("currentPosition", 0) + 1;

            assert actionBar != null;

            if (android.os.Build.VERSION.SDK_INT >= 24) {
                actionBar.setSubtitle(Html.fromHtml("<small><font color='" + couleurSousTitre + "'>" + track + "/" + total_track + "</font><small>", Html.FROM_HTML_MODE_LEGACY));
            } else {
                //noinspection deprecation
                actionBar.setSubtitle(Html.fromHtml("<small><font color='" + couleurSousTitre + "'>" + track + "/" + total_track + "</font><small>"));
            }
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            if (mPlayerService == null) {
                return;
            }
            switch (v.getId()) {
                case R.id.play_pause_toggle:
                    mPlayerService.toggle();
                    break;

                case R.id.prev:
                    mPlayerService.playPrev();
                    break;

                case R.id.next:
                    mPlayerService.playNext(true);
                    break;

                case R.id.shuffle:
                    boolean shuffle = PlayerService.isShuffleEnabled();
                    mPlayerService.setShuffleEnabled(!shuffle);
                    updateShuffleButton();
                    break;

                case R.id.repeat:
                    int mode = mPlayerService.getNextRepeatMode();
                    mPlayerService.setRepeatMode(mode);
                    updateRepeatButton();
                    break;

                default:
                    break;
            }
        }
    };


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            PlayerService.PlaybackBinder binder = (PlayerService.PlaybackBinder) service;
            mPlayerService = binder.getService();
            if (mPlayerService == null || !PlayerService.hasPlaylist()) {
                finish();
            }
            mServiceBound = true;

            mPlaybackRequests.sendRequests();

            updateAll();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;

        }
    };

    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPlayerService == null) {
                return;
            }
            String action = intent.getAction();

            switch (action) {
                case PlayerService.PLAYSTATE_CHANGED:
                    setButtonDrawable();
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
                    updateQueue();
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.player_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                NavigationUtils.showMainActivity(this);
                return true;
            case R.id.action_equalizer:
                NavigationUtils.showEqualizer(this);
                return true;
            case R.id.action_view_queue:
                toggleQueue();
                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleQueue() {
        if (mQueueLayout.getVisibility() != View.VISIBLE) {
            mQueueLayout.setVisibility(View.VISIBLE);
            mBlurView.setVisibility(View.VISIBLE);
        } else {
            mQueueLayout.setVisibility(View.GONE);
            mBlurView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (mQueueLayout.getVisibility() == View.VISIBLE) {
            mQueueLayout.setVisibility(View.GONE);
            mBlurView.setVisibility(View.GONE);
        } else {
            NavigationUtils.showMainActivity(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mServiceListener);
        mPlayerService = null;

        if (mServiceBound) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
        mHandler.removeCallbacks(mUpdateSeekBarRunnable);
    }

    @Override
    protected void onResume() {
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
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

    private void updateAll() {
        if (mPlayerService != null) {
            updateQueue();
            updateTrackInfo();
            setButtonDrawable();
            if (PlayerService.isPlaying()) {
                mHandler.post(mUpdateSeekBarRunnable);
            }
            updateShuffleButton();
            updateRepeatButton();
        }
    }


    private void updateTrackInfo() {
        if (mPlayerService != null) {

            String title = PlayerService.getSongTitle();
            String artist = PlayerService.getArtistName();

            if (title != null) {
                //noinspection ConstantConditions
                ((TextView) findViewById(R.id.song_title)).setText(title);
            }

            if (artist != null) {
                //noinspection ConstantConditions
                ((TextView) findViewById(R.id.song_artist)).setText(artist);
            }

            ImageView artworkView = (ImageView) findViewById(R.id.artwork);
            ArtworkCache.getInstance().loadBitmap(PlayerService.getAlbumId(), artworkView, mArtworkSize, mArtworkSize);

            int duration = PlayerService.getTrackDuration();
            if (duration != -1) {
                mSeekBar.setMax(duration);
                //noinspection ConstantConditions
                ((TextView) findViewById(R.id.track_duration)).setText(msToText(duration));
                updateSeekBar();
            }

            setQueueSelection(PlayerService.getPositionWithinPlayList());
        }
    }


    private void setButtonDrawable() {

        Context context = getApplicationContext();

        int couleur = ContextCompat.getColor(context, R.color.colorAccent);

        if (mPlayerService != null) {
            ImageView button = (ImageView) findViewById(R.id.play_pause_toggle);
            if (PlayerService.isPlaying()) {
                assert button != null;
                button.setImageResource(R.drawable.ic_pause_circle_filled_grey_600_48dp);
            } else {
                assert button != null;
                button.setImageResource(R.drawable.ic_play_circle_filled_grey_600_48dp);
            }
            button.setColorFilter(couleur, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private void updateShuffleButton() {
        boolean shuffle = PlayerService.isShuffleEnabled();
        ImageView shuffleButton = (ImageView) findViewById(R.id.shuffle);
        if (shuffle) {
            assert shuffleButton != null;
            shuffleButton.setImageResource(R.drawable.ic_shuffle_grey_600_24dp);

        } else {
            assert shuffleButton != null;
            shuffleButton.setImageResource(R.drawable.ic_shuffle_grey_400_24dp);

        }
    }

    private void updateRepeatButton() {
        ImageView repeatButton = (ImageView) findViewById(R.id.repeat);
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

    private String msToText(int msec) {
        return String.format(Locale.getDefault(), "%d:%02d", msec / 60000,
                (msec % 60000) / 1000);
    }

    private void updateSeekBar() {
        if (mPlayerService != null) {
            int position = PlayerService.getPlayerPosition();
            mSeekBar.setProgress(position);

            //noinspection ConstantConditions
            ((TextView) findViewById(R.id.current_position)).setText(msToText(position));
        }
    }


    private void setQueueSelection(int position) {
        mQueueAdapter.setSelection(position);

        mQueueView.smoothScrollToPosition(position);
    }


    private class QueueItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnTouchListener {

        final TextView vTitle;
        final TextView vArtist;
        final ImageButton vReorderButton;
        final View itemView;

        public QueueItemViewHolder(View itemView) {
            super(itemView);
            vTitle = (TextView) itemView.findViewById(R.id.title);
            vArtist = (TextView) itemView.findViewById(R.id.artist);
            vReorderButton = (ImageButton) itemView.findViewById(R.id.reorder_button);
            vReorderButton.setOnTouchListener(this);
            itemView.findViewById(R.id.song_info).setOnClickListener(this);
            itemView.findViewById(R.id.delete_button).setOnClickListener(this);
            this.itemView = itemView;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mQueueView.startDrag(itemView);
            return false;
        }

        @Override
        public void onClick(View v) {
            if (mPlayerService != null) {

                int position = getAdapterPosition();

                switch (v.getId()) {
                    case R.id.song_info:
                        mPlayerService.setPosition(position, true);
                        break;

                    case R.id.delete_button:
                        if (mQueueAdapter.getItemCount() > 0)
                            mQueueAdapter.removeItem(position);
                        break;

                    default:
                        break;
                }

            }
        }
    }

    private class QueueAdapter extends RecyclerView.Adapter<QueueItemViewHolder> {

        private List<Song> mQueue;
        private int mSelectedItemPosition = -1;

        public void setQueue(List<Song> queue) {
            mQueue = queue;
            notifyDataSetChanged();
        }

        @Override
        public QueueItemViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.activity_player_queue_item, parent, false);

            return new QueueItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(QueueItemViewHolder viewHolder, int position) {
            Song song = mQueue.get(position);

            if (position == mSelectedItemPosition) {
                viewHolder.itemView.setSelected(true);
            } else {
                viewHolder.itemView.setSelected(false);

            }

            viewHolder.vTitle.setText(song.getTitle());
            viewHolder.vArtist.setText(song.getArtist());
        }

        @Override
        public int getItemCount() {
            if (mQueue == null) {
                return 0;
            }
            return mQueue.size();
        }

        void moveItem(int oldPosition, int newPosition) {
            if (oldPosition < 0 || oldPosition >= mQueue.size()
                    || newPosition < 0 || newPosition >= mQueue.size()) {
                return;
            }

            Collections.swap(mQueue, oldPosition, newPosition);

            if (mSelectedItemPosition == oldPosition) {
                mSelectedItemPosition = newPosition;
            } else if (mSelectedItemPosition == newPosition) {
                mSelectedItemPosition = oldPosition;
            }
            notifyItemMoved(oldPosition, newPosition);

        }

        void removeItem(int position) {
            mQueue.remove(position);
            notifyItemRemoved(position);
        }

        void setSelection(int position) {
            int oldSelection = mSelectedItemPosition;
            mSelectedItemPosition = position;

            if (oldSelection >= 0 && oldSelection < mQueue.size()) {
                notifyItemChanged(oldSelection);
            }

            if (mSelectedItemPosition >= 0 && mSelectedItemPosition < mQueue.size()) {
                notifyItemChanged(mSelectedItemPosition);
            }
        }
    }

    private class PlaybackRequests {

        private List<Song> mPlayList;
        private int mIndex;
        private boolean mAutoPlay;

        private Song mNextTrack;
        private Song mAddToQueue;

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

}
