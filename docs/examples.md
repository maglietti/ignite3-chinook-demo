# Apache Ignite 3 Code Examples and Patterns

This document provides practical code examples and patterns for working with Apache Ignite 3, using the Chinook database model.

## Connecting to an Ignite Cluster

```java
public static IgniteClient connectToCluster() {
    try {
        IgniteClient client = IgniteClient.builder()
                .addresses(NODE_ADDRESSES)
                .build();

        System.out.println("Connected to the cluster: " + client.connections());
        return client;
    } catch (Exception e) {
        System.err.println("An error occurred: " + e.getMessage());
        return null;
    }
}
```

Usage:

```java
try (IgniteClient client = ChinookUtils.connectToCluster()) {
    if (client == null) {
        System.err.println("Failed to connect to the cluster. Exiting.");
        return;
    }
    
    // Use the client...
}
```

## Creating Tables: POJO-based vs. SQL-based Approaches

### POJO-based Table Creation

```java
public static boolean createTables(IgniteClient client) {
    try {
        System.out.println("=== Creating tables ===");

        // Use IgniteCatalog.createTable to create tables from annotated classes
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

        System.out.println("=== All tables created successfully!");
        return true;
    } catch (Exception e) {
        System.err.println("Error creating tables: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}
```

### SQL-based Table Creation

```java
public static void createTablesWithSql(IgniteClient client) {
    try {
        // Create a distribution zone
        client.sql().execute(null, 
            "CREATE ZONE IF NOT EXISTS Chinook " +
            "WITH STORAGE_PROFILES='default', REPLICAS=2");
        
        // Create Artist table
        client.sql().execute(null,
            "CREATE TABLE IF NOT EXISTS Artist (" +
            "  ArtistId INT PRIMARY KEY, " +
            "  Name VARCHAR" +
            ") ZONE Chinook");
        
        // Create Album table with co-location
        client.sql().execute(null,
            "CREATE TABLE IF NOT EXISTS Album (" +
            "  AlbumId INT, " +
            "  Title VARCHAR NOT NULL, " +
            "  ArtistId INT, " +
            "  PRIMARY KEY (AlbumId, ArtistId)" +
            ") ZONE Chinook COLOCATE BY (ArtistId)");
            
        System.out.println("Tables created successfully.");
    } catch (Exception e) {
        System.err.println("Error creating tables with SQL: " + e.getMessage());
    }
}
```

## Basic CRUD Operations

### Inserting a Single Record

```java
public static boolean addArtist(IgniteClient client, Artist artist) {
    try {
        // Get the Artist table
        Table artistTable = client.tables().table("Artist");
        // Create a record view for Artist class
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        // Insert the artist
        artistView.upsert(null, artist);
        System.out.println("Added artist: " + artist.getName());
        return true;
    } catch (Exception e) {
        System.err.println("Error adding artist: " + e.getMessage());
        return false;
    }
}
```

Usage:

```java
Artist queen = new Artist(6, "Queen");
ChinookUtils.addArtist(client, queen);
```

### Retrieving a Record by Key

```java
public static Artist getArtistById(IgniteClient client, int artistId) {
    try {
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, Artist> keyValueView = artistTable.keyValueView(Integer.class, Artist.class);
        
        return keyValueView.get(null, artistId);
    } catch (Exception e) {
        System.err.println("Error getting artist: " + e.getMessage());
        return null;
    }
}
```

Usage:

```java
Artist artist = ChinookUtils.getArtistById(client, 6);
if (artist != null) {
    System.out.println("Found artist: " + artist.getName());
}
```

### Updating a Record

```java
public static boolean updateArtist(IgniteClient client, Artist artist) {
    try {
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        // Upsert can be used for both insert and update
        artistView.upsert(null, artist);
        System.out.println("Updated artist: " + artist.getName());
        return true;
    } catch (Exception e) {
        System.err.println("Error updating artist: " + e.getMessage());
        return false;
    }
}
```

Usage:

