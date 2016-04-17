package org.oucho.musicplayer.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.adapters.AdapterWithHeader;
import org.oucho.musicplayer.adapters.BaseAdapter;
import org.oucho.musicplayer.adapters.SongListAdapter;
import org.oucho.musicplayer.dialog.ID3TagEditorDialog;
import org.oucho.musicplayer.dialog.PlaylistPicker;
import org.oucho.musicplayer.loaders.SongLoader;
import org.oucho.musicplayer.loaders.SortOrder;
import org.oucho.musicplayer.model.Playlist;
import org.oucho.musicplayer.model.Song;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.utils.PrefUtils;
import org.oucho.musicplayer.utils.RecyclerViewUtils;
import org.oucho.musicplayer.widgets.FastScroller;

import java.util.List;


public class SongListFragment extends BaseFragment {


    private MainActivity mActivity;

    private SongListAdapter mAdapter;

    private boolean mShowScrollerBubble = true;
    private FastScroller mFastScroller;

    private Context context;

    private static final String fichier_préférence = "org.oucho.musicplayer_preferences";

    private static SharedPreferences préférences = null;

    private String titre;
    private String tri;

    private final LoaderManager.LoaderCallbacks<List<Song>> mLoaderCallbacks = new LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(getActivity());

            loader.setSortOrder(PrefUtils.getInstance().getSongSortOrder());
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> songList) {
            populateAdapter(songList);

            PrefUtils prefUtils = PrefUtils.getInstance();
            String sortOrder = prefUtils.getSongSortOrder();

            mShowScrollerBubble = SortOrder.SongSortOrder.SONG_A_Z.equals(sortOrder);

            if (mFastScroller != null) {
                mFastScroller.setShowBubble(mShowScrollerBubble);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            //  Auto-generated method stub

        }
    };


    private void populateAdapter(List<Song> songList) {
        mAdapter.setData(songList);
    }

    private final ID3TagEditorDialog.OnTagsEditionSuccessListener mOnTagsEditionSuccessListener = new ID3TagEditorDialog.OnTagsEditionSuccessListener() {
        @Override
        public void onTagsEditionSuccess() {
            ((MainActivity) getActivity()).refresh();
        }
    };

    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            switch (view.getId()) {
                case R.id.item_view:
                    selectSong(position);
                    break;
                case R.id.menu_button:
                    showMenu(position, view);
                    break;
                default: //do nothing
                    break;
            }
        }
    };

    private final AdapterWithHeader.OnHeaderClickListener mOnHeaderClickListener = new AdapterWithHeader.OnHeaderClickListener() {
        @Override
        public void onHeaderClick() {
            if (mActivity != null) {
                mActivity.onShuffleRequested(mAdapter.getSongList(), true);
            }
        }
    };

    public static SongListFragment newInstance() {

        return new SongListFragment();
    }



    /* *********************************************************************************************
     * Menu titre
     * ********************************************************************************************/

    private void showMenu(final int position, View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        final Song song = mAdapter.getItem(position);
        inflater.inflate(R.menu.song_list_item, popup.getMenu());
        popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

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

    private void showID3TagEditor(Song song) {
        ID3TagEditorDialog dialog = ID3TagEditorDialog.newInstance(song);
        dialog.setOnTagsEditionSuccessListener(mOnTagsEditionSuccessListener);
        dialog.show(getChildFragmentManager(), "edit_tags");
    }

    private void showPlaylistPicker(final Song song) {
        PlaylistPicker picker = PlaylistPicker.newInstance();
        picker.setListener(new PlaylistPicker.OnPlaylistPickedListener() {
            @Override
            public void onPlaylistPicked(Playlist playlist) {
                PlaylistsUtils.addSongToPlaylist(getActivity().getContentResolver(), playlist.getId(), song.getId());
            }
        });
        picker.show(getChildFragmentManager(), "pick_playlist");

    }

    private void selectSong(int position) {

        if (mActivity != null) {
            mActivity.onSongSelected(mAdapter.getSongList(), position);
        }
    }

    @Override
    public void load() {
        Log.d("frag", "ertr");

        getLoaderManager().restartLoader(0, null, getLoaderCallbacks());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mActivity = (MainActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }



    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        context = getContext();

        préférences = this.getActivity().getSharedPreferences(fichier_préférence, Context.MODE_PRIVATE);

        titre = context.getString(R.string.titles);

        setTri();

    }


    /* *********************************************************************************************
     * Création de la vue
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_song_list,
                container, false);

        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new SongListAdapter(getActivity());
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        View headerView = RecyclerViewUtils.inflateChild(inflater, R.layout.shuffle_list_item, mRecyclerView);

        mAdapter.setHeaderView(headerView);
        mAdapter.setOnHeaderClickListener(mOnHeaderClickListener);
        mRecyclerView.setAdapter(mAdapter);


        mFastScroller = (FastScroller) rootView.findViewById(R.id.fastscroller);
        mFastScroller.setShowBubble(mShowScrollerBubble);
        mFastScroller.setSectionIndexer(mAdapter);
        mFastScroller.setRecyclerView(mRecyclerView);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, getLoaderCallbacks());

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }



    /* *********************************************************************************************
     * Menu
     * ********************************************************************************************/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.song_sort_by, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PrefUtils prefUtils = PrefUtils.getInstance();
        switch (item.getItemId()) {
            case R.id.menu_sort_by_az:
                prefUtils.setSongSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
                load();
                tri = "a-z";
                setUserVisibleHint(true);
                break;

            case R.id.menu_sort_by_album:
                prefUtils.setSongSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
                load();
                tri = context.getString(R.string.title_sort_album);
                setUserVisibleHint(true);
                break;

            case R.id.menu_sort_by_artist:
                prefUtils.setSongSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
                load();
                tri = context.getString(R.string.title_sort_artist);
                setUserVisibleHint(true);
                break;

            case R.id.menu_sort_by_year:
                prefUtils.setSongSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
                load();
                tri = context.getString(R.string.title_sort_year);
                setUserVisibleHint(true);
                break;

            default: //do nothing
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private LoaderCallbacks<List<Song>> getLoaderCallbacks() {
        return mLoaderCallbacks;
    }




    /* *********************************************************************************************
     * Titre
     * ********************************************************************************************/

    private void setTri() {

        String getTri = préférences.getString("song_sort_order", "");

        if ("year DESC".equals(getTri)) {

            tri = context.getString(R.string.title_sort_year);

        } else if ("artist".equals(getTri)) {

            tri = context.getString(R.string.title_sort_artist);

        } else if ("album".equals(getTri)) {

            tri = context.getString(R.string.title_sort_album);

        } else {

            tri = "a-z";
        }
    }

    @Override
    public void setUserVisibleHint(boolean visible){
        super.setUserVisibleHint(visible);

        if (visible || isResumed())
            getActivity().setTitle(Html.fromHtml("<font>" + titre + " " + " " + " </font> <small> <font color=\"#CCCCCC\">" + tri + "</small></font>"));

    }

}