package com.example.util;

import com.example.model.*;
import org.apache.ignite.client.IgniteClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for generating reports and analysis from the Chinook database
 */
public class ReportingUtils {

    /**
     * List all artists using SQL
     */
    public static void listArtists(IgniteClient client) {
        try {
            List<Artist> artists = new ArrayList<>();

            // Use SQL to get all artists
            client.sql().execute(null, "SELECT ArtistId, Name FROM Artist ORDER BY ArtistId")
                    .forEachRemaining(row -> {
                        Artist artist = new Artist();
                        artist.setArtistId(row.intValue("ArtistId"));
                        artist.setName(row.stringValue("Name"));
                        artists.add(artist);
                    });

            System.out.println("\nRetrieved " + artists.size() + " artists using SQL");
            System.out.println("First 5 artists:");
            artists.stream().limit(5)
                    .forEach(artist -> System.out.println("  - " + artist.getArtistId() + ": " + artist.getName()));
        } catch (Exception e) {
            System.err.println("Error listing artists: " + e.getMessage());
        }
    }

    /**
     * Find an artist by ID and their albums
     */
    public static void findArtistAndAlbums(IgniteClient client, int targetArtistId) {
        try {
            // Get the artist using SQL
            Artist targetArtist = null;
            var artistResult = client.sql().execute(null,
                    "SELECT ArtistId, Name FROM Artist WHERE ArtistId = ?",
                    targetArtistId);

            if (artistResult.hasNext()) {
                var row = artistResult.next();
                targetArtist = new Artist();
                targetArtist.setArtistId(row.intValue("ArtistId"));
                targetArtist.setName(row.stringValue("Name"));
            }

            if (targetArtist != null) {
                System.out.println("\nFound artist by ID: " + targetArtist.getName());

                // Get their albums using SQL
                List<Album> artistAlbums = new ArrayList<>();
                client.sql().execute(null,
                                "SELECT AlbumId, Title, ArtistId FROM Album WHERE ArtistId = ?",
                                targetArtistId)
                        .forEachRemaining(row -> {
                            Album album = new Album();
                            album.setAlbumId(row.intValue("AlbumId"));
                            album.setTitle(row.stringValue("Title"));
                            album.setArtistId(row.intValue("ArtistId"));
                            artistAlbums.add(album);
                        });

                System.out.println("Albums by " + targetArtist.getName() + ":");
                artistAlbums
                        .forEach(album -> System.out.println("  - " + album.getAlbumId() + ": " + album.getTitle()));

                // Get tracks for the first album
                if (!artistAlbums.isEmpty()) {
                    Album firstAlbum = artistAlbums.get(0);
                    List<Track> albumTracks = new ArrayList<>();

                    client.sql().execute(null,
                                    "SELECT TrackId, Name, AlbumId, MediaTypeId, GenreId, Composer, " +
                                            "Milliseconds, Bytes, UnitPrice FROM Track WHERE AlbumId = ?",
                                    firstAlbum.getAlbumId())
                            .forEachRemaining(row -> {
                                Track track = new Track();
                                track.setTrackId(row.intValue("TrackId"));
                                track.setName(row.stringValue("Name"));
                                track.setAlbumId(row.intValue("AlbumId"));
                                track.setMediaTypeId(row.intValue("MediaTypeId"));

                                // Handle potentially null values defensively
                                try {
                                    track.setGenreId(row.intValue("GenreId"));
                                } catch (Exception e) { /* Value is null */ }
                                try {
                                    track.setComposer(row.stringValue("Composer"));
                                } catch (Exception e) { /* Value is null */ }

                                track.setMilliseconds(row.intValue("Milliseconds"));

                                try {
                                    track.setBytes(row.intValue("Bytes"));
                                } catch (Exception e) { /* Value is null */ }

                                try {
                                    track.setUnitPrice(row.decimalValue("UnitPrice"));
                                } catch (Exception e) { /* Value is null */ }

                                albumTracks.add(track);
                            });

                    System.out.println("\nTracks from album '" + firstAlbum.getTitle() + "':");
                    albumTracks
                            .forEach(track -> System.out.println("  - " + track.getTrackId() + ": " + track.getName()));
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding artist and albums: " + e.getMessage());
        }
    }

    /**
     * Calculate track statistics using SQL
     */
    public static void calculateTrackStatistics(IgniteClient client) {
        try {
            System.out.println("\nTrack Length Statistics (SQL-based):");

            // Get track statistics directly from SQL
            var statResult = client.sql().execute(null,
                    "SELECT COUNT(*) as TotalTracks, " +
                            "AVG(Milliseconds/60000.0) as AvgMinutes, " +
                            "MIN(Milliseconds/60000.0) as MinMinutes, " +
                            "MAX(Milliseconds/60000.0) as MaxMinutes " +
                            "FROM Track");

            if (statResult.hasNext()) {
                var row = statResult.next();
                long totalTracks = row.longValue("TotalTracks");
                BigDecimal avgMinutes = row.decimalValue("AvgMinutes");
                BigDecimal minMinutes = row.decimalValue("MinMinutes");
                BigDecimal maxMinutes = row.decimalValue("MaxMinutes");

                System.out.println("Total tracks: " + totalTracks);
                System.out.println("Average length: " + String.format("%.2f", avgMinutes) + " minutes");
                System.out.println("Shortest track: " + String.format("%.2f", minMinutes) + " minutes");
                System.out.println("Longest track: " + String.format("%.2f", maxMinutes) + " minutes");
            }
        } catch (Exception e) {
            System.err.println("Error calculating track statistics: " + e.getMessage());
        }
    }

    /**
     * Analyze composer information
     */
    public static void analyzeComposerInformation(IgniteClient client) {
        try {
            // Count tracks with and without composer information
            var composerStats = client.sql().execute(null,
                    "SELECT " +
                            "COUNT(*) as TotalTracks, " +
                            "SUM(CASE WHEN Composer IS NOT NULL THEN 1 ELSE 0 END) as TracksWithComposer, " +
                            "SUM(CASE WHEN Composer IS NULL THEN 1 ELSE 0 END) as TracksWithoutComposer " +
                            "FROM Track");

            if (composerStats.hasNext()) {
                var row = composerStats.next();
                long total = row.longValue("TotalTracks");
                long withComposer = row.longValue("TracksWithComposer");
                long withoutComposer = row.longValue("TracksWithoutComposer");

                System.out.println("\nComposer Information Analysis:");
                System.out.println("Total Tracks: " + total);
                System.out.println("Tracks with composer information: " + withComposer +
                        " (" + String.format("%.1f", (withComposer * 100.0 / total)) + "%)");
                System.out.println("Tracks without composer information: " + withoutComposer +
                        " (" + String.format("%.1f", (withoutComposer * 100.0 / total)) + "%)");
            }
        } catch (Exception e) {
            System.err.println("Error analyzing composer information: " + e.getMessage());
        }
    }

    /**
     * List all genres
     */
    public static void listGenres(IgniteClient client) {
        try {
            List<Genre> genres = new ArrayList<>();

            client.sql().execute(null, "SELECT GenreId, Name FROM Genre ORDER BY GenreId")
                    .forEachRemaining(row -> {
                        Genre genre = new Genre();
                        genre.setGenreId(row.intValue("GenreId"));
                        genre.setName(row.stringValue("Name"));
                        genres.add(genre);
                    });

            System.out.println("\nAll music genres:");
            genres.forEach(genre -> System.out.println("  - " + genre.getGenreId() + ": " + genre.getName()));
        } catch (Exception e) {
            System.err.println("Error listing genres: " + e.getMessage());
        }
    }

    /**
     * Analyze genre popularity based on track count
     */
    public static void analyzeGenrePopularity(IgniteClient client) {
        System.out.println("===== GENRE POPULARITY ANALYSIS =====");
        try {
            var result = client.sql().execute(null,
                    "SELECT g.Name as Genre, COUNT(t.TrackId) as TrackCount " +
                            "FROM Genre g " +
                            "JOIN Track t ON g.GenreId = t.GenreId " +
                            "GROUP BY g.Name " +
                            "ORDER BY TrackCount DESC");

            System.out.println("Top Genres by Track Count:");
            System.out.printf("%-25s | %s\n", "Genre", "Number of Tracks");
            System.out.println("------------------------------------------------------------");

            while (result.hasNext()) {
                var row = result.next();
                System.out.printf("%-25s | %d\n",
                        row.stringValue("Genre"),
                        row.longValue("TrackCount"));
            }

            System.out.println();
        } catch (Exception e) {
            System.err.println("Error analyzing genre popularity: " + e.getMessage());
        }
    }

    /**
     * Analyze track length distribution
     */
    public static void analyzeTrackLengths(IgniteClient client) {
        System.out.println("===== TRACK LENGTH ANALYSIS =====");
        try {
            System.out.println("Track Length Distribution:");

            // Define length categories in milliseconds
            int shortLength = 180000; // 3 minutes
            int mediumLength = 300000; // 5 minutes
            int longLength = 480000; // 8 minutes

            var result = client.sql().execute(null,
                    "SELECT " +
                            "SUM(CASE WHEN Milliseconds < ? THEN 1 ELSE 0 END) as ShortTracks, " +
                            "SUM(CASE WHEN Milliseconds >= ? AND Milliseconds < ? THEN 1 ELSE 0 END) as MediumTracks, " +
                            "SUM(CASE WHEN Milliseconds >= ? AND Milliseconds < ? THEN 1 ELSE 0 END) as LongTracks, " +
                            "SUM(CASE WHEN Milliseconds >= ? THEN 1 ELSE 0 END) as VeryLongTracks, " +
                            "CAST(AVG(Milliseconds/60000.0) AS DECIMAL(10,2)) as AvgMinutes, " +
                            "CAST(MIN(Milliseconds/60000.0) AS DECIMAL(10,2)) as MinMinutes, " +
                            "CAST(MAX(Milliseconds/60000.0) AS DECIMAL(10,2)) as MaxMinutes " +
                            "FROM Track",
                    shortLength, shortLength, mediumLength, mediumLength, longLength, longLength);

            if (result.hasNext()) {
                var row = result.next();

                System.out.println("Short Tracks (< 3 min): " + row.longValue("ShortTracks"));
                System.out.println("Medium Tracks (3-5 min): " + row.longValue("MediumTracks"));
                System.out.println("Long Tracks (5-8 min): " + row.longValue("LongTracks"));
                System.out.println("Very Long Tracks (> 8 min): " + row.longValue("VeryLongTracks"));
                System.out.println("Average Length: " + row.decimalValue("AvgMinutes") + " minutes");
                System.out.println("Shortest Track: " + row.decimalValue("MinMinutes") + " minutes");
                System.out.println("Longest Track: " + row.decimalValue("MaxMinutes") + " minutes");
            }

            // Find longest tracks in the database
            System.out.println("\nTop 5 Longest Tracks:");
            System.out.printf("%-40s | %-20s | %-10s\n", "Track", "Artist", "Length (min)");
            System.out.println("------------------------------------------------------------");

            var longestTracks = client.sql().execute(null,
                    "SELECT t.Name as Track, ar.Name as Artist, " +
                            "CAST(t.Milliseconds/60000.0 AS DECIMAL(10,2)) as Minutes " +
                            "FROM Track t " +
                            "JOIN Album a ON t.AlbumId = a.AlbumId " +
                            "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                            "ORDER BY t.Milliseconds DESC " +
                            "LIMIT 5");

            while (longestTracks.hasNext()) {
                var row = longestTracks.next();
                System.out.printf("%-40s | %-20s | %-10s\n",
                        truncateString(row.stringValue("Track"), 40),
                        truncateString(row.stringValue("Artist"), 20),
                        row.decimalValue("Minutes"));
            }

            System.out.println();
        } catch (Exception e) {
            System.err.println("Error analyzing track lengths: " + e.getMessage());
        }
    }

    /**
     * Find artists with the most albums
     */
    public static void findTopArtistsByAlbumCount(IgniteClient client) {
        System.out.println("===== TOP ARTISTS BY ALBUM COUNT =====");
        try {
            var result = client.sql().execute(null,
                    "SELECT ar.Name as Artist, COUNT(DISTINCT a.AlbumId) as AlbumCount, " +
                            "COUNT(DISTINCT t.TrackId) as TrackCount " +
                            "FROM Artist ar " +
                            "LEFT JOIN Album a ON ar.ArtistId = a.ArtistId " +
                            "LEFT JOIN Track t ON a.AlbumId = t.AlbumId " +
                            "GROUP BY ar.ArtistId, ar.Name " +
                            "HAVING COUNT(DISTINCT a.AlbumId) > 0 " +
                            "ORDER BY AlbumCount DESC, TrackCount DESC " +
                            "LIMIT 10");

            System.out.printf("%-25s | %-10s | %-10s\n", "Artist", "Albums", "Tracks");
            System.out.println("------------------------------------------------------------");

            while (result.hasNext()) {
                var row = result.next();
                System.out.printf("%-25s | %-10d | %-10d\n",
                        truncateString(row.stringValue("Artist"), 25),
                        row.longValue("AlbumCount"),
                        row.longValue("TrackCount"));
            }

            System.out.println();
        } catch (Exception e) {
            System.err.println("Error finding top artists: " + e.getMessage());
        }
    }

    /**
     * Analyze composers and their contributions
     */
    public static void analyzeComposers(IgniteClient client) {
        System.out.println("===== COMPOSER ANALYSIS =====");
        try {
            // Count tracks with and without composer information
            var composerStats = client.sql().execute(null,
                    "SELECT " +
                            "COUNT(*) as TotalTracks, " +
                            "SUM(CASE WHEN Composer IS NOT NULL THEN 1 ELSE 0 END) as TracksWithComposer, " +
                            "SUM(CASE WHEN Composer IS NULL THEN 1 ELSE 0 END) as TracksWithoutComposer " +
                            "FROM Track");

            if (composerStats.hasNext()) {
                var row = composerStats.next();
                long total = row.longValue("TotalTracks");
                long withComposer = row.longValue("TracksWithComposer");
                long withoutComposer = row.longValue("TracksWithoutComposer");

                System.out.println("Composer Information Stats:");
                System.out.println("Total Tracks: " + total);
                System.out.println("Tracks with Composer Info: " + withComposer +
                        " (" + String.format("%.1f", (withComposer * 100.0 / total)) + "%)");
                System.out.println("Tracks without Composer Info: " + withoutComposer +
                        " (" + String.format("%.1f", (withoutComposer * 100.0 / total)) + "%)");
            }

            // Find top composers by track count
            System.out.println("\nTop 10 Composers by Track Count:");
            System.out.printf("%-30s | %-10s\n", "Composer", "Track Count");
            System.out.println("------------------------------------------------------------");

            var topComposers = client.sql().execute(null,
                    "SELECT Composer, COUNT(*) as TrackCount " +
                            "FROM Track " +
                            "WHERE Composer IS NOT NULL " +
                            "GROUP BY Composer " +
                            "ORDER BY TrackCount DESC " +
                            "LIMIT 10");

            while (topComposers.hasNext()) {
                var row = topComposers.next();
                System.out.printf("%-30s | %-10d\n",
                        truncateString(row.stringValue("Composer"), 30),
                        row.longValue("TrackCount"));
            }

            System.out.println();
        } catch (Exception e) {
            System.err.println("Error analyzing composers: " + e.getMessage());
        }
    }

    /**
     * Generate playlist recommendations based on genre
     */
    public static void generatePlaylistRecommendations(IgniteClient client) {
        System.out.println("===== PLAYLIST RECOMMENDATIONS =====");
        try {
            // Create rock playlist recommendation
            System.out.println("Generated Rock Playlist Recommendation:");
            System.out.printf("%-40s | %-25s | %-25s\n", "Track", "Artist", "Album");
            System.out.println("------------------------------------------------------------");

            var rockPlaylist = client.sql().execute(null,
                    "SELECT t.Name as Track, ar.Name as Artist, a.Title as Album " +
                            "FROM Track t " +
                            "JOIN Album a ON t.AlbumId = a.AlbumId " +
                            "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                            "JOIN Genre g ON t.GenreId = g.GenreId " +
                            "WHERE g.Name = 'Rock' " +
                            "ORDER BY RAND() " +
                            "LIMIT 10");

            while (rockPlaylist.hasNext()) {
                var row = rockPlaylist.next();
                System.out.printf("%-40s | %-25s | %-25s\n",
                        truncateString(row.stringValue("Track"), 40),
                        truncateString(row.stringValue("Artist"), 25),
                        truncateString(row.stringValue("Album"), 25));
            }

            // Create Jazz playlist recommendation
            System.out.println("\nGenerated Jazz Playlist Recommendation:");
            System.out.printf("%-40s | %-25s | %-25s\n", "Track", "Artist", "Album");
            System.out.println("------------------------------------------------------------");

            var jazzPlaylist = client.sql().execute(null,
                    "SELECT t.Name as Track, ar.Name as Artist, a.Title as Album " +
                            "FROM Track t " +
                            "JOIN Album a ON t.AlbumId = a.AlbumId " +
                            "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                            "JOIN Genre g ON t.GenreId = g.GenreId " +
                            "WHERE g.Name = 'Jazz' " +
                            "ORDER BY RAND() " +
                            "LIMIT 10");

            while (jazzPlaylist.hasNext()) {
                var row = jazzPlaylist.next();
                System.out.printf("%-40s | %-25s | %-25s\n",
                        truncateString(row.stringValue("Track"), 40),
                        truncateString(row.stringValue("Artist"), 25),
                        truncateString(row.stringValue("Album"), 25));
            }

            System.out.println();
        } catch (Exception e) {
            System.err.println("Error generating playlist recommendations: " + e.getMessage());
        }
    }

    /**
     * Perform sales analysis by artist and genre
     */
    public static void performSalesAnalysis(IgniteClient client) {
        System.out.println("===== SALES ANALYSIS =====");
        try {
            // Analyze sales by artist
            System.out.println("Top 10 Artists by Sales Revenue:");
            System.out.printf("%-25s | %-15s | %-15s\n", "Artist", "Tracks Sold", "Revenue ($)");
            System.out.println("------------------------------------------------------------");

            var artistSales = client.sql().execute(null,
                    "SELECT ar.Name as Artist, " +
                            "COUNT(il.InvoiceLineId) as TracksSold, " +
                            "SUM(il.UnitPrice * il.Quantity) as Revenue " +
                            "FROM Artist ar " +
                            "JOIN Album a ON ar.ArtistId = a.ArtistId " +
                            "JOIN Track t ON a.AlbumId = t.AlbumId " +
                            "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                            "GROUP BY ar.Name " +
                            "ORDER BY Revenue DESC " +
                            "LIMIT 10");

            while (artistSales.hasNext()) {
                var row = artistSales.next();
                System.out.printf("%-25s | %-15d | %-15.2f\n",
                        truncateString(row.stringValue("Artist"), 25),
                        row.longValue("TracksSold"),
                        row.decimalValue("Revenue").doubleValue());
            }

            // Analyze sales by genre
            System.out.println("\nSales by Genre:");
            System.out.printf("%-20s | %-15s | %-15s\n", "Genre", "Tracks Sold", "Revenue ($)");
            System.out.println("------------------------------------------------------------");

            var genreSales = client.sql().execute(null,
                    "SELECT g.Name as Genre, " +
                            "COUNT(il.InvoiceLineId) as TracksSold, " +
                            "SUM(il.UnitPrice * il.Quantity) as Revenue " +
                            "FROM Genre g " +
                            "JOIN Track t ON g.GenreId = t.GenreId " +
                            "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                            "GROUP BY g.Name " +
                            "ORDER BY Revenue DESC");

            while (genreSales.hasNext()) {
                var row = genreSales.next();
                System.out.printf("%-20s | %-15d | %-15.2f\n",
                        truncateString(row.stringValue("Genre"), 20),
                        row.longValue("TracksSold"),
                        row.decimalValue("Revenue").doubleValue());
            }

            // Analyze customer spending
            System.out.println("\nTop 5 Customers by Spending:");
            System.out.printf("%-30s | %-15s | %-15s\n", "Customer", "Purchases", "Total Spent ($)");
            System.out.println("------------------------------------------------------------");

            var customerSpending = client.sql().execute(null,
                    "SELECT c.FirstName || ' ' || c.LastName as Customer, " +
                            "COUNT(i.InvoiceId) as Purchases, " +
                            "SUM(i.Total) as TotalSpent " +
                            "FROM Customer c " +
                            "JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                            "GROUP BY c.CustomerId, c.FirstName, c.LastName " +
                            "ORDER BY TotalSpent DESC " +
                            "LIMIT 5");

            while (customerSpending.hasNext()) {
                var row = customerSpending.next();
                System.out.printf("%-30s | %-15d | %-15.2f\n",
                        truncateString(row.stringValue("Customer"), 30),
                        row.longValue("Purchases"),
                        row.decimalValue("TotalSpent").doubleValue());
            }

            System.out.println();
        } catch (Exception e) {
            System.err.println("Error performing sales analysis: " + e.getMessage());
        }
    }

    /**
     * Helper method to truncate strings to a maximum length
     */
    private static String truncateString(String str, int maxLength) {
        if (str == null) {
            return "";
        }

        if (str.length() <= maxLength) {
            return str;
        }

        return str.substring(0, maxLength - 3) + "...";
    }
}