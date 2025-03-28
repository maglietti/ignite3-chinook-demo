package com.example.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Zone;

/**
 * Represents a customer in the Chinook database.
 * This class maps to the Customer table which contains information about customers.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        indexes = {
            @Index(value = "IFK_CustomerSupportRepId", columns = { @ColumnRef("SupportRepId") })
        }
)
public class Customer {
    // Primary key field
    @Id
    @Column(value = "CustomerId", nullable = false)
    private Integer customerId;

    @Column(value = "FirstName", nullable = false)
    private String firstName;

    @Column(value = "LastName", nullable = false)
    private String lastName;

    @Column(value = "Company", nullable = true)
    private String company;

    @Column(value = "Address", nullable = true)
    private String address;

    @Column(value = "City", nullable = true)
    private String city;

    @Column(value = "State", nullable = true)
    private String state;

    @Column(value = "Country", nullable = true)
    private String country;

    @Column(value = "PostalCode", nullable = true)
    private String postalCode;

    @Column(value = "Phone", nullable = true)
    private String phone;

    @Column(value = "Fax", nullable = true)
    private String fax;

    @Column(value = "Email", nullable = false)
    private String email;

    @Column(value = "SupportRepId", nullable = true)
    private Integer supportRepId;

    /**
     * Default constructor required for serialization
     */
    public Customer() { }

    /**
     * Constructs a Customer with specified details
     *
     * @param customerId The unique identifier for the customer
     * @param firstName The first name of the customer
     * @param lastName The last name of the customer
     * @param email The email of the customer
     */
    public Customer(Integer customerId, String firstName, String lastName, String email) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /**
     * Constructs a Customer with all details
     *
     * @param customerId The unique identifier for the customer
     * @param firstName The first name of the customer
     * @param lastName The last name of the customer
     * @param company The company of the customer
     * @param address The address of the customer
     * @param city The city of the customer
     * @param state The state of the customer
     * @param country The country of the customer
     * @param postalCode The postal code of the customer
     * @param phone The phone number of the customer
     * @param fax The fax number of the customer
     * @param email The email of the customer
     * @param supportRepId The ID of the employee who supports this customer
     */
    public Customer(Integer customerId, String firstName, String lastName, String company,
                    String address, String city, String state, String country, String postalCode,
                    String phone, String fax, String email, Integer supportRepId) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.company = company;
        this.address = address;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postalCode = postalCode;
        this.phone = phone;
        this.fax = fax;
        this.email = email;
        this.supportRepId = supportRepId;
    }

    // Getters and setters

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getSupportRepId() {
        return supportRepId;
    }

    public void setSupportRepId(Integer supportRepId) {
        this.supportRepId = supportRepId;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
