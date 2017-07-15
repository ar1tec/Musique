package org.oucho.musicplayer.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Artist;
import org.oucho.musicplayer.fragments.adapters.AlbumListAdapter;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.fragments.loaders.AlbumLoader;
import org.oucho.musicplayer.fragments.loaders.SortOrder;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Playlist;
import org.oucho.musicplayer.dialog.AlbumEditorDialog;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.utils.CustomGridLayoutManager;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.utils.PrefUtils;
import org.oucho.musicplayer.widgets.LockableViewPager;
import org.oucho.musicplayer.widgets.fastscroll.FastScrollRecyclerView;

import java.util.List;

public class AlbumListFragment extends BaseFragment implements MusiqueKeys {

    private static final String TAG_LOG = "AlbumListFragment";

    private Context mContext;
    private Menu menu;

    private AlbumListAdapter mAdapter;
    private ReloadView reloadReceiver;
    private SharedPreferences préférences = null;

    private String titre;
    private String tri;

    private boolean run = false;
    private boolean isRegistered = false;

    private FastScrollRecyclerView mRecyclerView;
    private final Handler mHandler = new Handler();

    private Artist mArtist = null;


    private List<Album> listeTitre;


    private static final String PARAM_ARTIST_ID = "artist_id";
    private static final String PARAM_ARTIST_NAME = "artist_name";
    private static final String PARAM_ALBUM_COUNT = "album_count";
    private static final String PARAM_TRACK_COUNT = "track_count";


    public static AlbumListFragment newInstance() {

        return new AlbumListFragment();
    }


    public static AlbumListFragment newInstance(Artist artist) {

        Log.d(TAG_LOG, "newInstance" + artist);

        AlbumListFragment fragment = new AlbumListFragment();
        Bundle args = new Bundle();
        args.putLong(PARAM_ARTIST_ID, artist.getId());
        args.putString(PARAM_ARTIST_NAME, artist.getName());
        args.putInt(PARAM_TRACK_COUNT, artist.getTrackCount());
        fragment.setArguments(args);
        return fragment;
    }


    private final LoaderManager.LoaderCallbacks<List<Album>> mLoaderCallbacks = new LoaderCallbacks<List<Album>>() {

        @Override
        public Loader<List<Album>> onCreateLoader(int id, Bundle args) {

            AlbumLoader loader;
            if(mArtist != null) {

                Log.d(TAG_LOG, "Artist loaded = " + mArtist.getName());
                loader = new AlbumLoader(mContext, mArtist.getName());
                loader.setSortOrder(PrefUtils.getInstance().getAlbumSortOrder());

            }  else {

                loader = new AlbumLoader(mContext);
                loader.setSortOrder(PrefUtils.getInstance().getAlbumSortOrder());
            }

            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Album>> loader, List<Album> albumList) {
            mAdapter.setData(albumList);

            Log.d(TAG_LOG, "onLoadFinished = " + albumList);


            listeTitre = albumList;
        }

        @Override
        public void onLoaderReset(Loader<List<Album>> loader) {
            //  Auto-generated method stub
        }
    };




    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        setHasOptionsMenu(true);


        if (args != null) {
            long id = args.getLong(PARAM_ARTIST_ID);
            String name = args.getString(PARAM_ARTIST_NAME);
            int albumCount = args.getInt(PARAM_ALBUM_COUNT);
            int trackCount = args.getInt(PARAM_TRACK_COUNT);
            mArtist = new Artist(id, name, albumCount, trackCount);
        }


        mContext = getContext();

        préférences = this.mContext.getSharedPreferences(FICHIER_PREFS, Context.MODE_PRIVATE);

        titre = mContext.getString(R.string.albums);

        setTri();

        reloadReceiver = new ReloadView();
        IntentFilter filter = new IntentFilter("reload");
        mContext.registerReceiver(reloadReceiver, filter);
        isRegistered = true;

