package com.example.model;

import org.apache.ignite.catalog.annotations.*;

import java.math.BigDecimal;

/**
 * Represents a track (song) in the Chinook database.
 * This class maps to the Track table which contains information about music tracks.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("AlbumId"),
        indexes = {
            @Index(value = "IFK_TrackAlbumId", columns = { @ColumnRef("AlbumId") }),
            @Index(value = "IFK_TrackGenreId", columns = { @ColumnRef("GenreId") }),
            @Index(value = "IFK_TrackMediaTypeId", columns = { @ColumnRef("MediaTypeId") })
        }
)
public class Track {
    // Primary key field
    @Id
    @Column(value = "TrackId", nullable = false)
    private Integer trackId;

    @Column(value = "Name", nullable = false)
    private String name;

    // Foreign keys
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

    /**
     * Default constructor required for serialization
     */
    public Track() { }

    /**
     * Constructs a Track with specified details
     *
     * @param trackId The unique identifier for the track
     * @param name The name of the track
     * @param albumId The ID of the album this track belongs to
     * @param mediaTypeId The ID of the media type (e.g., MPEG, AAC)
     * @param genreId The ID of the music genre
     * @param composer The composer of the track
     * @param milliseconds The length of the track in milliseconds
     * @param bytes The size of the track in bytes
     * @param unitPrice The price per unit
     */
    public Track(Integer trackId, String name, Integer albumId, Integer mediaTypeId,
                 Integer genreId, String composer, Integer milliseconds,
                 Integer bytes, BigDecimal unitPrice) {
        this.trackId = trackId;
        this.name = name;
        this.albumId = albumId;
        this.mediaTypeId = mediaTypeId;
        this.genreId = genreId;
        this.composer = composer;
        this.milliseconds = milliseconds;
        this.bytes = bytes;
        this.unitPrice = unitPrice;
    }

    // Getters and setters

    public Integer getTrackId() { return trackId; }
    public void setTrackId(Integer trackId) { this.trackId = trackId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAlbumId() { return albumId; }
    public void setAlbumId(Integer albumId) { this.albumId = albumId; }

    public Integer getMediaTypeId() { return mediaTypeId; }
    public void setMediaTypeId(Integer mediaTypeId) { this.mediaTypeId = mediaTypeId; }

    public Integer getGenreId() { return genreId; }
    public void setGenreId(Integer genreId) { this.genreId = genreId; }

    public String getComposer() { return composer; }
    public void setComposer(String composer) { this.composer = composer; }

    public Integer getMilliseconds() { return milliseconds; }
    public void setMilliseconds(Integer milliseconds) { this.milliseconds = milliseconds; }

    public Integer getBytes() { return bytes; }
    public void setBytes(Integer bytes) { this.bytes = bytes; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    @Override
    public String toString() {
        return "Track{" +
                "trackId=" + trackId +
                ", name='" + name + '\'' +
                ", albumId=" + albumId +
                ", mediaTypeId=" + mediaTypeId +
                ", genreId=" + genreId +
                ", composer='" + composer + '\'' +
                ", milliseconds=" + milliseconds +
                ", bytes=" + bytes +
                ", unitPrice=" + unitPrice +
                '}';
    }
}