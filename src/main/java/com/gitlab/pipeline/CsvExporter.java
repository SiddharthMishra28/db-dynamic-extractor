package com.gitlab.pipeline;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CsvExporter {

    private static final String DB_HOSTNAME = System.getenv("DB_HOSTNAME");
    private static final String DB_NAME = System.getenv("DB_NAME");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    private static final String QUERIES = System.getenv("QUERIES");

    public static void main(String[] args) {
        if (QUERIES == null || QUERIES.isEmpty()) {
            System.err.println("QUERIES environment variable is not set.");
            return;
        }

        if (DB_HOSTNAME == null || DB_NAME == null || DB_USER == null || DB_PASSWORD == null) {
            System.err.println("Database connection environment variables are not fully set.");
            return;
        }

        Map<String, String> queryMap = parseQueries(QUERIES);

        for (Map.Entry<String, String> entry : queryMap.entrySet()) {
            String artifactName = entry.getKey();
            String query = entry.getValue();
            try {
                exportQueryToCsv(artifactName, query);
                System.out.println("Successfully exported query for artifact: " + artifactName);
            } catch (SQLException | IOException e) {
                System.err.println("Error exporting query for artifact " + artifactName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static Map<String, String> parseQueries(String queries) {
        Map<String, String> queryMap = new HashMap<>();
        String[] pairs = queries.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                queryMap.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return queryMap;
    }

    private static void exportQueryToCsv(String artifactName, String query) throws SQLException, IOException {
        String connectionUrl = String.format("jdbc:sqlserver://%s;databaseName=%s;user=%s;password=%s;",
                DB_HOSTNAME, DB_NAME, DB_USER, DB_PASSWORD);

        try (Connection connection = DriverManager.getConnection(connectionUrl);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            String fileName = "target/" + artifactName + ".csv";
            try (FileWriter fileWriter = new FileWriter(fileName);
                 CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT.withHeader(resultSet))) {
                csvPrinter.printRecords(resultSet);
            }
        }
    }
}
