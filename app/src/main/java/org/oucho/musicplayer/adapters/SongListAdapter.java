package org.oucho.musicplayer.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.images.ArtworkHelper;
import org.oucho.musicplayer.db.model.Song;

import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class SongListAdapter extends Adapter<SongListAdapter.SongViewHolder> {

    private final int mThumbWidth;
    private final int mThumbHeight;
    private final Context mContext;
    private List<Song> mSongList = Collections.emptyList();

    public SongListAdapter(Context c) {
        mThumbWidth = c.getResources().getDimensionPixelSize(R.dimen.art_thumbnail_size);
        //noinspection SuspiciousNameCombination
        mThumbHeight = mThumbWidth;
        mContext = c;
    }

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
                R.layout.fragment_song_item, parent, false);

        return new SongViewHolder(itemView);
    }

    @Override
    public void onBindViewHolderImpl(SongViewHolder holder, int position) {
        Song song = getItem(position);

        holder.vTitle.setText(song.getTitle());
        holder.vArtist.setText(song.getArtist());
        holder.vDuration.setText("(" + msToText(song.getDuration()) + ")");

        //évite de charger des images dans les mauvaises vues si elles sont recyclées
        holder.vArtwork.setTag(position);

        ArtworkCache.getInstance().loadBitmap(song.getAlbumId(), holder.vArtwork, mThumbWidth, mThumbHeight, ArtworkHelper.getDefaultThumbDrawable(mContext));
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
        private final TextView vArtist;
        private final TextView vDuration;

        private final ImageView vArtwork;


        public SongViewHolder(View itemView) {
            super(itemView);
            vTitle = (TextView) itemView.findViewById(R.id.title);
            vArtist = (TextView) itemView.findViewById(R.id.artist);
            vDuration = (TextView) itemView.findViewById(R.id.duration);

            vArtwork = (ImageView) itemView.findViewById(R.id.artwork);
            itemView.setOnClickListener(this);

            ImageButton menuButton = (ImageButton) itemView.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            triggerOnItemClickListener(position, v);


        }
    }

    private String msToText(int msec) {
        return String.format(Locale.getDefault(), "%d:%02d", msec / 60000, (msec % 60000) / 1000);
    }
}
