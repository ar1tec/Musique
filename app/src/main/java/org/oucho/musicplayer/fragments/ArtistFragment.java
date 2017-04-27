package org.oucho.musicplayer.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.activities.SearchActivity;
import org.oucho.musicplayer.db.loaders.SongLoader;
import org.oucho.musicplayer.db.loaders.SortOrder;
import org.oucho.musicplayer.db.model.Artist;
import org.oucho.musicplayer.db.model.Playlist;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.dialog.SongEditorDialog;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.widgets.FastScroller;

import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class ArtistFragment extends BaseFragment implements MusiqueKeys {

    private static final String PARAM_ARTIST_ID = "artist_id";
    private static final String PARAM_ARTIST_NAME = "artist_name";
    private static final String PARAM_ALBUM_COUNT = "album_count";
    private static final String PARAM_TRACK_COUNT = "track_count";

    private final String TAG_LOG = "Artist Fragment";

    private Artist mArtist;

    private SongListAdapter mSongListAdapter;


    private final LoaderManager.LoaderCallbacks<List<Song>> mSongLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(getActivity());

            loader.setSelection(MediaStore.Audio.Media.ARTIST_ID + " = ?", new String[]{String.valueOf(mArtist.getId())});

            loader.setSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);

            return loader;
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            //  Auto-generated method stub
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> songList) {
            mSongListAdapter.setData(songList);
        }
    };


    private final SongEditorDialog.OnTagsEditionSuccessListener mOnTagsEditionSuccessListener
            = new SongEditorDialog.OnTagsEditionSuccessListener() {
        @Override
        public void onTagsEditionSuccess() {
            ((MainActivity) getActivity()).refresh();
        }
    };


    private int mThumbWidth;
    private int mThumbHeight;


    public static ArtistFragment newInstance(Artist artist) {
        ArtistFragment fragment = new ArtistFragment();
        Bundle args = new Bundle();
        args.putLong(PARAM_ARTIST_ID, artist.getId());
        args.putString(PARAM_ARTIST_NAME, artist.getName());
        args.putInt(PARAM_TRACK_COUNT, artist.getTrackCount());
        fragment.setArguments(args);
        return fragment;
    }

    private void selectSong(int position) {

        if (MainActivity.getChercheActivity()) {

            Log.i(TAG_LOG, "selectedSong(), if (MainActivity.getChercheActivity())");

            Song song = mSongListAdapter.getItem(position);
            Bundle data = songToBundle(song);
            returnToMain(ACTION_PLAY_SONG, data);
        }
    }

    private Bundle songToBundle(Song song) {
        Bundle data = new Bundle();
        data.putLong(SONG_ID, song.getId());
        data.putString(SONG_TITLE, song.getTitle());
        data.putString(SONG_ARTIST, song.getArtist());
        data.putString(SONG_ALBUM, song.getAlbum());
        data.putLong(SONG_ALBUM_ID, song.getAlbumId());
        data.putInt(SONG_TRACK_NUMBER, song.getTrackNumber());
        return data;
    }

    private void returnToMain(String action, Bundle data) {
        Intent i = new Intent(action);

        if (data != null)
            i.putExtras(data);

        getActivity().setResult(RESULT_OK, i);
        getActivity().finish();
    }

    private void showSongMenu(final int position, View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        final Song song = mSongListAdapter.getItem(position);
        inflater.inflate(R.menu.album_song_item, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_add_to_queue:
                        ((MainActivity) getActivity()).addToQueue(song);
                        return true;
                    case R.id.action_edit_tags:
                        showID3TagEditor(song);
                        return true;
                    case R.id.action_add_to_playlist:
                        showPlaylistPicker(song);
                        return true;
                    default: //do nothing
                        break;
                }
                return false;
            }
        });
        popup.show();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mThumbWidth = context.getResources().getDimensionPixelSize(R.dimen.art_thumbnail_size);
        //noinspection SuspiciousNameCombination
        mThumbHeight = mThumbWidth;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setHasOptionsMenu(true);

        String name = "";

        if (args != null) {
            long id = args.getLong(PARAM_ARTIST_ID);
            name = args.getString(PARAM_ARTIST_NAME);
            int albumCount = args.getInt(PARAM_ALBUM_COUNT);
            int trackCount = args.getInt(PARAM_TRACK_COUNT);
            mArtist = new Artist(id, name, albumCount, trackCount);
        }

        if (MainActivity.getChercheActivity())
            MainActivity.setArtistFragmentState(true);


        if (android.os.Build.VERSION.SDK_INT >= 24) {
            getActivity().setTitle(Html.fromHtml("<font color=\"#FFA000\">" + name + " </font> <small> <font color=\"#CCCCCC\">", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            getActivity().setTitle(Html.fromHtml("<font color=\"#FFA000\">" + name + " </font>"));
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_artist, container, false);

        RecyclerView mSongListView = (RecyclerView) rootView.findViewById(R.id.song_list);


        mSongListView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mSongListAdapter = new SongListAdapter();
        mSongListView.setAdapter(mSongListAdapter);

        FastScroller mFastScroller = (FastScroller) rootView.findViewById(R.id.fastscroller);
        mFastScroller.setRecyclerView(mSongListView);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, mSongLoaderCallbacks);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.artist, menu);
    }



    @Override
    public void load() {
        getLoaderManager().restartLoader(0, null, mSongLoaderCallbacks);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG_LOG, "onPause()");

        SearchActivity.setActionBar();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG_LOG, "onResume()");


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

                    Log.i(TAG_LOG, "onResume(), KeyEvent");

                    MainActivity.setArtistFragmentState(false);

                    SearchActivity.setActionBar();


                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                    ft.remove(getFragmentManager().findFragmentById(R.id.container_search));
                    ft.commit();

                    return true;

                }
                return false;
            }
        });

    }


    private class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView vTitle;
        private final TextView vArtist;
        private final ImageView vArtwork;
        private final TextView vDuration;

        SongViewHolder(View itemView) {
            super(itemView);
            vTitle = (TextView) itemView.findViewById(R.id.title);
            vArtist = (TextView) itemView.findViewById(R.id.artist);
            vArtwork = (ImageView) itemView.findViewById(R.id.artwork);
            vDuration = (TextView) itemView.findViewById(R.id.duration);

            itemView.findViewById(R.id.item_view).setOnClickListener(this);

            ImageButton menuButton = (ImageButton) itemView.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(this);

            Drawable drawable = menuButton.getDrawable();

            drawable.mutate();

        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition() - 1;

            switch (v.getId()) {
                case R.id.item_view:
                    selectSong(position);
                    break;
                case R.id.menu_button:
                    showSongMenu(position, v);
                    break;
                default: //do nothing
                    break;
            }
        }
    }


    private class SongListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int FIRST = 1;
        private static final int NORMAL = 2;

        private List<Song> mSongList;

        public void setData(List<Song> data) {
            mSongList = data;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_song_item, parent, false);

            return new SongViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);

            if (viewType == NORMAL) {

                Song song = getItem(position -1);

                ((SongViewHolder) holder).vTitle.setText(song.getTitle());
                ((SongViewHolder) holder).vArtist.setText(song.getArtist());
                ((SongViewHolder) holder).vDuration.setText("(" + msToText(song.getDuration()) + ")");

                ImageView artworkView = ((SongViewHolder) holder).vArtwork;

                //évite de charger des images dans les mauvaises vues si elles sont recyclées
                artworkView.setTag(position);

                ArtworkCache.getInstance().loadBitmap(song.getAlbumId(), artworkView, mThumbWidth, mThumbHeight);
            }
        }

        public Song getItem(int position) {
            return mSongList == null ? null : mSongList.get(position);
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? FIRST : NORMAL;
        }

        @Override
        public int getItemCount() {
            return mSongList == null ? 1 : mSongList.size() + 1;

        }
    }


    private void showID3TagEditor(Song song) {
        SongEditorDialog dialog = SongEditorDialog.newInstance(song);
        dialog.setOnTagsEditionSuccessListener(mOnTagsEditionSuccessListener);
        dialog.show(getChildFragmentManager(), "edit_tags");
    }

    private void showPlaylistPicker(final Song song) {
        PlaylistPickerDialog picker = PlaylistPickerDialog.newInstance();
        picker.setListener(new PlaylistPickerDialog.OnPlaylistPickedListener() {
            @Override
            public void onPlaylistPicked(Playlist playlist) {
                PlaylistsUtils.addSongToPlaylist(getActivity().getContentResolver(), playlist.getId(), song.getId());
            }
        });
        picker.show(getChildFragmentManager(), "pick_playlist");

    }

    private String msToText(int msec) {
        return String.format(Locale.getDefault(), "%d:%02d", msec / 60000, (msec % 60000) / 1000);
    }



}
