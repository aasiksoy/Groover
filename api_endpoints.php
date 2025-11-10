<?php
require_once("connection.php");

// Save a user's swipe
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['username']) && isset($_POST['songid']) && isset($_POST['liked'])) {
    $username = $_POST['username'];
    $songid = $_POST['songid'];
    $liked = $_POST['liked'];
    
    $query = "INSERT INTO user_song_swipes (username, songid, liked) VALUES (?, ?, ?)";
    
    try {
        $stmt = $conn->prepare($query);
        $stmt->execute([$username, $songid, $liked]);
        echo "Swipe saved successfully";
    } catch (PDOException $e) {
        http_response_code(500);
        echo "Error saving swipe: " . $e->getMessage();
    }
}

// Get all swiped song IDs for a user (for recommendations)
if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['action']) && $_GET['action'] === 'get_swiped_song_ids' && isset($_GET['username'])) {
    $username = $_GET['username'];
    
    $query = "SELECT DISTINCT songid FROM user_song_swipes WHERE username = ? AND songid NOT IN (
        SELECT songid FROM user_song_swipes us2 
        WHERE us2.username = ? 
        AND EXISTS (
            SELECT 1 FROM user_playlists up 
            WHERE up.username = ? 
            AND up.created_at > us2.swipe_time
        )
    )";
    
    try {
        $stmt = $conn->prepare($query);
        $stmt->execute([$username, $username, $username]);
        $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        echo json_encode($result);
    } catch (PDOException $e) {
        http_response_code(500);
        echo "Error fetching swiped songs: " . $e->getMessage();
    }
}

// Clear swipes for a user (called after playlist creation)
if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['action']) && $_GET['action'] === 'clear_user_swipes' && isset($_GET['username'])) {
    $username = $_GET['username'];
    
    // Instead of deleting, we'll mark these swipes as included in a playlist
    // by recording the playlist creation time
    $query = "INSERT INTO user_playlists (username, created_at) VALUES (?, CURRENT_TIMESTAMP)";
    
    try {
        $stmt = $conn->prepare($query);
        $stmt->execute([$username]);
        echo "Swipes marked as used in playlist";
    } catch (PDOException $e) {
        http_response_code(500);
        echo "Error clearing swipes: " . $e->getMessage();
    }
}
?> 