```java
Artist artist = ChinookUtils.getArtistById(client, 6);
if (artist != null) {
    artist.setName("Queen (Updated)");
    ChinookUtils.updateArtist(client, artist);
}
```

### Deleting a Record

```java
public static boolean deleteArtist(IgniteClient client, int artistId) {
    try {
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, Artist> keyValueView = artistTable.keyValueView(Integer.class, Artist.class);
        
        keyValueView.delete(null, artistId);
        System.out.println("Deleted artist with ID: " + artistId);
        return true;
    } catch (Exception e) {
        System.err.println("Error deleting artist: " + e.getMessage());
        return false;
    }
}
```

Usage:

```java
ChinookUtils.deleteArtist(client, 6);
```

### Batch Insert Operations

```java
public static boolean addTracksInBatch(IgniteClient client, List<Track> tracks) {
    try {
        // Get the Track table
        Table trackTable = client.tables().table("Track");
        // Create a record view for Track class
        RecordView<Track> trackView = trackTable.recordView(Track.class);
        // Insert all tracks in batch
        trackView.upsertAll(null, tracks);
        System.out.println("Added " + tracks.size() + " tracks in batch");
        return true;
    } catch (Exception e) {
        System.err.println("Error adding tracks in batch: " + e.getMessage());
        return false;
    }
}
```

Usage:

```java
List<Track> tracks = new ArrayList<>();
tracks.add(track1);
tracks.add(track2);
ChinookUtils.addTracksInBatch(client, tracks);
```

## Executing SQL Queries

### Basic SQL Query

```java
public static void findAlbumsByArtist(IgniteClient client, String artistName) {
    try {
        System.out.println("\n--- Finding albums by artist: " + artistName + " ---");
        client.sql().execute(null,
                        "SELECT a.Title, ar.Name as ArtistName " +
                                "FROM Album a JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                                "WHERE ar.Name = ?", artistName)
                .forEachRemaining(row ->
                        System.out.println("Album: " + row.stringValue("Title") +
                                " by " + row.stringValue("ArtistName")));
    } catch (Exception e) {
        System.err.println("Error finding albums by artist: " + e.getMessage());
    }
}
```

Usage:

```java
ChinookUtils.findAlbumsByArtist(client, "Queen");
```

### Query with Multiple Joins

```java
public static void findTracksByArtist(IgniteClient client, String artistName) {
    try {
        System.out.println("\n--- Finding tracks by artist: " + artistName + " ---");
        client.sql().execute(null,
                        "SELECT t.Name as Track, t.Composer, a.Title as Album, ar.Name as Artist " +
                                "FROM Track t " +
                                "JOIN Album a ON t.AlbumId = a.AlbumId " +
                                "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                                "WHERE ar.Name = ?", artistName)
                .forEachRemaining(row ->
                        System.out.println("Track: " + row.stringValue("Track") +
                                ", Composer: " + row.stringValue("Composer") +
                                ", Album: " + row.stringValue("Album") +
                                ", Artist: " + row.stringValue("Artist")));
    } catch (Exception e) {
        System.err.println("Error finding tracks by artist: " + e.getMessage());
    }
}
```

### Using SQL with Parameters

```java
public static List<Album> findAlbumsByYear(IgniteClient client, int year) {
    List<Album> albums = new ArrayList<>();
    
    try {
        client.sql().execute(null,
                "SELECT a.AlbumId, a.Title, a.ArtistId " +
                "FROM Album a " +
                "WHERE YEAR(a.ReleaseDate) = ?", year)
            .forEachRemaining(row -> {
                Album album = new Album();
                album.setAlbumId(row.intValue("AlbumId"));
                album.setTitle(row.stringValue("Title"));
                album.setArtistId(row.intValue("ArtistId"));
                albums.add(album);
            });
    } catch (Exception e) {
        System.err.println("Error finding albums by year: " + e.getMessage());
    }
    
    return albums;
}
```

### Query with Aggregation

