# Comparison of POJO-to-Table vs SQL-to-POJO Approaches in Apache Ignite 3

Comparison of the Java classes `Main_POJO.java` and `Main.java`, They demonstrate two different approaches for working with data in Apache Ignite 3.

## Key Differences

### 1. Data Access Methods

**Main_POJO.java (POJO-to-Table approach)**:

```java
// Get tables and their record views
Table artistTable = client.tables().table("Artist");
RecordView<Artist> artistView = artistTable.recordView(Artist.class);
KeyValueView<Integer, Artist> artistKeyValueView = artistTable.keyValueView(Integer.class, Artist.class);

// Finding an artist using KeyValueView
Artist targetArtist = artistKeyValueView.get(null, targetArtistId);
```

**Main.java (SQL-to-POJO approach)**:

```java
// Finding an artist using SQL
var artistResult = client.sql().execute(null,
        "SELECT ArtistId, Name FROM Artist WHERE ArtistId = ?",
        targetArtistId);

if (artistResult.hasNext()) {
    var row = artistResult.next();
    targetArtist = new Artist();
    targetArtist.setArtistId(row.intValue("ArtistId"));
    targetArtist.setName(row.stringValue("Name"));
}
```

### 2. Retrieving Multiple Records

**POJO-to-Table approach**:

```java
// Use SQL to get all artist IDs
List<Integer> artistIds = new ArrayList<>();
client.sql().execute(null, "SELECT ArtistId FROM Artist")
    .forEachRemaining(row -> artistIds.add(row.intValue("ArtistId")));

// Get all artists by ID using KeyValueView
List<Artist> artists = new ArrayList<>();
for (Integer id : artistIds) {
    Artist artist = artistKeyValueView.get(null, id);
    if (artist != null) {
        artists.add(artist);
    }
}
```

**SQL-to-POJO approach**:

```java
// List all artists using SQL and convert to POJOs
List<Artist> artists = new ArrayList<>();
client.sql().execute(null, "SELECT ArtistId, Name FROM Artist")
    .forEachRemaining(row -> {
        Artist artist = new Artist();
        artist.setArtistId(row.intValue("ArtistId"));
        artist.setName(row.stringValue("Name"));
        artists.add(artist);
    });
```

### 3. Handling Relationships

**POJO-to-Table approach**:

```java
// First get album IDs for the artist
List<Integer> albumIds = new ArrayList<>();
client.sql().execute(null, "SELECT AlbumId FROM Album WHERE ArtistId = ?", targetArtistId)
    .forEachRemaining(row -> albumIds.add(row.intValue("AlbumId")));

// Then get Album POJOs by ID using SQL rather than directly using RecordView
List<Album> artistAlbums = new ArrayList<>();
for (Integer albumId : albumIds) {
    client.sql().execute(null, "SELECT * FROM Album WHERE AlbumId = ?", albumId)
        .forEachRemaining(row -> {
            Album album = new Album();
            album.setAlbumId(row.intValue("AlbumId"));
            album.setTitle(row.stringValue("Title"));
            album.setArtistId(row.intValue("ArtistId"));
            artistAlbums.add(album);
        });
}
```

**SQL-to-POJO approach**:

```java
// Directly fetch albums for the artist using a single SQL query
List<Album> artistAlbums = new ArrayList<>();
client.sql().execute(null,
        "SELECT AlbumId, Title, ArtistId FROM Album WHERE ArtistId = ?",
        targetArtistId)
    .forEachRemaining(row -> {
        Album album = new Album();
        album.setAlbumId(row.intValue("AlbumId"));
        album.setTitle(row.stringValue("Title"));
        album.setArtistId(row.intValue("ArtistId"));
        artistAlbums.add(album);
    });
```

### 4. NULL Value Handling

**Both approaches use similar defensive programming**:

```java
// From both POJO-to-Table and SQL-to-POJO approaches
try { track.setGenreId(row.intValue("GenreId")); } 
catch (Exception e) { /* Value is null */ }

try { track.setComposer(row.stringValue("Composer")); } 
catch (Exception e) { /* Value is null */ }
```

