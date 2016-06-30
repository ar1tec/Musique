package org.oucho.musicplayer.adapters;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.model.Song;
import org.oucho.musicplayer.utils.GlobalVar;

import java.util.Collections;
import java.util.List;


public class SongAlbumListAdapter extends Adapter<SongAlbumListAdapter.SongViewHolder> {


    private List<Song> mSongList = Collections.emptyList();

    public void setData(List<Song> data) {
        mSongList = data;
        notifyDataSetChanged();
    }

    public List<Song> getSongList() {
        return mSongList;
    }

    @Override
    public SongViewHolder onCreateViewHolderImpl(ViewGroup parent) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.song_album_list_item, parent, false);

        return new SongViewHolder(itemView);
    }


    @Override
    public void onBindViewHolderImpl(SongViewHolder holder, int position) {
        Song song = getItem(position);

        holder.vTitle.setText(song.getTitle());

        holder.vTrackNumber.setText(String.valueOf(position + 1));

        if (song.getId() == GlobalVar.getCurrentSongID()) {

            holder.vTrackNumber.setVisibility(View.INVISIBLE);
            holder.PlayView.setVisibility(View.VISIBLE);

        } else {
            holder.PlayView.setVisibility(View.INVISIBLE);
            holder.vTrackNumber.setVisibility(View.VISIBLE);
        }

    }

    public Song getItem(int position) {
        return mSongList.get(position);
    }


    @Override
    public int getItemCountImpl() {
        return mSongList.size();
    }

    @Override
    public int getItemViewTypeImpl() {
        return 0;
    }


    public class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView vTitle;
        private final TextView vTrackNumber;

        final ImageView PlayView;

        public SongViewHolder(View itemView) {
            super(itemView);
            vTitle = (TextView) itemView.findViewById(R.id.title);
            vTrackNumber = (TextView) itemView.findViewById(R.id.track_number);

            PlayView = (ImageView) itemView.findViewById(R.id.play);

            itemView.setOnClickListener(this);

            ImageButton menuButton = (ImageButton) itemView.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(this);

        }


        @Override
        public void onClick(View v) {
            final int position = getAdapterPosition();
            triggerOnItemClickListener(position, v);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {

                    notifyItemRangeChanged(0, mSongList.size(), null);
                }
            }, 100);

        }
    }



}
