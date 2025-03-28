package com.example.app;

import com.example.util.ChinookUtils;
import com.example.util.SqlImportUtils;
import com.example.util.TableUtils;
import org.apache.ignite.client.IgniteClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Application to bulk load the Chinook database schema and data from SQL file
 */
public class BulkLoadApp {
    
    private static final String SQL_FILE_PATH = "/chinook-ignite3.sql";
    
    public static void main(String[] args) {
        // Control logging
        java.util.logging.LogManager.getLogManager().reset();
        org.apache.logging.log4j.jul.LogManager.getLogManager();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);

        // Connect to the Ignite cluster
        try (IgniteClient client = ChinookUtils.connectToCluster()) {
            if (client == null) {
                System.err.println("Failed to connect to the cluster. Exiting.");
                return;
            }

            System.out.println(">>> Connected to the cluster: " + client.connections());

            // Check if required distribution zones exist, create if not
            if (!TableUtils.zoneExists(client, "Chinook")) {
                System.out.println("Required distribution zones not found. Creating zones...");
                boolean zonesCreated = TableUtils.createDistributionZones(client);
                if (!zonesCreated) {
                    System.err.println("Failed to create distribution zones. Exiting.");
                    return;
                }
            }

            // Read SQL file from resources
            InputStream sqlFileStream = BulkLoadApp.class.getResourceAsStream(SQL_FILE_PATH);
            if (sqlFileStream == null) {
                System.err.println("SQL file not found: " + SQL_FILE_PATH);
                System.err.println("Please make sure the file is in the resources directory.");
                return;
            }

            // Parse SQL statements from file
            List<String> sqlStatements;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(sqlFileStream, StandardCharsets.UTF_8))) {
                sqlStatements = SqlImportUtils.parseSqlStatementsFromReader(reader);
            }
            
            System.out.println("Parsed " + sqlStatements.size() + " SQL statements from file.");

            // Prompt user to confirm load
            System.out.println("This will create tables and load data from the SQL file.");
            System.out.println("Existing tables may be dropped and recreated.");
            System.out.println("Do you want to proceed? (Y/N)");

            // Get user confirmation
            String input;
            try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
                input = scanner.nextLine().trim().toUpperCase();
            }

            if (!input.equals("Y")) {
                System.out.println("Operation cancelled by user.");
                return;
            }

            // Execute the SQL statements
            System.out.println("\n=== Starting bulk load from SQL file ===");
            int successCount = SqlImportUtils.executeSqlStatements(client, sqlStatements);
            
            System.out.println("\n=== Bulk load completed ===");
            System.out.println("Successfully executed " + successCount + " out of " + sqlStatements.size() + " statements.");
            
            // Verify the data was loaded by counting some records
            SqlImportUtils.verifyChinookData(client);

            System.out.println("\nChinook database has been loaded successfully!");
            System.out.println("You can now run Main to see various operations on the data.");

        } catch (Exception e) {
            System.err.println("Error during bulk load: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
