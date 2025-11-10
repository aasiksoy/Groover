package be.kuleuven.gt.myapplication2;

/**
 * Lets the user search for other Groover users.
 * • Displays a live-filtered list of usernames
 * • If launched from ProfileActivity (“fromProfile” flag), the adapter
 *   enables friend-request actions.
 * • Includes the global bottom-navigation bar.
 */
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    // --- UI ---
    private RecyclerView recyclerView;
    private EditText     searchInput;

    // --- Data + adapter ---
    private UserSearchAdapter adapter;
    private final List<String> allUsers      = new ArrayList<>();
    private final List<String> filteredUsers = new ArrayList<>();

    private String  currentUsername;
    private boolean fromProfile;

    /** Initializes UI components, bottom-nav, and loads user list. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

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

        // View references
        recyclerView = findViewById(R.id.userRecyclerView);
        searchInput  = findViewById(R.id.searchInput);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Current user and intent flag
        currentUsername = getSharedPreferences("grooverPrefs", MODE_PRIVATE)
                .getString("username", "testuser");
        fromProfile     = getIntent().getBooleanExtra("fromProfile", false);

        // Adapter
        adapter = new UserSearchAdapter(filteredUsers, currentUsername, fromProfile, this);
        recyclerView.setAdapter(adapter);

        // Initial load
        fetchAllUsers();

        // Live filtering
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /** Downloads the full user list from the backend (excluding self). */
    private void fetchAllUsers() {
        String url = "https://studev.groept.be/api/a24pt103/get_all_users";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    allUsers.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String user = response.getJSONObject(i).getString("username");
                            if (!user.equals(currentUsername)) allUsers.add(user);
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    filterUsers(searchInput.getText().toString());
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    /** Updates “filteredUsers” based on the search keyword and refreshes the adapter. */
    private void filterUsers(String keyword) {
        filteredUsers.clear();
        for (String user : allUsers) {
            if (user.toLowerCase().contains(keyword.toLowerCase())) {
                filteredUsers.add(user);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
