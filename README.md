# Ignite 3 Chinook Database Demo

A demonstration of Apache Ignite 3 Java API using the Chinook database model.

## Overview

This project demonstrates how to use Apache Ignite 3's Java API with POJO-based table mapping. It implements the Chinook database, a sample database schema for a digital media store, to showcase various Ignite 3 features including:

- Annotation-based table mapping
- Distribution zones and storage profiles
- Data co-location for optimized joins
- CRUD operations with POJOs
- SQL query execution
- Transaction management

## Prerequisites

- Java 17 or higher
- Apache Maven 3.6 or higher
- Docker and Docker Compose (for running Ignite nodes)

## Quick Start

1. **Start the Ignite cluster**

```bash
docker-compose up -d
```

2. **Initialize the cluster**

```bash
docker run --rm -it --network=host apacheignite/ignite:3.0.0 cli
connect http://localhost:10300
cluster init --name=ignite3 --metastorage-group=node1,node2,node3
exit
```

3. **Create the database schema**

```bash
mvn compile exec:java@create-tables
```

4. **Load sample data**

```bash
mvn compile exec:java@load-data
```

5. **Run the main application**

```bash
mvn compile exec:java@run-main
```

## Documentation

For detailed documentation, see the [docs](./docs) directory:

- [Getting Started](./docs/getting-started.md) - Detailed setup instructions
- [Data Model](./docs/data-model.md) - Chinook database schema explanation
- [POJO Mapping](./docs/pojo-mapping.md) - How POJOs are mapped to Ignite tables
- [Distribution Zones](./docs/distribution-zones.md) - Understanding data distribution
- [Storage Profiles](./docs/storage-profiles.md) - Storage engine options and configuration
- [Annotations](./docs/annotations.md) - Reference for Ignite 3 annotations
- [Examples](./docs/examples.md) - Code examples and patterns

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)