package com.example.model;

import org.apache.ignite.catalog.annotations.*;
import java.math.BigDecimal;

/**
 * Represents an invoice line in the Chinook database.
 * This class maps to the InvoiceLine table which contains the details of each invoice.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("InvoiceId"),
        indexes = {
            @Index(value = "IFK_InvoiceLineInvoiceId", columns = { @ColumnRef("InvoiceId") }),
            @Index(value = "IFK_InvoiceLineTrackId", columns = { @ColumnRef("TrackId") })
        }
)
public class InvoiceLine {
    // Primary key field
    @Id
    @Column(value = "InvoiceLineId", nullable = false)
    private Integer invoiceLineId;

    // Foreign key to Invoice
    @Id
    @Column(value = "InvoiceId", nullable = false)
    private Integer invoiceId;

    // Foreign key to Track
    @Column(value = "TrackId", nullable = false)
    private Integer trackId;

    @Column(value = "UnitPrice", nullable = false)
    private BigDecimal unitPrice;

    @Column(value = "Quantity", nullable = false)
    private Integer quantity;

    /**
     * Default constructor required for serialization
     */
    public InvoiceLine() { }

    /**
     * Constructs an InvoiceLine with all details
     *
     * @param invoiceLineId The unique identifier for the invoice line
     * @param invoiceId The ID of the invoice this line belongs to
     * @param trackId The ID of the track that was purchased
     * @param unitPrice The price per unit
     * @param quantity The quantity purchased
     */
    public InvoiceLine(Integer invoiceLineId, Integer invoiceId, Integer trackId, 
                       BigDecimal unitPrice, Integer quantity) {
        this.invoiceLineId = invoiceLineId;
        this.invoiceId = invoiceId;
        this.trackId = trackId;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    // Getters and setters

    public Integer getInvoiceLineId() {
        return invoiceLineId;
    }

    public void setInvoiceLineId(Integer invoiceLineId) {
        this.invoiceLineId = invoiceLineId;
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Integer getTrackId() {
        return trackId;
    }

    public void setTrackId(Integer trackId) {
        this.trackId = trackId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "InvoiceLine{" +
                "invoiceLineId=" + invoiceLineId +
                ", invoiceId=" + invoiceId +
                ", trackId=" + trackId +
                ", unitPrice=" + unitPrice +
                ", quantity=" + quantity +
                '}';
    }
}
