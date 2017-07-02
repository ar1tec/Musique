package org.oucho.musicplayer.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Playlist;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.dialog.SongEditorDialog;
import org.oucho.musicplayer.fragments.adapters.AlbumSongListAdapter;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.search.SearchActivity;
import org.oucho.musicplayer.utils.CustomLayoutManager;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.widgets.LockableViewPager;
import org.oucho.musicplayer.widgets.fastscroll.FastScrollRecyclerView;

import java.util.List;
import java.util.Locale;


public class AlbumFragment extends BaseFragment implements MusiqueKeys {

    private final String TAG_LOG = "Album Fragment";
    private static final String ARG_ID = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_ARTIST = "artist";
    private static final String ARG_YEAR = "year";
    private static final String ARG_TRACK_COUNT = "track_count";

    private Album mAlbum;
    private AlbumSongListAdapter mAdapter;
    private MainActivity mActivity;

    private FastScrollRecyclerView mRecyclerView;
    private Etat_player Etat_player_Receiver;
    private boolean isRegistered = false;

    private final Handler mHandler = new Handler();

    private int mArtworkWidth;
    private int mArtworkHeight;

    private String Titre = "";
    private String Artiste = "";
    private String Année = "";
    private String nb_Morceaux = "";

    private String tri = "a-z";

    private TextView durée;
    private List<Song> listeTitre;

    private Context mContext;

    private SearchActivity mSearchActivity = null;