        load();
    }





    /* *********************************************************************************************
     * Création de la vue
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        int layout;

        if (mArtist != null) {
            layout = R.layout.fragment_search_liste_album;
        } else {
            layout = R.layout.fragment_liste_album;
        }

        View rootView = inflater.inflate(layout, container, false);

        mRecyclerView = (FastScrollRecyclerView) rootView.findViewById(R.id.recycler_view);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        Resources res = mContext.getResources();

        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float screenWidth = size.x;
        float itemWidth = res.getDimension(R.dimen.fragmen_album_list_grid_item_width);
        mRecyclerView.setLayoutManager(new CustomGridLayoutManager(mContext, Math.round(screenWidth / itemWidth)));

        int artworkSize = res.getDimensionPixelSize(R.dimen.art_size);
        mAdapter = new AlbumListAdapter(mContext, artworkSize, artworkSize);
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.setOnItemLongClickListener(mOnItemLongClickListener);

        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }



    @Override
    public void load() {
            getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
    }



    /* *********************************************************************************************
     * Menu des albums
     * ********************************************************************************************/

    private void showMenu(final int position, View v) {

        PopupMenu popup = new PopupMenu(mContext, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.album_item, popup.getMenu());
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
                PlaylistsUtils.addAlbumToPlaylist(mContext.getContentResolver(), playlist.getId(), album.getId());
            }
        });
        picker.show(getChildFragmentManager(), "pick_playlist");

    }






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
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom);
                    ft.replace(R.id.fragment_album_list_layout, fragment);
                    ft.addToBackStack("AlbumFragment");
                    ft.commit();

                    mHandler.postDelayed(new Runnable() {

                        public void run() {
                            showOverflowMenu(false);
                        }
                    }, 300);


                    break;
                case R.id.menu_button:
                    showMenu(position, view);
                    break;
                default:
                    break;
            }
        }
    };


    private final BaseAdapter.OnItemLongClickListener mOnItemLongClickListener = new BaseAdapter.OnItemLongClickListener() {
        @Override
        public void onItemLongClick(int position, View view) {

            for (int i = 0; i < listeTitre.size(); i++) {
                if (listeTitre.get(i).getId() == PlayerService.getAlbumId())
                    mRecyclerView.smoothScrollToPosition( i );
            }
        }
    };

    /* *********************************************************************************************
     * Menu
     * ********************************************************************************************/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        this.menu = menu;

        inflater.inflate(R.menu.albumlist_sort_by, menu);
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
                tri = mContext.getString(R.string.title_sort_artist);
                setUserVisibleHint(true);
                break;
            case R.id.menu_sort_by_year:
                prefUtils.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
                load();
                tri = mContext.getString(R.string.title_sort_year);
                setUserVisibleHint(true);
                break;
            case R.id.menu_sort_by_ajout:
                prefUtils.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_AJOUT);
                load();
                tri = mContext.getString(R.string.title_sort_add);
                setUserVisibleHint(true);
                break;
            default: //do nothing
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showOverflowMenu(boolean showMenu){
        if(menu == null)
            return;

        menu.setGroupVisible(R.id.main_menu_group, showMenu);
    }


    @Override
    public void onPause() {
        super.onPause();

        if (isRegistered) {
            mContext.unregisterReceiver(reloadReceiver);
            isRegistered = false;
        }

        AlbumLoader.resetArtist();

    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG_LOG, "onResume()");

        if (!isRegistered) {
            IntentFilter filter = new IntentFilter("reload");
            mContext.registerReceiver(reloadReceiver, filter);
            isRegistered = true;
        }


        if (!MainActivity.getChercheActivity()) {
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

                        if (MainActivity.getQueueLayout()) {

                            Intent intent = new Intent();
                            intent.setAction(INTENT_QUEUEVIEW);
                            mContext.sendBroadcast(intent);

                            return true;

                        } else if (MainActivity.getAlbumFragmentState()) {

                            MainActivity.setAlbumFragmentState(false);
                            LockableViewPager.setSwipeLocked(false);
                            showOverflowMenu(true);

                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                            ft.remove(getFragmentManager().findFragmentById(R.id.fragment_album_list_layout));
                            ft.commit();

                            Intent shadow = new Intent();
                            shadow.setAction(INTENT_TOOLBAR_SHADOW);
                            shadow.putExtra("boolean", true);
                            mContext.sendBroadcast(shadow);

                            return true;
                        }

                        return false;

                    }
                    return false;
                }
            });

        }

    }

    /* *********************************************************************************************
     * Titre
     * ********************************************************************************************/

    private void setTri() {

        String getTri = préférences.getString("album_sort_order", "");

        if ("minyear DESC".equals(getTri)) {
            tri = mContext.getString(R.string.title_sort_year);
        } else if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {
            tri = mContext.getString(R.string.title_sort_artist);
        } else if ("_id DESC".equals(getTri)) {
            tri = mContext.getString(R.string.title_sort_add);
        } else {
            tri = "a-z";
        }
    }



    private class ReloadView extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            if ("reload".equals(receiveIntent)) {
                if (MainActivity.getViewID() != R.id.fragment_song_layout)
                    setUserVisibleHint(true);


                if (!MainActivity.getAlbumFragmentState())
                    showOverflowMenu(true);

            }

            // recharge la vue via Adapter
            mAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void setUserVisibleHint(boolean visible){
        super.setUserVisibleHint(visible);

        if (visible || isResumed()) {

            if (MainActivity.getChercheActivity()) {
                Intent intent0 = new Intent();
                intent0.setAction("search.setTitle");
                intent0.putExtra("title", titre);
                getActivity().sendBroadcast(intent0);
            }

            // délai affichage lors du premier chargement nom appli --> tri actuel

            if (run) {

                MainActivity.setViewID(R.id.fragment_album_list_layout);

                final int couleurTitre = ContextCompat.getColor(mContext, R.color.grey_400);

                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    getActivity().setTitle(Html.fromHtml("<font>"
                            + titre
                            + " </font> <small> <font color='" + couleurTitre + "'>"
                            + tri
                            + "</small></font>", Html.FROM_HTML_MODE_LEGACY));

                } else {

                    //noinspection deprecation
                    getActivity().setTitle(Html.fromHtml("<font>"
                            + titre
                            + " </font> <small> <font color='" + couleurTitre + "'>"
                            + tri
                            + "</small></font>"));
                }


            } else {

                MainActivity.setViewID(R.id.fragment_album_list_layout);

                run = true;

                mHandler.postDelayed(new Runnable() {

                    public void run() {
                        int couleurTitre = ContextCompat.getColor(mContext, R.color.grey_400);

                        // Actions to do after xx seconds
                        if (android.os.Build.VERSION.SDK_INT >= 24) {


                            getActivity().setTitle(Html.fromHtml("<font>"
                                    + titre
                                    + " </font> <small> <font color='" + couleurTitre + "'>"
                                    + tri
                                    + "</small></font>", Html.FROM_HTML_MODE_LEGACY));
                        } else {

                            try {
                                //noinspection deprecation
                                getActivity().setTitle(Html.fromHtml("<font>"
                                        + titre
                                        + " </font> <small> <font color='" + couleurTitre + "'"
                                        + tri
                                        + "</small></font>"));
                            } catch (NullPointerException ignore) {
                                // Plantage si sorti de l'application moins
                                // d'une seconde après son ouverture
                                Log.w(TAG_LOG, "Sortie trop rapide après le lancmenet de l'application");
                            }

                        }
                    }
                }, 1000);
            }
        }
    }
}
