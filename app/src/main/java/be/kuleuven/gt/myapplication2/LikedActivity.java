package be.kuleuven.gt.myapplication2;

/**
 * Shows the list of songs the user has liked in Groover.
 * Users can:
 * • remove a song from their Groover likes (DELETE call)
 * • “like” the song on Spotify (PUT to Spotify API)
 * The screen also includes the global bottom-navigation bar.
 */
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LikedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LikedSongAdapter adapter;
    private String currentUsername;
    private final List<Song> likedSongs = new ArrayList<>();

    /** Sets up UI components, adapters, and initial data fetch. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liked);

        // RecyclerView + adapter
        recyclerView = findViewById(R.id.recyclerViewLikedSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LikedSongAdapter(
                likedSongs,
                (song, position) -> {          // Delete button
                    deleteSongFromServer(song);
                    adapter.removeItem(position);
                },
                song -> likeSongOnSpotify(song) // Spotify like button
        );
        recyclerView.setAdapter(adapter);

        // Current user
        SharedPreferences prefs = getSharedPreferences("grooverPrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", "testuser");

        // Load liked songs from backend
        fetchLikedSongsFromServer();

        // Bottom-navigation bar (kept as original if-chain)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_swipe) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.nav_liked) {
                startActivity(new Intent(this, LikedActivity.class));
                return true;
            }
            return false;
        });
    }

    /**
     * Sends a “like” request to the Spotify Web API for the given song.
     * Requires a valid Spotify OAuth token stored in SharedPreferences.
     */
    private void likeSongOnSpotify(Song song) {
        SharedPreferences prefs = getSharedPreferences("grooverPrefs", MODE_PRIVATE);
        String token = prefs.getString("SPOTIFY_TOKEN", null);

        if (token == null) {
            Toast.makeText(this, "Spotify token missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        String songId = song.getId();
        String url = "https://api.spotify.com/v1/me/tracks?ids=" + songId;

        Log.d("SPOTIFY_LIKE", "Trying to like song with ID: " + songId);
        Log.d("SPOTIFY_LIKE", "Using token: " + token);
        Log.d("SPOTIFY_LIKE", "Request URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT, url, null,
                response -> Toast.makeText(this, "Liked on Spotify!", Toast.LENGTH_SHORT).show(),
                error -> {
                    error.printStackTrace();
                    if (error.networkResponse != null) {
                        String errorBody = new String(error.networkResponse.data);
                        Log.e("SPOTIFY_LIKE_ERROR", "Status: " + error.networkResponse.statusCode);
                        Log.e("SPOTIFY_LIKE_ERROR", "Body: " + errorBody);
                    }
                    // Optional toast removed in original code
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    /** Retrieves the user’s liked songs from the Groover backend. */
    private void fetchLikedSongsFromServer() {
        String url = "https://studev.groept.be/api/a24pt103/get_groover_liked_songs/" + currentUsername;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    likedSongs.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            String title  = obj.getString("title");
                            String artist = obj.getString("artist");
                            String songId = obj.getString("songid");

                            Song song = new Song(title, artist, "", null);
                            song.setId(songId);
                            likedSongs.add(song);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    adapter.notifyDataSetChanged();
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Failed to load liked songs", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    /**
     * Removes a liked song from the Groover database.
     * Called after the user presses the “delete” (trash) icon.
     */
    private void deleteSongFromServer(Song song) {
        String url = "https://studev.groept.be/api/a24pt103/delete_liked_song/"
                + Uri.encode(currentUsername) + "/" + Uri.encode(song.getId());

        Log.d("DELETE_URL", url);

        StringRequest req = new StringRequest(
                Request.Method.GET, url,
                response -> Log.d("DELETE", "Song deleted from DB: " + song.getName()),
                error -> Log.e("DELETE", "Failed to delete song from DB", error));

        Volley.newRequestQueue(this).add(req);
    }
}
