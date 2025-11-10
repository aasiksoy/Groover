package be.kuleuven.gt.myapplication2;

/**
 * Simple data-class representing a Spotify track.
 * Fields include Groover’s DB id, Spotify URI, title, artist,
 * album-cover URL, and preview URL.
 */
public class Song {

    // --- Mutable identifiers (set after construction for convenience) ---
    private String id;   // Groover / Spotify ID
    private String uri;  // Spotify URI

    // --- Immutable metadata ---
    private final String name;
    private final String artist;
    private final String albumCoverUrl;
    private final String previewUrl;

    /** Basic constructor when only metadata is known. */
    public Song(String name, String artist, String albumCoverUrl, String previewUrl) {
        this.name          = name;
        this.artist        = artist;
        this.albumCoverUrl = albumCoverUrl;
        this.previewUrl    = previewUrl;
    }

    /** Full constructor when ID and URI are already available. */
    public Song(String id, String uri,
                String name, String artist,
                String albumCoverUrl, String previewUrl) {
        this.id            = id;
        this.uri           = uri;
        this.name          = name;
        this.artist        = artist;
        this.albumCoverUrl = albumCoverUrl;
        this.previewUrl    = previewUrl;
    }

    // --- Getters ---
    public String getName()          { return name; }
    public String getArtist()        { return artist; }
    public String getAlbumCoverUrl() { return albumCoverUrl; }
    public String getPreviewUrl()    { return previewUrl; }
    public String getId()            { return id; }
    public String getUri()           { return uri; }

    // --- Setters (for mutable identifiers) ---
    public void setId(String id)   { this.id = id; }
    public void setUri(String uri) { this.uri = uri; }
}
