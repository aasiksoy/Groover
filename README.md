# Groover: Swipe-Based Spotify Music Discovery App

Groover is an Android app for discovering music by swiping.
Swipe right to like a track and left to skip. Groover turns your liked tracks into a Spotify playlist.

I built it for a university course and got **17/20**. It connects an Android client, the Spotify Web API and a SQL backend.

---

## Features

- **Spotify login** using OAuth 2.0 Authorization Code with PKCE, plus automatic token refresh
- **Swipe interface** built on CardStackView
- **Mood presets** that steer the recommendations
- **Liked songs, friends, profiles and bios**, synced to a SQL backend through REST calls (Volley)
- Previously swiped tracks are never recommended again
- **Automatic playlist creation** on Spotify
- Album art loaded with Picasso, and Lottie animations

---

## How login works

Groover uses two separate logins:

1. **Spotify (`LoginActivity`, `SpotifyAuth`)**
   - The app generates a random `code_verifier`, a `code_challenge` (its SHA-256 hash, encoded as base64url) and a random `state` value.
   - The browser opens `accounts.spotify.com/authorize` with `response_type=code`, the challenge and `state`.
   - Spotify redirects to `grooverapp://callback?code=…&state=…`. The app checks that `state` matches, then exchanges the code plus the original `code_verifier` at `accounts.spotify.com/api/token`.
   - The app has no client secret. PKCE makes a stolen authorization code useless without the verifier, which never leaves the device.
   - The access token, refresh token and expiry time are stored in the app's private SharedPreferences. They are excluded from backups and never logged.
   - When the access token expires (after 1 hour), the app uses the refresh token to get a new one without showing the login screen.
2. **Groover account (`RegisterActivity`, `GrooverLoginActivity`)**
   - You register with a username, email and password. Your likes, friends and profile are stored against this account in the backend.

---

## Tech stack

- **Android:** Java, CardStackView, Picasso, Volley, Lottie, SharedPreferences
- **Backend:** MySQL behind the GroepT (KU Leuven) SQL web-service API, returning JSON. `backend/` holds an earlier PHP and SQL version of the swipe endpoints for reference.
- **APIs:** Spotify Web API

---

## Running it yourself

1. Clone the repo and open it in **Android Studio** (a recent stable version, with JDK 17 or newer).
2. Create an app in the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
   - Add `grooverapp://callback` as a Redirect URI and select **Android**.
   - Replace `CLIENT_ID` in `SpotifyAuth.java` with your own.
3. While the Spotify app is in Development Mode, only accounts listed under **User Management** can log in. Add yourself and any testers there.
4. Run the `app` configuration on an emulator or a USB-connected phone (API 24+).

---

## Known limitations

These are design limits of the original course backend. I left them as they are on purpose, and this is how I would fix them:

- **Password hashing.** The app hashes passwords with unsalted SHA-256 before sending them, and the server compares hashes. In practice the hash becomes the password. The fix is to send the password over HTTPS and hash it on the server with `password_hash()`/`password_verify()` (bcrypt or Argon2). Existing accounts can be upgraded in place by storing `password_hash(old_sha256)` and verifying `sha256(input)` against it.
- **No API authentication.** The backend endpoints identify the user by a username in the URL and don't issue session tokens. A production version would return a signed session token at login and check it on every request.

---

## Open-source libraries

- [CardStackView](https://github.com/yuyakaido/CardStackView) by yuyakaido, Apache License 2.0
- [Picasso](https://github.com/square/picasso) by Square, Apache License 2.0
- [Lottie](https://github.com/airbnb/lottie-android) by Airbnb, Apache License 2.0
- [Volley](https://github.com/google/volley) by Google, Apache License 2.0

---

## Contributing

This is a student project, but suggestions and pull requests are welcome.
