package com.example.app;

import com.example.model.Artist;
import com.example.util.ChinookUtils;
import com.example.util.TableUtils;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;

import java.util.ArrayList;
import java.util.List;

public class testPojo {
    public static void main(String[] args) {
        // Control logging
        java.util.logging.LogManager.getLogManager().reset();
        org.apache.logging.log4j.jul.LogManager.getLogManager();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);

        try (IgniteClient client = ChinookUtils.connectToCluster()) {
            if (client == null) {
                System.err.println("Failed to connect to the Ignite cluster. Exiting.");
                System.exit(1);
            }

            System.out.println("Connected to the cluster: " + client.connections());

            // drop table
            System.out.println("--- Dropping personPojo table");
            if (client.tables().table("personPojo") != null) {
                client.catalog().dropTable("personPojo");
            }

            // drop zone
            System.out.println("--- Dropping test_zone");
            var zResult = client.sql().execute(null, "SELECT name FROM system.zones WHERE name = ?", "TEST_ZONE");
            if (zResult.hasNext()) {
                client.catalog().dropZone("TEST_ZONE");
            }

            // create test zone
            System.out.println("--- Creating test_zone");
            ZoneDefinition zoneTestZone = ZoneDefinition.builder("test_zone")
                    .ifNotExists()
                    .partitions(2)
                    .storageProfiles("default")
                    .build();
            client.catalog().createZone(zoneTestZone);

            // create table from personPojo
            System.out.println("--- Creating table from personPojo");
            client.catalog().createTable(personPojo.class);

            // Get the personPojo table
            org.apache.ignite.table.Table personTable = client.tables().table("personPojo");
            RecordView<personPojo> personView = personTable.recordView(personPojo.class);

            // Create a list of 10 people
            List<personPojo> people = new ArrayList<>();

            people.add(createPerson(1, "P001", "John", "Doe", "Software Engineer"));
            people.add(createPerson(2, "P002", "Jane", "Smith", "Data Scientist"));
            people.add(createPerson(3, "P003", "Michael", "Johnson", "Product Manager"));
            people.add(createPerson(4, "P004", "Emily", "Williams", "UX Designer"));
            people.add(createPerson(5, "P005", "David", "Brown", "System Administrator"));
            people.add(createPerson(6, "P006", "Sarah", "Taylor", "Marketing Specialist"));
            people.add(createPerson(7, "P007", "James", "Anderson", "HR Manager"));
            people.add(createPerson(8, "P008", "Emma", "Thomas", "Financial Analyst"));
            people.add(createPerson(9, "P009", "Robert", "Jackson", "Sales Executive"));
            people.add(createPerson(10, "P010", "Olivia", "White", "Content Writer"));

            // Insert one person
            System.out.println("\n--- Inserting: " + people.get(0));
            personView.upsert(null, people.get(0));

            // Inspect the table structure
            System.out.println("\n--- Inspect the personPojo table");
            TableUtils.displayTableColumns(client, "personPojo");

            // Retrieve the person
            System.out.println("\n--- Retrieving person with ID: 1 and idStr: P001");
            try {
                // Use SQL to retrieve the person since KeyValueView might have case sensitivity issues
                var result = client.sql().execute(null,
                        "SELECT * FROM personPojo WHERE id = ? AND id_str = ?", 1, "P001");

                if (result.hasNext()) {
                    var row = result.next();
                    System.out.println("Person found: ID=" + row.intValue("ID") +
                            ", ID_STR=" + row.stringValue("ID_STR") +
                            ", F_NAME=" + row.stringValue("F_NAME") +
                            ", L_NAME=" + row.stringValue("L_NAME") +
                            ", STR=" + row.stringValue("STR"));
                } else {
                    System.out.println("Person not found using SQL");
                }
            } catch (Exception e) {
                System.err.println("Error retrieving person: " + e.getMessage());
                e.printStackTrace();
            }

            // Insert the rest of the people
            System.out.println("\n--- Inserting remaining people");
            try {
                for (int i = 1; i < people.size(); i++) {
                    personView.upsert(null, people.get(i));
                    System.out.println("Inserted: " + people.get(i).getFirstName() + " " + people.get(i).getLastName());
                }
            } catch (Exception e) {
                System.err.println("Error inserting people: " + e.getMessage());
            }

            // Change a persons name
            System.out.println("\n--- Updating person with ID: 5");
            try {
                // Create key to fetch the person
                personPojo keyPerson = new personPojo();
                keyPerson.setId(5);
                keyPerson.setIdStr("P005");

                // Fetch current version from database
                personPojo originalPerson = personView.get(null, keyPerson);
                System.out.println("Before: " + originalPerson);

                // Update the person
                originalPerson.setFirstName("Daniel");
                personView.upsert(null, originalPerson);

                // Fetch again to verify changes
                personPojo updatedPerson = personView.get(null, keyPerson);
                System.out.println("After : " + updatedPerson);
            } catch (Exception e) {
                System.err.println("Error updating person: " + e.getMessage());
            }

            // Remove a person
            System.out.println("\n--- Removing person with ID: 10");
            try {
                // Create a person with just the ID fields (composite primary key)
                personPojo personToDelete = new personPojo();
                personToDelete.setId(10);
                personToDelete.setIdStr("P010");

                // Delete the person using the POJO with primary key fields
                personView.delete(null, personToDelete);
                System.out.println("Removed person with ID: 10");
            } catch (Exception e) {
                System.err.println("Error removing person: " + e.getMessage());
            }

            /*
             * Looking at the Ignite 3 documentation at:
             * https://ignite.apache.org/releases/3.0.0/javadoc/index.html,
             * I found no simple built-in method to iterate through all POJOs without using SQL.
             *
             * The most straightforward approach would be to use the SQL method and modify
             * it to map the results to POJO
             */

            // List all people in the table
            System.out.println("\n--- Listing all people");
            try {
                // Get all records using SQL
                var result = client.sql().execute(null, "SELECT * FROM personPojo ORDER BY id");

                // Transform SQL results to POJOs
                result.forEachRemaining(row -> {
                    personPojo person = new personPojo();
                    person.setId(row.intValue("ID"));
                    person.setIdStr(row.stringValue("ID_STR"));
                    person.setFirstName(row.stringValue("F_NAME"));
                    person.setLastName(row.stringValue("L_NAME"));
                    person.setStr(row.stringValue("STR"));

                    System.out.printf("ID: %d, Name: %s %s, Role: %s\n",
                            person.getId(),
                            person.getFirstName(),
                            person.getLastName(),
                            person.getStr());
                });
            } catch (Exception e) {
                System.err.println("Error listing people: " + e.getMessage());
            }
        }
    }

    /**
     * Helper method to create a personPojo object
     */
    private static personPojo createPerson(Integer id, String idStr, String firstName, String lastName, String str) {
        personPojo person = new personPojo();
        person.id = id;
        person.idStr = idStr;
        person.firstName = firstName;
        person.lastName = lastName;
        person.str = str;
        return person;
    }

    // test pojo
    @Table(
            zone = @Zone(value = "test_zone", storageProfiles = "default")
    )
    public static class personPojo {
        @Id
        Integer id;

        @Id
        @Column(value = "id_str", length = 20)
        String idStr;

        @Column("f_name")
        String firstName;

        @Column("l_name")
        String lastName;

        String str;

        // Getters and setters
        public Integer getId() {return id;}
        public void setId(Integer id) {this.id = id;}
        public String getIdStr() {return idStr;}
        public void setIdStr(String idStr) {this.idStr = idStr;}
        public String getFirstName() {return firstName;}
        public void setFirstName(String firstName) {this.firstName = firstName;}
        public String getLastName() {return lastName;}
        public void setLastName(String lastName) {this.lastName = lastName;}
        public String getStr() {return str;}
        public void setStr(String str) {this.str = str;}

        @Override
        public String toString() {
            return "personPojo{" +
                    "id=" + id +
                    ", idStr='" + idStr + '\'' +
                    ", firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    ", str='" + str + '\'' +
                    '}';
        }
    }
}