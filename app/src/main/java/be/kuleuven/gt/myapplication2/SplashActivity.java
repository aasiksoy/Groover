package be.kuleuven.gt.myapplication2;

/**
 * Displays the splash screen, then routes to LoginActivity after a short delay.
 * (Any registration / token logic can be expanded later if needed.)
 */
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // Splash duration (ms)
    private static final int SPLASH_DELAY = 4000;   // original value hard-coded in Groover

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay, then decide which screen to open
        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("grooverPrefs", MODE_PRIVATE);
            boolean isRegistered = prefs.getBoolean("isRegistered", false);
            String  spotifyToken = prefs.getString("SPOTIFY_TOKEN", null);

            // Currently always navigates to LoginActivity
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}
