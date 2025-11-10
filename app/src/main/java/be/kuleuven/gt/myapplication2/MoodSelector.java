package be.kuleuven.gt.myapplication2;

/**
 * Utility class that defines several “mood presets”.
 * Each preset specifies typical Spotify audio-feature targets that the
 * backend uses to generate track recommendations for that mood.
 */
public class MoodSelector {

    /**
     * Immutable container for a single mood’s target audio-feature values.
     * Fields correspond 1-to-1 with Spotify’s audio features.
     */
    public static class MoodPreset {
        public final double energy;
        public final int key;
        public final double loudness;
        public final int mode;
        public final double speechiness;
        public final double acousticness;
        public final double instrumentalness;
        public final double liveness;
        public final double valence;
        public final double tempo;

        /** Creates a fully specified mood preset. */
        public MoodPreset(double energy, int key, double loudness, int mode,
                          double speechiness, double acousticness, double instrumentalness,
                          double liveness, double valence, double tempo) {
            this.energy            = energy;
            this.key               = key;
            this.loudness          = loudness;
            this.mode              = mode;
            this.speechiness       = speechiness;
            this.acousticness      = acousticness;
            this.instrumentalness  = instrumentalness;
            this.liveness          = liveness;
            this.valence           = valence;
            this.tempo             = tempo;
        }
    }

    /** Upbeat, dance-oriented tracks. */
    public static MoodPreset getPartyPreset() {
        return new MoodPreset(
                0.8,  // High energy
                5,    // Key (irrelevant here)
                -5.0, // Moderate loudness
                1,    // Major mode
                0.1,  // Low speechiness
                0.2,  // Low acousticness
                0.0,  // Low instrumentalness
                0.3,  // Moderate liveness
                0.8,  // High valence
                125.0 // Dance-floor tempo
        );
    }

    /** Relaxed, laid-back tracks. */
    public static MoodPreset getChillPreset() {
        return new MoodPreset(
                0.3,  // Low energy
                5,    // Key
                -12.0,// Quieter
                1,    // Major mode
                0.1,  // Low speechiness
                0.7,  // High acousticness
                0.2,  // Low instrumentalness
                0.1,  // Low liveness
                0.4,  // Moderate valence
                90.0  // Slow tempo
        );
    }

    /** Instrument-friendly preset for concentration. */
    public static MoodPreset getFocusPreset() {
        return new MoodPreset(
                0.45, // Moderate energy
                5,    // Key
                -10.0,// Moderate-quiet
                1,    // Major mode
                0.05, // Very low speechiness
                0.4,  // Moderate acousticness
                0.6,  // High instrumentalness
                0.1,  // Low liveness
                0.5,  // Neutral valence
                110.0 // Mid-tempo
        );
    }

    /** High-energy tracks for workouts. */
    public static MoodPreset getWorkoutPreset() {
        return new MoodPreset(
                0.9,  // Very high energy
                5,    // Key
                -4.0, // Loud
                1,    // Major mode
                0.1,  // Low speechiness
                0.1,  // Low acousticness
                0.0,  // Low instrumentalness
                0.4,  // Moderate-high liveness
                0.8,  // High valence
                130.0 // Fast tempo
        );
    }

    /** Bright, upbeat tracks. */
    public static MoodPreset getHappyPreset() {
        return new MoodPreset(
                0.7,  // High energy
                5,    // Key
                -6.0, // Moderate loudness
                1,    // Major mode
                0.1,  // Low speechiness
                0.3,  // Moderately low acousticness
                0.0,  // Low instrumentalness
                0.2,  // Moderate liveness
                0.8,  // High valence
                115.0 // Upbeat tempo
        );
    }

    /** Slower, more melancholic tracks. */
    public static MoodPreset getSadPreset() {
        return new MoodPreset(
                0.3,  // Low energy
                5,    // Key
                -10.0,// Quiet
                0,    // Minor mode
                0.1,  // Low speechiness
                0.7,  // High acousticness
                0.1,  // Some instrumentalness
                0.1,  // Low liveness
                0.2,  // Low valence
                85.0  // Slow tempo
        );
    }
}
