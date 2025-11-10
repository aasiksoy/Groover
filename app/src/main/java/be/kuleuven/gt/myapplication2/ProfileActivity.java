package be.kuleuven.gt.myapplication2;

/**
 * User-profile screen.
 * • Shows username, bio, and profile picture
 * • Lets the user upload a photo, edit bio, view friends, and log out
 * • Bottom-navigation for quick app navigation
 */
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // --- UI elements ---
    private ImageView profileImage;
    private TextView  tvUsername;
    private EditText  editBio;
    private Button    btnUploadPhoto, btnFriends, btnSaveBio, btnLogout;

    // --- State ---
    private Uri   selectedImageUri;
    private String currentUsername;

    /** Image picker launcher (returns a Uri). */
    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedImageUri = uri;
                            uploadImageAsBase64(uri);
                        }
                    });

    /** Sets up UI, listeners, and loads existing profile data. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences prefs = getSharedPreferences("grooverPrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", "testuser");

        // View references
        profileImage   = findViewById(R.id.profileImage);
        tvUsername     = findViewById(R.id.tvUsername);
        editBio        = findViewById(R.id.editBio);
        btnSaveBio     = findViewById(R.id.btnSaveBio);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnFriends     = findViewById(R.id.btnFriends);
        btnLogout      = findViewById(R.id.btnLogout);
        tvUsername.setText(currentUsername);

        // --- Button listeners ---
        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent i = new Intent(ProfileActivity.this, GrooverLoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        btnUploadPhoto.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnFriends.setOnClickListener(v -> startActivity(new Intent(this, FriendsListActivity.class)));
        btnSaveBio.setOnClickListener(v -> saveBioToServer(editBio.getText().toString().trim()));

        // Initial data
        loadProfilePhoto();
        fetchBioFromServer();

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

    /** Downloads the user’s bio text from the backend. */
    private void fetchBioFromServer() {
        String url = "https://studev.groept.be/api/a24pt103/get_bio/" + currentUsername;

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            String bio = response.getJSONObject(0).getString("bio");
                            editBio.setText(bio);
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                },
                error -> {
                    Toast.makeText(this, "Couldn't load bio", Toast.LENGTH_SHORT).show();
                    Log.e("BIO-LOAD", "Volley error", error);
                });

        Volley.newRequestQueue(this).add(req);
    }

    /** Saves the edited bio back to the backend. */
    private void saveBioToServer(String bio) {
        String url = "https://studev.groept.be/api/a24pt103/set_bio";

        StringRequest req = new StringRequest(
                Request.Method.POST, url,
                response -> Toast.makeText(this, "Bio saved!", Toast.LENGTH_SHORT).show(),
                error -> {
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                    Log.e("BIO-SAVE", "Volley error", error);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("bio",      bio);
                p.put("username", currentUsername);
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    /** Compresses and encodes the selected image, then uploads it. */
    private void uploadImageAsBase64(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            String base64Image = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);

            sendImageToServer(base64Image);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    /** Sends the Base64-encoded profile image to the backend. */
    private void sendImageToServer(String base64Image) {
        String url = "https://studev.groept.be/api/a24pt103/upload_profile_image/";

        StringRequest req = new StringRequest(
                Request.Method.POST, url,
                resp -> {
                    Log.d("UPLOAD_RESPONSE", "Server: " + resp);
                    Toast.makeText(this, "Profile photo uploaded!", Toast.LENGTH_SHORT).show();
                    profileImage.setImageURI(selectedImageUri);
                },
                err -> {
                    Log.e("UPLOAD_FAIL", "Volley error", err);
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                }) {
            @Override protected Map<String,String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("username",  currentUsername);
                p.put("imagedata", base64Image);
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    /** Downloads and displays the stored profile photo (if any). */
    private void loadProfilePhoto() {
        String url = "https://studev.groept.be/api/a24pt103/get_profile_image/" + currentUsername;

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                resp -> {
                    try {
                        if (resp.length() > 0) {
                            String base64 = resp.getJSONObject(0).getString("image_data");
                            if (!base64.isEmpty()) {
                                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                                profileImage.setImageBitmap(
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                            }
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                },
                err -> Log.e("LOAD_IMAGE", "Failed", err));

        Volley.newRequestQueue(this).add(req);
    }
}
