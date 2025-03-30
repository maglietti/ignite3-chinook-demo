# SQL-to-POJO Pattern in Apache Ignite 3

This document explains how to use the SQL-to-POJO pattern in Apache Ignite 3 to work with database data in an object-oriented way. This pattern uses SQL queries to retrieve and manipulate data, and then manually converts the results to Java objects.

## Overview

The SQL-to-POJO pattern provides these benefits:

- Full SQL flexibility for complex queries and operations
- Explicit control over database interactions
- Resilience to schema mismatches and case sensitivity issues
- Ability to optimize database operations at the SQL level

## Key Principles

1. Use SQL for data access (retrieval, insertion, updates, deletion)
2. Manually convert SQL result rows to Java objects
3. Use Java objects for business logic and data manipulation
4. Maintain separation between data access and business logic layers

## Defining Your POJOs

Create Java classes that represent your domain model without needing annotations:

```java
public class Artist {
    private Integer artistId;
    private String name;
    
    // Constructors
    public Artist() {}
    
    public Artist(Integer artistId, String name) {
        this.artistId = artistId;
        this.name = name;
    }
    
    // Getters and setters
    public Integer getArtistId() { return artistId; }
    public void setArtistId(Integer artistId) { this.artistId = artistId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

Similarly for related entities:

```java
public class Album {
    private Integer albumId;
    private String title;
    private Integer artistId;
    
    // Constructors, getters, setters...
}
```

## Basic Data Access Operations

### Querying Data and Converting to POJOs

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

### Inserting Data

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
```

### Updating Data

```java
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
```

### Deleting Data

```java
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

## Handling Relationships

### Loading Related Objects

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
```

### Composite Data Structures

For cases where you need to combine related data:

```java
// A composite class to hold artist with their albums
public class ArtistWithAlbums {
    private Artist artist;
    private List<Album> albums;
    
    // Constructors, getters, setters...
}

// Load an artist with all their albums
public ArtistWithAlbums getArtistWithAlbums(IgniteClient client, int artistId) {
    // Get the artist
    Artist artist = getArtistById(client, artistId);
    if (artist == null) {
        return null;
    }
    
    // Get the albums for this artist
    List<Album> albums = getAlbumsForArtist(client, artistId);
    
    // Create and return the composite object
    ArtistWithAlbums result = new ArtistWithAlbums();
    result.setArtist(artist);
    result.setAlbums(albums);
    
    return result;
}
```

## Advanced SQL Operations

The SQL-to-POJO pattern really shines with complex SQL operations:

### Aggregation Queries

```java
// Get album counts by artist
public List<ArtistAlbumCount> getArtistAlbumCounts(IgniteClient client) {
    List<ArtistAlbumCount> results = new ArrayList<>();
    
    client.sql().execute(null,
        "SELECT ar.ArtistId, ar.Name, COUNT(a.AlbumId) as AlbumCount " +
        "FROM Artist ar " +
        "LEFT JOIN Album a ON ar.ArtistId = a.ArtistId " +
        "GROUP BY ar.ArtistId, ar.Name " +
        "ORDER BY AlbumCount DESC")
        .forEachRemaining(row -> {
            ArtistAlbumCount count = new ArtistAlbumCount();
            count.setArtistId(row.intValue("ArtistId"));
            count.setArtistName(row.stringValue("Name"));
            count.setAlbumCount(row.longValue("AlbumCount"));
            results.add(count);
        });
    
    return results;
}
```

### Multi-Table Joins

```java
// Get tracks with album and artist information
public List<TrackDetail> getTracksWithDetails(IgniteClient client, int limit) {
    List<TrackDetail> results = new ArrayList<>();
    
    client.sql().execute(null,
        "SELECT t.TrackId, t.Name as TrackName, " +
        "a.Title as AlbumTitle, ar.Name as ArtistName, " +
        "g.Name as GenreName " +
        "FROM Track t " +
        "JOIN Album a ON t.AlbumId = a.AlbumId " +
        "JOIN Artist ar ON a.ArtistId = ar.ArtistId " +
        "LEFT JOIN Genre g ON t.GenreId = g.GenreId " +
        "ORDER BY t.TrackId " +
        "LIMIT ?", 
        limit)
        .forEachRemaining(row -> {
            TrackDetail track = new TrackDetail();
            track.setTrackId(row.intValue("TrackId"));
            track.setTrackName(row.stringValue("TrackName"));
            track.setAlbumTitle(row.stringValue("AlbumTitle"));
            track.setArtistName(row.stringValue("ArtistName"));
            track.setGenreName(row.stringValue("GenreName"));
            results.add(track);
        });
    
    return results;
}
```

## Transaction Support

Using transactions with the SQL-to-POJO pattern:

```java
public boolean createArtistWithAlbums(IgniteClient client, Artist artist, List<Album> albums) {
    try {
        return client.transactions().runInTransaction(tx -> {
            try {
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
            } catch (Exception e) {
                System.err.println("Transaction error: " + e.getMessage());
                return false;
            }
        });
    } catch (Exception e) {
        System.err.println("Transaction failed: " + e.getMessage());
        return false;
    }
}
```

## Handling NULL Values and Errors

Explicit handling of NULL values is a major advantage of this pattern:

```java
// Safe handling of NULL values
try {
    client.sql().execute(null, "SELECT * FROM Track WHERE TrackId = ?", trackId)
        .forEachRemaining(row -> {
            Track track = new Track();
            track.setTrackId(row.intValue("TrackId"));
            track.setName(row.stringValue("Name"));
            
            // Safe handling of potentially NULL values
            try { 
                track.setAlbumId(row.intValue("AlbumId")); 
            } catch (Exception e) { 
                // Field is NULL, set to null in our object
                track.setAlbumId(null);
            }
            
            try { 
                track.setComposer(row.stringValue("Composer")); 
            } catch (Exception e) { 
                // Field is NULL, set to null in our object
                track.setComposer(null);
            }
            
            // Add to results
            tracks.add(track);
        });
} catch (Exception e) {
    System.err.println("Error processing track: " + e.getMessage());
}
```

