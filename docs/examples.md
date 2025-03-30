# Apache Ignite 3 Code Examples and Patterns

This document provides practical code examples and patterns for working with Apache Ignite 3, using the Chinook database model.

## Connecting to an Ignite Cluster

```java
public static IgniteClient connectToCluster() {
   try {
      // Define node addresses
      String[] nodeAddresses = {
              "localhost:10800", "localhost:10801", "localhost:10802"
      };

      // Build the client and connect
      IgniteClient client = IgniteClient.builder()
              .addresses(nodeAddresses)
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

      // Create tables from annotated classes
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

This method is used by the `BulkLoadApp` to create tables from SQL statements in a file.

## Basic CRUD Operations

### SQL-to-POJO Pattern

Instead of using direct POJO-to-table mapping (which can sometimes have case-sensitivity issues), you can use a SQL-to-POJO approach for greater reliability:

```java
// Retrieve artists using SQL and convert to POJOs
List<Artist> getAllArtists(IgniteClient client) {
   List<Artist> artists = new ArrayList<>();

   client.sql().execute(null, "SELECT ArtistId, Name FROM Artist")
           .forEachRemaining(row -> {
              Artist artist = new Artist();
              artist.setArtistId(row.intValue("ArtistId"));
              artist.setName(row.stringValue("Name"));
              artists.add(artist);
           });

   return artists;
}

// Get a specific artist by ID
Artist getArtistById(IgniteClient client, int artistId) {
   var result = client.sql().execute(null,
           "SELECT ArtistId, Name FROM Artist WHERE ArtistId = ?",
           artistId);

   if (result.hasNext()) {
      var row = result.next();
      Artist artist = new Artist();
      artist.setArtistId(row.intValue("ArtistId"));
      artist.setName(row.stringValue("Name"));
      return artist;
   }

   return null;
}

// Get all albums for an artist
List<Album> getArtistAlbums(IgniteClient client, int artistId) {
   List<Album> albums = new ArrayList<>();

   client.sql().execute(null,
                   "SELECT AlbumId, Title, ArtistId FROM Album WHERE ArtistId = ?",
                   artistId)
           .forEachRemaining(row -> {
              Album album = new Album();
              album.setAlbumId(row.intValue("AlbumId"));
              album.setTitle(row.stringValue("Title"));
              album.setArtistId(row.intValue("ArtistId"));
              albums.add(album);
           });

   return albums;
}

// Update an artist using SQL
boolean updateArtist(IgniteClient client, Artist artist) {
   try {
      client.sql().execute(null,
              "UPDATE Artist SET Name = ? WHERE ArtistId = ?",
              artist.getName(), artist.getArtistId());
      return true;
   } catch (Exception e) {
      System.err.println("Error updating artist: " + e.getMessage());
      return false;
   }
}

// Delete an artist using SQL
boolean deleteArtist(IgniteClient client, int artistId) {
   try {
      client.sql().execute(null,
              "DELETE FROM Artist WHERE ArtistId = ?", artistId);
      return true;
   } catch (Exception e) {
      System.err.println("Error deleting artist: " + e.getMessage());
      return false;
   }
}
```

This approach:

1. Uses SQL for data retrieval and modification
2. Converts SQL results to POJOs on the client side
3. Maintains type safety and object-oriented programming benefits
4. Avoids case-sensitivity and mapping issues

Usage:

```java
// Get all artists
List<Artist> artists = getAllArtists(client);
artists.forEach(artist -> System.out.println(artist.getName()));

