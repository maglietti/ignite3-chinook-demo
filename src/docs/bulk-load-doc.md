# Bulk Loading the Chinook Database in Ignite 3

This document explains the bulk loading process for the Chinook database in Apache Ignite 3, detailing how the SQL file is imported and processed.

## Overview

The Chinook database is a sample music store database that includes tables for artists, albums, tracks, playlists, customers, employees, and sales information. The bulk loading process allows you to quickly populate the entire database schema and data from a SQL file.

## SQL File Structure

The `chinook-ignite3.sql` file contains SQL statements to:

1. Create the necessary distribution zones
2. Create all tables with appropriate schema definitions
3. Populate these tables with sample data

The file follows a specific format:

- SQL statements are delimited by semicolons
- Table creation follows a dependency order (parent tables before child tables)
- Data insertion follows the same dependency order

## Placement of the SQL File

The SQL file should be placed in the `src/main/resources` directory of the project. This is a standard location for non-Java resources in Maven projects and allows the file to be loaded from the classpath at runtime.

```shell
src/main/resources/chinook-ignite3.sql
```

## The Bulk Loading Process

### 1. File Parsing

The `SqlImportUtils.parseSqlStatementsFromReader()` method reads the SQL file and parses it into individual SQL statements:

```java
List<String> sqlStatements = SqlImportUtils.parseSqlStatementsFromReader(reader);
```

This method:

- Reads the file line by line
- Handles statement delimiters (semicolons)
- Filters out comments and empty lines
- Produces a list of clean SQL statements ready for execution

### 2. Zone and Schema Creation

The bulk loader first processes zone and table creation statements:

```java
// First, process zones and DDL statements
for (String statement : statements) {
    // Skip non-zone/table statements in first pass
    if (!isZoneOrTableStatement(statement)) {
        continue;
    }
    // Execute the statement
    client.sql().execute(null, statement);
}
```

This ensures that:

- Distribution zones are created first
- Tables are created in the correct dependency order
- The database schema is fully established before data is loaded

### 3. Data Loading

After the schema is created, the bulk loader processes data insertion statements:

```java
// Then, process all DML statements
for (String statement : statements) {
    // Skip zone/table statements in second pass
    if (isZoneOrTableStatement(statement)) {
        continue;
    }
    // Execute the statement
    client.sql().execute(null, statement);
}
```

This approach:

- Ensures data is inserted after all tables are created
- Handles potential foreign key constraints correctly
- Provides a clean separation between schema creation and data loading

### 4. Error Handling

The bulk loader implements robust error handling:

- Zone-related errors are handled gracefully (zones may already exist)
- Table creation errors are reported but don't halt the process
- Data insertion errors are logged but allow the process to continue

This ensures that the bulk load process can recover from minor issues and still complete successfully.

### 5. Verification

After loading is complete, the process verifies that data was loaded correctly:

```java
SqlImportUtils.verifyChinookData(client);
```

This method checks record counts in major tables to confirm successful data loading.

## Using the BulkLoadApp

The `BulkLoadApp` provides a simple command-line interface for the bulk loading process:

```bash
mvn compile exec:java -Dexec.mainClass="com.example.app.BulkLoadApp"
```

The application:

1. Connects to the Ignite cluster
2. Checks if required distribution zones exist
3. Reads and parses the SQL file
4. Prompts for confirmation before proceeding
5. Executes the SQL statements
6. Verifies the data was loaded correctly

## Entity Model Integration

The bulk loader works alongside the existing entity model classes. When using the SQL file method, both approaches can coexist:

1. **POJO-based schema creation** using `TableUtils.createTables()`
2. **SQL-based schema creation** using statements from the SQL file

The bulk loader primarily uses the SQL approach but also includes support for POJO-based operations if needed.

## Co-location in Bulk Loading

The SQL file maintains the same co-location strategy as the POJO model:

- `Album` is co-located with `Artist` by `ArtistId`
- `Track` is co-located with `Album` by `AlbumId`
- `Invoice` is co-located with `Customer` by `CustomerId`
- `InvoiceLine` is co-located with `Invoice` by `InvoiceId`
- `PlaylistTrack` is co-located with `Playlist` by `PlaylistId`

This ensures that the performance benefits of co-location are preserved when using the bulk loading approach.

## Performance Considerations

Bulk loading offers significant performance benefits:

1. **Reduced Overhead**: Fewer network roundtrips between client and server
2. **Batch Processing**: Multiple records are processed in a single operation
3. **Streamlined Creation**: Schema and data creation happen in a coordinated manner

However, for very large datasets, you may want to consider additional optimizations:

- Increase batch sizes for insert operations
- Consider using multiple concurrent connections for parallel loading
- Monitor cluster resources during loading to avoid overload

## Extending the Bulk Loader

The bulk loading framework can be extended to handle additional data sources:

1. **Multiple SQL Files**: Load data from multiple SQL files for different parts of the schema
2. **CSV Import**: Add support for importing from CSV files
3. **Custom Data Generators**: Create synthetic data for testing or benchmarking

To extend the loader, add new methods to `SqlImportUtils` or create specialized importers for different data formats.

## Conclusion

The bulk loading approach provides a fast and efficient way to populate the Chinook database in Apache Ignite 3. By combining SQL execution with proper error handling and verification, it ensures a reliable and consistent loading process that integrates well with the existing POJO-based model.
