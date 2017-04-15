package org.oucho.musicplayer.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.images.ArtworkHelper;
import org.oucho.musicplayer.loaders.PlaylistLoader;
import org.oucho.musicplayer.model.Playlist;
import org.oucho.musicplayer.model.Song;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.widgets.DragRecyclerView;
import org.oucho.musicplayer.widgets.FastScroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PlaylistFragment extends BaseFragment {

    private static final String PARAM_PLAYLIST_ID = "playlist_id";
    private static final String PARAM_PLAYLIST_NAME = "playlist_name";

    private MainActivity mActivity;

    private ArrayList<Song> mSongList = new ArrayList<>();
    private DragRecyclerView mRecyclerView;

    private Playlist mPlaylist;

    private SongListAdapter mAdapter;

    private final LoaderManager.LoaderCallbacks<List<Song>> mLoaderCallbacks = new LoaderCallbacks<List<Song>>() {


        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {

            return new PlaylistLoader(getActivity(), mPlaylist.getId());
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
            mSongList = new ArrayList<>(data);
            mAdapter.notifyDataSetChanged();

        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            // This constructor is intentionally empty, pourquoi ? parce que !
        }
    };

    public static PlaylistFragment newInstance(Playlist playlist) {
        PlaylistFragment fragment = new PlaylistFragment();

        Bundle args = new Bundle();

        args.putLong(PARAM_PLAYLIST_ID, playlist.getId());
        args.putString(PARAM_PLAYLIST_NAME, playlist.getName());

        fragment.setArguments(args);
        return fragment;
    }


    private void selectSong(int position) {

        if (mActivity != null) {
            mActivity.onSongSelected(mSongList, position);
        }
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setHasOptionsMenu(true);
        if (args != null) {

            long id = args.getLong(PARAM_PLAYLIST_ID);
            String name = args.getString(PARAM_PLAYLIST_NAME);
            mPlaylist = new Playlist(id, name);

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);

        mRecyclerView = (DragRecyclerView) rootView.findViewById(R.id.list_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new SongListAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setOnItemMovedListener(new DragRecyclerView.OnItemMovedListener() {
            @Override
            public void onItemMoved(int oldPosition, int newPosition) {
                mAdapter.moveItem(oldPosition, newPosition);
            }
        });

        FastScroller scroller = (FastScroller) rootView.findViewById(R.id.fastscroller);
        scroller.setRecyclerView(mRecyclerView);

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        // Active la touche back
        if(getView() == null){
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){

                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                    ft.remove(getFragmentManager().findFragmentById(R.id.fragment_playlist_list));
                    ft.commit();

                    return true;
                }
                return false;
            }
        });
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        load();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }


    @Override
    public void load() {
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
    }

    private class SongViewHolder extends RecyclerView.ViewHolder implements OnClickListener, OnTouchListener {

        private final Context context = getContext(); // NOPMD

        final View itemView; // NOPMD
        final TextView vTitle; // NOPMD
        final TextView vArtist; // NOPMD
        final ImageButton vReorderButton; // NOPMD

        public SongViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            vTitle = (TextView) itemView.findViewById(R.id.title);
            vArtist = (TextView) itemView.findViewById(R.id.artist);

            vReorderButton = (ImageButton) itemView.findViewById(R.id.reorder_button);

            itemView.findViewById(R.id.song_info).setOnClickListener(this);
            itemView.findViewById(R.id.delete_button).setOnClickListener(this);

            vReorderButton.setOnTouchListener(this);

        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();

            switch (v.getId()) {
                case R.id.song_info:
                    selectSong(position);
                    break;
                case R.id.delete_button:
                    deleteSong(position);
                    break;
                default:
                    break;
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mRecyclerView.startDrag(itemView);

            return false;
        }

        private void deleteSong(int position) {


            final int song = position;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(context.getString(R.string.deleteTrackConfirm));
            builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    mAdapter.removeItem(song);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        }
    }


    class SongListAdapter extends RecyclerView.Adapter<SongViewHolder> {

        private final Context context = getContext(); // NOPMD

        @Override
        public SongViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_item, parent, false);


            return new SongViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(SongViewHolder viewHolder, int position) {

            final int mThumbSize = context.getResources().getDimensionPixelSize(R.dimen.art_thumbnail_playlist_size);


            Song song = mSongList.get(position);
            viewHolder.vTitle.setText(song.getTitle());
            viewHolder.vArtist.setText(song.getArtist());

            ArtworkCache.getInstance().loadBitmap(song.getAlbumId(), viewHolder.vReorderButton, mThumbSize, mThumbSize, ArtworkHelper.getDefaultThumbDrawable(context));

        }

        @Override
        public int getItemCount() {
            return mSongList.size();
        }

        public void moveItem(int oldPosition, int newPosition) {
            if (oldPosition < 0 || oldPosition >= mSongList.size()
                    || newPosition < 0 || newPosition >= mSongList.size()) {
                return;
            }
            Collections.swap(mSongList, oldPosition, newPosition);


            PlaylistsUtils.moveItem(getActivity().getContentResolver(), mPlaylist.getId(), oldPosition, newPosition);

            notifyItemMoved(oldPosition, newPosition);

        }

        public void removeItem(int position) {
            Song s = mSongList.remove(position);

            PlaylistsUtils.removeFromPlaylist(getActivity().getContentResolver(), mPlaylist.getId(), s.getId());
            notifyItemRemoved(position);
        }
    }
}
