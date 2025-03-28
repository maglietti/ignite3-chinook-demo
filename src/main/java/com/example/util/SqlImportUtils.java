package com.example.util;

import org.apache.ignite.client.IgniteClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for importing SQL data into Ignite 3
 */
public class SqlImportUtils {

    // SQL statement delimiter
    private static final String STATEMENT_DELIMITER = ";";
    
    // Types of statements to ignore during import
    private static final List<String> IGNORED_PREFIXES = Arrays.asList(
            "SET", "BEGIN TRANSACTION", "COMMIT", "--", "/*"
    );
    
    // Patterns for different SQL command types
    private static final Pattern CREATE_ZONE_PATTERN = Pattern.compile("CREATE\\s+ZONE\\s+.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("CREATE\\s+TABLE\\s+.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_PATTERN = Pattern.compile("DROP\\s+.*", Pattern.CASE_INSENSITIVE);
    
    /**
     * Parse SQL statements from a reader
     * 
     * @param reader The BufferedReader containing SQL statements
     * @return A list of SQL statements
     */
    public static List<String> parseSqlStatementsFromReader(BufferedReader reader) throws IOException {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            // Skip empty lines and comments
            line = line.trim();
            if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*")) {
                continue;
            }
            
            // Check if this line contains one or more statement delimiters
            boolean containsDelimiter = line.contains(STATEMENT_DELIMITER);
            
            if (containsDelimiter) {
                // This line contains one or more statements
                String[] parts = line.split(STATEMENT_DELIMITER);
                
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].trim();
                    
                    if (!part.isEmpty()) {
                        // Add part to current statement
                        currentStatement.append(part).append(" ");
                        
                        // If this is not the last part, finalize the statement
                        if (i < parts.length - 1) {
                            String statement = currentStatement.toString().trim();
                            if (!statement.isEmpty() && !shouldIgnoreStatement(statement)) {
                                statements.add(statement);
                            }
                            currentStatement = new StringBuilder();
                        }
                    }
                }
            } else {
                // Just add to current statement
                currentStatement.append(line).append(" ");
            }
        }
        
        // Add final statement if present
        String finalStatement = currentStatement.toString().trim();
        if (!finalStatement.isEmpty() && !shouldIgnoreStatement(finalStatement)) {
            statements.add(finalStatement);
        }
        
        return statements;
    }
    
    /**
     * Check if a SQL statement should be ignored
     * 
     * @param statement The SQL statement to check
     * @return true if the statement should be ignored, false otherwise
     */
    private static boolean shouldIgnoreStatement(String statement) {
        String upperStatement = statement.toUpperCase();
        
        // Check if statement starts with any of the ignored prefixes
        for (String prefix : IGNORED_PREFIXES) {
            if (upperStatement.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Execute a list of SQL statements using the Ignite client
     * 
     * @param client The Ignite client
     * @param statements List of SQL statements to execute
     * @return Number of successfully executed statements
     */
    public static int executeSqlStatements(IgniteClient client, List<String> statements) {
        int successCount = 0;
        int currentStatement = 0;
        int totalStatements = statements.size();
        
        // First, process zones and DDL statements
        System.out.println("=== Processing distribution zones and table definitions ===");
        for (String statement : statements) {
            currentStatement++;
            
            // Skip non-zone/table statements in first pass
            if (!isZoneOrTableStatement(statement)) {
                continue;
            }
            
            try {
                // Print the statement (abbreviated if too long)
                String displayStatement = statement.length() > 100 
                    ? statement.substring(0, 97) + "..." 
                    : statement;
                System.out.println("[" + currentStatement + "/" + totalStatements + "] Executing: " + displayStatement);
                
                // Execute the statement
                client.sql().execute(null, statement);
                successCount++;
                System.out.println("  Success!");
            } catch (Exception e) {
                // Handle exceptions differently based on statement type
                if (isCreateZoneStatement(statement)) {
                    System.out.println("  Note: Zone may already exist, continuing: " + e.getMessage());
                } else if (isDropStatement(statement)) {
                    System.out.println("  Note: Drop operation failed, may be due to dependencies or non-existent object: " + e.getMessage());
                } else {
                    System.err.println("  Error executing statement: " + e.getMessage());
                }
            }
        }
        
        // Then, process all DML statements
        System.out.println("\n=== Loading data (DML statements) ===");
        currentStatement = 0;
        
        for (String statement : statements) {
            currentStatement++;
            
            // Skip zone/table statements in second pass
            if (isZoneOrTableStatement(statement)) {
                continue;
            }
            
            try {
                // Print the statement (abbreviated if too long)
                String displayStatement = statement.length() > 100 
                    ? statement.substring(0, 97) + "..." 
                    : statement;
                System.out.println("[" + currentStatement + "/" + totalStatements + "] Executing: " + displayStatement);
                
                // Execute the statement
                client.sql().execute(null, statement);
                successCount++;
                System.out.println("  Success!");
            } catch (Exception e) {
                System.err.println("  Error executing statement: " + e.getMessage());
            }
        }
        
        return successCount;
    }
    
    /**
     * Check if a statement is a zone or table creation/modification statement
     */
    private static boolean isZoneOrTableStatement(String statement) {
        return isCreateZoneStatement(statement) || isCreateTableStatement(statement) || isDropStatement(statement);
    }
    
    /**
     * Check if a statement is a zone creation statement
     */
    private static boolean isCreateZoneStatement(String statement) {
        return CREATE_ZONE_PATTERN.matcher(statement).find();
    }
    
    /**
     * Check if a statement is a table creation statement
     */
    private static boolean isCreateTableStatement(String statement) {
        return CREATE_TABLE_PATTERN.matcher(statement).find();
    }
    
    /**
     * Check if a statement is a DROP statement
     */
    private static boolean isDropStatement(String statement) {
        return DROP_PATTERN.matcher(statement).find();
    }
    

    
    /**
     * Verify Chinook data was loaded correctly
     * 
     * @param client The Ignite client
     */
    public static void verifyChinookData(IgniteClient client) {
        System.out.println("\n=== Verifying Chinook data ===");
        
        try {
            // Check Artist count
            var result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM Artist");
            if (result.hasNext()) {
                long count = result.next().longValue("cnt");
                System.out.println("Artists: " + count);
            }
            
            // Check Album count
            result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM Album");
            if (result.hasNext()) {
                long count = result.next().longValue("cnt");
                System.out.println("Albums: " + count);
            }
            
            // Check Track count
            result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM Track");
            if (result.hasNext()) {
                long count = result.next().longValue("cnt");
                System.out.println("Tracks: " + count);
            }
            
            // Check additional tables if they were created
            checkTableCount(client, "Customer");
            checkTableCount(client, "Employee");
            checkTableCount(client, "Invoice");
            checkTableCount(client, "InvoiceLine");
            checkTableCount(client, "Playlist");
            checkTableCount(client, "PlaylistTrack");
            
        } catch (Exception e) {
            System.err.println("Error verifying data: " + e.getMessage());
        }
    }
    
    /**
     * Check record count for a table if it exists
     */
    private static void checkTableCount(IgniteClient client, String tableName) {
        try {
            var result = client.sql().execute(null, "SELECT COUNT(*) as cnt FROM " + tableName);
            if (result.hasNext()) {
                long count = result.next().longValue("cnt");
                System.out.println(tableName + ": " + count);
            }
        } catch (Exception e) {
            // Table might not exist, ignore
            System.out.println(tableName + ": Not available");
        }
    }
}