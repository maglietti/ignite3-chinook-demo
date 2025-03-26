package com.example.util;

import com.example.model.*;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for Chinook database operations
 * Contains methods for common database operations like querying and modifying data
 */
public class ChinookUtils {

    // Constants to define connection parameters
    public static final String[] NODE_ADDRESSES = {
            "localhost:10800", "localhost:10801", "localhost:10802"
    };

    /**
     * Creates an IgniteClient connection to the cluster with proper error handling
     *
     * @return A connected IgniteClient instance or null if connection fails
     */
    public static IgniteClient connectToCluster() {
        try {
            IgniteClient client = IgniteClient.builder()
                    .addresses(NODE_ADDRESSES)
                    .build();

            System.out.println("Connected to the cluster: " + client.connections());
            return client;
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            return null;
        }
    }

    /**
     * Closes the Ignite client safely
     *
     * @param client The IgniteClient to close
     */
    public static void closeClient(IgniteClient client) {
        if (client != null) {
            try {
                client.close();
                System.out.println("<<< Client closed successfully");
            } catch (Exception e) {
                System.err.println("Error closing client: " + e.getMessage());
            }
        }
    }

    /**
     * Prints all artists in the database
     *
     * @param client The Ignite client
     */
    public static void printAllArtists(IgniteClient client) {
        try {
            System.out.println("\n--- Artists ---");
            client.sql().execute(null, "SELECT * FROM Artist")
                    .forEachRemaining(row ->
                            System.out.println("Artist ID: " + row.intValue("ArtistId") +
                                    ", Name: " + row.stringValue("Name")));
        } catch (Exception e) {
            System.err.println("Error retrieving artists: " + e.getMessage());
        }
    }

    /**
     * Adds a new artist to the database
     *
     * @param client The Ignite client
     * @param artist The Artist object to add
     * @return true if successful, false otherwise
     */
    public static boolean addArtist(IgniteClient client, Artist artist) {
        try {
            // Get the Artist table
            Table artistTable = client.tables().table("Artist");
            // Create a record view for Artist class
            RecordView<Artist> artistView = artistTable.recordView(Artist.class);
            // Insert the artist
            artistView.upsert(null, artist);
            System.out.println("Added artist: " + artist.getName());
            return true;
        } catch (Exception e) {
            System.err.println("Error adding artist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds a new album to the database
     *
     * @param client The Ignite client
     * @param album The Album object to add
     * @return true if successful, false otherwise
     */
    public static boolean addAlbum(IgniteClient client, Album album) {
        try {
            // Get the Album table
            Table albumTable = client.tables().table("Album");
            // Create a record view for Album class
            RecordView<Album> albumView = albumTable.recordView(Album.class);
            // Insert the album
            albumView.upsert(null, album);
            System.out.println("Added album: " + album.getTitle());
            return true;
        } catch (Exception e) {
            System.err.println("Error adding album: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds a new track to the database
     *
     * @param client The Ignite client
     * @param track The Track object to add
     * @return true if successful, false otherwise
     */
    public static boolean addTrack(IgniteClient client, Track track) {
        try {
            // Get the Track table
            Table trackTable = client.tables().table("Track");
            // Create a record view for Track class
            RecordView<Track> trackView = trackTable.recordView(Track.class);
            // Insert the track
            trackView.upsert(null, track);
            System.out.println("Added track: " + track.getName());
            return true;
        } catch (Exception e) {
            System.err.println("Error adding track: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds multiple tracks to the database in batch
     *
     * @param client The Ignite client
     * @param tracks List of Track objects to add
     * @return true if successful, false otherwise
     */
    public static boolean addTracksInBatch(IgniteClient client, List<Track> tracks) {
        try {
            // Get the Track table
            Table trackTable = client.tables().table("Track");
            // Create a record view for Track class
            RecordView<Track> trackView = trackTable.recordView(Track.class);
            // Insert all tracks in batch
            trackView.upsertAll(null, tracks);
            System.out.println("Added " + tracks.size() + " tracks in batch");
            return true;
        } catch (Exception e) {
            System.err.println("Error adding tracks in batch: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds albums by a specific artist
     *
     * @param client The Ignite client
     * @param artistName The name of the artist
     */
    public static void findAlbumsByArtist(IgniteClient client, String artistName) {
        try {
            System.out.println("\n--- Finding albums by artist: " + artistName + " ---");
            client.sql().execute(null,
                            "SELECT a.Title, ar.Name as ArtistName " +
                                    "FROM Album a JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                                    "WHERE ar.Name = ?", artistName)
                    .forEachRemaining(row ->
                            System.out.println("Album: " + row.stringValue("Title") +
                                    " by " + row.stringValue("ArtistName")));
        } catch (Exception e) {
            System.err.println("Error finding albums by artist: " + e.getMessage());
        }
    }

    /**
     * Finds tracks with album and artist information
     *
     * @param client The Ignite client
     * @param artistName The name of the artist
     */
    public static void findTracksByArtist(IgniteClient client, String artistName) {
        try {
            System.out.println("\n--- Finding tracks by artist: " + artistName + " ---");
            client.sql().execute(null,
                            "SELECT t.Name as Track, t.Composer, a.Title as Album, ar.Name as Artist " +
                                    "FROM Track t " +
                                    "JOIN Album a ON t.AlbumId = a.AlbumId " +
                                    "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                                    "WHERE ar.Name = ?", artistName)
                    .forEachRemaining(row ->
                            System.out.println("Track: " + row.stringValue("Track") +
                                    ", Composer: " + row.stringValue("Composer") +
                                    ", Album: " + row.stringValue("Album") +
                                    ", Artist: " + row.stringValue("Artist")));
        } catch (Exception e) {
            System.err.println("Error finding tracks by artist: " + e.getMessage());
        }
    }

    /**
     * Creates sample tracks for an album
     *
     * @param albumId The album ID
     * @param startTrackId The starting track ID (will be incremented)
     * @return A list of Track objects
     */
    public static List<Track> createSampleTracks(int albumId, int startTrackId) {
        List<Track> tracks = new ArrayList<>();

        // Add sample tracks for Queen's "A Night at the Opera"
        if (albumId == 6) {
            tracks.add(new Track(
                    startTrackId++,
                    "Bohemian Rhapsody",
                    albumId,
                    1,
                    1,
                    "Freddie Mercury",
                    354947,
                    5733664,
                    new BigDecimal("0.99")
            ));

            tracks.add(new Track(
                    startTrackId++,
                    "You're My Best Friend",
                    albumId,
                    1,
                    1,
                    "John Deacon",
                    175733,
                    2875239,
                    new BigDecimal("0.99")
            ));

            tracks.add(new Track(
                    startTrackId++,
                    "Love of My Life",
                    albumId,
                    1,
                    1,
                    "Freddie Mercury",
                    217571,
                    3375011,
                    new BigDecimal("0.99")
            ));
        }
        // Add sample tracks for Pink Floyd's "The Dark Side of the Moon"
        else if (albumId == 8) {
            tracks.add(new Track(
                    startTrackId++,
                    "Time",
                    albumId,
                    1,
                    1,
                    "David Gilmour, Nick Mason, Roger Waters, Richard Wright",
                    425032,
                    6905119,
                    new BigDecimal("0.99")
            ));

            tracks.add(new Track(
                    startTrackId++,
                    "Money",
                    albumId,
                    1,
                    1,
                    "Roger Waters",
                    382830,
                    6217216,
                    new BigDecimal("0.99")
            ));
        }

        return tracks;
    }
}