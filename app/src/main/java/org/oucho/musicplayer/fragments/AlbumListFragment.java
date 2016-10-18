package org.oucho.musicplayer.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.adapters.AlbumListAdapter;
import org.oucho.musicplayer.adapters.BaseAdapter;
import org.oucho.musicplayer.dialog.AlbumEditorDialog;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.loaders.AlbumLoader;
import org.oucho.musicplayer.loaders.SortOrder;
import org.oucho.musicplayer.model.Album;
import org.oucho.musicplayer.model.Playlist;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.utils.PrefUtils;
import org.oucho.musicplayer.widgets.FastScroller;

import java.util.List;

import static android.text.Html.FROM_HTML_OPTION_USE_CSS_COLORS;


public class AlbumListFragment extends BaseFragment {

    private AlbumListAdapter mAdapter;

    private Context context;

    private static final String fichier_préférence = "org.oucho.musicplayer_preferences";

    private SharedPreferences préférences = null;

    private String titre;
    private String tri;

    private boolean run = false;

    private final LoaderManager.LoaderCallbacks<List<Album>> mLoaderCallbacks = new LoaderCallbacks<List<Album>>() {

        @Override
        public Loader<List<Album>> onCreateLoader(int id, Bundle args) {

            AlbumLoader loader = new AlbumLoader(getActivity());
            loader.setSortOrder(PrefUtils.getInstance().getAlbumSortOrder());

            return loader;

        }

        @Override
        public void onLoadFinished(Loader<List<Album>> loader, List<Album> data) {
            mAdapter.setData(data);

        }

        @Override
        public void onLoaderReset(Loader<List<Album>> loader) {
            //  Auto-generated method stub

        }
    };


    private final AlbumEditorDialog.OnEditionSuccessListener mOnEditionSuccessListener = new AlbumEditorDialog.OnEditionSuccessListener() {
        @Override
        public void onEditionSuccess() {
            ((MainActivity) getActivity()).refresh();
        }
    };
    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            Album album = mAdapter.getItem(position);

            switch (view.getId()) {
                case R.id.album_artwork:
                case R.id.album_info:
                    Fragment fragment = AlbumFragment.newInstance(album);
                    ((MainActivity) getActivity()).setFragment(fragment);
                    break;
                case R.id.menu_button:
                    showMenu(position, view);
                    break;
                default:
                    break;
            }
        }
    };


    public static AlbumListFragment newInstance() {

        return new AlbumListFragment();
    }


    /* *********************************************************************************************
     * Menu des albums
     * ********************************************************************************************/

    private void showMenu(final int position, View v) {

        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.album_list_item, popup.getMenu());
        final Album album = mAdapter.getItem(position);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.action_edit_tags:
                        showEditorDialog(album);
                        return true;
                    case R.id.action_add_to_playlist:
                        showPlaylistPicker(album);
                        return true;
                    default: //do nothing
                        break;
                }
                return false;
            }
        });
        popup.show();
    }

    private void showEditorDialog(Album album) {
        AlbumEditorDialog dialog = AlbumEditorDialog.newInstance(album);
        dialog.setOnEditionSuccessListener(mOnEditionSuccessListener);
        dialog.show(getChildFragmentManager(), "edit_album_tags");
    }

    private void showPlaylistPicker(final Album album) {
        PlaylistPickerDialog picker = PlaylistPickerDialog.newInstance();
        picker.setListener(new PlaylistPickerDialog.OnPlaylistPickedListener() {
            @Override
            public void onPlaylistPicked(Playlist playlist) {
                PlaylistsUtils.addAlbumToPlaylist(getActivity().getContentResolver(), playlist.getId(), album.getId());
            }
        });
        picker.show(getChildFragmentManager(), "pick_playlist");

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

        titre = context.getString(R.string.albums);

        setTri();

    }



    /* *********************************************************************************************
     * Création de la vue
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_album_list, container, false);

        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list_view);
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);

        Resources res = getActivity().getResources();

        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        float screenWidth = size.x;

        float itemWidth = res.getDimension(R.dimen.album_grid_item_width);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), Math.round(screenWidth / itemWidth)));

        int artworkSize = res.getDimensionPixelSize(R.dimen.art_size);
        mAdapter = new AlbumListAdapter(getActivity(), artworkSize, artworkSize);
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mRecyclerView.setAdapter(mAdapter);

        FastScroller mFastScroller = (FastScroller) rootView.findViewById(R.id.fastscroller);
        mFastScroller.setRecyclerView(mRecyclerView);


        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public void load() {
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
    }



    /* *********************************************************************************************
     * Menu
     * ********************************************************************************************/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.album_sort_by, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PrefUtils prefUtils = PrefUtils.getInstance();
        switch (item.getItemId()) {
            case R.id.menu_sort_by_az:
                prefUtils.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);
                load();
                tri = "a-z";
                setUserVisibleHint(true);
                break;
            case R.id.menu_sort_by_artist:
                prefUtils.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_ARTIST);
                load();
                tri = context.getString(R.string.title_sort_artist);
                setUserVisibleHint(true);
                break;
            case R.id.menu_sort_by_year:
                prefUtils.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
                load();
                tri = context.getString(R.string.title_sort_year);
                setUserVisibleHint(true);
                break;
            default: //do nothing
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    /* *********************************************************************************************
     * Titre
     * ********************************************************************************************/

    private void setTri() {

        String getTri = préférences.getString("album_sort_order", "");

        if ("minyear DESC".equals(getTri)) {

            tri = context.getString(R.string.title_sort_year);

        } else if ("artist".equals(getTri)) {

            tri = context.getString(R.string.title_sort_artist);

        } else {

            tri = "a-z";
        }
    }



    @Override
    public void setUserVisibleHint(boolean visible){
        super.setUserVisibleHint(visible);

        if (visible || isResumed()) {

            // délai affichage lors du premier chargement nom appli --> tri actuel
            if (run) {
                getActivity().setTitle(Html.fromHtml("<font>" + titre + " </font> <small> <font color=\"#CCCCCC\">" + tri + "</small></font>"));
            } else {

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        // Actions to do after xx seconds
                        getActivity().setTitle(Html.fromHtml("<font>" + titre + " </font> <small> <font color=\"#CCCCCC\">" + tri + "</small></font>"));
                    }
                }, 1000);

                run = true;
            }

        }
    }

}
