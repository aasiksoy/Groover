-- Drop existing table if it exists
DROP TABLE IF EXISTS user_song_swipes;

-- Create the user_song_swipes table
CREATE TABLE user_song_swipes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    songid VARCHAR(255) NOT NULL,
    liked BOOLEAN NOT NULL,
    swipe_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_songid (songid)
);

-- Drop existing table if it exists
DROP TABLE IF EXISTS user_playlists;

-- Create the user_playlists table to track when playlists are created
CREATE TABLE user_playlists (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    playlist_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username_created (username, created_at)
);

-- Query to clear swipes (mark them as used in a playlist)
-- This should be called when a playlist is created
INSERT INTO user_playlists (username, playlist_id) 
VALUES (:username, :playlist_id); 