package com.gitlab.pipeline;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

public class CsvExporter {

    // Reading database connection details from environment variables
    private static final String DB_HOSTNAME = System.getenv("DB_HOSTNAME");
    private static final String DB_NAME = System.getenv("DB_NAME");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    // Reading the queries string from environment variables
    private static final String QUERIES = System.getenv("QUERIES");

    public static void main(String[] args) {
        // 1. Validate that all required environment variables are set
        if (QUERIES == null || QUERIES.trim().isEmpty()) {
            System.err.println("Error: The 'QUERIES' environment variable is not set or is empty.");
            System.exit(1); // Exit with error
        }

        if (DB_HOSTNAME == null || DB_NAME == null || DB_USER == null || DB_PASSWORD == null) {
            System.err.println("Error: One or more database connection environment variables (DB_HOSTNAME, DB_NAME, DB_USER, DB_PASSWORD) are not set.");
            System.exit(1); // Exit with error
        }

        // 2. Parse the QUERIES string
        // The string is expected to be in the format: "artifactName1=query1,artifactName2=query2"
        String[] pairs = QUERIES.split(",");

        System.out.println("Found " + pairs.length + " query pairs to process.");

        // 3. Process each artifact/query pair
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);

            if (keyValue.length == 2) {
                // The 0th index is the artifact name, which is used for the CSV filename
                String artifactName = keyValue[0].trim();
                // The 1st index is the SQL query to be executed
                String query = keyValue[1].trim();

                if (artifactName.isEmpty() || query.isEmpty()) {
                    System.err.println("Skipping malformed pair. Both artifact name and query must be non-empty. Pair: '" + pair + "'");
                    continue;
                }

                System.out.println("Processing artifact '" + artifactName + "'...");
                try {
                    // 4. Execute the query and write the result to a CSV file
                    exportQueryToCsv(artifactName, query);
                    System.out.println("Successfully exported CSV for artifact: " + artifactName);
                } catch (SQLException | IOException e) {
                    System.err.println("ERROR: Failed to export artifact '" + artifactName + "'. Reason: " + e.getMessage());
                    e.printStackTrace(System.err); // Print stack trace to stderr
                }
            } else {
                System.err.println("Skipping malformed pair (does not contain '='): '" + pair + "'");
            }
        }
        System.out.println("All processing finished.");
    }

    private static void exportQueryToCsv(String artifactName, String query) throws SQLException, IOException {
        // Construct the JDBC connection URL from environment variables
        String connectionUrl = String.format("jdbc:sqlserver://%s;databaseName=%s;user=%s;password=%s;",
                DB_HOSTNAME, DB_NAME, DB_USER, DB_PASSWORD);

        // Use try-with-resources to ensure database resources are closed automatically
        try (Connection connection = DriverManager.getConnection(connectionUrl);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            String fileName = "target/" + artifactName + ".csv";
            System.out.println("Writing results to file: " + fileName);

            // Use try-with-resources for file writers
            try (FileWriter fileWriter = new FileWriter(fileName);
                 CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT.withHeader(resultSet))) {

                // Write the rest of the ResultSet to the CSV file
                csvPrinter.printRecords(resultSet);
            }
            System.out.println("File write complete.");
        }
    }
}
