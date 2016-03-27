package org.oucho.musicplayer.adapters;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.oucho.musicplayer.PlaybackService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.model.Song;
import org.oucho.musicplayer.model.db.queue.QueueDbHelper;
import org.oucho.musicplayer.utils.VuMetre.VuMeterView;
import org.oucho.musicplayer.widgets.FastScroller;

import java.util.Collections;
import java.util.List;


public class SongAlbumListAdapter extends AdapterWithHeader<SongAlbumListAdapter.SongViewHolder>
        implements FastScroller.SectionIndexer {


    private List<Song> mSongList = Collections.emptyList();

    public SongAlbumListAdapter(Context c) {

    }

    public void setData(List<Song> data) {
        mSongList = data;
        notifyDataSetChanged();
    }

    public List<Song> getSongList() {
        return mSongList;
    }

    @Override
    public SongViewHolder onCreateViewHolderImpl(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.song_album_list_item, parent, false);


        return new SongViewHolder(itemView);
    }


    @Override
    public void onBindViewHolderImpl(SongViewHolder holder, int position) {
        Song song = getItem(position);



        String Track = String.valueOf(position + 1);

        holder.vTitle.setText(song.getTitle());

        holder.vTrackNumber.setText(Track);
        holder.vTrackNumber.setVisibility(View.VISIBLE);

        holder.vuMeter.setVisibility(View.GONE);



    }

    public Song getItem(int position) {
        return mSongList.get(position);
    }

    @Override
    public int getItemCountImpl() {
        return mSongList.size();
    }

    @Override
    public int getItemViewTypeImpl(int position) {
        return 0;
    }

    @Override
    public String getSectionForPosition(int position) {
        if(position >= 1) { // on ne prend pas en compte le header
            position--; // je répète : on ne prend pas en compte le header
            String title = getItem(position).getTitle();
            if (title.length() > 0) {
                return title.substring(0, 1);
            }
        }
        return "";
    }

    public class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView vTitle;
        private final TextView vTrackNumber;
        private final VuMeterView vuMeter;


        public SongViewHolder(View itemView) {
            super(itemView);
            vTitle = (TextView) itemView.findViewById(R.id.title);
            vTrackNumber = (TextView) itemView.findViewById(R.id.track_number);
            vuMeter = (VuMeterView) itemView.findViewById(R.id.vu_meter);

            itemView.setOnClickListener(this);

            ImageButton menuButton = (ImageButton) itemView.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(this);
        }


        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            triggerOnItemClickListener(position, v);

            vumetre(v, position);

        }
    }

    int PrevCurrentPos = -1;

    public void vumetre(View v, int position) {

        if (PrevCurrentPos != position)
        notifyItemChanged(PrevCurrentPos, null);

        View trackNumber = v.findViewById(R.id.track_number);
        View VuMeter = v.findViewById(R.id.vu_meter);

        trackNumber.setVisibility(View.INVISIBLE);
        VuMeter.setVisibility(View.VISIBLE);

        PrevCurrentPos = position;
    }

}
