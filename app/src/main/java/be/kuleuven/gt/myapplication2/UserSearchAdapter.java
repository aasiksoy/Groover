package be.kuleuven.gt.myapplication2;

/**
 * RecyclerView adapter used on the user-search screen.
 * • Shows each username with an “Add Friend” button
 * • Opens profile on name tap; sends friend-request on button tap
 * • Behaviour can vary slightly when launched from ProfileActivity
 */
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private final List<String> users;
    private final String currentUser;
    private final boolean fromProfile;
    private final Activity activity;

    /** Adapter receives the user list, current user, and calling Activity. */
    public UserSearchAdapter(List<String> users,
                             String currentUser,
                             boolean fromProfile,
                             Activity activity) {
        this.users       = users;
        this.currentUser = currentUser;
        this.fromProfile = fromProfile;
        this.activity    = activity;
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(v);
    }

    /** Binds username text and click-listeners for each row. */
    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        String username = users.get(position);
        holder.usernameText.setText(username);

        // Open selected user’s profile
        holder.usernameText.setOnClickListener(v ->
                activity.startActivity(
                        new android.content.Intent(activity, OtherUserProfileActivity.class)
                                .putExtra("username", username)));

        // Send friend request
        holder.btnAddFriend.setOnClickListener(v ->
                FriendsListActivity.sendFriendRequest(currentUser, username, activity));
    }

    @Override
    public int getItemCount() { return users.size(); }

    /** Holds the views for a single list item. */
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        Button   btnAddFriend;
        UserViewHolder(View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.usernameText);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
        }
    }
}
