package be.kuleuven.gt.myapplication2;

/**
 * Simple RecyclerView adapter that lists usernames.
 * Clicking a username opens OtherUserProfileActivity for that user.
 */
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<String> userList;
    private final Activity activity;

    /** Constructs the adapter with the initial list and calling Activity. */
    public UserAdapter(List<String> userList, Activity activity) {
        this.userList = userList;
        this.activity = activity;
    }

    /** Replaces the current list and refreshes the view. */
    public void updateList(List<String> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        String username = userList.get(position);
        holder.usernameTextView.setText(username);

        holder.usernameTextView.setOnClickListener(v -> {
            Intent intent = new Intent(activity, OtherUserProfileActivity.class);
            intent.putExtra("username", username);
            activity.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return userList.size(); }

    /** Holds the TextView for each row. */
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        UserViewHolder(View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(android.R.id.text1);
        }
    }
}
