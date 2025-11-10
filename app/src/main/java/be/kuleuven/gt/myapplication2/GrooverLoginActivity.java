package be.kuleuven.gt.myapplication2;

/**
 * Login screen for Groover.
 * Validates email/password, sends hashed password to the backend,
 * and stores basic user info in SharedPreferences on success.
 */
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class GrooverLoginActivity extends AppCompatActivity {

    // UI elements
    EditText inputEmail, inputPassword;
    Button btnLogin;

    /** Initializes the UI and sets click-listeners. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groover_login);

        // View references
        btnLogin       = findViewById(R.id.btnLogin);
        inputEmail     = findViewById(R.id.inputEmailLogin);
        inputPassword  = findViewById(R.id.inputPasswordLogin);

        // “Register” text link
        TextView tvDontHaveAccount = findViewById(R.id.tvDontHaveAccount);
        tvDontHaveAccount.setOnClickListener(v -> {
            Intent intent = new Intent(GrooverLoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Login button behaviour
        btnLogin.setOnClickListener(v -> {
            String email    = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });
    }

    /**
     * Sends a POST request to log the user in.
     * On success: saves user info and navigates to the main screen.
     */
    private void loginUser(String email, String password) {
        String URL = "https://studev.groept.be/api/a24pt103/login_user";
        String hashedPassword = sha256(password);  // hash before sending

        StringRequest request = new StringRequest(
                Request.Method.POST, URL,
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        if (array.length() > 0) {
                            JSONObject user   = array.getJSONObject(0);
                            String username   = user.getString("username");

                            // Persist basic user data
                            SharedPreferences prefs = getSharedPreferences("grooverPrefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putBoolean("isRegistered", true)
                                    .putString("username", username)
                                    .putString("email", email)
                                    .apply();

                            Toast.makeText(this, "Welcome back, " + username + "!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Login failed: Incorrect email or password", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Login failed: Response parsing error", Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this, "Login error: " + error.getMessage(), Toast.LENGTH_LONG).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> postData = new HashMap<>();
                postData.put("email", email);
                postData.put("password", hashedPassword);  // send hashed password
                return postData;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }


    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
