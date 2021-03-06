package org.oucho.musicplayer.fragments.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.angelo.Angelo;
import org.oucho.musicplayer.view.fastscroll.FastScroller;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;


public class AlbumListAdapter extends BaseAdapter<AlbumListAdapter.AlbumViewHolder> implements FastScroller.SectionIndexer, MusiqueKeys {

    private final int mArtworkWidth;
    private final int mArtworkHeight;
    private final Context mContext;
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


    @Override
    public AlbumViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        int mLayoutId = R.layout.fragment_liste_album_item;
        View itemView = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);

        return new AlbumViewHolder(itemView);
    }

    @Override
    public String getSectionText(int position) {

        Album album = mAlbumList.get(position);

        SharedPreferences préférences = mContext.getSharedPreferences(FICHIER_PREFS, Context.MODE_PRIVATE);


        String getTri = préférences.getString("album_sort_order", "");


        if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {

            String toto = String.valueOf(album.getArtistName()).replaceFirst("The ", "");

            return stripAccents(String.valueOf(toto.toUpperCase().charAt(0)));

        } else if ("minyear DESC".equals(getTri)) {

            String toto = String.valueOf(album.getYear());

            return String.valueOf(toto);

        } else {

            String toto = String.valueOf(album.getAlbumName())
                    .replaceFirst("The ", "")
                    .replaceFirst("A ", "");

            return stripAccents(String.valueOf(toto.toUpperCase().charAt(0)));
        }
    }

    private static String stripAccents(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s;
    }

    @Override
    public void onBindViewHolder(AlbumViewHolder viewHolder, int position) {
        Album album = mAlbumList.get(position);

        SharedPreferences préférences = mContext.getSharedPreferences(FICHIER_PREFS, Context.MODE_PRIVATE);

        String getTri = préférences.getString("album_sort_order", "");

            if (album.getId() == PlayerService.getAlbumId()) {

                viewHolder.vPlayerStatus.setVisibility(View.VISIBLE);
                viewHolder.vPlayerStatusFond.setVisibility(View.VISIBLE);

            } else {

                viewHolder.vPlayerStatus.setVisibility(View.INVISIBLE);
                viewHolder.vPlayerStatusFond.setVisibility(View.INVISIBLE);
            }

            if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {

                viewHolder.vName.setTextColor(ContextCompat.getColor(mContext, R.color.grey_600));
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

                viewHolder.vArtist.setTextColor(ContextCompat.getColor(mContext, R.color.grey_600));
                viewHolder.vArtist.setTextSize(14);
                viewHolder.vArtist.setTypeface(null, Typeface.NORMAL);

                viewHolder.vYear.setText(String.valueOf(album.getYear()));
                viewHolder.vYear.setVisibility(View.VISIBLE);

                viewHolder.vBackgroundYear.setVisibility(View.VISIBLE);

            } else {

                viewHolder.vName.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
                viewHolder.vName.setTextSize(15);
                viewHolder.vName.setTypeface(null, Typeface.BOLD);

                viewHolder.vArtist.setTextColor(ContextCompat.getColor(mContext, R.color.grey_600));
                viewHolder.vArtist.setTextSize(14);
                viewHolder.vArtist.setTypeface(null, Typeface.NORMAL);

                viewHolder.vYear.setVisibility(View.INVISIBLE);
                viewHolder.vBackgroundYear.setVisibility(View.INVISIBLE);
            }

        viewHolder.vName.setText(album.getAlbumName());
        viewHolder.vArtist.setText(album.getArtistName());

        //évite de charger des images dans les mauvaises vues si elles sont recyclées
        viewHolder.vArtwork.setTag(position);


        if (album.getId() == -1) {
            return;
        }

        Uri uri = ContentUris.withAppendedId(ARTWORK_URI, album.getId());
        Angelo.with(viewHolder.itemView.getContext())
                .load(uri)
                .config(Bitmap.Config.RGB_565)
                .resize(mArtworkWidth, mArtworkHeight)
                .centerCrop()
                .into(viewHolder.vArtwork);
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

        private final TextView vArtist;

        private AlbumViewHolder(View itemView) {
            super(itemView);

            vArtwork = itemView.findViewById(R.id.album_artwork);
            vName = itemView.findViewById(R.id.album_name);
            vYear = itemView.findViewById(R.id.year);
            vBackgroundYear = itemView.findViewById(R.id.background_year);
            vPlayerStatus = itemView.findViewById(R.id.album_play);
            vPlayerStatusFond = itemView.findViewById(R.id.album_play_fond);

            vAlbumInfo = itemView.findViewById(R.id.album_info);

            vArtwork.setOnClickListener(this);

            vArtist = itemView.findViewById(R.id.artist_name);
            itemView.findViewById(R.id.album_info).setOnClickListener(this);

            vArtwork.setOnLongClickListener(this);
            vAlbumInfo.setOnLongClickListener(this);

            ImageButton menuButton = itemView.findViewById(R.id.menu_button);
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