### 5. Data Processing and Analysis

**POJO-to-Table approach**:

```java
// Get all tracks using SQL query, then analyze using Java streams
List<Track> allTracks = new ArrayList<>();
client.sql().execute(null, "SELECT * FROM Track")
    .forEachRemaining(row -> {
        Track track = new Track();
        track.setTrackId(row.intValue("TrackId"));
        track.setName(row.stringValue("Name"));
        // Other fields...
        allTracks.add(track);
    });

// Calculate statistics using Java streams
OptionalDouble avgLength = allTracks.stream()
    .mapToInt(Track::getMilliseconds)
    .average();
    
OptionalInt minLength = allTracks.stream()
    .mapToInt(Track::getMilliseconds)
    .min();
    
OptionalInt maxLength = allTracks.stream()
    .mapToInt(Track::getMilliseconds)
    .max();
```

**SQL-to-POJO approach**:

```java
// Similar approach, but with a more focused SQL query
List<Track> allTracks = new ArrayList<>();
client.sql().execute(null, "SELECT TrackId, Name, Milliseconds FROM Track")
    .forEachRemaining(row -> {
        Track track = new Track();
        track.setTrackId(row.intValue("TrackId"));
        track.setName(row.stringValue("Name"));
        track.setMilliseconds(row.intValue("Milliseconds"));
        allTracks.add(track);
    });

// Same Java stream operations for calculations
OptionalDouble avgLength = allTracks.stream()
    .mapToInt(Track::getMilliseconds)
    .average();
```

### 6. Complex Analytics (SQL-Based in Both Approaches)

Both implementations use similar SQL-based approach for complex analytics:

```java
// From both implementations - SQL is better for complex aggregations
var result = client.sql().execute(null,
    "SELECT g.Name as Genre, COUNT(t.TrackId) as TrackCount " +
        "FROM Genre g " +
        "JOIN Track t ON g.GenreId = t.GenreId " +
        "GROUP BY g.Name " +
        "ORDER BY TrackCount DESC");
```

## Key Advantages of Each Approach

### POJO-to-Table Advantages

- Provides type safety with direct methods like `KeyValueView.get()`
- Cleaner code for simple CRUD operations
- Less manual field mapping for retrieved objects

**Example with KeyValueView**:

```java
// Clean, direct lookup by primary key
Artist artist = artistKeyValueView.get(null, id);
```

### SQL-to-POJO Advantages

- More straightforward for complex data retrieval
- Less code for retrieving multiple related records
- More flexibility for filtering data at the database level

**Example of efficient data retrieval**:

```java
// Efficient query that filters at the database level
client.sql().execute(null, 
    "SELECT TrackId, Name, Composer FROM Track WHERE Composer IS NOT NULL")
    .forEachRemaining(row -> {
        // Map to POJO
    });
```

## Practical Implementation Notes

1. **Mixing Approaches**: `Main_POJO.java` actually mixes both approaches - it uses `KeyValueView` for some operations but falls back to SQL for others.

2. **Missed Opportunities in POJO-to-Table implementation**: The code doesn't fully leverage the RecordView for bulk operations. Instead of:

   ```java
   // What's in the code
   for (Integer albumId : albumIds) {
       client.sql().execute(null, "SELECT * FROM Album WHERE AlbumId = ?", albumId)
           .forEachRemaining(row -> {
               Album album = new Album();
               // Map fields...
           });
   }
   ```

   It could have used:

   ```java
   // More efficient POJO approach
   List<Album> albums = new ArrayList<>();
   for (Integer albumId : albumIds) {
       Album album = albumView.get(null, albumId);
       if (album != null) {
           albums.add(album);
       }
   }
   ```

3. **Consistent use for simple operations**: The SQL-to-POJO approach is more consistent in its implementation, using the same pattern for all data access.

Both approaches are valid and can be mixed in the same application. The choice depends on your specific needs, with POJO-to-Table being better for object-oriented CRUD operations and SQL-to-POJO being more flexible for complex queries and data processing.
