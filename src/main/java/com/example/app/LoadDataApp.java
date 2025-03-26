package com.example.app;

import com.example.util.ChinookUtils;
import com.example.util.DataUtils;
import com.example.util.TableUtils;
import org.apache.ignite.client.IgniteClient;

/**
 * Application to load sample data into the Chinook database
 * Populates all tables with example data
 */
public class LoadDataApp {
    public static void main(String[] args) {
        // Control logging
        java.util.logging.LogManager.getLogManager().reset();
        org.apache.logging.log4j.jul.LogManager.getLogManager();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);

        // Connect to the Ignite cluster
        try (IgniteClient client = ChinookUtils.connectToCluster()) {
            if (client == null) {
                System.err.println("Failed to connect to the Ignite cluster. Exiting.");
                return;
            }

            System.out.println("Connected to the cluster: " + client.connections());

            // First, check if tables exist
            if (!TableUtils.tableExists(client, "Artist")) {
                System.err.println("Tables do not exist. Please run CreateTablesApp first. Exiting.");
                return;
            }

            // Check if data already exists
            long artistCount = 0;
            var result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM Artist");
            if (result.hasNext()) {
                artistCount = result.next().longValue("cnt");
            }

            if (artistCount > 0) {
                System.out.println("Database already contains data (" + artistCount + " artists found).");
                System.out.println("Do you want to load additional sample data? (Y/N)");

                // Simple way to get user input
                java.util.Scanner scanner = new java.util.Scanner(System.in);
                String input = scanner.nextLine().trim().toUpperCase();

                if (!input.equals("Y")) {
                    System.out.println("Exiting without loading additional data.");
                    return;
                }
            }

            // Load basic sample data
            boolean dataLoaded = DataUtils.loadSampleData(client);
            if (!dataLoaded) {
                System.err.println("Failed to load basic sample data.");
                // Continue anyway to try the other data loading methods
            }

            // Create the Queen example (albums and tracks)
            boolean queenCreated = DataUtils.createQueenExample(client);
            if (!queenCreated) {
                System.err.println("Failed to create Queen example.");
                // Continue anyway
            }

            // Add batch data
            boolean batchAdded = DataUtils.addBatchData(client);
            if (!batchAdded) {
                System.err.println("Failed to add batch data.");
                // Continue anyway
            }

            // Query all data to verify everything loaded correctly
            DataUtils.queryAllData(client);

            System.out.println("\nSample data loaded successfully!");
            System.out.println("You can now run Main to see various operations on the data.");

        } catch (Exception e) {
            System.err.println("Error loading data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}