```java
public static void findTopSellingTracks(IgniteClient client, int limit) {
    try {
        System.out.println("\n--- Finding top " + limit + " selling tracks ---");
        client.sql().execute(null,
                        "SELECT t.Name as Track, ar.Name as Artist, " +
                        "COUNT(il.InvoiceLineId) as SalesCount, " +
                        "SUM(il.UnitPrice * il.Quantity) as Revenue " +
                        "FROM Track t " +
                        "JOIN Album a ON t.AlbumId = a.AlbumId " +
                        "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                        "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
                        "GROUP BY t.TrackId, t.Name, ar.Name " +
                        "ORDER BY SalesCount DESC " +
                        "LIMIT ?", limit)
                .forEachRemaining(row ->
                        System.out.println("Track: " + row.stringValue("Track") +
                                ", Artist: " + row.stringValue("Artist") +
                                ", Sales: " + row.intValue("SalesCount") +
                                ", Revenue: $" + row.bigDecimalValue("Revenue")));
    } catch (Exception e) {
        System.err.println("Error finding top selling tracks: " + e.getMessage());
    }
}
```

## Working with Transactions

```java
public static boolean createRelatedEntitiesWithTransaction(IgniteClient client) {
    System.out.println("\n--- Creating related entities with transaction ---");

    try {
        return client.transactions().runInTransaction(tx -> {
            // Get tables and views
            Table artistTable = client.tables().table("Artist");
            RecordView<Artist> artistView = artistTable.recordView(Artist.class);

            Table albumTable = client.tables().table("Album");
            RecordView<Album> albumView = albumTable.recordView(Album.class);

            Table trackTable = client.tables().table("Track");
            RecordView<Track> trackView = trackTable.recordView(Track.class);

            // Create a new artist
            Artist newArtist = new Artist(7, "Pink Floyd");
            artistView.upsert(tx, newArtist);

            // Create a new album for this artist
            Album newAlbum = new Album(8, "The Dark Side of the Moon", 7);
            albumView.upsert(tx, newAlbum);

            // Create tracks for this album
            List<Track> newTracks = ChinookUtils.createSampleTracks(8, 10);
            trackView.upsertAll(tx, newTracks);

            System.out.println("Created artist: " + newArtist.getName());
            System.out.println("Created album: " + newAlbum.getTitle());
            System.out.println("Created " + newTracks.size() + " tracks");

            return true;
        });
    } catch (Exception e) {
        System.err.println("Transaction failed: " + e.getMessage());
        return false;
    }
}
```

Usage:

```java
DataUtils.createRelatedEntitiesWithTransaction(client);
```

### Transaction with Error Handling

```java
public static boolean transferFunds(IgniteClient client, int fromAccountId, int toAccountId, BigDecimal amount) {
    try {
        return client.transactions().runInTransaction(tx -> {
            // Get Account table
            Table accountTable = client.tables().table("Account");
            RecordView<Account> accountView = accountTable.recordView(Account.class);
            
            // Retrieve accounts
            Account fromAccount = accountView.get(tx, fromAccountId);
            Account toAccount = accountView.get(tx, toAccountId);
            
            // Validate accounts exist
            if (fromAccount == null || toAccount == null) {
                System.err.println("One or both accounts not found");
                return false;
            }
            
            // Validate sufficient funds
            if (fromAccount.getBalance().compareTo(amount) < 0) {
                System.err.println("Insufficient funds");
                return false;
            }
            
            // Update balances
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));
            
            // Save changes
            accountView.upsert(tx, fromAccount);
            accountView.upsert(tx, toAccount);
            
            // Create transaction record
            Table transactionTable = client.tables().table("Transaction");
            RecordView<Transaction> transactionView = transactionTable.recordView(Transaction.class);
            
            Transaction transaction = new Transaction(
                    UUID.randomUUID().toString(),
                    fromAccountId,
                    toAccountId,
                    amount,
                    LocalDateTime.now()
            );
            
            transactionView.upsert(tx, transaction);
            
            return true;
        });
    } catch (Exception e) {
        System.err.println("Transaction failed: " + e.getMessage());
        return false;
    }
}
```

## Checking If a Table Exists

