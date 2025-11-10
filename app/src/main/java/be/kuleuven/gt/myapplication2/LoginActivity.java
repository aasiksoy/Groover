package be.kuleuven.gt.myapplication2;

/**
 * Launches the Spotify OAuth flow, stores the returned access-token,
 * and routes the user either to registration or the main screen
 * depending on their “isRegistered” flag in SharedPreferences.
 */
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    // Spotify OAuth parameters
    private static final String CLIENT_ID     = "1a5a73d460754f54b4d63431c494ee0f";
    private static final String REDIRECT_URI  = "grooverapp://callback";

    /** Entry point: sets up the UI and Spotify-login button. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Read registration state (value may be needed by other flows)
        SharedPreferences prefs = getSharedPreferences("grooverPrefs", MODE_PRIVATE);
        boolean isRegistered = prefs.getBoolean("isRegistered", false); // kept for future use

        // Spotify login button
        findViewById(R.id.btnSpotifyLogin).setOnClickListener(v -> launchSpotifyLogin());
    }

    /** Builds and fires the Spotify authorization Intent. */
    private void launchSpotifyLogin() {
        Uri uri = Uri.parse("https://accounts.spotify.com/authorize")
                .buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("response_type", "token")
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter(
                        "scope",
                        "user-read-private user-library-modify user-read-email "
                                + "user-top-read playlist-modify-public playlist-modify-private")
                .appendQueryParameter("show_dialog", "true")
                .build();

        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    /** Receives the redirect Intent when the activity is already running. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSpotifyRedirect(intent);
    }

    /** Called when the activity regains focus (handles pending redirect). */
    @Override
    protected void onResume() {
        super.onResume();
        handleSpotifyRedirect(getIntent());
    }

    /** Extracts the access token from the redirect URI fragment, saves it, then routes onward. */
    private void handleSpotifyRedirect(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            String fragment = uri.getFragment();
            if (fragment != null) {
                for (String param : fragment.split("&")) {
                    if (param.startsWith("access_token=")) {
                        String token = param.substring("access_token=".length());

                        // Persist token for later API calls
                        getSharedPreferences("grooverPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("SPOTIFY_TOKEN", token)
                                .apply();

                        Toast.makeText(this, "Logged in with Spotify!", Toast.LENGTH_SHORT).show();
                        Log.d("SPOTIFY_TOKEN", token);

                        checkUserRegistration();
                        return;
                    }
                }
            }
        }
    }

    /** Decides whether to send the user to MainActivity or RegisterActivity. */
    private void checkUserRegistration() {
        SharedPreferences prefs = getSharedPreferences("grooverPrefs", MODE_PRIVATE);
        boolean isRegistered = prefs.getBoolean("isRegistered", false);

        Intent intent = isRegistered
                ? new Intent(LoginActivity.this, MainActivity.class)
                : new Intent(LoginActivity.this, RegisterActivity.class);

        startActivity(intent);
        finish();
    }

    /** Convenience method: opens MainActivity and finishes this activity. */
    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
