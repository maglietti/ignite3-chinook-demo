# POJO-to-Table Mapping Pattern in Apache Ignite 3

This document explains how to map Java POJOs (Plain Old Java Objects) directly to database tables in Apache Ignite 3. This pattern uses Java annotations to define the mapping between POJO fields and table columns, providing an object-oriented interface for data operations.

## Overview

The POJO-to-Table mapping pattern provides these benefits:

- Type safety with compile-time error checking
- Object-oriented approach to data handling
- Automatic mapping between Java objects and database tables
- Integration with Ignite's transaction system

## Defining POJO Mapping with Annotations

To map a POJO to a table, you need:

1. `@Table` annotation on the class with distribution zone information
2. `@Column` annotation on fields to map them to table columns
3. `@Id` annotation on fields that form the primary key

### Example: Artist Model

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default")
)
public class Artist {
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;

    @Column(value = "Name", nullable = true)
    private String name;
    
    // Constructors, getters, setters...
}
```

### Example: Album Model with Co-location

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId"),
    indexes = {
        @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })
    }
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

## Creating a Table from a POJO

```java
// Create a table from an annotated class
client.catalog().createTable(Artist.class);
```

This method:

1. Analyzes the annotations in the POJO class
2. Creates a corresponding table in the database
3. Sets up the appropriate indexes and constraints

## POJO-Based CRUD Operations

### Creating Table References

```java
// Get a reference to the table
Table artistTable = client.tables().table("Artist");
```

### Insert Operations

```java
// Create a RecordView for the Artist class
RecordView<Artist> artistView = artistTable.recordView(Artist.class);

// Create a new Artist object
Artist artist = new Artist(1, "AC/DC");

// Insert the record
artistView.upsert(null, artist);
```

### Query by Primary Key

```java
// Create a KeyValueView for efficient primary key lookups
KeyValueView<Integer, Artist> keyValueView = artistTable.keyValueView(Integer.class, Artist.class);

// Lookup an artist by ID
Artist artist = keyValueView.get(null, 1);
if (artist != null) {
    System.out.println("Found artist: " + artist.getName());
}
```

### Batch Operations

```java
// Create multiple artist objects
List<Artist> artists = Arrays.asList(
    new Artist(1, "AC/DC"),
    new Artist(2, "Accept"),
    new Artist(3, "Aerosmith")
);

// Insert all artists in a single batch operation
artistView.upsertAll(null, artists);
```

### Update Operations

Update operations use the same `upsert` method - if the record exists, it will be updated:

```java
// Get an existing artist
Artist artist = keyValueView.get(null, 1);

// Modify the artist
artist.setName("AC/DC (Updated)");

// Update the record
artistView.upsert(null, artist);
```

### Delete Operations

```java
// Delete by primary key
keyValueView.delete(null, 1);

// Or delete using a record
artistView.delete(null, artist);
```

## Using Transactions with POJOs

Transactions ensure that multiple related operations succeed or fail as a unit:

```java
boolean success = client.transactions().runInTransaction(tx -> {
    try {
        // Get table references and views
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        
        Table albumTable = client.tables().table("Album");
        RecordView<Album> albumView = albumTable.recordView(Album.class);
        
        // Create a new artist
        Artist newArtist = new Artist(7, "Pink Floyd");
        artistView.upsert(tx, newArtist);
        
        // Create albums for this artist
        Album album1 = new Album(8, "The Dark Side of the Moon", 7);
        Album album2 = new Album(9, "Wish You Were Here", 7);
        
        albumView.upsert(tx, album1);
        albumView.upsert(tx, album2);
        
        return true; // Commit transaction
    } catch (Exception e) {
        // Transaction will be rolled back
        System.err.println("Error: " + e.getMessage());
        return false;
    }
});
```

## Combined SQL and POJO Operations

You can combine SQL queries with POJO operations for greater flexibility:

### Finding Data with SQL and Working with POJOs

```java
// Query for all albums by a specific artist
List<Album> artistAlbums = new ArrayList<>();

client.sql().execute(null, 
    "SELECT AlbumId, Title, ArtistId FROM Album WHERE ArtistId = ?", 
    artistId)
    .forEachRemaining(row -> {
        Album album = new Album();
        album.setAlbumId(row.intValue("AlbumId"));
        album.setTitle(row.stringValue("Title"));
        album.setArtistId(row.intValue("ArtistId"));
        artistAlbums.add(album);
    });

