package com.example.model;

import org.apache.ignite.catalog.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an invoice in the Chinook database.
 * This class maps to the Invoice table which contains billing information for sales.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("CustomerId")
)
public class Invoice {
    // Primary key field
    @Id
    @Column(value = "InvoiceId", nullable = false)
    private Integer invoiceId;

    // Foreign key to Customer
    @Id
    @Column(value = "CustomerId", nullable = false)
    private Integer customerId;

    @Column(value = "InvoiceDate", nullable = false)
    private LocalDate invoiceDate;

    @Column(value = "BillingAddress", nullable = true)
    private String billingAddress;

    @Column(value = "BillingCity", nullable = true)
    private String billingCity;

    @Column(value = "BillingState", nullable = true)
    private String billingState;

    @Column(value = "BillingCountry", nullable = true)
    private String billingCountry;

    @Column(value = "BillingPostalCode", nullable = true)
    private String billingPostalCode;

    @Column(value = "Total", nullable = false)
    private BigDecimal total;

    /**
     * Default constructor required for serialization
     */
    public Invoice() { }

    /**
     * Constructs an Invoice with essential details
     *
     * @param invoiceId The unique identifier for the invoice
     * @param customerId The ID of the customer who made the purchase
     * @param invoiceDate The date of the invoice
     * @param total The total amount of the invoice
     */
    public Invoice(Integer invoiceId, Integer customerId, LocalDate invoiceDate, BigDecimal total) {
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.invoiceDate = invoiceDate;
        this.total = total;
    }

    /**
     * Constructs an Invoice with full details
     *
     * @param invoiceId The unique identifier for the invoice
     * @param customerId The ID of the customer who made the purchase
     * @param invoiceDate The date of the invoice
     * @param billingAddress The billing address
     * @param billingCity The billing city
     * @param billingState The billing state
     * @param billingCountry The billing country
     * @param billingPostalCode The billing postal code
     * @param total The total amount of the invoice
     */
    public Invoice(Integer invoiceId, Integer customerId, LocalDate invoiceDate, 
                   String billingAddress, String billingCity, String billingState, 
                   String billingCountry, String billingPostalCode, BigDecimal total) {
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.invoiceDate = invoiceDate;
        this.billingAddress = billingAddress;
        this.billingCity = billingCity;
        this.billingState = billingState;
        this.billingCountry = billingCountry;
        this.billingPostalCode = billingPostalCode;
        this.total = total;
    }

    // Getters and setters

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getBillingCity() {
        return billingCity;
    }

    public void setBillingCity(String billingCity) {
        this.billingCity = billingCity;
    }

    public String getBillingState() {
        return billingState;
    }

    public void setBillingState(String billingState) {
        this.billingState = billingState;
    }

    public String getBillingCountry() {
        return billingCountry;
    }

    public void setBillingCountry(String billingCountry) {
        this.billingCountry = billingCountry;
    }

    public String getBillingPostalCode() {
        return billingPostalCode;
    }

    public void setBillingPostalCode(String billingPostalCode) {
        this.billingPostalCode = billingPostalCode;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "invoiceId=" + invoiceId +
                ", customerId=" + customerId +
                ", invoiceDate=" + invoiceDate +
                ", total=" + total +
                '}';
    }
}