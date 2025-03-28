package com.example.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Zone;

import java.time.LocalDate;

/**
 * Represents an employee in the Chinook database.
 * This class maps to the Employee table which contains information about Chinook employees.
 */
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        indexes = {
            @Index(value = "IFK_EmployeeReportsTo", columns = { @ColumnRef("ReportsTo") })
        }
)
public class Employee {
    // Primary key field
    @Id
    @Column(value = "EmployeeId", nullable = false)
    private Integer employeeId;

    @Column(value = "LastName", nullable = false)
    private String lastName;

    @Column(value = "FirstName", nullable = false)
    private String firstName;

    @Column(value = "Title", nullable = true)
    private String title;

    @Column(value = "ReportsTo", nullable = true)
    private Integer reportsTo;

    @Column(value = "BirthDate", nullable = true)
    private LocalDate birthDate;

    @Column(value = "HireDate", nullable = true)
    private LocalDate hireDate;

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

    @Column(value = "Email", nullable = true)
    private String email;

    /**
     * Default constructor required for serialization
     */
    public Employee() { }

    /**
     * Constructs an Employee with essential details
     *
     * @param employeeId The unique identifier for the employee
     * @param lastName The last name of the employee
     * @param firstName The first name of the employee
     */
    public Employee(Integer employeeId, String lastName, String firstName) {
        this.employeeId = employeeId;
        this.lastName = lastName;
        this.firstName = firstName;
    }

    /**
     * Constructs an Employee with full details
     *
     * @param employeeId The unique identifier for the employee
     * @param lastName The last name of the employee
     * @param firstName The first name of the employee
     * @param title The job title of the employee
     * @param reportsTo The ID of the employee's manager
     * @param birthDate The birth date of the employee
     * @param hireDate The hire date of the employee
     * @param address The address of the employee
     * @param city The city of the employee
     * @param state The state of the employee
     * @param country The country of the employee
     * @param postalCode The postal code of the employee
     * @param phone The phone number of the employee
     * @param fax The fax number of the employee
     * @param email The email of the employee
     */
    public Employee(Integer employeeId, String lastName, String firstName, String title,
                    Integer reportsTo, LocalDate birthDate, LocalDate hireDate, String address,
                    String city, String state, String country, String postalCode, String phone,
                    String fax, String email) {
        this.employeeId = employeeId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.title = title;
        this.reportsTo = reportsTo;
        this.birthDate = birthDate;
        this.hireDate = hireDate;
        this.address = address;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postalCode = postalCode;
        this.phone = phone;
        this.fax = fax;
        this.email = email;
    }

    // Getters and setters

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getReportsTo() {
        return reportsTo;
    }

    public void setReportsTo(Integer reportsTo) {
        this.reportsTo = reportsTo;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
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

    @Override
    public String toString() {
        return "Employee{" +
                "employeeId=" + employeeId +
                ", lastName='" + lastName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}