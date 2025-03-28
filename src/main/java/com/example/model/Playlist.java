package com.example.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Represents a playlist in the Chinook database.
 * This class maps to the Playlist table which contains information about playlists.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default")
)
public class Playlist {
    // Primary key field
    @Id
    @Column(value = "PlaylistId", nullable = false)
    private Integer playlistId;

    @Column(value = "Name", nullable = true)
    private String name;

    /**
     * Default constructor required for serialization
     */
    public Playlist() { }

    /**
     * Constructs a Playlist with specified details
     *
     * @param playlistId The unique identifier for the playlist
     * @param name The name of the playlist
     */
    public Playlist(Integer playlistId, String name) {
        this.playlistId = playlistId;
        this.name = name;
    }

    // Getters and setters

    public Integer getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(Integer playlistId) {
        this.playlistId = playlistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "playlistId=" + playlistId +
                ", name='" + name + '\'' +
                '}';
    }
}