## Practical Example: Music Catalog System

Here's a complete example of a Music Catalog system using the SQL-to-POJO pattern:

```java
public class MusicCatalogDao {
    private final IgniteClient client;
    
    public MusicCatalogDao(IgniteClient client) {
        this.client = client;
    }
    
    // Artist methods
    public Artist getArtistById(int artistId) {
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
    
    public List<Artist> getAllArtists() {
        List<Artist> artists = new ArrayList<>();
        
        client.sql().execute(null, "SELECT ArtistId, Name FROM Artist ORDER BY Name")
            .forEachRemaining(row -> {
                Artist artist = new Artist();
                artist.setArtistId(row.intValue("ArtistId"));
                artist.setName(row.stringValue("Name"));
                artists.add(artist);
            });
        
        return artists;
    }
    
    public boolean addArtist(Artist artist) {
        try {
            client.sql().execute(null,
                "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
                artist.getArtistId(), artist.getName());
            return true;
        } catch (Exception e) {
            System.err.println("Error adding artist: " + e.getMessage());
            return false;
        }
    }
    
    // Album methods
    public List<Album> getAlbumsByArtist(int artistId) {
        List<Album> albums = new ArrayList<>();
        
        client.sql().execute(null, 
            "SELECT AlbumId, Title, ArtistId FROM Album WHERE ArtistId = ? ORDER BY Title", 
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
    
    public boolean addAlbum(Album album) {
        try {
            client.sql().execute(null,
                "INSERT INTO Album (AlbumId, Title, ArtistId) VALUES (?, ?, ?)",
                album.getAlbumId(), album.getTitle(), album.getArtistId());
            return true;
        } catch (Exception e) {
            System.err.println("Error adding album: " + e.getMessage());
            return false;
        }
    }
    
    // Composite queries
    public ArtistWithAlbums getArtistWithAlbums(int artistId) {
        Artist artist = getArtistById(artistId);
        if (artist == null) {
            return null;
        }
        
        List<Album> albums = getAlbumsByArtist(artistId);
        
        ArtistWithAlbums result = new ArtistWithAlbums();
        result.setArtist(artist);
        result.setAlbums(albums);
        
        return result;
    }
    
    // Transaction example
    public boolean addArtistWithAlbums(Artist artist, List<Album> albums) {
        try {
            return client.transactions().runInTransaction(tx -> {
                try {
                    // Insert artist
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
                } catch (Exception e) {
                    System.err.println("Transaction error: " + e.getMessage());
                    return false;
                }
            });
        } catch (Exception e) {
            System.err.println("Transaction failed: " + e.getMessage());
            return false;
        }
    }
    
    // Advanced analytics
    public List<ArtistSales> getTopSellingArtists(int limit) {
        List<ArtistSales> results = new ArrayList<>();
        
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
                results.add(sales);
            });
        
        return results;
    }
}
```

## Business Logic Layer Example

This pattern encourages separation of concerns. Here's an example business service that uses the DAO:

```java
public class MusicCatalogService {
    private final MusicCatalogDao dao;
    
    public MusicCatalogService(IgniteClient client) {
        this.dao = new MusicCatalogDao(client);
    }
    
    public List<ArtistSummary> getPopularArtists(int count) {
        List<ArtistSales> sales = dao.getTopSellingArtists(count);
        
        // Transform sales data into business-oriented summaries
        return sales.stream()
            .map(sale -> {
                ArtistSummary summary = new ArtistSummary();
                summary.setId(sale.getArtistId());
                summary.setName(sale.getArtistName());
                summary.setPopularity(calculatePopularity(sale.getTracksSold()));
                summary.setTotalRevenue(formatRevenue(sale.getRevenue()));
                return summary;
            })
            .collect(Collectors.toList());
    }
    
    public boolean createNewArtist(String name, List<String> albumTitles) {
        // Get next available IDs
        int artistId = getNextArtistId();
        
        // Create artist
        Artist artist = new Artist(artistId, name);
        
        // Create albums
        List<Album> albums = new ArrayList<>();
        int albumId = getNextAlbumId();
        
        for (String title : albumTitles) {
            albums.add(new Album(albumId++, title, artistId));
        }
        
        // Add artist with albums in a transaction
        return dao.addArtistWithAlbums(artist, albums);
    }
    
    // Helper methods
    private int getNextArtistId() {
        // Implementation to get next available ID
        return 0;
    }
    
    private int getNextAlbumId() {
        // Implementation to get next available ID
        return 0;
    }
    
    private int calculatePopularity(long tracksSold) {
        // Convert sales to popularity rating (1-10)
        return (int) Math.min(10, Math.max(1, tracksSold / 100));
    }
    
    private String formatRevenue(java.math.BigDecimal revenue) {
        // Format revenue as currency string
        return "$" + revenue.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
```

## When to Use This Pattern

The SQL-to-POJO pattern is best suited when:

- You need direct control over SQL queries
- You're working with complex queries involving multiple tables
- You need specialized database operations (aggregations, window functions, etc.)
- You're dealing with an existing database schema
- You need explicit handling of NULL values and edge cases
- You want clear separation between data access and business logic

This pattern works as a complementary approach to the [POJO-to-Table pattern](POJO-to-Table-Pattern.md) and both can be used within the same application
