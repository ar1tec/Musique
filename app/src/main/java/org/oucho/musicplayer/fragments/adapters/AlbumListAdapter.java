package org.oucho.musicplayer.fragments.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.oucho.musicplayer.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.images.ArtworkCache;
import org.oucho.musicplayer.images.ArtworkHelper;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.utils.MusiqueKeys;

import java.util.Collections;
import java.util.List;


public class AlbumListAdapter extends BaseAdapter<AlbumListAdapter.AlbumViewHolder> implements MusiqueKeys {

    private final int mArtworkWidth;
    private final int mArtworkHeight;
    private final Context mContext;
    private int mLayoutId = R.layout.fragment_album_list_item;
    private List<Album> mAlbumList = Collections.emptyList();


    public AlbumListAdapter(Context context, int artworkWidth, int artworkHeight) {
        mArtworkWidth = artworkWidth;
        mArtworkHeight = artworkHeight;
        mContext = context;
    }

    public void setData(List<Album> data) {
        mAlbumList = data;
        notifyDataSetChanged();
    }

    public void setLayoutId() {
        mLayoutId = R.layout.small_album_grid_item;
    }

    @Override
    public AlbumViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                mLayoutId, parent, false);


        return new AlbumViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(AlbumViewHolder viewHolder, int position) {
        Album album = mAlbumList.get(position);

        SharedPreferences préférences = mContext.getSharedPreferences(fichier_préférence, Context.MODE_PRIVATE);


        String getTri = préférences.getString("album_sort_order", "");

        if (mLayoutId != R.layout.small_album_grid_item) {

            if (album.getId() == PlayerService.getAlbumId()) {

                viewHolder.vPlayerStatus.setVisibility(View.VISIBLE);
                viewHolder.vPlayerStatusFond.setVisibility(View.VISIBLE);

            } else {

                viewHolder.vPlayerStatus.setVisibility(View.INVISIBLE);
                viewHolder.vPlayerStatusFond.setVisibility(View.INVISIBLE);
            }

            if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {

                viewHolder.vName.setTextColor(ContextCompat.getColor(mContext, R.color.secondary_text));
                viewHolder.vName.setTextSize(14);
                viewHolder.vName.setTypeface(null, Typeface.NORMAL);

                viewHolder.vArtist.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
                viewHolder.vArtist.setTextSize(15);
                viewHolder.vArtist.setTypeface(null, Typeface.BOLD);

                viewHolder.vYear.setVisibility(View.INVISIBLE);
                viewHolder.vBackgroundYear.setVisibility(View.INVISIBLE);

            } else if ("minyear DESC".equals(getTri)) {

                viewHolder.vName.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
                viewHolder.vName.setTextSize(15);
                viewHolder.vName.setTypeface(null, Typeface.NORMAL);

                viewHolder.vArtist.setTextColor(ContextCompat.getColor(mContext, R.color.secondary_text));
                viewHolder.vArtist.setTextSize(14);
                viewHolder.vArtist.setTypeface(null, Typeface.NORMAL);

                viewHolder.vYear.setText(String.valueOf(album.getYear()));
                viewHolder.vYear.setVisibility(View.VISIBLE);

                viewHolder.vBackgroundYear.setVisibility(View.VISIBLE);

            } else {

                viewHolder.vName.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
                viewHolder.vName.setTextSize(15);
                viewHolder.vName.setTypeface(null, Typeface.BOLD);

                viewHolder.vArtist.setTextColor(ContextCompat.getColor(mContext, R.color.secondary_text));
                viewHolder.vArtist.setTextSize(14);
                viewHolder.vArtist.setTypeface(null, Typeface.NORMAL);

                viewHolder.vYear.setVisibility(View.INVISIBLE);
                viewHolder.vBackgroundYear.setVisibility(View.INVISIBLE);
            }
        }

        viewHolder.vName.setText(album.getAlbumName());
        if (mLayoutId != R.layout.small_album_grid_item) {
            viewHolder.vArtist.setText(album.getArtistName());
        }

        //évite de charger des images dans les mauvaises vues si elles sont recyclées
        viewHolder.vArtwork.setTag(position);

        ArtworkCache.getInstance().loadBitmap(album.getId(),
                viewHolder.vArtwork,
                mArtworkWidth,
                mArtworkHeight,
                ArtworkHelper.getDefaultArtworkDrawable(mContext));
    }

    @Override
    public int getItemCount() {
        return mAlbumList.size();
    }


    public Album getItem(int position) {
        return mAlbumList.get(position);
    }

    class AlbumViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        final ImageView vArtwork;
        final TextView vName;
        final TextView vYear;
        final ImageView vBackgroundYear;

        private final ImageView vPlayerStatus;
        private final ImageView  vPlayerStatusFond;

        private final LinearLayout vAlbumInfo;

        private TextView vArtist;

        private AlbumViewHolder(View itemView) {
            super(itemView);

            vArtwork = (ImageView) itemView.findViewById(R.id.album_artwork);
            vName = (TextView) itemView.findViewById(R.id.album_name);
            vYear = (TextView) itemView.findViewById(R.id.year);
            vBackgroundYear = (ImageView) itemView.findViewById(R.id.background_year);
            vPlayerStatus = (ImageView) itemView.findViewById(R.id.album_play);
            vPlayerStatusFond = (ImageView) itemView.findViewById(R.id.album_play_fond);

            vAlbumInfo = (LinearLayout) itemView.findViewById(R.id.album_info);

            vArtwork.setOnClickListener(this);


            if (mLayoutId != R.layout.small_album_grid_item) {
                vArtist = (TextView) itemView.findViewById(R.id.artist_name);
                itemView.findViewById(R.id.album_info).setOnClickListener(this);

                vArtwork.setOnLongClickListener(this);
                vAlbumInfo.setOnLongClickListener(this);

            } else {
                vName.setOnClickListener(this);
            }

            ImageButton menuButton = (ImageButton) itemView.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();

            triggerOnItemClickListener(position, v);
        }

        @Override
        public boolean onLongClick(View v) {
            int position = getAdapterPosition();

            triggerOnItemLongClickListener(position, v);

            return true;
        }
    }

}
