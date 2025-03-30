# SQL-to-POJO Pattern

The SQL-to-POJO pattern provides a flexible and robust approach to working with database data in an object-oriented way. Instead of relying on automatic mapping between POJOs and tables, this pattern uses SQL queries to retrieve data and manually converts the results to Java objects.

## Key Principles of the SQL-to-POJO Pattern

1. Use SQL for data access (retrieval, insertion, updates, and deletion)
2. Manually convert SQL result rows to Java objects
3. Use Java objects for business logic and data manipulation
4. Maintain clear separation between data access and business logic layers

## Implementation Steps

### 1. Define Your POJOs

Create Java classes that represent your domain model without needing to match database schema exactly:

```java
public class Artist {
    private Integer artistId;
    private String name;
    
    // Constructors, getters, setters...
}
```

Notice that, unlike the POJO-to-Table approach, these classes don't require any special annotations.

### 2. Create Data Access Methods

Implement methods that use SQL to access the database and manually convert results to POJOs:

```java
// Get all artists
public List<Artist> getAllArtists(IgniteClient client) {
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

// Get artist by ID
public Artist getArtistById(IgniteClient client, int artistId) {
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
```

### 3. Handle Related Object Loading

For related objects, implement methods that load relationships:

```java
// Get albums for an artist
public List<Album> getAlbumsForArtist(IgniteClient client, int artistId) {
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

// Load complete artist with albums and tracks
public ArtistWithDetails getArtistWithDetails(IgniteClient client, int artistId) {
    // Get the artist
    Artist artist = getArtistById(client, artistId);
    if (artist == null) {
        return null;
    }
    
    // Create the detailed artist object
    ArtistWithDetails artistWithDetails = new ArtistWithDetails(artist);
    
    // Load albums
    List<Album> albums = getAlbumsForArtist(client, artistId);
    artistWithDetails.setAlbums(albums);
    
    // Load tracks for each album
    for (Album album : albums) {
        List<Track> tracks = getTracksForAlbum(client, album.getAlbumId());
        artistWithDetails.addTracksForAlbum(album.getAlbumId(), tracks);
    }
    
    return artistWithDetails;
}
```

### 4. Implement Modification Operations

```java
// Insert a new artist
public boolean insertArtist(IgniteClient client, Artist artist) {
    try {
        client.sql().execute(null,
            "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
            artist.getArtistId(), artist.getName());
        return true;
    } catch (Exception e) {
        System.err.println("Error inserting artist: " + e.getMessage());
        return false;
    }
}

// Update an existing artist
public boolean updateArtist(IgniteClient client, Artist artist) {
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

// Delete an artist
public boolean deleteArtist(IgniteClient client, int artistId) {
    try {
        client.sql().execute(null,
            "DELETE FROM Artist WHERE ArtistId = ?", 
            artistId);
        return true;
    } catch (Exception e) {
        System.err.println("Error deleting artist: " + e.getMessage());
        return false;
    }
}
```

### 5. Use Transaction Support

```java
public boolean createArtistWithAlbums(IgniteClient client, Artist artist, List<Album> albums) {
    try {
        return client.transactions().runInTransaction(tx -> {
            // Insert the artist
            client.sql().execute(tx,
                "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
                artist.getArtistId(), artist.getName());
            
            // Insert all albums
            for (Album album : albums) {
                client.sql().execute(tx,
                    "INSERT INTO Album (AlbumId, Title, ArtistId) VALUES (?, ?, ?)",
                    album.getAlbumId(), album.getTitle(), album.getArtistId());
            }
            
            return true;
        });
    } catch (Exception e) {
        System.err.println("Transaction failed: " + e.getMessage());
        return false;
    }
}
```

## Handling NULL Values and Errors

A major advantage of the SQL-to-POJO approach is the explicit handling of NULL values and other mapping issues:

```java
// Safe handling of NULL values
client.sql().execute(null, "SELECT * FROM Track WHERE TrackId = ?", trackId)
    .forEachRemaining(row -> {
        Track track = new Track();
        track.setTrackId(row.intValue("TrackId"));
        track.setName(row.stringValue("Name"));
        
        // Safe handling of potentially NULL values
        try { track.setAlbumId(row.intValue("AlbumId")); } 
        catch (Exception e) { /* Field is NULL */ }
        
        try { track.setComposer(row.stringValue("Composer")); } 
        catch (Exception e) { /* Field is NULL */ }
        
        try { track.setGenreId(row.intValue("GenreId")); } 
        catch (Exception e) { /* Field is NULL */ }
    });
```

## Advantages of the SQL-to-POJO Pattern

1. **Flexibility**: Direct control over SQL gives you full access to the database's capabilities
2. **Resilience**: Immunity to case sensitivity issues and schema mismatches
3. **Performance**: Ability to optimize queries and retrieve only needed fields
4. **SQL Power**: Leverage complex SQL operations like joins, aggregations, and window functions
5. **Explicit NULL Handling**: Clear handling of NULL values through defensive programming
6. **Separation of Concerns**: Clean separation between data access and object model

## When to Use SQL-to-POJO Pattern

This pattern works best when:
- You need more control over database queries
- Your application needs complex SQL operations
- You're dealing with existing schemas you don't control
- You've encountered issues with direct POJO mapping
- You need to handle NULL values explicitly
- You want to ensure consistent error handling

## Practical Example: Sales Analysis

```java
public List<ArtistSales> getTopSellingArtists(IgniteClient client, int limit) {
    List<ArtistSales> artistSales = new ArrayList<>();
    
    // Complex SQL query with joins, grouping, and aggregation
    client.sql().execute(null,
        "SELECT ar.ArtistId, ar.Name as ArtistName, " +
        "COUNT(il.InvoiceLineId) as TracksSold, " +
        "SUM(il.UnitPrice * il.Quantity) as Revenue " +
        "FROM Artist ar " +
        "JOIN Album a ON ar.ArtistId = a.ArtistId " +
        "JOIN Track t ON a.AlbumId = t.AlbumId " +
        "JOIN InvoiceLine il ON t.TrackId = il.TrackId " +
        "GROUP BY ar.ArtistId, ar.Name " +
        "ORDER BY Revenue DESC " +
        "LIMIT ?", 
        limit)
    .forEachRemaining(row -> {
        ArtistSales sales = new ArtistSales();
        sales.setArtistId(row.intValue("ArtistId"));
        sales.setArtistName(row.stringValue("ArtistName"));
        sales.setTracksSold(row.longValue("TracksSold"));
        sales.setRevenue(row.decimalValue("Revenue"));
        artistSales.add(sales);
    });
    
    return artistSales;
}
```

This approach gives you the full power of SQL combined with the convenience and structure of object-oriented programming, offering a robust alternative to direct POJO-to-Table mapping.
