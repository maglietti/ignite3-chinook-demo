package com.example.util;

import com.example.model.*;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Utility class for loading and managing sample data in the Chinook database
 */
public class DataUtils {

    /**
     * Loads sample data into the Chinook database
     *
     * @param client The Ignite client
     * @return true if successful, false otherwise
     */
    public static boolean loadSampleData(IgniteClient client) {
        System.out.println("\n--- Loading Sample Data ---");

        try {
            // Using runInTransaction to manage transaction lifecycle
            return client.transactions().runInTransaction(tx -> {
                try {
                    // Load Artists
                    System.out.println("Loading Artists...");
                    Table artistTable = client.tables().table("Artist");
                    RecordView<Artist> artistView = artistTable.recordView(Artist.class);

                    // Create a list of artists
                    List<Artist> artists = new ArrayList<>();
                    artists.add(new Artist(1, "AC/DC"));
                    artists.add(new Artist(2, "Accept"));
                    artists.add(new Artist(3, "Aerosmith"));
                    artists.add(new Artist(4, "Alanis Morissette"));
                    artists.add(new Artist(5, "Alice In Chains"));

                    // Insert all artists in batch
                    artistView.upsertAll(tx, artists);
                    System.out.println("Added " + artists.size() + " artists");

                    // Load Albums
                    System.out.println("Loading Albums...");
                    Table albumTable = client.tables().table("Album");
                    RecordView<Album> albumView = albumTable.recordView(Album.class);

                    // Create a list of albums
                    List<Album> albums = new ArrayList<>();
                    albums.add(new Album(1, "For Those About To Rock We Salute You", 1));
                    albums.add(new Album(2, "Balls to the Wall", 2));
                    albums.add(new Album(3, "Restless and Wild", 2));
                    albums.add(new Album(4, "Let There Be Rock", 1));
                    albums.add(new Album(5, "Big Ones", 3));

                    // Insert all albums in batch
                    albumView.upsertAll(tx, albums);
                    System.out.println("Added " + albums.size() + " albums");

                    // Load Genres
                    System.out.println("Loading Genres...");
                    Table genreTable = client.tables().table("Genre");
                    RecordView<Genre> genreView = genreTable.recordView(Genre.class);

                    // Create a list of genres
                    List<Genre> genres = new ArrayList<>();
                    genres.add(new Genre(1, "Rock"));
                    genres.add(new Genre(2, "Jazz"));
                    genres.add(new Genre(3, "Metal"));
                    genres.add(new Genre(4, "Alternative & Punk"));
                    genres.add(new Genre(5, "Rock And Roll"));

                    // Insert all genres in batch
                    genreView.upsertAll(tx, genres);
                    System.out.println("Added " + genres.size() + " genres");

                    // Load Media Types
                    System.out.println("Loading Media Types...");
                    Table mediaTypeTable = client.tables().table("MediaType");
                    RecordView<MediaType> mediaTypeView = mediaTypeTable.recordView(MediaType.class);

                    // Create a list of media types
                    List<MediaType> mediaTypes = new ArrayList<>();
                    mediaTypes.add(new MediaType(1, "MPEG audio file"));
                    mediaTypes.add(new MediaType(2, "Protected AAC audio file"));
                    mediaTypes.add(new MediaType(3, "Protected MPEG-4 video file"));
                    mediaTypes.add(new MediaType(4, "Purchased AAC audio file"));
                    mediaTypes.add(new MediaType(5, "AAC audio file"));

                    // Insert all media types in batch
                    mediaTypeView.upsertAll(tx, mediaTypes);
                    System.out.println("Added " + mediaTypes.size() + " media types");

                    // Load Tracks
                    System.out.println("Loading Tracks...");
                    Table trackTable = client.tables().table("Track");
                    RecordView<Track> trackView = trackTable.recordView(Track.class);

                    // Create a list of tracks
                    List<Track> tracks = new ArrayList<>();
                    tracks.add(new Track(
                            1,
                            "For Those About To Rock (We Salute You)",
                            1,
                            1,
                            1,
                            "Angus Young, Malcolm Young, Brian Johnson",
                            343719,
                            11170334,
                            new BigDecimal("0.99")
                    ));

                    tracks.add(new Track(
                            2,
                            "Balls to the Wall",
                            2,
                            2,
                            1,
                            "U. Dirkschneider, W. Hoffmann, H. Frank, P. Baltes, S. Kaufmann, G. Hoffmann",
                            342562,
                            5510424,
                            new BigDecimal("0.99")
                    ));

                    tracks.add(new Track(
                            3,
                            "Fast As a Shark",
                            3,
                            2,
                            1,
                            "F. Baltes, S. Kaufman, U. Dirkscneider & W. Hoffman",
                            230619,
                            3990994,
                            new BigDecimal("0.99")
                    ));

                    tracks.add(new Track(
                            4,
                            "Restless and Wild",
                            3,
                            2,
                            1,
                            "F. Baltes, R.A. Smith-Diesel, S. Kaufman, U. Dirkscneider & W. Hoffman",
                            252051,
                            4331779,
                            new BigDecimal("0.99")
                    ));

                    tracks.add(new Track(
                            5,
                            "Princess of the Dawn",
                            3,
                            2,
                            1,
                            "Deaffy & R.A. Smith-Diesel",
                            375418,
                            6290521,
                            new BigDecimal("0.99")
                    ));

                    // Insert all tracks in batch
                    trackView.upsertAll(tx, tracks);
                    System.out.println("Added " + tracks.size() + " tracks");

                    System.out.println("Sample data loaded successfully");
                    return true;
                } catch (Exception e) {
                    System.err.println("Error loading sample data: " + e.getMessage());
                    e.printStackTrace();
                    // The exception will be propagated and trigger transaction rollback
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            System.err.println("Transaction failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Demonstrates using a transaction to create related entities
     *
     * @param client The Ignite client
     * @return true if successful, false otherwise
     */
    public static boolean createRelatedEntitiesWithTransaction(IgniteClient client) {
        System.out.println("\n--- Creating related entities with transaction ---");

        try {
            return client.transactions().runInTransaction(tx -> {
                // Get tables and views
                Table artistTable = client.tables().table("Artist");
                RecordView<Artist> artistView = artistTable.recordView(Artist.class);

                Table albumTable = client.tables().table("Album");
                RecordView<Album> albumView = albumTable.recordView(Album.class);

                Table trackTable = client.tables().table("Track");
                RecordView<Track> trackView = trackTable.recordView(Track.class);

                // Create a new artist
                Artist newArtist = new Artist(7, "Pink Floyd");
                artistView.upsert(tx, newArtist);

                // Create a new album for this artist
                Album newAlbum = new Album(8, "The Dark Side of the Moon", 7);
                albumView.upsert(tx, newAlbum);

                // Create tracks for this album
                List<Track> newTracks = ChinookUtils.createSampleTracks(8, 10);
                trackView.upsertAll(tx, newTracks);

                System.out.println("Created artist: " + newArtist.getName());
                System.out.println("Created album: " + newAlbum.getTitle());
                System.out.println("Created " + newTracks.size() + " tracks");

                return true;
            });
        } catch (Exception e) {
            System.err.println("Transaction failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Demonstrates adding entities in batch for better performance
     * (not using transactions for demonstration purposes)
     *
     * @param client The Ignite client
     * @return true if successful, false otherwise
     */
    public static boolean addBatchData(IgniteClient client) {
        try {
            System.out.println("\n--- Adding batch data ---");

            // Create a list of artists
            List<Artist> artists = new ArrayList<>();
            artists.add(new Artist(10, "Metallica"));
            artists.add(new Artist(11, "Led Zeppelin"));
            artists.add(new Artist(12, "Deep Purple"));

            // Get the Artist table
            Table artistTable = client.tables().table("Artist");
            RecordView<Artist> artistView = artistTable.recordView(Artist.class);

            // Insert all artists in batch
            artistView.upsertAll(null, artists);
            System.out.println("Added " + artists.size() + " artists in batch");

            // Create a list of albums
            List<Album> albums = new ArrayList<>();
            albums.add(new Album(10, "Master of Puppets", 10));
            albums.add(new Album(11, "Led Zeppelin IV", 11));
            albums.add(new Album(12, "Machine Head", 12));

            // Get the Album table
            Table albumTable = client.tables().table("Album");
            RecordView<Album> albumView = albumTable.recordView(Album.class);

            // Insert all albums in batch
            albumView.upsertAll(null, albums);
            System.out.println("Added " + albums.size() + " albums in batch");

            return true;
        } catch (Exception e) {
            System.err.println("Error adding batch data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates a full example of Queen and their albums/tracks
     * Demonstrates transactions and relationships
     *
     * @param client The Ignite client
     * @return true if successful, false otherwise
     */
    public static boolean createQueenExample(IgniteClient client) {
        System.out.println("\n--- Creating Queen example ---");

        try {
            return client.transactions().runInTransaction(tx -> {
                // Add Queen artist
                Artist queen = new Artist(6, "Queen");
                Table artistTable = client.tables().table("Artist");
                RecordView<Artist> artistView = artistTable.recordView(Artist.class);
                artistView.upsert(tx, queen);

                // Add two Queen albums
                List<Album> queenAlbums = new ArrayList<>();
                queenAlbums.add(new Album(6, "A Night at the Opera", 6));
                queenAlbums.add(new Album(7, "News of the World", 6));

                Table albumTable = client.tables().table("Album");
                RecordView<Album> albumView = albumTable.recordView(Album.class);
                albumView.upsertAll(tx, queenAlbums);

                // Add tracks for the first album
                List<Track> operaTracks = new ArrayList<>();
                operaTracks.add(new Track(
                        6,
                        "Bohemian Rhapsody",
                        6,
                        1,
                        1,
                        "Freddie Mercury",
                        354947,
                        5733664,
                        new BigDecimal("0.99")
                ));

                operaTracks.add(new Track(
                        7,
                        "You're My Best Friend",
                        6,
                        1,
                        1,
                        "John Deacon",
                        175733,
                        2875239,
                        new BigDecimal("0.99")
                ));

                // Add tracks for the second album
                List<Track> newsTracks = new ArrayList<>();
                newsTracks.add(new Track(
                        8,
                        "We Will Rock You",
                        7,
                        1,
                        1,
                        "Brian May",
                        120000,
                        1947610,
                        new BigDecimal("0.99")
                ));

                newsTracks.add(new Track(
                        9,
                        "We Are The Champions",
                        7,
                        1,
                        1,
                        "Freddie Mercury",
                        180000,
                        2871563,
                        new BigDecimal("0.99")
                ));

                // Combine all tracks
                List<Track> allTracks = new ArrayList<>();
                allTracks.addAll(operaTracks);
                allTracks.addAll(newsTracks);

                Table trackTable = client.tables().table("Track");
                RecordView<Track> trackView = trackTable.recordView(Track.class);
                trackView.upsertAll(tx, allTracks);

                System.out.println("Created artist: " + queen.getName());
                System.out.println("Created " + queenAlbums.size() + " albums");
                System.out.println("Created " + allTracks.size() + " tracks");

                return true;
            });
        } catch (Exception e) {
            System.err.println("Transaction failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Queries all data from the Chinook database
     * Demonstrates various query patterns
     *
     * @param client The Ignite client
     */
    public static void queryAllData(IgniteClient client) {
        try {
            System.out.println("\n--- Querying all data ---");

            // Query all artists
            System.out.println("\nArtists:");
            client.sql().execute(null, "SELECT * FROM Artist ORDER BY ArtistId")
                    .forEachRemaining(row ->
                            System.out.println("  " + row.intValue("ArtistId") +
                                    ": " + row.stringValue("Name")));

            // Query all albums with artist names
            System.out.println("\nAlbums with Artists:");
            client.sql().execute(null,
                            "SELECT a.AlbumId, a.Title, ar.Name as ArtistName " +
                                    "FROM Album a " +
                                    "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                                    "ORDER BY a.AlbumId")
                    .forEachRemaining(row ->
                            System.out.println("  " + row.intValue("AlbumId") +
                                    ": " + row.stringValue("Title") +
                                    " by " + row.stringValue("ArtistName")));

            // Query all tracks with album and artist information
            System.out.println("\nTracks with Album and Artist Info:");
            client.sql().execute(null,
                            "SELECT t.TrackId, t.Name as TrackName, t.Composer, " +
                                    "a.Title as AlbumTitle, ar.Name as ArtistName " +
                                    "FROM Track t " +
                                    "JOIN Album a ON t.AlbumId = a.AlbumId " +
                                    "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                                    "ORDER BY t.TrackId")
                    .forEachRemaining(row ->
                            System.out.println("  " + row.intValue("TrackId") +
                                    ": " + row.stringValue("TrackName") +
                                    " by " + row.stringValue("ArtistName") +
                                    " on " + row.stringValue("AlbumTitle") +
                                    (row.stringValue("Composer") != null ?
                                            " (Composer: " + row.stringValue("Composer") + ")" : "")));

            // Query all genres
            System.out.println("\nGenres:");
            client.sql().execute(null, "SELECT * FROM Genre ORDER BY GenreId")
                    .forEachRemaining(row ->
                            System.out.println("  " + row.intValue("GenreId") +
                                    ": " + row.stringValue("Name")));

            // Query all media types
            System.out.println("\nMedia Types:");
            client.sql().execute(null, "SELECT * FROM MediaType ORDER BY MediaTypeId")
                    .forEachRemaining(row ->
                            System.out.println("  " + row.intValue("MediaTypeId") +
                                    ": " + row.stringValue("Name")));

        } catch (Exception e) {
            System.err.println("Error querying data: " + e.getMessage());
        }
    }
}