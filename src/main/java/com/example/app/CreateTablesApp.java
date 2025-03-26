package com.example.app;

import com.example.util.ChinookUtils;
import com.example.util.TableUtils;
import org.apache.ignite.client.IgniteClient;

/**
 * Application to create the Chinook database schema
 * This includes distribution zones and all tables required for the Chinook database
 */
public class CreateTablesApp {
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

            System.out.println(">>> Connected to the cluster: " + client.connections());

            // First, check if tables already exist
            if (TableUtils.tableExists(client, "Artist")) {
                System.out.println("Tables appear to already exist.");
                System.out.println("Do you want to drop existing tables and recreate them? (Y/N)");

                // Simple way to get user input
                java.util.Scanner scanner = new java.util.Scanner(System.in);
                String input = scanner.nextLine().trim().toUpperCase();

                if (input.equals("Y")) {
                    // Drop existing tables
                    System.out.println("Dropping existing tables...");
                    TableUtils.dropTables(client);
                } else {
                    System.out.println("Exiting without recreating tables.");
                    return;
                }
            }

            // Create distribution zones
            boolean zonesCreated = TableUtils.createDistributionZones(client);
            if (!zonesCreated) {
                System.err.println("Failed to create distribution zones. Exiting.");
                return;
            }

            // Create tables
            boolean tablesCreated = TableUtils.createTables(client);
            if (!tablesCreated) {
                System.err.println("Failed to create tables. Exiting.");
                return;
            }

            ChinookUtils.closeClient(client);

            System.out.println("\nChinook database schema created successfully!");
            System.out.println("You can now run LoadDataApp to populate the database with sample data.");

        } catch (Exception e) {
            System.err.println("Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
}