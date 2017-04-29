package org.oucho.musicplayer.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.loaders.AlbumLoader;
import org.oucho.musicplayer.db.loaders.ArtistLoader;
import org.oucho.musicplayer.db.loaders.BaseLoader;
import org.oucho.musicplayer.db.loaders.SongLoader;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Artist;
import org.oucho.musicplayer.db.model.Playlist;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.fragments.AlbumFragment;
import org.oucho.musicplayer.fragments.ArtistFragment;
import org.oucho.musicplayer.images.ArtistImageCache;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.widgets.fastscroll.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements MusiqueKeys {


    private static final String FILTER = "filter";
    private boolean mAlbumListLoaded = false;
    private boolean mArtistListLoaded = false;
    private boolean mSongListLoaded = false;
    private View mEmptyView;
    private SearchAdapter mAdapter;
    private int mThumbSize;
    private FastScrollRecyclerView mRecyclerView;

    private static final String TAG_LOG = "Search Activity";

    private final LoaderManager.LoaderCallbacks<List<Album>> mAlbumLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<Album>>() {

        @Override
        public Loader<List<Album>> onCreateLoader(int id, Bundle args) {

            AlbumLoader loader = new AlbumLoader(SearchActivity.this, null);
            setLoaderFilter(args, loader);

            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Album>> loader, List<Album> data) {
            mAlbumListLoaded = true;
            mAdapter.setAlbumList(data);
        }

        @Override
        public void onLoaderReset(Loader<List<Album>> loader) {
            // This constructor is intentionally empty, pourquoi ? parce que !
        }
    };



    private final LoaderManager.LoaderCallbacks<List<Artist>> mArtistLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<Artist>>() {

        @Override
        public void onLoadFinished(Loader<List<Artist>> loader, List<Artist> data) {
            mArtistListLoaded = true;
            mAdapter.setArtistList(data);
        }

        @Override
        public void onLoaderReset(Loader<List<Artist>> loader) {
            // This constructor is intentionally empty, pourquoi ? parce que !
        }

        @Override
        public Loader<List<Artist>> onCreateLoader(int id, Bundle args) {

            ArtistLoader loader = new ArtistLoader(SearchActivity.this);
            setLoaderFilter(args, loader);

            return loader;
        }
    };


    private final LoaderManager.LoaderCallbacks<List<Song>> mSongLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<Song>>() {

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            //  Auto-generated method stub
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> songList) {

            mSongListLoaded = true;
            mAdapter.setSongList(songList);
        }

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(SearchActivity.this);
            setLoaderFilter(args, loader);

            return loader;
        }
    };


    private final SearchView.OnQueryTextListener searchQueryListener = new SearchView.OnQueryTextListener() {

        @Override
        public boolean onQueryTextSubmit(String query) {
            //  Auto-generated method stub
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            refresh(newText);

            return true;
        }

    };

    private final RecyclerView.AdapterDataObserver mEmptyObserver = new RecyclerView.AdapterDataObserver() {

        @Override
        public void onChanged() {
            if (mAdapter.getItemCount() == 0) {
                mEmptyView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }

        }
    };



    private static void setLoaderFilter(Bundle args, BaseLoader loader) {
        String filter;
        if (args != null) {
            filter = args.getString(FILTER);
        } else {
            filter = "";
        }
        loader.setFilter(filter);
    }

    static ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context mContext = getApplicationContext();

        MainActivity.setChercheActivity(true);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

            getWindow().getDecorView().setSystemUiVisibility(mUIFlag);
            getWindow().setStatusBarColor(ContextCompat.getColor(mContext, R.color.white));
        }


        setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        actionBar = getSupportActionBar();

        assert actionBar != null;
        SearchView searchView = new SearchView(actionBar.getThemedContext());

        searchView.setIconifiedByDefault(false);
        actionBar.setCustomView(searchView);
        actionBar.setDisplayShowCustomEnabled(true);

        searchView.setOnQueryTextListener(searchQueryListener);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mThumbSize = getResources().getDimensionPixelSize(R.dimen.art_thumbnail_search_size);
        mEmptyView = findViewById(R.id.empty_view);

        mRecyclerView = (FastScrollRecyclerView) findViewById(R.id.recycler_view);
        mAdapter = new SearchAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mRecyclerView.setAdapter(mAdapter);

        mAdapter.registerAdapterDataObserver(mEmptyObserver);

        getSupportLoaderManager().initLoader(0, null, mAlbumLoaderCallbacks);
        getSupportLoaderManager().initLoader(1, null, mArtistLoaderCallbacks);
        getSupportLoaderManager().initLoader(2, null, mSongLoaderCallbacks);

    }



    private void refresh(String newText) {
        Bundle args = null;
        if (newText != null) {
            args = new Bundle();
            args.putString(FILTER, newText);
        }

        mAlbumListLoaded = false;
        mArtistListLoaded = false;
        mSongListLoaded = false;
        getSupportLoaderManager().restartLoader(0, args, mAlbumLoaderCallbacks);
        getSupportLoaderManager().restartLoader(1, args, mArtistLoaderCallbacks);
        getSupportLoaderManager().restartLoader(2, args, mSongLoaderCallbacks);
    }


    private void returnToMain(String action, Bundle data) {
        Intent i = new Intent(action);

        if (data != null)
            i.putExtras(data);

        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.i(TAG_LOG, "onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG_LOG, "onResume()");

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Log.i(TAG_LOG, "onBackPressed()");

        finish();
    }

    public static void setActionBar() {
        actionBar.setDisplayShowCustomEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG_LOG, "onDestroy()");

        MainActivity.setChercheActivity(false);
        MainActivity.setArtistFragmentState(false);
        MainActivity.setAlbumFragmentState(false);
    }

    /* *********************************************************************************************
     * Vue liste album
     * ********************************************************************************************/

    private class AlbumViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView vArtwork;
        final TextView vName;
        final TextView vArtist;


        private AlbumViewHolder(View itemView) {
            super(itemView);
            vArtwork = (ImageView) itemView.findViewById(R.id.album_artwork);
            vName = (TextView) itemView.findViewById(R.id.album_name);
            vArtist = (TextView) itemView.findViewById(R.id.artist_name);
            vArtwork.setOnClickListener(this);
            itemView.findViewById(R.id.album_info).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();

            Album album = (Album) mAdapter.getItem(position);

            switch (v.getId()) {
                case R.id.album_info:
                    Log.i(TAG_LOG, "album id " + album.getId() + " " + album.getAlbumName());
                    Bundle data = new Bundle();
                    data.putLong(ALBUM_ID, album.getId());
                    data.putString(ALBUM_NAME, album.getAlbumName());
                    data.putString(ALBUM_ARTIST, album.getArtistName());
                    data.putInt(ALBUM_YEAR, album.getYear());
                    data.putInt(ALBUM_TRACK_COUNT, album.getTrackCount());

                    Album toto = getAlbumFromBundle(data);
                    AlbumFragment fragment = AlbumFragment.newInstance(toto);

                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_in_bottom);
                    ft.replace(R.id.container_search, fragment);
                    ft.addToBackStack(null);
                    ft.commit();

                    actionBar.setDisplayShowCustomEnabled(false);

                    break;

                default:
                    break;
            }
        }
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


    /* *********************************************************************************************
     * Vue liste artiste
     * ********************************************************************************************/

    private class ArtistViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView vName;
        final ImageView vArtistImage;

        ArtistViewHolder(View itemView) {
            super(itemView);
            vName = (TextView) itemView.findViewById(R.id.artist_name);
            vArtistImage = (ImageView) itemView.findViewById(R.id.artwork);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            int position = getAdapterPosition();

            Artist artist = (Artist) mAdapter.getItem(position);

            Bundle data = new Bundle();
            data.putLong(ARTIST_ARTIST_ID, artist.getId());
            data.putString(ARTIST_ARTIST_NAME, artist.getName());
            data.putInt(ARTIST_TRACK_COUNT, artist.getTrackCount());

            Artist toto =  getArtistFromBundle(data);
            ArtistFragment fragment = ArtistFragment.newInstance(toto);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_in_bottom);
            ft.replace(R.id.container_search, fragment, "Artist");
            ft.addToBackStack(null);
            ft.commit();

            actionBar.setDisplayShowCustomEnabled(false);
        }
    }



    /* *********************************************************************************************
     * Vue liste morceau
     * ********************************************************************************************/

    private class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView vTitle;
        final TextView vArtist;
        final ImageView vArtwork;

        SongViewHolder(View itemView) {

            super(itemView);
            vTitle = (TextView) itemView.findViewById(R.id.title);
            vArtist = (TextView) itemView.findViewById(R.id.artist);
            vArtwork = (ImageView) itemView.findViewById(R.id.artwork);
            itemView.findViewById(R.id.item_view).setOnClickListener(this);

            ImageButton menuButton = (ImageButton) itemView.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();

            Song song = (Song) mAdapter.getItem(position);
            switch (v.getId()) {
                case R.id.item_view:
                    selectSong(song);
                    break;

                case R.id.menu_button:
                    showMenu(song, v);
                    break;

                default:
                    break;
            }
        }


        private void showMenu(final Song song, View v) {
            PopupMenu popup = new PopupMenu(SearchActivity.this, v);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.search_song_list_item, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Bundle data;
                    switch (item.getItemId()) {
                        case R.id.action_add_to_queue:
                            data = songToBundle(song);
                            returnToMain(ACTION_ADD_TO_QUEUE, data);
                            return true;

                        case R.id.action_add_to_playlist:
                            showPlaylistPicker(song);
                            return true;

                        default:
                            break;
                    }
                    return false;
                }
            });
            popup.show();
        }

        private void showPlaylistPicker(final Song song) {
            PlaylistPickerDialog picker = PlaylistPickerDialog.newInstance();
            picker.setListener(new PlaylistPickerDialog.OnPlaylistPickedListener() {
                @Override
                public void onPlaylistPicked(Playlist playlist) {
                    PlaylistsUtils.addSongToPlaylist(getContentResolver(), playlist.getId(), song.getId());
                }
            });
            picker.show(getSupportFragmentManager(), "pick_playlist");

        }

        private void selectSong(Song song) {
            Bundle data = songToBundle(song);

            returnToMain(ACTION_PLAY_SONG, data);
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
    }


    private class SectionViewHolder extends RecyclerView.ViewHolder {

        final TextView vSection;

        SectionViewHolder(View itemView) {
            super(itemView);
            vSection = (TextView) itemView;
        }
    }

    private class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int ALBUM = 1;
        private static final int ARTIST = 2;
        private static final int SONG = 3;
        private static final int SECTION_ALBUMS = 4;
        private static final int SECTION_ARTISTS = 5;
        private static final int SECTION_SONGS = 6;

        private final List<Album> mAlbumList = Collections.synchronizedList(new ArrayList<Album>());
        private final List<Artist> mArtistList = Collections.synchronizedList(new ArrayList<Artist>());
        private final List<Song> mSongList = Collections.synchronizedList(new ArrayList<Song>());

        void setAlbumList(List<Album> albumList) {
            mAlbumList.clear();
            mAlbumList.addAll(albumList);
            refreshIfNecessary();
        }

        private void refreshIfNecessary() {
            if (mAlbumListLoaded && mArtistListLoaded && mSongListLoaded) {
                notifyDataSetChanged();
            }
        }

        void setArtistList(List<Artist> artistList) {
            mArtistList.clear();
            mArtistList.addAll(artistList);
            refreshIfNecessary();
        }

        void setSongList(List<Song> songList) {
            mSongList.clear();
            mSongList.addAll(songList);
            refreshIfNecessary();
        }

        public Object getItem(int position) {

            int albumRows = mAlbumList.size() > 0 ? mAlbumList.size() + 1 : 0;

            if (albumRows > position && position != 0)
                return mAlbumList.get(position - 1);


            int artistRows = mArtistList.size() > 0 ? mArtistList.size() + 1 : 0;

            if (albumRows + artistRows > position && position - albumRows != 0)
                return mArtistList.get(position - albumRows - 1);


            int songRows = mSongList.size() > 0 ? mSongList.size() + 1 : 0;

            if (albumRows + artistRows + songRows > position && position - albumRows - artistRows != 0)
                return mSongList.get(position - albumRows - artistRows - 1);

            return null;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {

            View itemView;
            RecyclerView.ViewHolder viewHolder;

            switch (type) {
                case ALBUM:
                    itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_search_album_list_item, parent, false);
                    viewHolder = new SearchActivity.AlbumViewHolder(itemView);
                    return viewHolder;

                case ARTIST:
                    itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_search_artist_list_item, parent, false);
                    viewHolder = new SearchActivity.ArtistViewHolder(itemView);
                    return viewHolder;

                case SONG:
                    itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_search_song_list_item, parent, false);
                    viewHolder = new SearchActivity.SongViewHolder(itemView);
                    return viewHolder;

                case SECTION_ALBUMS:
                case SECTION_ARTISTS:
                case SECTION_SONGS:
                    itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_search_section, parent, false);
                    viewHolder = new SearchActivity.SectionViewHolder(itemView);
                    return viewHolder;

                default:
                    break;
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

            int type = getItemViewType(position);

            int albumRows = mAlbumList.size() > 0 ? mAlbumList.size() + 1 : 0;

            int artistRows = mArtistList.size() > 0 ? mArtistList.size() + 1 : 0;

            switch (type) {
                case ALBUM:
                    Album album = mAlbumList.get(position - 1);
                    ((SearchActivity.AlbumViewHolder) viewHolder).vName.setText(album.getAlbumName());
                    ((SearchActivity.AlbumViewHolder) viewHolder).vArtist.setText(album.getArtistName());
                    ((SearchActivity.AlbumViewHolder) viewHolder).vArtwork.setTag(position);
                    ArtworkCache.getInstance().loadBitmap(album.getId(), ((SearchActivity.AlbumViewHolder) viewHolder).vArtwork, mThumbSize, mThumbSize);
                    break;

                case ARTIST:
                    Artist artist = mArtistList.get(position - albumRows - 1);
                    ((SearchActivity.ArtistViewHolder) viewHolder).vName.setText(artist.getName());
                    ((SearchActivity.ArtistViewHolder) viewHolder).vArtistImage.setTag(position);
                    ArtistImageCache.getInstance().loadBitmap(artist.getName(), ((SearchActivity.ArtistViewHolder) viewHolder).vArtistImage, mThumbSize, mThumbSize);
                    break;

                case SONG:

                    Song song = mSongList.get(position - albumRows - artistRows - 1);
                    ((SearchActivity.SongViewHolder) viewHolder).vTitle.setText(song.getTitle());
                    ((SearchActivity.SongViewHolder) viewHolder).vArtist.setText(song.getArtist());
                    ((SearchActivity.SongViewHolder) viewHolder).vArtwork.setTag(position);
                    ArtworkCache.getInstance().loadBitmap(song.getAlbumId(), ((SearchActivity.SongViewHolder) viewHolder).vArtwork, mThumbSize, mThumbSize);
                    break;

                case SECTION_ALBUMS:
                    ((SearchActivity.SectionViewHolder) viewHolder).vSection.setText(R.string.albums);
                    break;

                case SECTION_ARTISTS:
                    ((SearchActivity.SectionViewHolder) viewHolder).vSection.setText(R.string.artists);
                    break;

                case SECTION_SONGS:
                    ((SearchActivity.SectionViewHolder) viewHolder).vSection.setText(R.string.titles);
                    break;

                default:
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            int albumRows = mAlbumList.size() > 0 ? mAlbumList.size() + 1 : 0;

            if (albumRows > position) {
                if (position == 0) {
                    return SECTION_ALBUMS;
                }
                return ALBUM;
            }

            int artistRows = mArtistList.size() > 0 ? mArtistList.size() + 1 : 0;

            if (albumRows + artistRows > position) {
                if (position - albumRows == 0) {
                    return SECTION_ARTISTS;
                }
                return ARTIST;
            }

            int songRows = mSongList.size() > 0 ? mSongList.size() + 1 : 0;

            if (albumRows + artistRows + songRows > position) {
                if (position - albumRows - artistRows == 0) {
                    return SECTION_SONGS;
                }
                return SONG;
            }
            return 0;
        }

        @Override
        public int getItemCount() {

            int count = 0;

            if (mAlbumList.size() > 0)
                count += mAlbumList.size() + 1;

            if (mArtistList.size() > 0)
                count += mArtistList.size() + 1;

            if (mSongList.size() > 0)
                count += mSongList.size() + 1;

            return count;
        }
    }

}
