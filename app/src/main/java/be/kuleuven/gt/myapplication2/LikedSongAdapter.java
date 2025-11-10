package be.kuleuven.gt.myapplication2;

/**
 * RecyclerView adapter that displays songs the user has liked in Groover.
 * Provides two listener hooks:
 * • deleteListener – remove from Groover’s backend
 * • likeListener   – “like” the track on Spotify
 */
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LikedSongAdapter extends RecyclerView.Adapter<LikedSongAdapter.LikedSongViewHolder> {

    private final List<Song> likedSongs;
    private final OnDeleteClickListener deleteListener;
    private final OnSpotifyLikeClickListener likeListener;

    /** Callback for the “delete” (trash-bin) icon. */
    public interface OnDeleteClickListener {
        void onDeleteClick(Song song, int position);
    }

    /** Callback for the “like on Spotify” button. */
    public interface OnSpotifyLikeClickListener {
        void onLikeClick(Song song);
    }

    /** Adapter constructor receives the data set and two action listeners. */
    public LikedSongAdapter(List<Song> likedSongs,
                            OnDeleteClickListener deleteListener,
                            OnSpotifyLikeClickListener likeListener) {
        this.likedSongs = likedSongs;
        this.deleteListener = deleteListener;
        this.likeListener = likeListener;
    }

    /** Inflates a single row view. */
    @NonNull
    @Override
    public LikedSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_liked_song, parent, false);
        return new LikedSongViewHolder(view);
    }

    /** Binds the song data and click listeners to the view holder. */
    @Override
    public void onBindViewHolder(@NonNull LikedSongViewHolder holder, int position) {
        Song song = likedSongs.get(position);
        holder.titleText.setText(song.getName());
        holder.artistText.setText(song.getArtist());

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteClick(song, position);
        });

        holder.btnLikeOnSpotify.setOnClickListener(v -> {
            if (likeListener != null) likeListener.onLikeClick(song);
        });
    }

    @Override
    public int getItemCount() {
        return likedSongs.size();
    }

    /** Removes an item locally and notifies the adapter. */
    public void removeItem(int position) {
        likedSongs.remove(position);
        notifyItemRemoved(position);
    }

    /** Holds view references for an individual row. */
    public static class LikedSongViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, artistText;
        Button btnDelete, btnLikeOnSpotify;

        public LikedSongViewHolder(View itemView) {
            super(itemView);
            titleText       = itemView.findViewById(R.id.songTitleText);
            artistText      = itemView.findViewById(R.id.artistText);
            btnDelete       = itemView.findViewById(R.id.btnDelete);
            btnLikeOnSpotify = itemView.findViewById(R.id.btnLikeOnSpotify);
        }
    }
}