// Get a specific artist
Artist queen = getArtistById(client, 6);
if (queen != null) {
    System.out.println("Found artist: " + queen.getName());
    
    // Get all albums for this artist
    List<Album> albums = getArtistAlbums(client, queen.getArtistId());
    albums.forEach(album -> System.out.println("Album: " + album.getTitle()));
    
    // Update the artist
    queen.setName("Queen (Updated)");
    updateArtist(client, queen);
}
```

### Direct Key-Value Operations (Traditional Approach)

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
                                ", Revenue: $" + row.decimalValue("Revenue")));
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

### SQL Transaction with SQL-to-POJO Approach

```java
public static boolean createRelatedEntitiesWithSqlTransaction(IgniteClient client) {
    System.out.println("\n--- Creating related entities with SQL transaction ---");

    try {
        return client.transactions().runInTransaction(tx -> {
            // Create a new artist
            client.sql().execute(tx,
                "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
                7, "Pink Floyd");
                
            // Create a new album for this artist
            client.sql().execute(tx,
                "INSERT INTO Album (AlbumId, Title, ArtistId) VALUES (?, ?, ?)",
                8, "The Dark Side of the Moon", 7);
                
            // Create tracks for the album (direct SQL)
            client.sql().execute(tx,
                "INSERT INTO Track (TrackId, Name, AlbumId, MediaTypeId, Milliseconds, UnitPrice) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                10, "Time", 8, 1, 425032, new BigDecimal("0.99"));
                
            client.sql().execute(tx,
                "INSERT INTO Track (TrackId, Name, AlbumId, MediaTypeId, Milliseconds, UnitPrice) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                11, "Money", 8, 1, 382830, new BigDecimal("0.99"));

            // Verify we can retrieve the data within the transaction
            var result = client.sql().execute(tx, 
                "SELECT a.Title, ar.Name FROM Album a " +
                "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
                "WHERE ar.ArtistId = ?", 7);
                
            if (result.hasNext()) {
                var row = result.next();
                System.out.println("Created album: " + row.stringValue("Title") + 
                    " by " + row.stringValue("Name"));
            }

            return true;
        });
    } catch (Exception e) {
        System.err.println("Transaction failed: " + e.getMessage());
        return false;
    }
}
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

4. **Use SQL for Complex Operations**: For complex aggregations and joins, SQL often provides better performance than client-side operations.

```java
// Efficient SQL-based aggregation
client.sql().execute(null,
    "SELECT ar.Name as Artist, COUNT(a.AlbumId) as AlbumCount " +
    "FROM Artist ar " +
    "LEFT JOIN Album a ON ar.ArtistId = a.ArtistId " +
    "GROUP BY ar.Name " +
    "ORDER BY AlbumCount DESC");
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

## Handling Case Sensitivity in Column Names

When working with POJOs and Ignite tables, case sensitivity can sometimes cause issues. Here are strategies to handle this:

1. **Use SQL-to-POJO Pattern**: Avoid direct POJO mapping issues by using SQL to retrieve data and manually convert to POJOs.

```java
// Safe approach that handles case sensitivity
List<Artist> artists = new ArrayList<>();
client.sql().execute(null, "SELECT ArtistId, Name FROM Artist")
    .forEachRemaining(row -> {
        Artist artist = new Artist();
        artist.setArtistId(row.intValue("ArtistId"));
        artist.setName(row.stringValue("Name"));
        artists.add(artist);
    });
```

2. **Match Case Exactly in Annotations**: Ensure your Java field annotations match the database column names exactly.

```java
// Correct annotation matching database column exactly
@Column(value = "ArtistId", nullable = false)
private Integer artistId;

// Instead of
@Column(value = "artistId", nullable = false)
private Integer artistId;
```

3. **Handle Nullable Fields Safely**: Use try-catch blocks when retrieving potentially null values from SQL results.

```java
Track track = new Track();
// Required fields
track.setTrackId(row.intValue("TrackId"));
track.setName(row.stringValue("Name"));

// Potentially null fields
try { track.setGenreId(row.intValue("GenreId")); } catch (Exception e) { /* Value is null */ }
try { track.setComposer(row.stringValue("Composer")); } catch (Exception e) { /* Value is null */ }
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

## Handling API Limitations and Mismatches

When working with Ignite 3, you might occasionally encounter API limitations or mapping issues. Here are some strategies to handle these situations:

### Field Case Sensitivity

1. **Align annotation case exactly with database columns**

```java
@Column(value = "ArtistId", nullable = false)  // Correct
private Integer artistId;
```

2. **Use SQL for retrieval and manually construct POJOs**

```java
// Reliable approach avoiding case sensitivity issues
List<Artist> artists = new ArrayList<>();
client.sql().execute(null, "SELECT ArtistId, Name FROM Artist")
    .forEachRemaining(row -> {
        Artist artist = new Artist();
        artist.setArtistId(row.intValue("ArtistId"));
        artist.setName(row.stringValue("Name"));
        artists.add(artist);
    });
```

### Handling NULL Values

SQL result sets require defensive programming when handling potentially NULL values:

```java
Track track = new Track();

// Safe approach using try-catch for potentially null values
try { track.setGenreId(row.intValue("GenreId")); } 
catch (Exception e) { /* Field was null */ }

try { track.setComposer(row.stringValue("Composer")); } 
catch (Exception e) { /* Field was null */ }
```

### Managing Decimal Values

For decimal/monetary fields, use the correct method to retrieve values:

```java
// Correct way to get decimal values
track.setUnitPrice(row.decimalValue("UnitPrice"));

// NOT: row.bigDecimalValue("UnitPrice")
```

## Combining OOP and SQL Approaches

A hybrid approach combining object-oriented programming with SQL can often yield the best results:

```java
// 1. Use SQL for efficient data retrieval with filtering/joining
var result = client.sql().execute(null,
    "SELECT t.TrackId, t.Name, t.Composer, t.Milliseconds, " +
    "a.Title as AlbumTitle, ar.Name as ArtistName " +
    "FROM Track t " +
    "JOIN Album a ON t.AlbumId = a.AlbumId " +
    "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
    "WHERE ar.Name = ? " +
    "ORDER BY t.Name", 
    artistName);

// 2. Convert SQL results to domain objects
List<TrackDetail> trackDetails = new ArrayList<>();
result.forEachRemaining(row -> {
    TrackDetail detail = new TrackDetail();
    detail.setTrackId(row.intValue("TrackId"));
    detail.setTrackName(row.stringValue("Name"));
    detail.setAlbumTitle(row.stringValue("AlbumTitle"));
    detail.setArtistName(row.stringValue("ArtistName"));
    try { detail.setComposer(row.stringValue("Composer")); } 
    catch (Exception e) { /* NULL value */ }
    detail.setDurationMinutes(row.intValue("Milliseconds") / 60000.0);
    trackDetails.add(detail);
});

// 3. Use Java streams for client-side processing
Map<String, List<TrackDetail>> tracksByArtist = trackDetails.stream()
    .collect(Collectors.groupingBy(TrackDetail::getArtistName));

// 4. Generate business insights
tracksByArtist.forEach((artist, tracks) -> {
    double avgDuration = tracks.stream()
        .mapToDouble(TrackDetail::getDurationMinutes)
        .average()
        .orElse(0);
    System.out.println("Artist: " + artist);
    System.out.println("Number of tracks: " + tracks.size());
    System.out.println("Average track duration: " + String.format("%.2f", avgDuration) + " minutes");
    System.out.println("Tracks: " + tracks.stream()
        .map(TrackDetail::getTrackName)
        .collect(Collectors.joining(", ")));
    System.out.println();
});
```

This combines:

- SQL's power for data retrieval, joining, and filtering
- Object-oriented approach for code organization and domain modeling
- Java streams for in-memory data transformation and analysis

## Further Reading

- [Apache Ignite 3 Java API Documentation](https://ignite.apache.org/docs/ignite3/latest/)
- [SQL Reference for Ignite 3](https://ignite.apache.org/docs/ignite3/latest/sql-reference/ddl)
- [Transaction Processing in Ignite 3](https://ignite.apache.org/docs/ignite3/latest/developers-guide/transactions)
- [Distribution Zones in Ignite 3](./distribution-zones.md)
- [POJO Mapping in Ignite 3](./pojo-mapping.md)
- [Bulk Loading in Ignite 3](./bulk-load.md)
