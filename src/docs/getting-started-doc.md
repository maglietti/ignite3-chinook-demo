# Getting Started with Ignite 3 Chinook Demo

This guide provides detailed instructions for setting up and running the Ignite 3 Chinook Database Demo.

## Prerequisites

Before you begin, ensure you have the following software installed:

- **Java 17 or higher**: Required for building and running the application
- **Apache Maven 3.6 or higher**: Used for dependency management and running the application
- **Docker and Docker Compose**: Required for running the Ignite cluster

## Setting Up the Ignite Cluster

### Step 1: Start the Ignite nodes

The `docker-compose.yml` file defines a 3-node Ignite cluster. To start it:

```bash
docker-compose up -d
```

This command starts three Ignite nodes (`node1`, `node2`, and `node3`) in detached mode.

To verify the nodes are running:

```bash
docker ps
```

You should see three containers running with names like `ignite3-node1-1`, `ignite3-node2-1`, and `ignite3-node3-1`.

### Step 2: Initialize the cluster

Before you can use the cluster, you need to initialize it with a name and metastorage group:

```bash
docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 apacheignite/ignite:3.0.0 cli
```

This command starts the Ignite CLI in an interactive Docker container. Inside the CLI:

```bash
connect http://localhost:10300
cluster init --name=ignite3 --metastorage-group=node1,node2,node3
exit
```

> ⚠️ **Important**: The initialization step MUST be performed for a new cluster. You only need to do this once when setting up the cluster for the first time.

## Setting Up the Distribution Zones and Storage Profiles

Before creating tables, the application sets up the necessary distribution zones and storage profiles:

### Distribution Zones

Distribution zones control how data is partitioned and replicated across the cluster. The `TableUtils.createDistributionZones()` method creates two zones:

```java
// Create the Chinook distribution zone with 2 replicas
ZoneDefinition zoneChinook = ZoneDefinition.builder("Chinook")
        .ifNotExists()
        .replicas(2)
        .storageProfiles("default")
        .build();

// Create the ChinookReplicated distribution zone with 3 replicas and 25 partitions
ZoneDefinition zoneChinookReplicated = ZoneDefinition.builder("ChinookReplicated")
        .ifNotExists()
        .replicas(3)
        .partitions(25)
        .storageProfiles("default")
        .build();
```

Each zone is configured with:

- A name
- Number of replicas (copies of data)
- Number of partitions (how data is split)
- Storage profiles to use

### Storage Profiles

Storage profiles determine which storage engine to use and its configuration. By default, Ignite 3 creates a `default` storage profile that uses the persistent Apache Ignite Page Memory (B+ tree) engine.

## Setting Up the Database Schema

### Step 1: Create the database schema

Run the `CreateTablesApp` to set up the database schema:

```bash
mvn compile exec:java@create-tables
```

This application will:

- Create the required distribution zones
- Create all Chinook data model tables
- Handle proper error conditions if tables already exist

If the tables already exist, the application will prompt you to confirm whether you want to drop and recreate them.

### Step 2: Load sample data

Populate the database with sample data:

```bash
mvn compile exec:java@load-data
```

This will:

- Add sample artists, albums, and tracks
- Demonstrate batch data operations
- Create sample related entities with proper relationships

If data already exists in the database, the application will prompt you to confirm whether you want to load additional data.

## Running the Main Application

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

## Understanding the Application Structure

### Main Application Components

- **CreateTablesApp.java**: Creates the distribution zones and database schema
- **LoadDataApp.java**: Populates the database with sample data
- **Main.java**: Demonstrates various operations on the data

### Model Classes

- **Artist.java**: Represents music artists
- **Album.java**: Represents music albums, co-located with artists
- **Track.java**: Represents music tracks, co-located with albums
- **Genre.java**: Represents music genres
- **MediaType.java**: Represents media format types

Each model class uses annotations to define:

- Table schema
- Distribution zone
- Co-location strategy (where applicable)
- Primary key and column properties

### Utility Classes

- **ChinookUtils.java**: Provides methods for common operations like connecting to the cluster, adding data, and querying
- **TableUtils.java**: Provides methods for creating distribution zones and managing tables
- **DataUtils.java**: Provides methods for loading and manipulating sample data

## Using SQL with the Cluster

You can interact with the Ignite cluster using SQL. The Ignite CLI provides an interactive SQL shell:

```bash
docker run --rm -it --network=host apacheignite/ignite:3.0.0 cli
connect http://localhost:10300
sql-cli
```

Once in the SQL shell, you can run queries:

```sql
-- List all tables
SHOW TABLES;

-- Query the Artist table
SELECT * FROM Artist;

-- Create a new distribution zone
CREATE ZONE IF NOT EXISTS MyZone 
WITH STORAGE_PROFILES='default', PARTITIONS=20, REPLICAS=2;

-- Create a new table in the zone
CREATE TABLE MyTable (id INT PRIMARY KEY, value VARCHAR) 
ZONE MyZone;
```

## Troubleshooting

### Common Issues

1. **Connection Refused Errors**:
   - Ensure the Docker containers are running (`docker ps`)
   - Check if the ports specified in `docker-compose.yml` are available
   - Try restarting the containers: `docker-compose restart`

2. **Failed to Initialize Cluster**:
   - Make sure all three nodes are running
   - Check for errors in the container logs: `docker-compose logs`

3. **Storage Profile Errors**:
   - If you get errors about storage profiles not existing, verify that the profile names match those configured on the nodes
   - Check the node configuration with `node config show ignite.storage.profiles`

4. **Java Version Issues**:
   - Verify you have Java 17 or higher: `java -version`
   - Ensure JAVA_HOME is set correctly

5. **Maven Errors**:
   - Verify Maven installation: `mvn -version`
   - Try clearing the Maven cache: `mvn clean`

6. **Data Inconsistencies**:
   - If you encounter data issues, try dropping and recreating the schema
   - Run `mvn compile exec:java@create-tables` and choose "Y" when prompted to drop tables

### Getting Help

If you encounter issues that aren't covered here:

1. Check the Apache Ignite 3 [documentation](https://ignite.apache.org/docs/ignite3/latest/)
2. Search or post questions on the [Apache Ignite User List](https://ignite.apache.org/community/resources.html)
3. Check GitHub issues or open a new issue in the repository
