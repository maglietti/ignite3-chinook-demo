package com.example.util;

import org.apache.ignite.client.IgniteClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for importing SQL data into Ignite 3
 */
public class SqlImportUtils {
    
    // Types of statements to ignore during import
    private static final List<String> IGNORED_PREFIXES = Arrays.asList(
            "SET", "BEGIN TRANSACTION", "COMMIT", "--", "/*"
    );
    
    // Patterns for different SQL command types
    private static final Pattern CREATE_ZONE_PATTERN = Pattern.compile("CREATE\\s+ZONE\\s+.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("CREATE\\s+TABLE\\s+.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_INDEX_PATTERN = Pattern.compile("CREATE\\s+INDEX\\s+.*", Pattern.CASE_INSENSITIVE);
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
        boolean insideMultilineComment = false;
        boolean insideQuote = false;
        
        while ((line = reader.readLine()) != null) {
            // Handle multiline comments
            if (line.contains("/*") && !line.contains("*/")) {
                insideMultilineComment = true;
                continue;
            }
            
            if (insideMultilineComment) {
                if (line.contains("*/")) {
                    insideMultilineComment = false;
                }
                continue;
            }
            
            // Skip empty lines and single-line comments
            line = line.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }
            
            // Process the line
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                
                // Handle quotes (to avoid detecting statement delimiters inside quoted strings)
                if (c == '\'' && (i == 0 || line.charAt(i-1) != '\\')) {
                    insideQuote = !insideQuote;
                }
                
                // If we find a delimiter outside of quotes, split the statement
                if (c == ';' && !insideQuote) {
                    String statement = currentStatement.toString().trim();
                    if (!statement.isEmpty() && !shouldIgnoreStatement(statement)) {
                        statements.add(statement);
                    }
                    currentStatement = new StringBuilder();
                } else {
                    currentStatement.append(c);
                }
            }
            
