package be.kuleuven.gt.myapplication2;

/**
 * Registration screen.
 * • Validates input, checks for existing username/email via backend,
 *   then creates a new Groover account with a SHA-256-hashed password.
 * • Offers a link back to the login screen.
 */
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // --- UI ---
    EditText inputUsername, inputEmail, inputPassword;
    Button   btnRegister;

    /** Sets up UI elements, listeners, and window insets. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ConstraintLayout root = findViewById(R.id.registerLayout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // View references
        inputUsername = findViewById(R.id.inputUsername);
        inputEmail    = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnRegister   = findViewById(R.id.btnRegister);

        // “Already have account?” link
        TextView tvAlready = findViewById(R.id.tvAlreadyHaveAccount);
        tvAlready.setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, GrooverLoginActivity.class)));

        // Register button behaviour
        btnRegister.setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            String email    = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            } else {
                checkUserAndRegister(username, email, password);
            }
        });
    }

    /** Checks whether username or email already exists before registration. */
    private void checkUserAndRegister(String username, String email, String password) {
        String checkUrl = "https://studev.groept.be/api/a24pt103/check_user_exists";

        StringRequest checkReq = new StringRequest(
                Request.Method.POST, checkUrl,
                response -> {
                    if (response.contains("1")) {
                        Toast.makeText(this, "Username or email already exists!", Toast.LENGTH_LONG).show();
                    } else {
                        registerUser(username, email, password);
                    }
                },
                error -> Toast.makeText(this, "Check failed: " + error.getMessage(), Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("a", username);
                p.put("b", email);
                return p;
            }
        };

        Volley.newRequestQueue(this).add(checkReq);
    }

    /** SHA-256 helper; returns a hex string. */
    private String sha256(String input) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] hash = d.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                String hx = Integer.toHexString(0xff & b);
                if (hx.length() == 1) sb.append('0');
                sb.append(hx);
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /** Sends the registration POST request with hashed password. */
    private void registerUser(String username, String email, String password) {
        String url           = "https://studev.groept.be/api/a24pt103/register_user";
        String hashedPass    = sha256(password);

        StringRequest req = new StringRequest(
                Request.Method.POST, url,
                response -> Toast.makeText(this, "Registered successfully!", Toast.LENGTH_LONG).show(),
                error -> Toast.makeText(this, "Registration failed: " + error.getMessage(), Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> data = new HashMap<>();
                data.put("a", username);
                data.put("b", email);
                data.put("c", hashedPass);
                return data;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }
}
