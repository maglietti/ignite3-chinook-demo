package com.example.util;

import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.client.IgniteClient;

import com.example.model.*;

import java.util.List;
import java.util.Arrays;

/**
 * Utility class for managing tables in the Chinook database
 */
public class TableUtils {

    /**
     * Checks if a distribution zone exists in the database
     *
     * @param client The Ignite client
     * @param zoneName The name of the zone to check
     * @return True if the zone exists, false otherwise
     */
    public static boolean zoneExists(IgniteClient client, String zoneName) {
        try {
            // Query system.zones to check if the zone exists
            var result = client.sql().execute(null, 
                "SELECT name FROM system.zones WHERE name = ?", zoneName.toUpperCase());
            return result.hasNext(); // If any rows are returned, the zone exists
        } catch (Exception e) {
            System.err.println("Error checking if zone exists: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a table exists in the database
     *
     * @param client The Ignite client
     * @param tableName The name of the table to check
     * @return True if the table exists, false otherwise
     */
    public static boolean tableExists(IgniteClient client, String tableName) {
        try {
            return client.tables().table(tableName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Drop all tables in the Chinook database
     *
     * @param client The Ignite client
     * @return True if all tables were dropped successfully, false otherwise
     */
    public static boolean dropTables(IgniteClient client) {
        try {
            System.out.println("\n=== Dropping Tables");
            // List of tables to drop in reverse order to avoid foreign key constraints
            List<String> tableNames = Arrays.asList(
                "PlaylistTrack", "Playlist", "InvoiceLine", "Invoice", 
                "Customer", "Employee", "Track", "Album", "Artist", 
                "Genre", "MediaType"
            );

            for (String tableName : tableNames) {
                if (tableExists(client, tableName)) {
                    System.out.println("--- Dropping table: " + tableName);
                    client.catalog().dropTable(tableName);
                }
            }
            System.out.println("=== Tables dropped successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Error dropping tables: " + e.getMessage());
            return false;
        }
    }

    /**
     * Drops the distribution zones for the Chinook database
     *
     * @param client The Ignite client
     * @return true if successful, false otherwise
     */
    public static boolean dropDistributionZones(IgniteClient client) {
        try {
            System.out.println("\n=== Dropping Distribution Zones");

            // Drop zones in reverse order of creation
            System.out.println("--- Dropping Distribution Zone: ChinookReplicated");
            client.catalog().dropZone("ChinookReplicated");
            System.out.println("--- Dropping Distribution Zone: Chinook");
            client.catalog().dropZone("Chinook");

            System.out.println("=== Distribution zones dropped successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Error dropping distribution zones: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates the distribution zones for the Chinook database
     *
     * @param client The Ignite client
     * @return true if successful, false otherwise
     */
    public static boolean createDistributionZones(IgniteClient client) {
        try {
            System.out.println("\n=== Creating Distribution Zones");

            // Create the Chinook distribution zone with 2 replicas
            ZoneDefinition zoneChinook = ZoneDefinition.builder("Chinook")
                    .ifNotExists()
                    .replicas(2)
                    .storageProfiles("default")
                    .build();
            System.out.println("--- Creating Zone: " + zoneChinook);
            client.catalog().createZone(zoneChinook);

            // Create the ChinookReplicated distribution zone with 3 replicas and 25 partitions
            ZoneDefinition zoneChinookReplicated = ZoneDefinition.builder("ChinookReplicated")
                    .ifNotExists()
                    .replicas(3)
                    .partitions(25)
                    .storageProfiles("default")
                    .build();
            System.out.println("--- Creating Zone: " + zoneChinookReplicated);
            client.catalog().createZone(zoneChinookReplicated);

            System.out.println("=== Distribution zones created successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Error creating distribution zones: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create all tables for the Chinook database
     *
     * @param client The Ignite client
     * @return True if all tables were created successfully, false otherwise
     */
    public static boolean createTables(IgniteClient client) {
        try {
            System.out.println("\n=== Creating tables");

            // Use IgniteCatalog.createTable to create tables from annotated classes
            // Create tables in order to handle dependencies

            System.out.println("--- Creating Artist table");
            client.catalog().createTable(Artist.class);

            System.out.println("--- Creating Genre table");
            client.catalog().createTable(Genre.class);

            System.out.println("--- Creating MediaType table");
            client.catalog().createTable(MediaType.class);

            System.out.println("--- Creating Album table");
            client.catalog().createTable(Album.class);

            System.out.println("--- Creating Track table");
            client.catalog().createTable(Track.class);

            System.out.println("--- Creating Employee table");
            client.catalog().createTable(Employee.class);

            System.out.println("--- Creating Customer table");
            client.catalog().createTable(Customer.class);

            System.out.println("--- Creating Invoice table");
            client.catalog().createTable(Invoice.class);

            System.out.println("--- Creating InvoiceLine table");
            client.catalog().createTable(InvoiceLine.class);

            System.out.println("--- Creating Playlist table");
            client.catalog().createTable(Playlist.class);

            System.out.println("--- Creating PlaylistTrack table");
            client.catalog().createTable(PlaylistTrack.class);

            System.out.println("=== All tables created successfully!");
            return true;
        } catch (Exception e) {
            System.err.println("Error creating tables: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}