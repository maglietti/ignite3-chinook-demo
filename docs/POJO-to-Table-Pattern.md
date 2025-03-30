# POJO-to-Table Mapping Pattern

The POJO-to-Table mapping pattern allows Ignite to directly map Java objects to database tables, providing an object-oriented interface for data operations. This approach uses Java annotations to define the mapping between POJO fields and table columns.

## Defining POJO Mapping with Annotations

To map a POJO to a table, you need to:

1. Add `@Table` annotation to the class with distribution zone information
2. Add `@Column` annotation to fields to map them to table columns
3. Add `@Id` annotation to fields that form the primary key

Example POJO mapping:

```java
@Table(
    zone = @Zone(value = "Chinook", storageProfiles = "default")
)
public class Artist {
    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;

    @Column(value = "Name", nullable = true)
    private String name;
    
    // Constructors, getters, setters...
}
```

Note that the column names in the annotations (`ArtistId`, `Name`) must exactly match the case of the column names in the database.

## Creating a Table from a POJO

```java
// Create a table from an annotated class
client.catalog().createTable(Artist.class);
```

This method:

1. Analyzes the annotations in the POJO class
2. Creates a corresponding table in the database
3. Sets up the appropriate indexes and constraints

## Basic CRUD Operations with POJO Mapping

### Creating a Table Instance

```java
// Get a reference to the table
Table artistTable = client.tables().table("Artist");
```

### Insert/Update Operations

```java
// Create a RecordView for the Artist class
RecordView<Artist> artistView = artistTable.recordView(Artist.class);

// Create a new Artist object
Artist artist = new Artist(1, "AC/DC");

// Insert or update the record
artistView.upsert(null, artist);
```

### Batch Insert/Update Operations

```java
// Create multiple artist objects
List<Artist> artists = Arrays.asList(
    new Artist(1, "AC/DC"),
    new Artist(2, "Accept"),
    new Artist(3, "Aerosmith")
);

// Insert all artists in a single batch operation
artistView.upsertAll(null, artists);
```

### Lookup by Primary Key

```java
// Create a KeyValueView for more efficient primary key lookups
KeyValueView<Integer, Artist> keyValueView = artistTable.keyValueView(Integer.class, Artist.class);

// Lookup an artist by ID
Artist artist = keyValueView.get(null, 1);
if (artist != null) {
    System.out.println("Found artist: " + artist.getName());
}
```

### Delete Operations

```java
// Delete by primary key
keyValueView.delete(null, 1);

// Or delete using a record
artistView.delete(null, artist);
```

## Advantages of the POJO-to-Table Mapping Pattern

1. **Type Safety**: Compile-time type checking helps catch errors early
2. **Object-Oriented**: Work with domain objects directly
3. **Reduced Boilerplate**: No need to manually convert between database rows and objects
4. **Integration with ORM Frameworks**: Familiar pattern for developers used to JPA/Hibernate

## Potential Challenges

1. **Case Sensitivity**: Field annotations must exactly match column names in the database
2. **Schema Evolution**: Changes to the POJO class may require database schema updates
3. **Complex Relationships**: May require additional configuration for complex object relationships
4. **Handling NULL Values**: Requires proper handling of nullable fields

## When to Use POJO-to-Table Mapping

This pattern works best when:

- You need type safety and object-oriented programming
- Your schema is relatively stable
- You prefer working with objects rather than SQL
- The application has control over both database schema and code

If you encounter issues with direct POJO mapping (such as case sensitivity problems), consider the SQL-to-POJO approach described earlier for more flexibility and robustness.
