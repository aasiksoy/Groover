package be.kuleuven.gt.myapplication2;

/**
 * Main swipe-screen of Groover.
 * • Displays stacked recommendation cards (CardStackView).
 * • Lets the user swipe to like/dislike, create playlists, and choose a mood.
 * • Handles fetching recommendations from Groover’s backend and the Spotify API.
 * • Integrates the global bottom-navigation bar.
 */
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yuyakaido.android.cardstackview.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class MainActivity extends AppCompatActivity {

    // --- UI & adapter ---
    private CardStackView cardStackView;
    private SongCardAdapter adapter;
    private CardStackLayoutManager manager;

    // --- User / auth ---
    private String currentUsername;
    private String spotifyToken;

    // --- Data structures ---
    private final List<Song> allRecommendedSongs = new ArrayList<>();
    private final List<Song> loadedSongs         = new ArrayList<>();
    private final List<String> swipedIds         = new ArrayList<>();
    private final List<String> likedURIs         = new ArrayList<>();

    private int expectedRecommendations = 0;
    private int fetchedCount            = 0;

    private MoodSelector.MoodPreset currentMoodPreset;

    /** Activity entry-point: sets up UI, listeners, and default mood selection. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Mood-selector FAB
        FloatingActionButton fabChangeMood = findViewById(R.id.fabChangeMood);
        fabChangeMood.setOnClickListener(v -> showMoodSelectionDialog());

        // Bottom navigation (kept as original if-chain)
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

        // Retrieve stored prefs
        SharedPreferences prefs = getSharedPreferences("grooverPrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", "testuser");
        spotifyToken    = prefs.getString("SPOTIFY_TOKEN", null);

        // If no Spotify token, redirect to login
        if (spotifyToken == null || spotifyToken.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Create-playlist button
        Button btnCreatePlaylist = findViewById(R.id.btnCreatePlaylist);
        btnCreatePlaylist.setOnClickListener(v -> createPlaylistAndAddSongs());

        // --- CardStackView configuration ---
        cardStackView = findViewById(R.id.card_stack_view);
        adapter       = new SongCardAdapter(new ArrayList<>());
        manager       = new CardStackLayoutManager(this, new CardStackListener() {
            @Override public void onCardDragging(Direction d, float r) {}
            @Override public void onCardRewound() {}
            @Override public void onCardCanceled() {}
            @Override public void onCardAppeared(View v, int p) {}
            @Override public void onCardDisappeared(View v, int p) {}

            /** Saves each swipe (like/dislike) and loads more songs when nearing the end. */
            @Override
            public void onCardSwiped(Direction direction) {
                int pos = manager.getTopPosition() - 1;
                if (pos >= 0 && pos < adapter.getSongList().size()) {
                    Song swipedSong = adapter.getSongList().get(pos);
                    boolean liked   = direction == Direction.Right;

                    if (!swipedIds.contains(swipedSong.getId())) {
                        swipedIds.add(swipedSong.getId());
                        saveSwipeToServer(swipedSong, liked);
                        if (liked) likedURIs.add(swipedSong.getUri());
                        Log.d("SWIPE", (liked ? "Liked: " : "Disliked: ") + swipedSong.getName());
                    }

                    // Preload when only 3 cards remain
                    if (adapter.getItemCount() - manager.getTopPosition() <= 3) {
                        loadMoreSongs();
                    }
                }
            }
        });

        manager.setStackFrom(StackFrom.Top);
        manager.setVisibleCount(3);
        manager.setTranslationInterval(8.0f);
        manager.setScaleInterval(0.95f);
        manager.setSwipeThreshold(0.3f);
        manager.setMaxDegree(20.0f);
        manager.setDirections(Direction.HORIZONTAL);
        manager.setCanScrollHorizontal(true);
        manager.setCanScrollVertical(false);

        cardStackView.setLayoutManager(manager);
        cardStackView.setAdapter(adapter);

        // Default mood
        currentMoodPreset = MoodSelector.getPartyPreset();

        // Prompt for mood immediately
        showMoodSelectionDialog();
    }

    /** Retrieves previously swiped song-IDs and then generates recommendations. */
    private void fetchSwipedIdsAndRecommend() {
        String url = "https://studev.groept.be/api/a24pt103/get_swiped_song_ids/" + currentUsername;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            swipedIds.add(response.getJSONObject(i).getString("song_id"));
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    fetchTopTrackAndRecommend();
                },
                error -> {
                    error.printStackTrace();
                    fetchTopTrackAndRecommend();
                });

        Volley.newRequestQueue(this).add(request);
    }

    /** Calls Spotify’s “top tracks” endpoint, then fetches audio-features for each track. */
    private void fetchTopTrackAndRecommend() {
        Log.d("DEBUG_FLOW", "fetchTopTrackAndRecommend() started");

        String url = "https://api.spotify.com/v1/me/top/tracks?limit=10";
        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray items = new JSONObject(response).getJSONArray("items");
                        for (int i = 0; i < items.length(); i++) {
                            String id = items.getJSONObject(i).getString("id");
                            checkAudioFeatures(id);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                },
                error -> error.printStackTrace()) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + spotifyToken);
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    /** Fetches stored audio-features for a Spotify track from Groover’s backend. */
    private void checkAudioFeatures(String trackId) {
        String url = "https://studev.groept.be/api/a24pt103/audio_features_by_id/" + trackId;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    if (response.length() > 0) {
                        try {
                            JSONObject o = response.getJSONObject(0);
                            fetchRecommendations(
                                    o.getDouble("energy"), o.getInt("key"), o.getDouble("loudness"),
                                    o.getInt("mode"), o.getDouble("speechiness"), o.getDouble("acousticness"),
                                    o.getDouble("instrumentalness"), o.getDouble("liveness"),
                                    o.getDouble("valence"), o.getDouble("tempo"));
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                },
                error -> error.printStackTrace());

        Volley.newRequestQueue(this).add(request);
    }

    /** Retrieves recommendation IDs from Groover backend given a set of audio-features. */
    private void fetchRecommendations(double energy, int key, double loudness, int mode,
                                      double speechiness, double acousticness, double instrumentalness,
                                      double liveness, double valence, double tempo) {

        String url = "https://studev.groept.be/api/a24pt103/song_recommendations/"
                + energy + "/" + key + "/" + loudness + "/" + mode + "/"
                + speechiness + "/" + acousticness + "/" + instrumentalness + "/"
                + liveness + "/" + valence + "/" + tempo + "/" + currentUsername;

        Log.d("RECOMMEND", "Fetching recommendations from: " + url);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    expectedRecommendations = response.length();
                    fetchedCount            = 0;
                    allRecommendedSongs.clear();
                    loadedSongs.clear();

                    List<String> batchIds = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String trackId = response.getJSONObject(i).getString("id");
                            batchIds.add(trackId);          // keep every ID (no filtering)
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    fetchSpotifyDetailsInBatch(batchIds);
                },
                error -> error.printStackTrace());

        Volley.newRequestQueue(this).add(request);
    }

    /** Pulls full track details for a list of Spotify IDs (max 50 per call). */
    private void fetchSpotifyDetailsInBatch(List<String> trackIds) {
        if (trackIds.isEmpty()) return;

        String idsParam = String.join(",", trackIds);
        String url      = "https://api.spotify.com/v1/tracks?ids=" + idsParam;
        Log.d("SPOTIFY_BATCH_URL", "Calling: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray tracks = response.getJSONArray("tracks");
                        List<Song> newBatch = new ArrayList<>();

                        for (int i = 0; i < tracks.length(); i++) {
                            JSONObject obj   = tracks.getJSONObject(i);
                            String name      = obj.optString("name", "Unknown Title");
                            String preview   = obj.optString("preview_url", null);
                            String uri       = obj.optString("uri", "");
                            String id        = obj.optString("id", "");
                            String artist    = "Unknown Artist";

                            JSONArray artists = obj.optJSONArray("artists");
                            if (artists != null && artists.length() > 0) {
                                artist = artists.getJSONObject(0).optString("name", "Unknown Artist");
                            }

                            String cover = obj.getJSONObject("album")
                                    .getJSONArray("images")
                                    .getJSONObject(0)
                                    .optString("url", "");

                            Song s = new Song(name, artist, cover, preview);
                            s.setId(id);
                            s.setUri(uri);
                            newBatch.add(s);
                        }

                        runOnUiThread(() -> {
                            adapter.updateData(newBatch);   // replace current cards
                            manager.setTopPosition(0);
                            cardStackView.requestLayout();
                        });

                    } catch (Exception e) {
                        Log.e("SPOTIFY_BATCH_ERR", "Parsing error", e);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Error loading song details", Toast.LENGTH_SHORT).show();
                            adapter.updateData(new ArrayList<>());
                            manager.setTopPosition(0);
                            cardStackView.requestLayout();
                        });
                    }
                },
                error -> {
                    Log.e("SPOTIFY_BATCH_ERR", "Request failed: " + error, error);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to load song details", Toast.LENGTH_SHORT).show();
                        adapter.updateData(new ArrayList<>());
                        manager.setTopPosition(0);
                        cardStackView.requestLayout();
                    });
                }) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + spotifyToken);
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    /** Adds the next five songs from allRecommendedSongs into the card stack. */
    private void loadMoreSongs() {
        int start = loadedSongs.size();
        int end   = Math.min(start + 5, allRecommendedSongs.size());

        if (start < end) {
            List<Song> newBatch = new ArrayList<>(allRecommendedSongs.subList(start, end));
            loadedSongs.addAll(newBatch);

            runOnUiThread(() -> adapter.addData(newBatch));
        }
    }

    /** Persists a single swipe (like / dislike) to the Groover backend. */
    private void saveSwipeToServer(Song song, boolean liked) {
        String url = "https://studev.groept.be/api/a24pt103/save_user_swipes";

        StringRequest request = new StringRequest(
                Request.Method.POST, url,
                response -> Log.d("SWIPE", "Swipe saved: " + song.getId()),
                error -> Log.e("SWIPE", "Swipe failed", error)) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("username", currentUsername);
                p.put("songid",  song.getId());
                p.put("liked",   liked ? "1" : "0");
                p.put("title",   song.getName());
                p.put("artist",  song.getArtist());
                return p;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    /** Creates a private playlist on Spotify and fills it with all liked URIs. */
    private void createPlaylistAndAddSongs() {
        if (likedURIs.isEmpty()) {
            Toast.makeText(this, "No liked songs to create playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Step 1: get current user ID
        String url = "https://api.spotify.com/v1/me";
        JsonObjectRequest userRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        String userId      = response.getString("id");
                        String playlistUrl = "https://api.spotify.com/v1/users/" + userId + "/playlists";

                        // Step 2: create playlist
                        JSONObject body = new JSONObject();
                        body.put("name", "Groover Playlist");
                        body.put("description", "Created by Groover App");
                        body.put("public", false);

                        JsonObjectRequest createRequest = new JsonObjectRequest(
                                Request.Method.POST, playlistUrl, body,
                                createResp -> {
                                    try {
                                        String playlistId = createResp.getString("id");
                                        addTracksToPlaylist(playlistId);   // Step 3
                                    } catch (Exception e) { e.printStackTrace(); }
                                },
                                error -> Toast.makeText(this, "Create failed", Toast.LENGTH_SHORT).show()) {
                            @Override public Map<String, String> getHeaders() {
                                Map<String, String> h = new HashMap<>();
                                h.put("Authorization", "Bearer " + spotifyToken);
                                h.put("Content-Type",  "application/json");
                                return h;
                            }
                        };
                        Volley.newRequestQueue(this).add(createRequest);

                    } catch (Exception e) { e.printStackTrace(); }
                },
                error -> Toast.makeText(this, "Get user ID failed", Toast.LENGTH_SHORT).show()) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Authorization", "Bearer " + spotifyToken);
                return h;
            }
        };
        Volley.newRequestQueue(this).add(userRequest);
    }

    /** Adds collected likedURIs to the given playlist ID. */
    private void addTracksToPlaylist(String playlistId) {
        String url  = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";
        JSONObject body = new JSONObject();
        try {
            JSONArray uris = new JSONArray();
            for (String uri : likedURIs) uris.put(uri);
            body.put("uris", uris);
        } catch (JSONException e) { e.printStackTrace(); }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> savePlaylistIdAndClearSwipes(playlistId),
                error -> Toast.makeText(this, "Failed to add tracks", Toast.LENGTH_SHORT).show()) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Authorization", "Bearer " + spotifyToken);
                h.put("Content-Type",  "application/json");
                return h;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    /** Stores the playlist ID in Groover DB, then clears local swipe history. */
    private void savePlaylistIdAndClearSwipes(String playlistId) {
        String url = "https://studev.groept.be/api/a24pt103/add_user_playlist/"
                + currentUsername + "/" + playlistId;

        StringRequest req = new StringRequest(
                Request.Method.GET, url,
                response -> {
                    clearSwipes(playlistId);
                    Toast.makeText(this, "Playlist created and songs added!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Log.e("PLAYLIST_DEBUG", "Error saving playlist: " + error.getMessage());
                    Toast.makeText(this, "Failed to save playlist", Toast.LENGTH_SHORT).show();
                });
        Volley.newRequestQueue(this).add(req);
    }

    /** Clears stored swipes once a playlist has been created. */
    private void clearSwipes(String playlistId) {
        String url = "https://studev.groept.be/api/a24pt103/clear_user_swipes/"
                + currentUsername + "/" + playlistId;

        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                response -> {
                    swipedIds.clear();
                    likedURIs.clear();
                },
                error -> {
                    Log.e("SWIPES", "Failed to clear swipe table", error);
                    Toast.makeText(this, "Failed to clear swipes", Toast.LENGTH_SHORT).show();
                });
        Volley.newRequestQueue(this).add(request);
    }

    /** Shows the bottom-sheet mood selector dialog and triggers song reload. */
    private void showMoodSelectionDialog() {
        Dialog dialog = new Dialog(this, R.style.MoodDialogTheme);
        dialog.setContentView(R.layout.mood_selection_layout);
        dialog.setCancelable(true);

        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.setBackgroundDrawableResource(R.drawable.rounded_dialog_background);
        }

        View.OnClickListener moodClickListener = v -> {
            if (v.getId() == R.id.btnParty)   currentMoodPreset = MoodSelector.getPartyPreset();
            else if (v.getId() == R.id.btnChill)   currentMoodPreset = MoodSelector.getChillPreset();
            else if (v.getId() == R.id.btnFocus)   currentMoodPreset = MoodSelector.getFocusPreset();
            else if (v.getId() == R.id.btnWorkout) currentMoodPreset = MoodSelector.getWorkoutPreset();
            else if (v.getId() == R.id.btnHappy)   currentMoodPreset = MoodSelector.getHappyPreset();
            else if (v.getId() == R.id.btnSad)     currentMoodPreset = MoodSelector.getSadPreset();

            Toast.makeText(this, "Loading new songs for selected mood...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadNewSong();
        };

        // Attach same listener to all mood buttons
        dialog.findViewById(R.id.btnParty).setOnClickListener(moodClickListener);
        dialog.findViewById(R.id.btnChill).setOnClickListener(moodClickListener);
        dialog.findViewById(R.id.btnFocus).setOnClickListener(moodClickListener);
        dialog.findViewById(R.id.btnWorkout).setOnClickListener(moodClickListener);
        dialog.findViewById(R.id.btnHappy).setOnClickListener(moodClickListener);
        dialog.findViewById(R.id.btnSad).setOnClickListener(moodClickListener);

        // “Top Songs” button uses Spotify top tracks
        dialog.findViewById(R.id.btnTopSongs).setOnClickListener(v -> {
            Toast.makeText(this, "Loading recommendations based on your top songs...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            fetchSwipedIdsAndRecommend();
        });

        dialog.show();
    }

    /** Requests recommendations for the currentMoodPreset and refreshes the card stack. */
    private void loadNewSong() {
        // Clear local lists + reset card stack
        allRecommendedSongs.clear();
        loadedSongs.clear();
        adapter.updateData(new ArrayList<>());
        manager.setTopPosition(0);
        cardStackView.requestLayout();

        // Build backend URL with mood values
        String url = String.format(Locale.US,
                "https://studev.groept.be/api/a24pt103/song_recommendations/%.3f/%d/%.1f/%d/%.3f/%.3f/%.3f/%.3f/%.3f/%.1f/%s",
                currentMoodPreset.energy, currentMoodPreset.key, currentMoodPreset.loudness,
                currentMoodPreset.mode, currentMoodPreset.speechiness, currentMoodPreset.acousticness,
                currentMoodPreset.instrumentalness, currentMoodPreset.liveness,
                currentMoodPreset.valence, currentMoodPreset.tempo, currentUsername);

        Log.d("MOOD_DEBUG", "Loading songs with URL: " + url);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject s = response.getJSONObject(i);
                                Song song = new Song(
                                        s.optString("name",    "Unknown"),
                                        s.optString("artists", "Unknown Artist"),
                                        "", null);
                                song.setId(s.getString("id"));
                                allRecommendedSongs.add(song);
                            }
                            // Load first batch
                            List<String> firstIds = new ArrayList<>();
                            for (int i = 0; i < Math.min(5, allRecommendedSongs.size()); i++) {
                                firstIds.add(allRecommendedSongs.get(i).getId());
                            }
                            fetchSpotifyDetailsInBatch(firstIds);
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "No songs found for this mood", Toast.LENGTH_LONG).show();
                            adapter.updateData(new ArrayList<>());
                            manager.setTopPosition(0);
                            cardStackView.requestLayout();
                        }
                    } catch (JSONException e) {
                        Log.e("MOOD_DEBUG", "Parsing error", e);
                        Toast.makeText(MainActivity.this, "Error processing songs", Toast.LENGTH_SHORT).show();
                        adapter.updateData(new ArrayList<>());
                        manager.setTopPosition(0);
                        cardStackView.requestLayout();
                    }
                },
                error -> {
                    Log.e("MOOD_DEBUG", "Network error", error);
                    Toast.makeText(MainActivity.this,
                            "Error loading songs. Please try again.", Toast.LENGTH_SHORT).show();
                    adapter.updateData(new ArrayList<>());
                    manager.setTopPosition(0);
                    cardStackView.requestLayout();
                });

        // Increase timeout for slower requests
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(request);
    }
}
