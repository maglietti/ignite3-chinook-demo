package com.example.model;

import org.apache.ignite.catalog.annotations.*;

/**
 * Represents a playlist track in the Chinook database.
 * This class maps to the PlaylistTrack table which contains the many-to-many relationship
 * between playlists and tracks.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("PlaylistId"),
        indexes = {
            @Index(value = "IFK_PlaylistTrackPlaylistId", columns = { @ColumnRef("PlaylistId") }),
            @Index(value = "IFK_PlaylistTrackTrackId", columns = { @ColumnRef("TrackId") })
        }
)
public class PlaylistTrack {
    // Composite primary key fields
    @Id
    @Column(value = "PlaylistId", nullable = false)
    private Integer playlistId;

    @Id
    @Column(value = "TrackId", nullable = false)
    private Integer trackId;

    /**
     * Default constructor required for serialization
     */
    public PlaylistTrack() { }

    /**
     * Constructs a PlaylistTrack with specified details
     *
     * @param playlistId The ID of the playlist
     * @param trackId The ID of the track
     */
    public PlaylistTrack(Integer playlistId, Integer trackId) {
        this.playlistId = playlistId;
        this.trackId = trackId;
    }

    // Getters and setters

    public Integer getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(Integer playlistId) {
        this.playlistId = playlistId;
    }

    public Integer getTrackId() {
        return trackId;
    }

    public void setTrackId(Integer trackId) {
        this.trackId = trackId;
    }

    @Override
    public String toString() {
        return "PlaylistTrack{" +
                "playlistId=" + playlistId +
                ", trackId=" + trackId +
                '}';
    }
}