// Now we have a list of Album POJOs we can work with
```

## Advanced POJO Mapping Features

### Co-location

Co-location ensures related data is stored together for efficient joins:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("ArtistId")
)
public class Album { ... }
```

### Indexes

Create indexes for faster lookups:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    indexes = {
        @Index(value = "IDX_ARTIST_NAME", columns = { @ColumnRef("Name") })
    }
)
public class Artist { ... }
```

### Composite Primary Keys

Use multiple `@Id` annotations for composite keys:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default"),
    colocateBy = @ColumnRef("PlaylistId")
)
public class PlaylistTrack {
    @Id
    @Column(value = "PlaylistId", nullable = false)
    private Integer playlistId;

    @Id
    @Column(value = "TrackId", nullable = false)
    private Integer trackId;
    
    // Constructors, getters, setters...
}
```

## Handling NULL Values

For nullable fields:

- Use object types (Integer) instead of primitives (int)
- Mark columns as nullable: `@Column(value = "Name", nullable = true)`
- Check for null values in your code

```java
// Safe handling of potentially null values
if (artist.getName() != null) {
    // Process artist name
}
```

## Best Practices

1. **Consistent Naming**: Follow a consistent naming convention for Java fields and database columns
2. **Case Sensitivity**: Column names in annotations must match database column names exactly (including case)
3. **Complete Objects**: Include all necessary fields in your POJOs
4. **Proper Types**: Use appropriate Java types for columns
5. **Null Handling**: Use object types for nullable fields
6. **Transactions**: Use transactions for operations that modify multiple related objects

## Practical Example: Music Catalog

The following example demonstrates a complete workflow for a music catalog system:

```java
public class MusicCatalog {
    private final IgniteClient client;
    
    public MusicCatalog(IgniteClient client) {
        this.client = client;
    }
    
    public Artist addArtist(Artist artist) {
        Table artistTable = client.tables().table("Artist");
        RecordView<Artist> artistView = artistTable.recordView(Artist.class);
        artistView.upsert(null, artist);
        return artist;
    }
    
    public Album addAlbum(Album album) {
        Table albumTable = client.tables().table("Album");
        RecordView<Album> albumView = albumTable.recordView(Album.class);
        albumView.upsert(null, album);
        return album;
    }
    
    public Artist getArtistById(int artistId) {
        Table artistTable = client.tables().table("Artist");
        KeyValueView<Integer, Artist> keyValueView = 
            artistTable.keyValueView(Integer.class, Artist.class);
        return keyValueView.get(null, artistId);
    }
    
    public List<Album> getAlbumsByArtist(int artistId) {
        Table albumTable = client.tables().table("Album");
        RecordView<Album> albumView = albumTable.recordView(Album.class);
        
        // SQL filter to find only albums for this artist
        Condition condition = Condition.eq("ArtistId", artistId);
        
        List<Album> artistAlbums = new ArrayList<>();
        albumView.getAll(null, condition).forEachRemaining(artistAlbums::add);
        
        return artistAlbums;
    }
    
    public boolean addArtistWithAlbums(Artist artist, List<Album> albums) {
        return client.transactions().runInTransaction(tx -> {
            try {
                // Get table references
                Table artistTable = client.tables().table("Artist");
                RecordView<Artist> artistView = artistTable.recordView(Artist.class);
                
                Table albumTable = client.tables().table("Album");
                RecordView<Album> albumView = albumTable.recordView(Album.class);
                
                // Insert artist
                artistView.upsert(tx, artist);
                
                // Insert all albums
                albumView.upsertAll(tx, albums);
                
                return true;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return false;
            }
        });
    }
}
```

## When to Use This Pattern

The POJO-to-Table mapping pattern is best suited when:

- You need type safety and object-oriented programming
- Your schema is relatively stable
- You have control over both database schema and code
- You prefer working directly with objects rather than SQL

If you need more flexibility with SQL or have to deal with complex queries and joins, consider using the [SQL-to-POJO pattern](SQL-to-POJO-Pattern.md) as a complementary approach.