    public static AlbumFragment newInstance(Album album) {
        AlbumFragment fragment = new AlbumFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, album.getId());
        args.putString(ARG_NAME, album.getAlbumName());
        args.putString(ARG_ARTIST, album.getArtistName());
        args.putInt(ARG_YEAR, album.getYear());
        args.putInt(ARG_TRACK_COUNT, album.getTrackCount());
        fragment.setArguments(args);
        return fragment;
    }


    private final LoaderManager.LoaderCallbacks<List<Song>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(getActivity());

            loader.setSelection(MediaStore.Audio.Media.ALBUM_ID + " = ?", new String[]{String.valueOf(mAlbum.getId())});
            loader.setSortOrder(MediaStore.Audio.Media.TRACK);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> songList) {
            mAdapter.setData(songList);

            listeTitre = songList;

            int duréeTotal = 0;

            for (int i = 0; i < listeTitre.size(); i++) {

                if (listeTitre.get(i).getId() == PlayerService.getSongID())
                    mRecyclerView.smoothScrollToPosition( i );

                duréeTotal =  duréeTotal + listeTitre.get(i).getDuration();
            }

            if (msToTextMinut(duréeTotal).equals("0") || msToTextMinut(duréeTotal).equals("1")) {

                String temps = msToTextMinut(duréeTotal) + " " + getString(R.string.minute_singulier);
                durée.setText(temps);

            } else {

                String temps = msToTextMinut(duréeTotal) + " " + getString(R.string.minute_pluriel);
                durée.setText(temps);
            }

        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            //  Auto-generated method stub
        }
    };

    private String msToTextMinut(int msec) {
        //noinspection MalformedFormatString
        return String.format(Locale.getDefault(), "%d", msec / 60000, (msec % 60000) / 1000);
    }


    /* *********************************************************************************************
     * Création du fragment
     * ********************************************************************************************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        mContext = getContext();

        Etat_player_Receiver = new Etat_player();
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_STATE);
        mContext.registerReceiver(Etat_player_Receiver, filter);
        isRegistered = true;

        if (args != null) {

            long id = args.getLong(ARG_ID);
            String title = args.getString(ARG_NAME);
            String artist = args.getString(ARG_ARTIST);
            int year = args.getInt(ARG_YEAR);
            int trackCount = args.getInt(ARG_TRACK_COUNT);

            mAlbum = new Album(id, title, artist, year, trackCount);

            Titre = title;
            Artiste = artist;
            Année = String.valueOf(year);

            String singulier = getString(R.string.title);
            String pluriel = getString(R.string.titles);

            if (trackCount < 2) {
                nb_Morceaux = String.valueOf(trackCount) + " " + singulier;
            } else {
                nb_Morceaux = String.valueOf(trackCount) + " " + pluriel;
            }
        }

        mArtworkWidth = getResources().getDimensionPixelSize(R.dimen.fragment_album_artist_image_req_width);
        mArtworkHeight = getResources().getDimensionPixelSize(R.dimen.fragment_album_artist_image_req_height);


        setTri();


        if (MainActivity.getChercheActivity()) {

            mSearchActivity = (SearchActivity) mContext;

            if (android.os.Build.VERSION.SDK_INT >= 24) {
                getActivity().setTitle(Html.fromHtml("<font color=\"#FFA000\">" + Titre + " </font> <small> <font color=\"#CCCCCC\">", Html.FROM_HTML_MODE_LEGACY));
            } else {
                //noinspection deprecation
                getActivity().setTitle(Html.fromHtml("<font color=\"#FFA000\">" + Titre + " </font>"));
            }

        } else {

            mHandler.postDelayed(new Runnable() {

                public void run() {

                    if (tri.equals("a-z"))
                        getActivity().setTitle(Titre);

                    if (tri.equals(getString(R.string.title_sort_artist)))
                        getActivity().setTitle(Artiste);

                    if (tri.equals(getString(R.string.title_sort_year)))
                        getActivity().setTitle(Année);


                    Intent intent = new Intent();
                    intent.setAction(INTENT_TOOLBAR8_SHADOW);
                    intent.putExtra("boolean", false);
                    mContext.sendBroadcast(intent);

                }
            }, 300);
        }

    }


    private void setTri() {

        SharedPreferences préférences = this.getActivity().getSharedPreferences(fichier_préférence, Context.MODE_PRIVATE);

        String getTri = préférences.getString("album_sort_order", "");

        if ("minyear DESC".equals(getTri)) {
            tri = mContext.getString(R.string.title_sort_year);
        } else if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {
            tri = mContext.getString(R.string.title_sort_artist);
        } else {
            tri = "a-z";
        }
    }



    /* *********************************************************************************************
     * Création du visuel
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView;

        if (MainActivity.getChercheActivity()) {
            rootView = inflater.inflate(R.layout.fragment_album_search, container, false);
        } else {
            rootView = inflater.inflate(R.layout.fragment_album, container, false);
        }


        ImageView artworkView = (ImageView) rootView.findViewById(R.id.album_artwork);
        ArtworkCache.getInstance().loadBitmap(mAlbum.getId(), artworkView, mArtworkWidth, mArtworkHeight);

        TextView titreAlbum = (TextView) rootView.findViewById(R.id.line1);

        TextView artiste = (TextView) rootView.findViewById(R.id.line2);
        artiste.setText(Artiste);

        TextView an = (TextView) rootView.findViewById(R.id.line3);
        an.setText(Année);

        TextView morceaux = (TextView) rootView.findViewById(R.id.line4);
        morceaux.setText(nb_Morceaux);


        if (tri.equals("a-z")) {
            titreAlbum.setText(Artiste);
            titreAlbum.setVisibility(View.VISIBLE);
            artiste.setVisibility(View.GONE);
            an.setVisibility(View.VISIBLE);
        }

        if (tri.equals(mContext.getString(R.string.title_sort_artist))) {
            titreAlbum.setText(Titre);
            titreAlbum.setVisibility(View.VISIBLE);
            artiste.setVisibility(View.GONE);
            an.setVisibility(View.VISIBLE);
        }

        if (tri.equals(mContext.getString(R.string.title_sort_year))) {
            titreAlbum.setText(Titre);
            titreAlbum.setVisibility(View.VISIBLE);
            artiste.setVisibility(View.VISIBLE);
            an.setVisibility(View.GONE);
        }


        durée = (TextView) rootView.findViewById(R.id.duration);

        mRecyclerView = (FastScrollRecyclerView) rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new CustomLayoutManager(getActivity()));
        mAdapter = new AlbumSongListAdapter();
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.setOnItemLongClickListener(mOnItemLongClickListener);

        mRecyclerView.setAdapter(mAdapter);


        return rootView;
    }


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {

            mHandler.postDelayed(new Runnable() {

                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            }, 100);


            switch (view.getId()) {
                case R.id.item_view:
                    selectSong(position);
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


            mHandler.postDelayed(new Runnable() {

                public void run() {

                    mAdapter.notifyDataSetChanged();

                }
            }, 100);


            switch (view.getId()) {
                case R.id.item_view:
                    showMenu(position, view);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public void load() {
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
    }

    private void selectSong(int position) {

        Log.i(TAG_LOG, "selectedSong(), position:" + String.valueOf(position));

        if (mSearchActivity != null) {
            mSearchActivity.onSongSelected(mAdapter.getSongList(), position);

        } else if (mActivity != null) {
            mActivity.onSongSelected(mAdapter.getSongList(), position);
        }
    }


    private final SongEditorDialog.OnTagsEditionSuccessListener mOnTagsEditionSuccessListener
            = new SongEditorDialog.OnTagsEditionSuccessListener() {
        @Override
        public void onTagsEditionSuccess() {

            Log.e(TAG_LOG, "mOnTagsEditionSuccessListener");

            ((MainActivity) getActivity()).refresh();

        }
    };

    private void showMenu(final int position, View v) {

        PopupMenu popup = new PopupMenu(getActivity(), v);

        MenuInflater inflater = popup.getMenuInflater();

        final Song song = mAdapter.getItem(position);

        inflater.inflate(R.menu.album_song_item, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_add_to_queue:

                        if (MainActivity.getChercheActivity()) {
                            ((SearchActivity) getActivity()).addToQueue(song);
                        } else {
                            ((MainActivity) getActivity()).addToQueue(song);
                        }

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (! MainActivity.getChercheActivity()) {
            try {
                mActivity = (MainActivity) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement OnFragmentInteractionListener");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isRegistered) {
            mContext.unregisterReceiver(Etat_player_Receiver);

            isRegistered = false;
        }

        MainActivity.setAlbumFragmentState(false);
    }


    @Override
    public void onResume() {
        super.onResume();

        MainActivity.setAlbumFragmentState(true);

        LockableViewPager.setSwipeLocked(true);

        if (!isRegistered) {
            IntentFilter filter = new IntentFilter(INTENT_STATE);
            mContext.registerReceiver(Etat_player_Receiver, filter);
            isRegistered = true;
        }


        if (!MainActivity.getChercheActivity()) {

            if (getView() == null) {
                return;
            }

            getView().setFocusableInTouchMode(true);
            getView().requestFocus();
            getView().setOnKeyListener(new View.OnKeyListener() {

                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {

                    if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {

                        LockableViewPager.setSwipeLocked(false);

                        if (MainActivity.getQueueLayout()) {

                            Intent intent = new Intent();
                            intent.setAction(INTENT_QUEUEVIEW);
                            mContext.sendBroadcast(intent);

                            return true;


                        } else if (getFragmentManager().findFragmentById(R.id.fragment_album_list_layout) != null) {

                            MainActivity.setAlbumFragmentState(false);

                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                            ft.remove(getFragmentManager().findFragmentById(R.id.fragment_album_list_layout));
                            ft.commit();

                            Intent intent = new Intent();
                            intent.setAction("reload");
                            mContext.sendBroadcast(intent);

                            Intent shadow = new Intent();
                            shadow.setAction(INTENT_TOOLBAR8_SHADOW);
                            shadow.putExtra("boolean", true);
                            mContext.sendBroadcast(shadow);

                            return true;
                        }

                        MainActivity.setAlbumFragmentState(false);

                        return false;
                    }

                    return false;
                }
            });

        }

    }


    private class Etat_player extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();


            if (INTENT_STATE.equals(receiveIntent)
                    && intent.getStringExtra("state").equals("prev")
                    || intent.getStringExtra("state").equals("next")
                    || intent.getStringExtra("state").equals("play")) {

                for (int i = 0; i < listeTitre.size(); i++) {
                    if (listeTitre.get(i).getId() == PlayerService.getSongID())
                        mRecyclerView.smoothScrollToPosition( i );
                }

                //   rustine lag next.prev    //
                if (PlayerService.isPlaying()) {
                    mAdapter.notifyDataSetChanged();

                } else {

                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            mAdapter.notifyDataSetChanged();
                        }
                    }, 100);

                }
            }
        }
    }


}
