package be.kuleuven.gt.myapplication2;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class SpotifyAuth {

    private static final String CLIENT_ID = "1a5a73d460754f54b4d63431c494ee0f";
    private static final String REDIRECT_URI = "grooverapp://callback";
    private static final String AUTHORIZE_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SCOPES = "user-read-private user-library-modify user-read-email "
            + "user-top-read playlist-modify-public playlist-modify-private";

    private static final String PREFS_NAME = "grooverPrefs";
    private static final String KEY_ACCESS_TOKEN = "SPOTIFY_TOKEN";
    private static final String KEY_REFRESH_TOKEN = "SPOTIFY_REFRESH_TOKEN";
    private static final String KEY_EXPIRES_AT = "SPOTIFY_EXPIRES_AT";
    private static final String KEY_CODE_VERIFIER = "PKCE_CODE_VERIFIER";
    private static final String KEY_STATE = "PKCE_STATE";

    private static final int VERIFIER_BYTES = 64;
    private static final int STATE_BYTES = 16;
    private static final long EXPIRY_MARGIN_MS = 60_000L;
    private static final int URL_SAFE_FLAGS = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Context context;
    private final SharedPreferences prefs;

    public SpotifyAuth(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public Uri buildAuthorizationUri() {
        String codeVerifier = randomUrlSafeString(VERIFIER_BYTES);
        String state = randomUrlSafeString(STATE_BYTES);

        prefs.edit()
                .putString(KEY_CODE_VERIFIER, codeVerifier)
                .putString(KEY_STATE, state)
                .apply();

        return Uri.parse(AUTHORIZE_URL)
                .buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("code_challenge", codeChallengeFor(codeVerifier))
                .appendQueryParameter("state", state)
                .appendQueryParameter("scope", SCOPES)
                .appendQueryParameter("show_dialog", "true")
                .build();
    }

    public boolean isRedirect(Uri uri) {
        return uri != null && uri.toString().startsWith(REDIRECT_URI);
    }

    public void completeLogin(Uri redirectUri, Runnable onSuccess, Consumer<String> onError) {
        String expectedState = prefs.getString(KEY_STATE, null);
        String codeVerifier = prefs.getString(KEY_CODE_VERIFIER, null);
        prefs.edit().remove(KEY_STATE).remove(KEY_CODE_VERIFIER).apply();

        String error = redirectUri.getQueryParameter("error");
        if (error != null) {
            onError.accept(error);
            return;
        }

        String code = redirectUri.getQueryParameter("code");
        String state = redirectUri.getQueryParameter("state");
        if (code == null || codeVerifier == null || expectedState == null || !expectedState.equals(state)) {
            onError.accept("invalid login response");
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", REDIRECT_URI);
        params.put("client_id", CLIENT_ID);
        params.put("code_verifier", codeVerifier);
        requestTokens(params, onSuccess, onError);
    }

    public boolean hasValidAccessToken() {
        String token = prefs.getString(KEY_ACCESS_TOKEN, null);
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L);
        return token != null && System.currentTimeMillis() < expiresAt - EXPIRY_MARGIN_MS;
    }

    public boolean canRefresh() {
        return prefs.getString(KEY_REFRESH_TOKEN, null) != null;
    }

    public void refresh(Runnable onSuccess, Consumer<String> onError) {
        String refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null);
        if (refreshToken == null) {
            onError.accept("no refresh token");
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);
        params.put("client_id", CLIENT_ID);
        requestTokens(params, onSuccess, onError);
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    private void requestTokens(Map<String, String> params, Runnable onSuccess, Consumer<String> onError) {
        StringRequest request = new StringRequest(
                Request.Method.POST, TOKEN_URL,
                response -> {
                    try {
                        saveTokens(new JSONObject(response));
                        onSuccess.run();
                    } catch (JSONException e) {
                        onError.accept("unexpected token response");
                    }
                },
                error -> onError.accept("token request failed")) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

    private void saveTokens(JSONObject json) throws JSONException {
        long expiresAt = System.currentTimeMillis() + json.getLong("expires_in") * 1000L;
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_ACCESS_TOKEN, json.getString("access_token"))
                .putLong(KEY_EXPIRES_AT, expiresAt);

        String refreshToken = json.optString("refresh_token", "");
        if (!refreshToken.isEmpty()) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        editor.apply();
    }

    private static String randomUrlSafeString(int byteCount) {
        byte[] bytes = new byte[byteCount];
        RANDOM.nextBytes(bytes);
        return Base64.encodeToString(bytes, URL_SAFE_FLAGS);
    }

    private static String codeChallengeFor(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.encodeToString(hash, URL_SAFE_FLAGS);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
