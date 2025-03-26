package com.example.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Represents an artist in the Chinook database.
 * This class is mapped to the Artist table which contains information about music artists.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default")
)
public class Artist {
    // Primary key field
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;

    @Column(value = "Name", nullable = true)
    private String name;

    /**
     * Default constructor required for serialization
     */
    public Artist() { }

    /**
     * Constructs an Artist with specified details
     *
     * @param artistId The unique identifier for the artist
     * @param name The name of the artist
     */
    public Artist(Integer artistId, String name) {
        this.artistId = artistId;
        this.name = name;
    }

    // Getters and setters

    /**
     * @return The artist's unique identifier
     */
    public Integer getArtistId() {
        return artistId;
    }

    /**
     * @param artistId The artist's unique identifier to set
     */
    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }

    /**
     * @return The artist's name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The artist's name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Artist{" +
                "artistId=" + artistId +
                ", name='" + name + '\'' +
                '}';
    }
}