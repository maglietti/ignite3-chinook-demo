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

## Creating Tables from POJO Classes

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

### Executing SQL Queries

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

## Co-location for Optimized Joins

Co-location ensures related data is stored on the same nodes for better query performance:

### Java Annotation Approach

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId")
)
public class Album {
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer albumId;

    @Column(value = "Title", nullable = false)
    private String title;

    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;
    
    // Constructors, getters, setters...
}
```

### SQL Approach

```sql
CREATE TABLE Album (
    AlbumId INT,
    Title VARCHAR NOT NULL,
    ArtistId INT,
    PRIMARY KEY (AlbumId, ArtistId)
) ZONE Chinook STORAGE PROFILE 'default' COLOCATE BY (ArtistId);
```

## Querying with Joins

When tables are co-located, join queries execute more efficiently as the data is already on the same node:

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

## Advanced Patterns

### Using KeyValueView for Simple Key-based Operations

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

### Using Custom Queries with Parameters

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

### Using SQL DDL to Create Tables and Indexes

```java
public static void createTablesWithSql(IgniteClient client) {
    try {
        // Create a distribution zone
        client.sql().execute(null, 
            "CREATE ZONE IF NOT EXISTS CustomZone " +
            "WITH STORAGE_PROFILES='default', PARTITIONS=50, REPLICAS=2");
        
        // Create a table
        client.sql().execute(null,
            "CREATE TABLE IF NOT EXISTS Customer (" +
            "  CustomerId INT PRIMARY KEY, " +
            "  FirstName VARCHAR NOT NULL, " +
            "  LastName VARCHAR NOT NULL, " +
            "  Email VARCHAR NOT NULL" +
            ") ZONE CustomZone");
        
        // Create an index
        client.sql().execute(null,
            "CREATE INDEX idx_customer_email ON Customer(Email)");
            
        System.out.println("Tables and indexes created successfully.");
    } catch (Exception e) {
        System.err.println("Error creating tables with SQL: " + e.getMessage());
    }
}
```

## Performance Best Practices

1. **Use Batch Operations**: When inserting or updating multiple records, use batch operations like `upsertAll()` instead of individual operations.

2. **Leverage Co-location**: Design your schema to co-locate related data to minimize network transfers during joins.

3. **Choose Appropriate Storage Profiles**:
   - Write-heavy workloads: Consider RocksDB storage engine
   - Read-heavy workloads: Use Apache Ignite Page Memory engine

4. **Use Prepared Statements**: For frequently executed queries, reuse SQL statements to reduce parsing overhead.

5. **Keep Transactions Short**: Long-running transactions can cause contention; keep them as short as possible.

6. **Distribution Zones**:
   - Use higher replica counts for critical data (3+)
   - Use lower replica counts for less critical data to save storage space
   - Choose partition counts based on your data size and cluster size

7. **Use Indexes Wisely**: Create indexes for frequently queried columns but avoid over-indexing.

## CLI Examples

Interacting with Ignite 3 via CLI:

```bash
# Start the CLI
docker run --rm -it --network=host apacheignite/ignite:3.0.0 cli

# Connect to the cluster
connect http://localhost:10300

# Start SQL CLI
sql-cli

# List zones
SELECT * FROM system.zones;

# List tables
SHOW TABLES;

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

## Further Reading

- [Apache Ignite 3 Java API Documentation](https://ignite.apache.org/docs/ignite3/latest/)
- [SQL Reference for Ignite 3](https://ignite.apache.org/docs/ignite3/latest/sql-reference/ddl)
- [Transaction Processing in Ignite 3](https://ignite.apache.org/docs/ignite3/latest/developers-guide/transactions)
- [Distribution Zones in Ignite 3](https://ignite.apache.org/docs/ignite3/latest/administrators-guide/distribution-zones)