```java
public static boolean tableExists(IgniteClient client, String tableName) {
    try {
        return client.tables().table(tableName) != null;
    } catch (Exception e) {
        return false;
    }
}
```

Usage:

```java
if (TableUtils.tableExists(client, "Artist")) {
    System.out.println("The Artist table exists!");
} else {
    System.out.println("The Artist table does not exist!");
}
```

## Error Handling Patterns

### Try-with-resources for Client Connection

```java
try (IgniteClient client = ChinookUtils.connectToCluster()) {
    if (client == null) {
        System.err.println("Failed to connect to the cluster. Exiting.");
        return;
    }
    
    // Use the client...
    
} catch (IgniteClientConnectionException e) {
    System.err.println("Connection error: " + e.getMessage());
    System.err.println("Affected endpoint: " + e.endpoint());
} catch (IgniteClientFeatureNotSupportedByServerException e) {
    System.err.println("Feature not supported: " + e.getMessage());
} catch (Exception e) {
    System.err.println("Error: " + e.getMessage());
    e.printStackTrace();
}
```

### Checking Preconditions

```java
// First, check if tables exist
if (!TableUtils.tableExists(client, "Artist")) {
    System.err.println("Tables do not exist. Please run CreateTablesApp first. Exiting.");
    return;
}
```

### Prompting for User Confirmation

```java
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
```

## Bulk Loading Data from SQL

The Chinook demo includes a `BulkLoadApp` that can load data from SQL files. Here's an example of how to use it:

```java
public static int executeSqlStatements(IgniteClient client, List<String> statements) {
    int successCount = 0;
    
    // First, process schema statements (zones, tables, indexes)
    System.out.println("=== Processing schema statements ===");
    for (String statement : statements) {
        if (!isSchemaStatement(statement)) {
            continue;
        }
        
        try {
            System.out.println("Executing: " + getStatementPreview(statement));
            client.sql().execute(null, statement);
            successCount++;
            System.out.println("  Success!");
        } catch (Exception e) {
            System.err.println("  Error: " + e.getMessage());
        }
    }
    
    // Then, process data statements (INSERT, UPDATE, etc.)
    System.out.println("\n=== Processing data statements ===");
    for (String statement : statements) {
        if (isSchemaStatement(statement)) {
            continue;
        }
        
        try {
            System.out.println("Executing: " + getStatementPreview(statement));
            client.sql().execute(null, statement);
            successCount++;
            System.out.println("  Success!");
        } catch (Exception e) {
            System.err.println("  Error: " + e.getMessage());
        }
    }
    
    return successCount;
}

private static boolean isSchemaStatement(String statement) {
    String normalized = statement.trim().toUpperCase();
    return normalized.startsWith("CREATE ") || normalized.startsWith("DROP ") || normalized.startsWith("ALTER ");
}

private static String getStatementPreview(String statement) {
    int maxLength = 50;
    if (statement.length() <= maxLength) {
        return statement;
    }
    return statement.substring(0, maxLength - 3) + "...";
}
```

Usage:

```java
List<String> sqlStatements = parseSqlFile("chinook-ignite3.sql");
int successCount = executeSqlStatements(client, sqlStatements);
System.out.println("Successfully executed " + successCount + " statements.");
```

## Performance Comparison: POJO vs. SQL

Here's a simple benchmark to compare POJO-based operations with SQL-based operations:

