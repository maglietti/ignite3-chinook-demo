package com.example.app;

import com.example.util.ChinookUtils;
import com.example.util.ReportingUtils;
import com.example.util.TableUtils;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.IgniteClientConnectionException;
import org.apache.ignite.client.IgniteClientFeatureNotSupportedByServerException;

/**
 * Music Catalog Analytics Application
 * Demonstrates advanced use of Apache Ignite 3 with the Chinook database
 * Using SQL-based approach to avoid case sensitivity issues with POJO mapping
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

            // Ensure we have data to analyze
            long artistCount = 0;
            var result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM Artist");
            if (result.hasNext()) {
                artistCount = result.next().longValue("cnt");
            }

            if (artistCount == 0) {
                System.err.println("No data found. Please run LoadDataApp or BulkLoadApp first.");
                return;
            }

            System.out.println("\n===== CHINOOK MUSIC CATALOG ANALYTICS =====\n");

            // Display table structure
            TableUtils.displayTableColumns(client, "Artist");

            // Basic entity reports
            ReportingUtils.listArtists(client);
            ReportingUtils.findArtistAndAlbums(client, 1); // AC/DC
            ReportingUtils.calculateTrackStatistics(client);
            ReportingUtils.analyzeComposerInformation(client);
            ReportingUtils.listGenres(client);

            // More complex analytics
            System.out.println("\n===== SQL-BASED ANALYTICS =====\n");

            ReportingUtils.analyzeGenrePopularity(client);
            ReportingUtils.analyzeTrackLengths(client);
            ReportingUtils.findTopArtistsByAlbumCount(client);
            ReportingUtils.analyzeComposers(client);
            ReportingUtils.generatePlaylistRecommendations(client);
            ReportingUtils.performSalesAnalysis(client);

            System.out.println("\n===== ANALYSIS COMPLETE =====");

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