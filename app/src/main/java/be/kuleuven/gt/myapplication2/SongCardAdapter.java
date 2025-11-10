package be.kuleuven.gt.myapplication2;

/**
 * CardStackView adapter that shows song cards with album-art and a
 * “Play Preview” button.
 * • updateData()  – replaces the entire list
 * • addData()     – appends a batch
 * • clear()       – removes all cards
 * Media playback is handled inside the ViewHolder.
 */
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.yuyakaido.android.cardstackview.CardStackView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.kuleuven.gt.myapplication2.databinding.ItemSongCardBinding;

public class SongCardAdapter extends CardStackView.Adapter<SongCardAdapter.SongViewHolder> {

    private final List<Song> songList;

    /** Constructs the adapter with an initial (possibly empty) list. */
    public SongCardAdapter(List<Song> songList) {
        this.songList = new ArrayList<>(songList);
    }

    /** Replaces current data set and refreshes the view. */
    public void updateData(List<Song> newSongs) {
        songList.clear();
        songList.addAll(newSongs);
        notifyDataSetChanged();
    }

    /** Adds a batch of songs and notifies the range inserted. */
    public void addData(List<Song> newSongs) {
        int start = songList.size();
        songList.addAll(newSongs);
        notifyItemRangeInserted(start, newSongs.size());
    }

    public List<Song> getSongList() { return songList; }

    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemSongCardBinding binding = ItemSongCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SongViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, int position) {
        holder.bind(songList.get(position));
    }

    @Override
    public int getItemCount() { return songList.size(); }

    /** Clears all cards from the adapter. */
    public void clear() {
        int size = songList.size();
        songList.clear();
        notifyItemRangeRemoved(0, size);
    }

    /** ViewHolder that manages UI binding and preview playback. */
    public static class SongViewHolder extends RecyclerView.ViewHolder {

        private final ItemSongCardBinding binding;
        private MediaPlayer mediaPlayer;
        private boolean isPreparingMedia = false;

        public SongViewHolder(ItemSongCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /** Populates the card UI and sets up the play button. */
        public void bind(Song song) {
            binding.setSong(song);      // Data-binding variables in layout
            binding.executePendingBindings();

            // Load album artwork (Picasso handles caching)
            if (song.getAlbumCoverUrl() != null && !song.getAlbumCoverUrl().isEmpty()) {
                Picasso.get()
                        .load(song.getAlbumCoverUrl())
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(binding.albumCoverImageView);
            }

            // Play / Stop preview button logic
            binding.btnPlayPreview.setOnClickListener(v -> {
                if (isPreparingMedia) {
                    Toast.makeText(v.getContext(), "Please wait…", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    stopPlayback();
                } else {
                    if (song.getPreviewUrl() == null || song.getPreviewUrl().isEmpty()) {
                        Toast.makeText(v.getContext(), "No preview available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startPlayback(song.getPreviewUrl(), v);
                }
            });
        }

        /** Starts asynchronous playback of the 30-sec Spotify preview. */
        private void startPlayback(String previewUrl, View view) {
            isPreparingMedia = true;
            binding.btnPlayPreview.setText("⏳ Loading…");

            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayer = new MediaPlayer();

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    mediaPlayer.setAudioAttributes(
                            new android.media.AudioAttributes.Builder()
                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                    .build());
                } else {
                    mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
                }

                mediaPlayer.setDataSource(previewUrl);
                mediaPlayer.setOnPreparedListener(mp -> {
                    isPreparingMedia = false;
                    binding.btnPlayPreview.setText("⏹ Stop");
                    mp.start();
                });
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    isPreparingMedia = false;
                    binding.btnPlayPreview.setText("Play Preview");
                    Toast.makeText(view.getContext(), "Failed to play preview", Toast.LENGTH_SHORT).show();
                    return true;
                });
                mediaPlayer.setOnCompletionListener(mp -> {
                    binding.btnPlayPreview.setText("Play Preview");
                    stopPlayback();
                });

                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                isPreparingMedia = false;
                binding.btnPlayPreview.setText("Play Preview");
                Toast.makeText(view.getContext(), "Error playing preview", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        /** Stops and releases the MediaPlayer if active. */
        private void stopPlayback() {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            binding.btnPlayPreview.setText("Play Preview");
            isPreparingMedia = false;
        }

        /** Should be called from Activity/Fragment to free resources. */
        public void releaseResources() { stopPlayback(); }
    }
}
