package com.example.app;

import com.example.model.*;
import com.example.util.ChinookUtils;
import com.example.util.DataUtils;
import com.example.util.TableUtils;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.IgniteClientConnectionException;
import org.apache.ignite.client.IgniteClientFeatureNotSupportedByServerException;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application class for the Chinook database demo
 * Demonstrates various operations using Ignite Java API
 */
public class Main {
    public static void main(String[] args) {
        // Control logging
        java.util.logging.LogManager.getLogManager().reset();
        org.apache.logging.log4j.jul.LogManager.getLogManager();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);

        // Connect to the Ignite cluster with proper error handling
        try (IgniteClient client = ChinookUtils.connectToCluster()) {
            if (client == null) {
                System.err.println("Failed to connect to the Ignite cluster. Exiting.");
                return;
            }

            System.out.println("Connected to the cluster: " + client.connections());

            // Query existing artists
            ChinookUtils.printAllArtists(client);

            // Add a new artist using our model class
            Artist queen = new Artist(6, "Queen");
            ChinookUtils.addArtist(client, queen);

            // Verify the new artist was added
            System.out.println("\n--- Verifying new artist ---");
            client.sql().execute(null, "SELECT * FROM Artist WHERE ArtistId = 6")
                    .forEachRemaining(row ->
                            System.out.println("New artist added - ID: " + row.intValue("ArtistId") +
                                    ", Name: " + row.stringValue("Name")));

            // Get the Album table
            Table albumTable = client.tables().table("Album");
            System.out.println("\nRetrieved Album table: " + albumTable.name());

            // Add a new album using the Album class
            System.out.println("\n--- Adding new album using RecordView with POJO ---");
            Album newAlbum = new Album(6, "A Night at the Opera", 6);
            ChinookUtils.addAlbum(client, newAlbum);

            // Add another album
            ChinookUtils.addAlbum(client, new Album(7, "News of the World", 6));

            // Find albums by Queen
            ChinookUtils.findAlbumsByArtist(client, "Queen");

            // Add tracks for "A Night at the Opera"
            System.out.println("\n--- Adding tracks for Queen albums ---");
            Track track1 = new Track(
                    6,
                    "Bohemian Rhapsody",
                    6,
                    1,
                    1,
                    "Freddie Mercury",
                    354947,
                    5733664,
                    new BigDecimal("0.99")
            );

            Track track2 = new Track(
                    7,
                    "You're My Best Friend",
                    6,
                    1,
                    1,
                    "John Deacon",
                    175733,
                    2875239,
                    new BigDecimal("0.99")
            );

            // Use batch operations for better performance
            List<Track> tracks = new ArrayList<>();
            tracks.add(track1);
            tracks.add(track2);
            ChinookUtils.addTracksInBatch(client, tracks);

            // Find tracks by Queen
            ChinookUtils.findTracksByArtist(client, "Queen");

            // Demonstrate transaction usage with Pink Floyd example
            System.out.println("\n--- Using a transaction ---");
            DataUtils.createRelatedEntitiesWithTransaction(client);

            // Find albums by the newly added artist
            ChinookUtils.findAlbumsByArtist(client, "Pink Floyd");

        } catch (IgniteClientConnectionException e) {
            System.err.println("Connection error: " + e.getMessage());
            System.err.println("Affected endpoint: " + e.endpoint());
        } catch (IgniteClientFeatureNotSupportedByServerException e) {
            System.err.println("Feature not supported: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}