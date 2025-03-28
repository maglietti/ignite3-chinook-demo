package com.example.app;

import com.example.util.ChinookUtils;
import com.example.util.SqlImportUtils;
import com.example.util.TableUtils;
import org.apache.ignite.client.IgniteClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Application to bulk load the Chinook database schema and data from SQL file
 */
public class BulkLoadApp {
    
    public static void main(String[] args) {
        // Control logging
        java.util.logging.LogManager.getLogManager().reset();
        org.apache.logging.log4j.jul.LogManager.getLogManager();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);

        // Create a single scanner for all user input
        Scanner scanner = new Scanner(System.in);

        // Connect to the Ignite cluster
        try (IgniteClient client = ChinookUtils.connectToCluster()) {
            if (client == null) {
                System.err.println("Failed to connect to the cluster. Exiting.");
                return;
            }

            System.out.println(">>> Connected to the cluster: " + client.connections());

            // Check if tables already exist and ask to drop them
            if (TableUtils.tableExists(client, "Artist") || 
                TableUtils.tableExists(client, "Album") || 
                TableUtils.tableExists(client, "Track")) {
                
                System.out.println("Existing tables detected in the database.");
                System.out.println("Do you want to drop existing tables before loading new data? (Y/N)");
                String dropTablesInput = scanner.nextLine().trim().toUpperCase();
                
                if (dropTablesInput.equals("Y")) {
                    // Drop all existing tables
                    boolean tablesDropped = TableUtils.dropTables(client);
                    if (!tablesDropped) {
                        System.err.println("Failed to drop existing tables. Continuing anyway...");
                    }
                    
                    // Ask if zones should be dropped too
                    System.out.println("Do you also want to drop distribution zones? (Y/N)");
                    String dropZonesInput = scanner.nextLine().trim().toUpperCase();
                    
                    if (dropZonesInput.equals("Y")) {
                        boolean zonesDropped = TableUtils.dropDistributionZones(client);
                        if (!zonesDropped) {
                            System.err.println("Failed to drop distribution zones. Continuing anyway...");
                        }
                    }
                }
            }

            // Check if required distribution zones exist, create if not
            if (!TableUtils.zoneExists(client, "Chinook")) {
                System.out.println("Required distribution zones not found. Creating zones...");
                boolean zonesCreated = TableUtils.createDistributionZones(client);
                if (!zonesCreated) {
                    System.err.println("Failed to create distribution zones. Exiting.");
                    return;
                }
            }

            // Find SQL files in resources
            List<String> sqlFiles = findSqlFilesInResources();
            
            if (sqlFiles.isEmpty()) {
                System.err.println("No SQL files found in resources directory.");
                return;
            }
            
            // Display available SQL files
            System.out.println("Available SQL files:");
            for (int i = 0; i < sqlFiles.size(); i++) {
                System.out.println((i + 1) + ". " + sqlFiles.get(i));
            }
            
            // Ask user to select a file
            System.out.print("Select a file to load (1-" + sqlFiles.size() + "): ");
            int fileIndex = 0;
            try {
                fileIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (fileIndex < 0 || fileIndex >= sqlFiles.size()) {
                    System.err.println("Invalid selection. Exiting.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid input. Please enter a number. Exiting.");
                return;
            }
            
            String selectedFile = sqlFiles.get(fileIndex);
            System.out.println("Selected file: " + selectedFile);

            // Read SQL file from resources
            InputStream sqlFileStream = BulkLoadApp.class.getResourceAsStream("/" + selectedFile);
            if (sqlFileStream == null) {
                System.err.println("SQL file not found: " + selectedFile);
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
            System.out.println("Do you want to proceed? (Y/N)");

            // Get user confirmation
            String input = scanner.nextLine().trim().toUpperCase();

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

        } catch (Exception e) {
            System.err.println("Error during bulk load: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always close the scanner
            scanner.close();
        }
    }
    
    /**
     * Find all SQL files in the resources directory
     * 
     * @return List of SQL file names found in resources
     */
    private static List<String> findSqlFilesInResources() {
        List<String> sqlFiles = new ArrayList<>();
        
        try {
            // First try to find resources directly
            URL resourceUrl = BulkLoadApp.class.getClassLoader().getResource("");
            
            if (resourceUrl != null) {
                String protocol = resourceUrl.getProtocol();
                
                if ("file".equals(protocol)) {
                    // Running from file system, scan directory
                    try {
                        java.io.File resourceDir = new java.io.File(resourceUrl.toURI());
                        scanDirectoryForSqlFiles(resourceDir, sqlFiles);
                    } catch (Exception e) {
                        System.err.println("Error scanning directory: " + e.getMessage());
                    }
                } else if ("jar".equals(protocol)) {
                    // Running from JAR, scan JAR entries
                    try {
                        String jarPath = resourceUrl.getPath();
                        if (jarPath.startsWith("file:")) {
                            jarPath = jarPath.substring(5, jarPath.indexOf("!"));
                        }
                        
                        try (JarFile jar = new JarFile(jarPath)) {
                            Enumeration<JarEntry> entries = jar.entries();
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                String name = entry.getName();
                                if (name.toLowerCase().endsWith(".sql")) {
                                    sqlFiles.add(name);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error scanning JAR: " + e.getMessage());
                    }
                }
            }
            
            // If no files found, add defaults
            if (sqlFiles.isEmpty()) {
                sqlFiles.add("chinook-ignite3.sql");
                sqlFiles.add("model_sample_data.sql");
                
                // Verify these files actually exist
                for (int i = sqlFiles.size() - 1; i >= 0; i--) {
                    if (BulkLoadApp.class.getClassLoader().getResource(sqlFiles.get(i)) == null) {
                        sqlFiles.remove(i);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error finding SQL files: " + e.getMessage());
            
            // Add default files as fallback
            sqlFiles.add("chinook-ignite3.sql");
            sqlFiles.add("model_sample_data.sql");
        }
        
        return sqlFiles;
    }
    
    /**
     * Recursively scan a directory for SQL files
     * 
     * @param directory The directory to scan
     * @param sqlFiles List to populate with found SQL files
     */
    private static void scanDirectoryForSqlFiles(java.io.File directory, List<String> sqlFiles) {
        if (directory.exists() && directory.isDirectory()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isDirectory()) {
                        scanDirectoryForSqlFiles(file, sqlFiles);
                    } else if (file.getName().toLowerCase().endsWith(".sql")) {
                        sqlFiles.add(file.getName());
                    }
                }
            }
        }
    }
}