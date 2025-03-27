# Ignite 3 Chinook Database Demo

A demonstration of Apache Ignite 3 Java API using the Chinook database model.

## Prerequisites

- Java 17 or higher
- Apache Maven 3.6 or higher
- Docker and Docker Compose (for running Ignite nodes)

## Getting Started

This guide will walk you through setting up and running the Ignite 3 Chinook Database Demo.

### Step 1: Start the Ignite cluster

Launch the Ignite nodes using Docker Compose:

```bash
docker-compose up -d
```

> [!IMPORTANT]
> Wait for all Ignite nodes to start completely before proceeding to the next steps. This typically takes about 30 seconds.

### Step 2: Create the database schema

Run the CreateTablesApp to set up the database schema:

```bash
mvn compile exec:java@create-tables
```

This application will:

- Create the required distribution zones
- Create all Chinook data model tables
- Handle proper error conditions if tables already exist

### Step 3: Load sample data

Populate the database with sample data:

```bash
mvn compile exec:java@load-data
```

This will:

- Add sample artists, albums, and tracks
- Demonstrate batch data operations
- Create sample related entities with proper relationships

### Step 4: Run the main application

Execute the main application to see various operations with the Chinook database:

```bash
mvn compile exec:java@run-main
```

The application demonstrates:

- Connecting to an Ignite cluster
- Performing CRUD operations
- Using transactions
- Executing SQL queries
- Working with relationships between entities

## Project Structure

- `src/main/java/com/example/app/` - Application entry points
- `src/main/java/com/example/model/` - Data model classes with Ignite annotations
- `src/main/java/com/example/util/` - Utility classes for database operations
- `src/main/resources/` - Configuration files

## Understanding Annotations

This project uses Apache Ignite's annotation-based schema definition to map Java classes to database tables. Here's how annotations are used:

### Table Definition Annotations

- `@Table` - Marks a Java class as an Ignite table and configures its properties
- `@Zone` - Defines which distribution zone the table belongs to
- `@Id` - Marks a field as part of the primary key
- `@Column` - Maps a Java field to a table column with specific properties
- `@ColumnRef` - References a column for co-location purposes

### Example from `Artist.java`

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
    
    // Methods omitted for brevity
}
```

### Example from `Album.java` with Co-location

```java
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default"),
        colocateBy = @ColumnRef("ArtistId")
)
public class Album {
    @Id
    @Column(value = "AlbumId", nullable = false)
    private Integer albumId;

    @Column(value = "Title", nullable = false)
    private String title;

    @Id
    @Column(value = "ArtistId", nullable = false)
    private Integer artistId;
    
    // Methods omitted for brevity
}
```

## Working with IgniteCatalog

The `TableUtils.java` class demonstrates how to use the `IgniteCatalog` API to create tables programmatically:

```java
public static boolean createTables(IgniteClient client) {
    try {
        System.out.println("=== Creating tables ===");

        // Use IgniteCatalog.createTable to create tables from annotated classes
        System.out.println("--- Creating Artist table");
        client.catalog().createTable(Artist.class);

        System.out.println("--- Creating Genre table");
        client.catalog().createTable(Genre.class);

        // Additional tables...

        return true;
    } catch (Exception e) {
        System.err.println("Error creating tables: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}
```

The `IgniteCatalog` API reads the annotations from your model classes and creates the corresponding database tables, columns, and indexes automatically.

## Customizing the Demo

### Understanding Distribution Zones

Distribution zones control how your data is distributed and replicated across the Ignite cluster. This project uses two zones:

1. **Chinook** - For primary entity tables with 2 replicas

   ```java
   ZoneDefinition zoneChinook = ZoneDefinition.builder("Chinook")
           .ifNotExists()
           .replicas(2)
           .storageProfiles("default")
           .build();
   ```

2. **ChinookReplicated** - For reference tables with 3 replicas

   ```java
   ZoneDefinition zoneChinookReplicated = ZoneDefinition.builder("ChinookReplicated")
           .ifNotExists()
           .replicas(3)
           .partitions(25)
           .storageProfiles("default")
           .build();
   ```

Tables are assigned to zones using the `@Zone` annotation:

```java
@Table(
        zone = @Zone(value = "Chinook", storageProfiles = "default")
)
```

### Creating Custom Queries

Add new methods to `ChinookUtils.java` following this pattern:

```java
public static void findCustomData(IgniteClient client, String parameter) {
    try {
        client.sql().execute(null,
                "YOUR SQL QUERY HERE WITH ? PARAMETER", parameter)
            .forEachRemaining(row ->
                System.out.println("Result: " + row.stringValue("ColumnName")));
    } catch (Exception e) {
        System.err.println("Error executing query: " + e.getMessage());
    }
}
```

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