            // Add a space between lines for readability
            if (currentStatement.length() > 0) {
                currentStatement.append(' ');
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
        
        // Maximum rows per batch
        final int MAX_BATCH_SIZE = 1000;
        
        // First, process zones, tables, and index statements
        System.out.println("=== Processing distribution zones, table definitions, and indexes ===");
        for (String statement : statements) {
            currentStatement++;
            
            // Skip non-zone/table/index statements in first pass
            if (!isSchemaStatement(statement)) {
                continue;
            }
            
            try {
                // Get statement type for display
                String stmtType = "SQL";
                if (isCreateZoneStatement(statement)) stmtType = "CREATE ZONE";
                else if (isCreateTableStatement(statement)) stmtType = "CREATE TABLE";
                else if (isCreateIndexStatement(statement)) stmtType = "CREATE INDEX";
                else if (isDropStatement(statement)) stmtType = "DROP";
                
                // For CREATE INDEX statements, extract the table and index name
                String displayInfo = "";
                if (isCreateIndexStatement(statement)) {
                    String indexName = extractIndexName(statement);
                    String tableName = extractIndexTable(statement);
                    displayInfo = indexName + " ON " + tableName;
                } else {
                    // For other statements, just show a preview
                    displayInfo = statement.length() > 70 
                        ? statement.substring(0, 67) + "..." 
                        : statement;
                }
                
                System.out.println("[" + currentStatement + "/" + totalStatements + "] Executing: " + stmtType + " " + displayInfo);
                
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
                } else if (isCreateIndexStatement(statement)) {
                    System.out.println("  Note: Index creation failed, may already exist: " + e.getMessage());
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
            
            // Skip schema statements in second pass
            if (isSchemaStatement(statement)) {
                continue;
            }
            
            try {
                // Get statement type and target table
                String statementType = getStatementType(statement);
                String targetTable = getTargetTable(statement);
                
                // For INSERT statements, check if batch splitting is needed
                if (statementType.equals("INSERT")) {
                    int approxRows = countInsertRows(statement);
                    System.out.println("[" + currentStatement + "/" + totalStatements + "] Found " + 
                                        statementType + " for table " + targetTable + 
                                        " with " + approxRows + " rows");
                    
                    if (approxRows > MAX_BATCH_SIZE) {
                        System.out.println("  Splitting large INSERT into smaller batches...");
                        List<String> batches = splitLargeInsert(statement, MAX_BATCH_SIZE);
                        System.out.println("  Created " + batches.size() + " batches");
                        
                        // Execute each batch
                        int batchNum = 1;
                        for (String batch : batches) {
                            System.out.println("  Executing batch " + batchNum + "/" + batches.size());
                            client.sql().execute(null, batch);
                            batchNum++;
                        }
                        
                        successCount++;
                        System.out.println("  All batches executed successfully!");
                        continue;
                    }
                }
                
                // For non-INSERT statements or small INSERTs, execute directly
                System.out.println("[" + currentStatement + "/" + totalStatements + "] Executing " + 
                                  statementType + " for table " + targetTable);
                
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
     * Extract the index name from a CREATE INDEX statement
     */
    private static String extractIndexName(String statement) {
        Pattern pattern = Pattern.compile("CREATE\\s+INDEX\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w_]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(statement);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown_index";
    }
    
    /**
     * Extract the table name from a CREATE INDEX statement
     */
    private static String extractIndexTable(String statement) {
        Pattern pattern = Pattern.compile("ON\\s+([\\w_]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(statement);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown_table";
    }
    
    /**
     * Split large INSERT statements into smaller batches
     * @param statement The original INSERT statement
     * @param batchSize The maximum number of rows per batch
     * @return A list of smaller INSERT statements
     */
    public static List<String> splitLargeInsert(String statement, int batchSize) {
        List<String> batches = new ArrayList<>();
        
        // First, ensure this is an INSERT statement
        if (!statement.toUpperCase().trim().startsWith("INSERT")) {
            batches.add(statement);
            return batches;
        }
        
        // Find the VALUES keyword
        int valuesIndex = statement.toUpperCase().indexOf("VALUES");
        if (valuesIndex == -1) {
            batches.add(statement);
            return batches;
        }
        
        // Split into prefix (INSERT INTO table (columns)) and values part
        String prefix = statement.substring(0, valuesIndex + 6); // Include "VALUES"
        String valuesPart = statement.substring(valuesIndex + 6).trim();
        
        // Parse the value groups
        List<String> valueGroups = new ArrayList<>();
        StringBuilder currentGroup = new StringBuilder();
        int parenthesesLevel = 0;
        boolean inQuote = false;
        
        for (int i = 0; i < valuesPart.length(); i++) {
            char c = valuesPart.charAt(i);
            
            // Track quotes to avoid splitting inside quoted strings
            if (c == '\'' && (i == 0 || valuesPart.charAt(i-1) != '\\')) {
                inQuote = !inQuote;
            }
            
            // Track parentheses level
            if (c == '(' && !inQuote) parenthesesLevel++;
            if (c == ')' && !inQuote) parenthesesLevel--;
            
            // Add the character to the current group
            currentGroup.append(c);
            
            // If we've closed a top-level value group
            if (parenthesesLevel == 0 && currentGroup.length() > 0 && c == ')') {
                // Remove trailing commas
                String group = currentGroup.toString().trim();
                if (group.endsWith(",")) {
                    group = group.substring(0, group.length() - 1).trim();
                }
                
                valueGroups.add(group);
                currentGroup = new StringBuilder();
                
                // Skip past any commas
                while (i + 1 < valuesPart.length() && valuesPart.charAt(i + 1) == ',') {
                    i++;
                }
            }
        }
        
        // Create batched INSERT statements
        for (int i = 0; i < valueGroups.size(); i += batchSize) {
            StringBuilder batchStatement = new StringBuilder(prefix);
            for (int j = i; j < Math.min(i + batchSize, valueGroups.size()); j++) {
                if (j > i) batchStatement.append(", ");
                batchStatement.append(valueGroups.get(j));
            }
            batches.add(batchStatement.toString());
        }
        
        return batches;
    }
    
    /**
     * Check if a statement is a schema statement (zone, table, index, etc.)
     */
    private static boolean isSchemaStatement(String statement) {
        // Trim and normalize the statement
        String normalizedStatement = statement.trim().toUpperCase();
        
        // Check specifically for DDL statements
        return normalizedStatement.startsWith("CREATE ZONE") || 
            normalizedStatement.startsWith("CREATE TABLE") || 
            normalizedStatement.startsWith("CREATE INDEX") || 
            normalizedStatement.startsWith("DROP TABLE") ||
            normalizedStatement.startsWith("DROP ZONE") ||
            normalizedStatement.startsWith("DROP INDEX");
    }

    /**
     * Check if a statement is a zone creation statement
     */
    private static boolean isCreateZoneStatement(String statement) {
        String normalizedStatement = statement.trim().toUpperCase();
        return normalizedStatement.startsWith("CREATE ZONE");
    }

    /**
     * Check if a statement is a table creation statement
     */
    private static boolean isCreateTableStatement(String statement) {
        String normalizedStatement = statement.trim().toUpperCase();
        return normalizedStatement.startsWith("CREATE TABLE");
    }

    /**
     * Check if a statement is an index creation statement
     */
    private static boolean isCreateIndexStatement(String statement) {
        String normalizedStatement = statement.trim().toUpperCase();
        return normalizedStatement.startsWith("CREATE INDEX");
    }

    /**
     * Check if a statement is a DROP statement
     */
    private static boolean isDropStatement(String statement) {
        String normalizedStatement = statement.trim().toUpperCase();
        return normalizedStatement.startsWith("DROP TABLE") || 
            normalizedStatement.startsWith("DROP ZONE") ||
            normalizedStatement.startsWith("DROP INDEX");
    }
    
    /**
     * Get the type of SQL statement (INSERT, UPDATE, DELETE, etc.)
     */
    private static String getStatementType(String statement) {
        String upperStatement = statement.trim().toUpperCase();
        if (upperStatement.startsWith("INSERT")) return "INSERT";
        if (upperStatement.startsWith("UPDATE")) return "UPDATE";
        if (upperStatement.startsWith("DELETE")) return "DELETE";
        if (upperStatement.startsWith("SELECT")) return "SELECT";
        return "SQL";
    }

    /**
     * Get the target table name from an SQL statement
     */
    private static String getTargetTable(String statement) {
        // Simple regex to extract table name
        String pattern = "(?i)(INTO|FROM|UPDATE)\\s+([\\w]+)";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(statement);
        if (m.find()) {
            return m.group(2);
        }
        return "unknown";
    }

    /**
 * Count the exact number of INSERT rows
 */
private static int countInsertRows(String statement) {
    // Check if this is an INSERT with VALUES clause
    String upperStatement = statement.toUpperCase();
    if (!upperStatement.contains("VALUES")) {
        return 1;  // Not a standard multi-value insert
    }
    
    int valueCount = 0;
    boolean inParentheses = false;
    boolean inQuote = false;
    int parenthesesLevel = 0;
    
    // Find the VALUES keyword
    int valuesIndex = upperStatement.indexOf("VALUES");
    if (valuesIndex == -1) {
        return 1;
    }
    
    // Start counting after VALUES
    for (int i = valuesIndex + 6; i < statement.length(); i++) {
        char c = statement.charAt(i);
        
        // Handle quotes to avoid counting parentheses in strings
        if (c == '\'' && (i == 0 || statement.charAt(i-1) != '\\')) {
            inQuote = !inQuote;
        }
        
        if (!inQuote) {
            if (c == '(') {
                parenthesesLevel++;
                if (parenthesesLevel == 1) {
                    // Start of a new value group at the top level
                    valueCount++;
                }
            } else if (c == ')') {
                parenthesesLevel--;
            }
        }
    }
    
    return Math.max(1, valueCount);  // At least 1 row
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