```java
public static void benchmarkOperations(IgniteClient client) {
    System.out.println("\n=== Performance Benchmark ===");
    
    // Prepare data
    List<Artist> artists = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
        artists.add(new Artist(1000 + i, "Artist" + i));
    }
    
    // POJO-based insert
    long start = System.currentTimeMillis();
    Table artistTable = client.tables().table("Artist");
    RecordView<Artist> artistView = artistTable.recordView(Artist.class);
    artistView.upsertAll(null, artists);
    long pojoInsertTime = System.currentTimeMillis() - start;
    
    // SQL-based insert
    start = System.currentTimeMillis();
    for (Artist artist : artists) {
        client.sql().execute(null, 
            "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
            artist.getArtistId() + 1000, artist.getName() + "_SQL");
    }
    long sqlInsertTime = System.currentTimeMillis() - start;
    
    // POJO-based query
    start = System.currentTimeMillis();
    List<Artist> result1 = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        Artist artist = artistView.get(null, 1000 + i);
        if (artist != null) {
            result1.add(artist);
        }
    }
    long pojoQueryTime = System.currentTimeMillis() - start;
    
    // SQL-based query
    start = System.currentTimeMillis();
    List<Artist> result2 = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        client.sql().execute(null, 
            "SELECT * FROM Artist WHERE ArtistId = ?", 
            2000 + i)
        .forEachRemaining(row -> {
            Artist artist = new Artist(
                row.intValue("ArtistId"),
                row.stringValue("Name")
            );
            result2.add(artist);
        });
    }
    long sqlQueryTime = System.currentTimeMillis() - start;
    
    // Print results
    System.out.println("POJO-based batch insert (1000 records): " + pojoInsertTime + "ms");
    System.out.println("SQL-based individual inserts (1000 records): " + sqlInsertTime + "ms");
    System.out.println("POJO-based queries (100 records): " + pojoQueryTime + "ms");
    System.out.println("SQL-based queries (100 records): " + sqlQueryTime + "ms");
}
```

## Working with Complex Queries

### Using SQL Projections

```java
public static List<AlbumSummary> getAlbumSummaries(IgniteClient client) {
    List<AlbumSummary> summaries = new ArrayList<>();
    
    try {
        client.sql().execute(null,
            "SELECT a.AlbumId, a.Title, ar.Name as ArtistName, " +
            "COUNT(t.TrackId) as TrackCount, " +
            "SUM(t.Milliseconds) / 60000.0 as TotalMinutes " +
            "FROM Album a " +
            "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
            "JOIN Track t ON a.AlbumId = t.AlbumId " +
            "GROUP BY a.AlbumId, a.Title, ar.Name " +
            "ORDER BY a.Title")
        .forEachRemaining(row -> {
            AlbumSummary summary = new AlbumSummary();
            summary.setAlbumId(row.intValue("AlbumId"));
            summary.setTitle(row.stringValue("Title"));
            summary.setArtistName(row.stringValue("ArtistName"));
            summary.setTrackCount(row.intValue("TrackCount"));
            summary.setTotalMinutes(row.doubleValue("TotalMinutes"));
            summaries.add(summary);
        });
    } catch (Exception e) {
        System.err.println("Error getting album summaries: " + e.getMessage());
    }
    
    return summaries;
}
```

### Using SQL for Advanced Aggregations

```java
public static void getGenreStatistics(IgniteClient client) {
    try {
        System.out.println("\n--- Genre Statistics ---");
        client.sql().execute(null,
            "SELECT g.Name as Genre, " +
            "COUNT(t.TrackId) as TrackCount, " +
            "AVG(t.Milliseconds / 60000.0) as AvgDurationMinutes, " +
            "SUM(t.UnitPrice * il.Quantity) as TotalRevenue, " +
            "COUNT(DISTINCT ar.ArtistId) as ArtistCount " +
            "FROM Genre g " +
            "JOIN Track t ON g.GenreId = t.GenreId " +
            "JOIN Album a ON t.AlbumId = a.AlbumId " +
            "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
            "LEFT JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
            "GROUP BY g.GenreId, g.Name " +
            "ORDER BY TotalRevenue DESC")
        .forEachRemaining(row -> {
            System.out.println("Genre: " + row.stringValue("Genre"));
            System.out.println("  Track Count: " + row.intValue("TrackCount"));
            System.out.println("  Average Duration: " + String.format("%.2f", row.doubleValue("AvgDurationMinutes")) + " minutes");
            System.out.println("  Total Revenue: $" + row.bigDecimalValue("TotalRevenue"));
            System.out.println("  Artist Count: " + row.intValue("ArtistCount"));
            System.out.println();
        });
    } catch (Exception e) {
        System.err.println("Error getting genre statistics: " + e.getMessage());
    }
}
```

