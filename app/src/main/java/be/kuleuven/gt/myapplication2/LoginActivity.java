package be.kuleuven.gt.myapplication2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private SpotifyAuth spotifyAuth;

    public static Intent nextScreenIntent(Context context) {
        boolean isRegistered = context.getSharedPreferences("grooverPrefs", MODE_PRIVATE)
                .getBoolean("isRegistered", false);
        Class<?> target = isRegistered ? MainActivity.class : RegisterActivity.class;
        return new Intent(context, target);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        spotifyAuth = new SpotifyAuth(this);
        findViewById(R.id.btnSpotifyLogin).setOnClickListener(v -> launchSpotifyLogin());

        Uri data = getIntent().getData();
        if (savedInstanceState == null && spotifyAuth.isRedirect(data)) {
            completeLogin(data);
        } else if (spotifyAuth.hasValidAccessToken()) {
            openNextScreen();
        } else if (spotifyAuth.canRefresh()) {
            spotifyAuth.refresh(this::openNextScreen, message -> { });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri data = intent.getData();
        if (spotifyAuth.isRedirect(data)) {
            completeLogin(data);
        }
    }

    private void launchSpotifyLogin() {
        startActivity(new Intent(Intent.ACTION_VIEW, spotifyAuth.buildAuthorizationUri()));
    }

    private void completeLogin(Uri redirectUri) {
        spotifyAuth.completeLogin(
                redirectUri,
                () -> {
                    Toast.makeText(this, "Logged in with Spotify!", Toast.LENGTH_SHORT).show();
                    openNextScreen();
                },
                message -> Toast.makeText(this, "Spotify login failed: " + message, Toast.LENGTH_LONG).show());
    }

    private void openNextScreen() {
        startActivity(nextScreenIntent(this));
        finish();
    }
}
