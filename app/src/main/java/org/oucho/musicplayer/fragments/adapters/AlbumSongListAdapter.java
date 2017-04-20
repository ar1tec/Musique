package org.oucho.musicplayer.fragments.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.oucho.musicplayer.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Song;

import java.util.Collections;
import java.util.List;


public class AlbumSongListAdapter extends Adapter<AlbumSongListAdapter.SongViewHolder> {


    private List<Song> mSongList = Collections.emptyList();

    private Context mContext;

    public void setData(Context context, List<Song> data) {
        mSongList = data;
        mContext = context;
        notifyDataSetChanged();
    }

    public List<Song> getSongList() {
        return mSongList;
    }

    @Override
    public SongViewHolder onCreateViewHolderImpl(ViewGroup parent) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.fragment_album_song_list_item, parent, false);

        return new SongViewHolder(itemView);
    }


    @Override
    public void onBindViewHolderImpl(SongViewHolder holder, int position) {
        Song song = getItem(position);

        long secondes = song.getDuration() / 1000;

        String duration = "(" + String.valueOf( (secondes % 3600) / 60 ) + ":" + String.format("%02d", (secondes % 3600) % 60 ) + ")";

        holder.vTime.setText(duration);
        holder.vTitle.setText(song.getTitle());

        holder.vTrackNumber.setText(String.valueOf(position + 1));

        if (song.getId() == PlayerService.getSongID()) {

            if (PlayerService.isPlaying()) {

                holder.vTrackNumber.setVisibility(View.INVISIBLE);
                holder.PlayView.setVisibility(View.VISIBLE);
            } else {
                holder.vTrackNumber.setVisibility(View.INVISIBLE);
                holder.PlayView.setImageResource(R.drawable.ic_pause_jaune_24dp);
                holder.PlayView.setVisibility(View.VISIBLE);
            }

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


    class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView vTime;
        private final TextView vTitle;
        private final TextView vTrackNumber;


        final ImageView PlayView;

        public SongViewHolder(View itemView) {
            super(itemView);

            vTime = (TextView) itemView.findViewById(R.id.time);

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

        }
    }

}
