package be.kuleuven.gt.myapplication2;

/**
 * Displays another user’s public profile:
 * • username, bio, liked songs, and profile picture
 * • bottom-navigation for quick app navigation
 */
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

public class OtherUserProfileActivity extends AppCompatActivity {

    private TextView tvUsername, tvBio, tvLikedSongs;
    private ImageView profileImage;

    private String username;

    /** Initializes UI elements and triggers data fetches. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_user_profile);

        // Username passed via Intent
        username = getIntent().getStringExtra("username");

        // View references
        tvUsername    = findViewById(R.id.tvUsername);
        tvBio         = findViewById(R.id.tvBio);
        tvLikedSongs  = findViewById(R.id.tvLikedSongs);
        profileImage  = findViewById(R.id.profileImage);
        tvUsername.setText(username);

        // Fetch and display profile data
        fetchBio();
        fetchLikedSongs();
        fetchProfileImage();

        // Bottom-navigation (kept as original if-chain)
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

    /** Retrieves the user’s bio from the backend. */
    private void fetchBio() {
        String url = "https://studev.groept.be/api/a24pt103/get_bio/" + username;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            String bio = response.getJSONObject(0).getString("bio");
                            tvBio.setText(bio.isEmpty() ? "No bio available." : bio);
                        } else {
                            tvBio.setText("No bio found.");
                        }
                    } catch (JSONException e) {
                        tvBio.setText("Error loading bio.");
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, "Failed to load bio", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                });

        Volley.newRequestQueue(this).add(request);
    }

    /** Retrieves and formats the list of liked songs. */
    private void fetchLikedSongs() {
        String url = "https://studev.groept.be/api/a24pt103/get_groover_liked_songs/" + username;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    StringBuilder sb = new StringBuilder();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject song = response.getJSONObject(i);
                            sb.append("• ")
                                    .append(song.getString("title"))
                                    .append(" – ")
                                    .append(song.getString("artist"))
                                    .append("\n");
                        }
                        tvLikedSongs.setText(
                                sb.length() > 0 ? sb.toString() : "No liked songs yet."
                        );
                    } catch (JSONException e) {
                        tvLikedSongs.setText("Error loading songs.");
                        e.printStackTrace();
                    }
                },
                error -> {
                    tvLikedSongs.setText("Failed to load liked songs.");
                    error.printStackTrace();
                });

        Volley.newRequestQueue(this).add(request);
    }

    /** Downloads the Base64-encoded profile image and displays it. */
    private void fetchProfileImage() {
        String url = "https://studev.groept.be/api/a24pt103/get_profile_image/" + username;
        Log.d("ProfileImage", "Fetching profile image");

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            JSONObject obj = response.getJSONObject(0);
                            if (obj.has("image_data") && !obj.isNull("image_data")) {
                                String base64 = obj.getString("image_data");
                                if (!base64.isEmpty()) {
                                    byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                                    profileImage.setImageBitmap(
                                            BitmapFactory.decodeByteArray(bytes, 0, bytes.length)
                                    );
                                }
                            }
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                },
                error -> {
                    Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                });

        Volley.newRequestQueue(this).add(request);
    }
}
