# Chinook Database Model

This document explains the Chinook database model used in this demo application. The Chinook database represents a digital media store, including tables for artists, albums, tracks, and more.

## Database Schema

```mermaid
erDiagram
    Artist ||--o{ Album : creates
    Album ||--o{ Track : contains
    MediaType ||--o{ Track : format
    Genre ||--o{ Track : categorizes
    
    Artist {
        int ArtistId PK
        string Name
    }
    
    Album {
        int AlbumId PK
        string Title
        int ArtistId FK
    }
    
    Track {
        int TrackId PK
        string Name
        int AlbumId FK
        int MediaTypeId FK
        int GenreId FK
        string Composer
        int Milliseconds
        int Bytes
        decimal UnitPrice
    }
    
    Genre {
        int GenreId PK
        string Name
    }
    
    MediaType {
        int MediaTypeId PK
        string Name
    }
```

## Entity Descriptions

### Artist

Represents music artists (bands or individuals) who create albums.

**Fields**:
- `ArtistId`: Unique identifier (Primary Key)
- `Name`: Artist name

**Model Class**: `Artist.java`

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
    
    // Methods omitted for brevity
}
```

### Album

Represents music albums created by artists.

**Fields**:
- `AlbumId`: Unique identifier (Primary Key)
- `Title`: Album title
- `ArtistId`: Reference to the Artist (Foreign Key)

**Model Class**: `Album.java`

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
    
    // Methods omitted for brevity
}
```

### Track

Represents individual songs or compositions on an album.

**Fields**:
- `TrackId`: Unique identifier (Primary Key)
- `Name`: Track name
- `AlbumId`: Reference to the Album (Foreign Key)
- `MediaTypeId`: Reference to the MediaType (Foreign Key)
- `GenreId`: Reference to the Genre (Foreign Key)
- `Composer`: Name of the composer
- `Milliseconds`: Track length in milliseconds
- `Bytes`: Track size in bytes
- `UnitPrice`: Price of the track

**Model Class**: `Track.java`

```java
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("AlbumId")
)
public class Track {
    @Id
    @Column(value = "TrackId", nullable = false)
    private Integer trackId;

    @Column(value = "Name", nullable = false)
    private String name;

    @Id
    @Column(value = "AlbumId", nullable = true)
    private Integer albumId;

    @Column(value = "MediaTypeId", nullable = false)
    private Integer mediaTypeId;

    @Column(value = "GenreId", nullable = true)
    private Integer genreId;

    @Column(value = "Composer", nullable = true)
    private String composer;

    @Column(value = "Milliseconds", nullable = false)
    private Integer milliseconds;

    @Column(value = "Bytes", nullable = true)
    private Integer bytes;

    @Column(value = "UnitPrice", nullable = false)
    private BigDecimal unitPrice;
    
    // Methods omitted for brevity
}
```

### Genre

Represents music genres for classification.

**Fields**:
- `GenreId`: Unique identifier (Primary Key)
- `Name`: Genre name

**Model Class**: `Genre.java`

```java
@Table(
        zone = @Zone(value = "ChinookReplicated", storageProfiles = "default")
)
public class Genre {
    @Id
    @Column(value = "GenreId", nullable = false)
    private Integer genreId;

    @Column(value = "Name", nullable = true)
    private String name;
    
    // Methods omitted for brevity
}
```

### MediaType

Represents different media formats (e.g., MPEG, AAC).

**Fields**:
- `MediaTypeId`: Unique identifier (Primary Key)
- `Name`: Media type name

**Model Class**: `MediaType.java`

```java
@Table(
        zone = @Zone(value = "ChinookReplicated", storageProfiles = "default")
)
public class MediaType {
    @Id
    @Column(value = "MediaTypeId", nullable = false)
    private Integer mediaTypeId;

    @Column(value = "Name", nullable = true)
    private String name;
    
    // Methods omitted for brevity
}
```

## Distribution Strategy

The Chinook model uses two different distribution zones:

1. **Chinook Zone (2 replicas)**:
   - `Artist`: Primary entity
   - `Album`: Co-located with Artist by ArtistId
   - `Track`: Co-located with Album by AlbumId

2. **ChinookReplicated Zone (3 replicas)**:
   - `Genre`: Reference data, frequently read
   - `MediaType`: Reference data, frequently read

## Co-location Strategy

This model demonstrates a hierarchical co-location strategy:

1. `Artist` is the root entity
2. `Album` is co-located with the corresponding `Artist`
3. `Track` is co-located with the corresponding `Album`

This ensures that when you query data across these entities (e.g., all tracks by a specific artist), the data is already located on the same physical node, minimizing network transfers and improving performance.

## Example Queries

Here are some example queries that demonstrate the relationships between entities:

### Finding Albums by Artist

```java
client.sql().execute(null,
        "SELECT a.Title, ar.Name as ArtistName " +
        "FROM Album a JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
        "WHERE ar.Name = ?", artistName)
```

### Finding Tracks with Album and Artist Information

```java
client.sql().execute(null,
        "SELECT t.Name as Track, t.Composer, a.Title as Album, ar.Name as Artist " +
        "FROM Track t " +
        "JOIN Album a ON t.AlbumId = a.AlbumId " +
        "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
        "WHERE ar.Name = ?", artistName)
```

## Using the Model Classes

The model classes are used throughout the application for data operations:

### Creating a New Artist

```java
Artist queen = new Artist(6, "Queen");
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artistView = artistTable.recordView(Artist.class);
artistView.upsert(null, queen);
```

### Creating a New Album

```java
Album newAlbum = new Album(6, "A Night at the Opera", 6);
Table albumTable = client.tables().table("Album");
RecordView<Album> albumView = albumTable.recordView(Album.class);
albumView.upsert(null, newAlbum);
```

### Creating Tracks

```java
Track track1 = new Track(
        6,
        "Bohemian Rhapsody",
        6,
        1,
        1,
        "Freddie Mercury",
        354947,
        5733664,
        new BigDecimal("0.99")
);

Table trackTable = client.tables().table("Track");
RecordView<Track> trackView = trackTable.recordView(Track.class);
trackView.upsert(null, track1);
```

## Extending the Model

To extend the Chinook model with additional entities:

1. Create a new Java class with appropriate annotations
2. Define the class fields with @Column and @Id annotations
3. Include appropriate constructors, getters, and setters
4. Update the `TableUtils.createTables()` method to create the new table
5. Add methods to `ChinookUtils.java` or `DataUtils.java` for common operations on the new entity

For example, to add a `Playlist` entity:

```java
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default")
)
public class Playlist {
    @Id
    @Column(value = "PlaylistId", nullable = false)
    private Integer playlistId;

    @Column(value = "Name", nullable = false)
    private String name;
    
    // Constructors, getters, setters...
}
```

## Further Reading

- [Original Chinook Database](https://github.com/lerocha/chinook-database)
- [Database Design Patterns for Distributed Systems](https://ignite.apache.org/docs/latest/concepts/data-modeling)
