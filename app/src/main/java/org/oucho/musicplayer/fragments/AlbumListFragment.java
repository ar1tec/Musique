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
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.fragments.adapters.AlbumListAdapter;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.dialog.AlbumEditorDialog;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.db.loaders.AlbumLoader;
import org.oucho.musicplayer.db.loaders.SortOrder;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Playlist;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.utils.PrefUtils;
import org.oucho.musicplayer.widgets.FastScroller;

import java.util.List;

public class AlbumListFragment extends BaseFragment implements MusiqueKeys {

    private Context context;
    private Menu menu;

    private AlbumListAdapter mAdapter;
    private ReloadView reloadReceiver;
    private SharedPreferences préférences = null;

    private String titre;
    private String tri;

    private boolean run = false;
    private boolean isRegistered = false;

    private RecyclerView mRecyclerView;



    private List<Album> listeTitre;

    private final LoaderManager.LoaderCallbacks<List<Album>> mLoaderCallbacks = new LoaderCallbacks<List<Album>>() {

        @Override
        public Loader<List<Album>> onCreateLoader(int id, Bundle args) {

            AlbumLoader loader = new AlbumLoader(getActivity());
            loader.setSortOrder(PrefUtils.getInstance().getAlbumSortOrder());

            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Album>> loader, List<Album> albumList) {
            mAdapter.setData(albumList);

            listeTitre = albumList;
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


                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {

                        public void run() {

                            Intent intent = new Intent();
                            intent.setAction(INTENT_LAYOUTVIEW);
                            intent.putExtra("vue", "layoutB");
                            context.sendBroadcast(intent);

                        }
                    }, 300);


                    Fragment fragment = AlbumFragment.newInstance(album);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_in_bottom);
                    ft.replace(R.id.fragment_album_list_layout, fragment);
                    ft.commit();

                    showOverflowMenu(false);

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



    public static AlbumListFragment newInstance() {

        return new AlbumListFragment();
    }


    /* *********************************************************************************************
     * Menu des albums
     * ********************************************************************************************/

    private void showMenu(final int position, View v) {

        PopupMenu popup = new PopupMenu(getActivity(), v);
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

        reloadReceiver = new ReloadView();
        IntentFilter filter = new IntentFilter("reload");
        context.registerReceiver(reloadReceiver, filter);
        isRegistered = true;
    }

    private class ReloadView extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            if ("reload".equals(receiveIntent)) {
                setUserVisibleHint(true);
                showOverflowMenu(true);
                LibraryFragment.setLock(false);

            }

            // recharge la vue via Adapter
            mAdapter.notifyDataSetChanged();

        }
    }


    /* *********************************************************************************************
     * Création de la vue
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_liste_album, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list_view);
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
        mAdapter.setOnItemLongClickListener(mOnItemLongClickListener);

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

    private void showOverflowMenu(boolean showMenu){
        if(menu == null)
            return;

        menu.setGroupVisible(R.id.main_menu_group, showMenu);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isRegistered) {
            context.unregisterReceiver(reloadReceiver);
            isRegistered = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // attendre la fin du chargement de l'interface avant d'activer blurview, bug charge CPU
        Intent intent = new Intent();
        intent.setAction(INTENT_BLURVIEW);
        context.sendBroadcast(intent);


        if (!isRegistered) {
            IntentFilter filter = new IntentFilter("reload");
            context.registerReceiver(reloadReceiver, filter);
            isRegistered = true;
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

                    if (MainActivity.getQueueLayout()) {

                        Intent intent = new Intent();
                        intent.setAction(INTENT_QUEUEVIEW);
                        context.sendBroadcast(intent);

                        return true;

                    }

                    return false;

                }
                return false;
            }
        });
    }

    /* *********************************************************************************************
     * Titre
     * ********************************************************************************************/

    private void setTri() {

        String getTri = préférences.getString("album_sort_order", "");

        if ("minyear DESC".equals(getTri)) {
            tri = context.getString(R.string.title_sort_year);
        } else if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {
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

                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    getActivity().setTitle(Html.fromHtml("<font>"
                            + titre
                            + " </font> <small> <font color=\"#CCCCCC\">"
                            + tri
                            + "</small></font>", Html.FROM_HTML_MODE_LEGACY));
                } else {
                    //noinspection deprecation
                    getActivity().setTitle(Html.fromHtml("<font>"
                            + titre
                            + " </font> <small> <font color=\"#CCCCCC\">"
                            + tri
                            + "</small></font>"));
                }



            } else {

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    public void run() {

                        // Actions to do after xx seconds
                        if (android.os.Build.VERSION.SDK_INT >= 24) {
                            getActivity().setTitle(Html.fromHtml("<font>"
                                    + titre
                                    + " </font> <small> <font color=\"#CCCCCC\">"
                                    + tri
                                    + "</small></font>", Html.FROM_HTML_MODE_LEGACY));
                        } else {
                            //noinspection deprecation
                            getActivity().setTitle(Html.fromHtml("<font>"
                                    + titre
                                    + " </font> <small> <font color=\"#CCCCCC\">"
                                    + tri
                                    + "</small></font>"));
                        }
                    }
                }, 1000);

                run = true;
            }
        }
    }
}
