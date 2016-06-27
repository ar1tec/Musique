package org.oucho.musicplayer.fragments;

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.adapters.BaseAdapter;
import org.oucho.musicplayer.adapters.SongAlbumListAdapter;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.dialog.SongEditorDialog;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.loaders.SongLoader;
import org.oucho.musicplayer.model.Album;
import org.oucho.musicplayer.model.Playlist;
import org.oucho.musicplayer.model.Song;
import org.oucho.musicplayer.utils.PlaylistsUtils;

import java.util.List;


public class AlbumFragment extends BaseFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_ARTIST = "artist";
    private static final String ARG_YEAR = "year";
    private static final String ARG_TRACK_COUNT = "track_count";

    private Album mAlbum;

    private SongAlbumListAdapter mAdapter;

    private MainActivity mActivity;

    private int mArtworkWidth;
    private int mArtworkHeight;

    private String Titre = "";
    private String Artiste = "";
    private String Année = "";
    private String nb_Morceaux = "";


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
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            //  Auto-generated method stub
        }
    };


    /* *********************************************************************************************
     * Création du fragment
     * ********************************************************************************************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();


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

        mArtworkWidth = getResources().getDimensionPixelSize(R.dimen.artist_image_req_width);
        mArtworkHeight = getResources().getDimensionPixelSize(R.dimen.artist_image_req_height);

        getActivity().setTitle(R.string.album);
    }



    /* *********************************************************************************************
     * Création du visuel
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_album, container, false);

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        TextView titreAlbum = (TextView) rootView.findViewById(R.id.line1);
        titreAlbum.setText(Titre);

        TextView artiste = (TextView) rootView.findViewById(R.id.line2);
        artiste.setText(Artiste);

        TextView an = (TextView) rootView.findViewById(R.id.line3);
        an.setText(Année);

        TextView morceaux = (TextView) rootView.findViewById(R.id.line4);
        morceaux.setText(nb_Morceaux);


        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.song_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new SongAlbumListAdapter();
        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mRecyclerView.setAdapter(mAdapter);


        //mRecyclerView.smoothScrollToPosition(mAdapter.getPosition());



        ImageView artworkView = (ImageView) rootView.findViewById(R.id.album_artwork);

        ArtworkCache.getInstance().loadBitmap(mAlbum.getId(), artworkView, mArtworkWidth, mArtworkHeight);

        return rootView;
    }

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

        if (mActivity != null) {
            mActivity.onSongSelected(mAdapter.getSongList(), position);
        }
    }

    private final SongEditorDialog.OnTagsEditionSuccessListener mOnTagsEditionSuccessListener
            = new SongEditorDialog.OnTagsEditionSuccessListener() {
        @Override
        public void onTagsEditionSuccess() {
            ((MainActivity) getActivity()).refresh();
        }
    };

    private void showMenu(final int position, View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        final Song song = mAdapter.getItem(position);
        inflater.inflate(R.menu.song_list_item, popup.getMenu());
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
        try {
            mActivity = (MainActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
}
