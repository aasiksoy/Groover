package be.kuleuven.gt.myapplication2;

/**
 * Displays the current user’s friends, allows adding/removing friends,
 * and provides navigation to other main screens.
 */
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FriendAdapter adapter;
    private final List<String> friendList = new ArrayList<>();
    private String currentUsername;

    /** Sets up UI, listeners, and initial data fetch. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        // RecyclerView setup
        recyclerView = findViewById(R.id.friendsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter(friendList);
        recyclerView.setAdapter(adapter);

        // Retrieve current username
        currentUsername = getSharedPreferences("grooverPrefs", MODE_PRIVATE)
                .getString("username", "testuser");

        // “Add Friend” button action
        Button btnAddFriend = findViewById(R.id.btnAddFriend);
        btnAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra("fromProfile", true);
            startActivity(intent);
        });

        // Get friends from backend
        fetchFriends();

        // Bottom-navigation item selection (unchanged logic: if-chain)
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

    /** Downloads the friend list for the current user. */
    private void fetchFriends() {
        String url = "https://studev.groept.be/api/a24pt103/get_friends/" + currentUsername;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    friendList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String friend = response.getJSONObject(i).getString("user2");
                            friendList.add(friend);
                            Log.d("DEBUG", "Fetched friend: " + friend);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    adapter.notifyDataSetChanged();
                },
                error -> {
                    Toast.makeText(this, "Failed to load friends", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                });

        Volley.newRequestQueue(this).add(request);
    }

    /**
     * Removes a friendship on the backend, then refreshes the list.
     *
     * @param user1 initiator
     * @param user2 friend to remove
     */
    private void removeFriend(String user1, String user2) {
        String url = "https://studev.groept.be/api/a24pt103/remove_friends";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Friend removed", Toast.LENGTH_SHORT).show();
                    fetchFriends();
                },
                error -> {
                    Toast.makeText(this, "Failed to remove friend", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("userone", user1);
                params.put("usertwo", user2);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    /**
     * Sends a friend-request API call.
     */
    public static void sendFriendRequest(String currentUser, String targetUser, Context context) {
        String url = "https://studev.groept.be/api/a24pt103/add_friend/";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> Toast.makeText(context, "Friend added successfully!", Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(context, "Failed to add friend", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("userone", currentUser);
                params.put("usertwo", targetUser);
                return params;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

    /** Adapter for the friend list RecyclerView. */
    private class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

        private final List<String> friends;

        FriendAdapter(List<String> friends) {
            this.friends = friends;
        }

        /** Inflates a row view. */
        @NonNull
        @Override
        public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend_with_button, parent, false);
            return new FriendViewHolder(view);
        }

        /** Binds data to a row. */
        @Override
        public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
            String friend = friends.get(position);
            holder.friendName.setText(friend);

            holder.btnRemove.setOnClickListener(v -> removeFriend(currentUsername, friend));

            holder.friendName.setOnClickListener(v -> {
                Intent intent = new Intent(FriendsListActivity.this, OtherUserProfileActivity.class);
                intent.putExtra("username", friend);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        /** Holds row views. */
        class FriendViewHolder extends RecyclerView.ViewHolder {
            TextView friendName;
            Button btnRemove;

            FriendViewHolder(View itemView) {
                super(itemView);
                friendName = itemView.findViewById(R.id.friendName);
                btnRemove = itemView.findViewById(R.id.btnRemoveFriend);
            }
        }
    }
}
