package com.example.model;

import org.apache.ignite.catalog.annotations.*;

/**
 * Represents an album in the Chinook database.
 * This class maps to the Album table which contains information about music albums.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("ArtistId"),
        indexes = {
            @Index(value = "IFK_AlbumArtistId", columns = { @ColumnRef("ArtistId") })
        }
)
public class Album {
    // Primary key field
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer albumId;

    @Column(value = "Title", nullable = false)
    private String title;

    // Foreign key to Artist table
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;

    /**
     * Default constructor required for serialization
     */
    public Album() { }

    /**
     * Constructs an Album with specified details
     *
     * @param albumId The unique identifier for the album
     * @param title The title of the album
     * @param artistId The ID of the artist who created the album
     */
    public Album(Integer albumId, String title, Integer artistId) {
        this.albumId = albumId;
        this.title = title;
        this.artistId = artistId;
    }

    // Getters and setters

    /**
     * @return The album's unique identifier
     */
    public Integer getAlbumId() {
        return albumId;
    }

    /**
     * @param albumId The album's unique identifier to set
     */
    public void setAlbumId(Integer albumId) {
        this.albumId = albumId;
    }

    /**
     * @return The album's title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title The album's title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The ID of the artist who created this album
     */
    public Integer getArtistId() {
        return artistId;
    }

    /**
     * @param artistId The ID of the artist to set
     */
    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }

    @Override
    public String toString() {
        return "Album{" +
                "albumId=" + albumId +
                ", title='" + title + '\'' +
                ", artistId=" + artistId +
                '}';
    }
}