## Performance Best Practices

1. **Use Batch Operations**: When inserting or updating multiple records, use batch operations like `upsertAll()` instead of individual operations.

```java
// Inefficient approach
for (Artist artist : artists) {
    artistView.upsert(null, artist);
}

// Efficient approach
artistView.upsertAll(null, artists);
```

2. **Leverage Co-location**: Design your schema to co-locate related data to minimize network transfers during joins.

```java
// Co-located tables enable efficient joins
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId")
)
public class Album { ... }
```

3. **Choose Appropriate Storage Profiles**:
   - Write-heavy workloads: Consider RocksDB storage engine
   - Read-heavy workloads: Use Apache Ignite Page Memory engine

4. **Use Prepared Statements**: For frequently executed queries, reuse SQL statements to reduce parsing overhead.

```java
// Create a prepared statement
SqlFieldsQuery query = new SqlFieldsQuery("SELECT * FROM Artist WHERE Name LIKE ?");

// Execute with different parameters
query.setArgs("A%");
List<List<?>> result1 = client.sql().execute(null, query);

query.setArgs("B%");
List<List<?>> result2 = client.sql().execute(null, query);
```

5. **Keep Transactions Short**: Long-running transactions can cause contention; keep them as short as possible.

6. **Distribution Zones**:
   - Use higher replica counts for critical data (3+)
   - Use lower replica counts for less critical data to save storage space
   - Choose partition counts based on your data size and cluster size

7. **Use Indexes Wisely**: Create indexes for frequently queried columns but avoid over-indexing.

```java
// Creating an index in SQL
client.sql().execute(null, "CREATE INDEX idx_artist_name ON Artist(Name)");

// Creating an index with Java annotations
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    indexes = {
        @Index(value = "idx_artist_name", columns = { @ColumnRef("Name") })
    }
)
public class Artist { ... }
```

## CLI Examples

Interacting with Ignite 3 via CLI:

```bash
# Start the CLI
docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 apacheignite/ignite:3.0.0 cli

# Connect to the cluster
connect http://localhost:10300

# Start SQL CLI
sql

# List zones
SELECT * FROM system.zones;

# List tables
SELECT * from SYSTEM.TABLES;

# Create a distribution zone
CREATE ZONE IF NOT EXISTS ExampleZone 
WITH STORAGE_PROFILES='default', PARTITIONS=50, REPLICAS=2;

# Create a table
CREATE TABLE ExampleTable (
  id INT PRIMARY KEY,
  name VARCHAR NOT NULL
) ZONE ExampleZone;

# Insert data
INSERT INTO ExampleTable VALUES (1, 'Example');

# Query data
SELECT * FROM ExampleTable;

# Exit SQL CLI
exit;

# Exit CLI
exit
```

## Comparison of Data Loading Approaches

| Feature | CreateTablesApp + LoadDataApp | BulkLoadApp |
|---------|-------------------------------|-------------|
| Implementation | POJO-based | SQL-based |
| Schema Creation | Java annotations | SQL statements |
| Data Loading | Individual API calls | SQL INSERT statements |
| Flexibility | More control over each step | All-in-one approach |
| Performance | Good for small datasets | Better for large datasets |
| Extensibility | Easy to extend with custom code | Requires SQL knowledge |
| Error Handling | Fine-grained error handling | Statement-level error handling |
| Learning Curve | Requires understanding of Java API | Requires SQL knowledge |

## Further Reading

- [Apache Ignite 3 Java API Documentation](https://ignite.apache.org/docs/ignite3/latest/)
- [SQL Reference for Ignite 3](https://ignite.apache.org/docs/ignite3/latest/sql-reference/ddl)
- [Transaction Processing in Ignite 3](https://ignite.apache.org/docs/ignite3/latest/developers-guide/transactions)
- [Distribution Zones in Ignite 3](./distribution-zones.md)
- [POJO Mapping in Ignite 3](./pojo-mapping.md)
- [Bulk Loading in Ignite 3](./bulk-load.md)
