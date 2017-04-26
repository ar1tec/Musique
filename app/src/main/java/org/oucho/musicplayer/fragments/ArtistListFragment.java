package org.oucho.musicplayer.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.adapters.ArtistListAdapter;
import org.oucho.musicplayer.adapters.BaseAdapter;
import org.oucho.musicplayer.db.loaders.ArtistLoader;
import org.oucho.musicplayer.db.loaders.SortOrder;
import org.oucho.musicplayer.db.model.Artist;
import org.oucho.musicplayer.utils.PrefUtils;
import org.oucho.musicplayer.widgets.FastScroller;

import java.util.List;


public class ArtistListFragment extends BaseFragment {

    String TAG_LOG = "Artiste List Fragment";

    private static final String STATE_SHOW_FASTSCROLLER = "fastscroller";

    private ArtistListAdapter mAdapter;

    private boolean mShowFastScroller = true;

    private Context mContext;

    private final LoaderManager.LoaderCallbacks<List<Artist>> mLoaderCallbacks = new LoaderCallbacks<List<Artist>>() {

        @Override
        public void onLoaderReset(Loader<List<Artist>> loader) {
            //  Auto-generated method stub

        }

        @Override
        public void onLoadFinished(Loader<List<Artist>> loader, List<Artist> data) {
            mAdapter.setData(data);

            Log.i(TAG_LOG, "onLoadFinished()");
        }

        @Override
        public Loader<List<Artist>> onCreateLoader(int id, Bundle args) {
            ArtistLoader loader = new ArtistLoader(getActivity());
            loader.setSortOrder(PrefUtils.getInstance().getArtistSortOrder());

            return loader;
        }
    };


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            Artist artist = mAdapter.getItem(position);

            Fragment fragment = ArtistFragment.newInstance(artist);

            ((MainActivity) getActivity()).setFragment(fragment);
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mContext = getContext();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_list,
                container, false);
        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));


        mAdapter = new ArtistListAdapter(getActivity());
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mRecyclerView.setAdapter(mAdapter);

        if (savedInstanceState != null) {
            mShowFastScroller = savedInstanceState
                    .getBoolean(STATE_SHOW_FASTSCROLLER) || mShowFastScroller;
        }

        FastScroller mFastScroller = (FastScroller) rootView.findViewById(R.id.fastscroller);

        if (mShowFastScroller) {
            mFastScroller.setRecyclerView(mRecyclerView);
        } else {
            mFastScroller.setVisibility(View.GONE);
        }

        return rootView;
    }

    @Override
    public void load() {
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.artist_sort_by, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PrefUtils prefUtils = PrefUtils.getInstance();
        switch (item.getItemId()) {
            case R.id.menu_sort_by_az:
                prefUtils.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_A_Z);
                load();
                break;

            case R.id.menu_sort_by_number_of_songs:
                prefUtils.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
                load();
                break;
            case R.id.menu_sort_by_number_of_albums:
                prefUtils.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS);
                load();
                break;
            default: //do nothing
                break;


        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void setUserVisibleHint(boolean visible){
        super.setUserVisibleHint(visible);

        if (visible && isResumed()){
            getActivity().setTitle(mContext.getString(R.string.artists));
        }else  if (visible){
            getActivity().setTitle(mContext.getString(R.string.artists));
        }
    }

}