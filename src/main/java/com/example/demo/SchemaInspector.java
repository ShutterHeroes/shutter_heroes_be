package com.example.demo;

import java.sql.*;
import java.util.*;

/**
 * Database schema inspector for PostgreSQL
 * This utility connects to PostgreSQL and inspects the 'app' schema
 */
public class SchemaInspector {

    private static final String DB_URL = "jdbc:postgresql://localhost:5433/postgres";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "8033";
    private static final String SCHEMA_NAME = "app";

    public static void main(String[] args) {
        try {
            // Load PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");

            // Connect to database
            try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
                System.out.println("Connected to PostgreSQL database successfully!");
                System.out.println("=".repeat(80));

                inspectSchema(conn);

            } catch (SQLException e) {
                System.err.println("Database connection error: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void inspectSchema(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();

        // Get all tables in the 'app' schema
        System.out.println("TABLES IN SCHEMA '" + SCHEMA_NAME + "':");
        System.out.println("-".repeat(50));

        List<String> tableNames = new ArrayList<>();

        try (ResultSet tables = metaData.getTables(null, SCHEMA_NAME, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tableNames.add(tableName);
                System.out.println("• " + tableName);
            }
        }

        if (tableNames.isEmpty()) {
            System.out.println("No tables found in schema '" + SCHEMA_NAME + "'");

            // Let's check what schemas exist
            System.out.println("\nAvailable schemas:");
            try (ResultSet schemas = metaData.getSchemas()) {
                while (schemas.next()) {
                    System.out.println("• " + schemas.getString("TABLE_SCHEM"));
                }
            }

            // Let's also check tables in public schema
            System.out.println("\nTables in 'public' schema:");
            try (ResultSet publicTables = metaData.getTables(null, "public", null, new String[]{"TABLE"})) {
                while (publicTables.next()) {
                    System.out.println("• " + publicTables.getString("TABLE_NAME"));
                }
            }

            return;
        }

        System.out.println("\nTotal tables found: " + tableNames.size());
        System.out.println("=".repeat(80));

        // Inspect each table in detail
        for (String tableName : tableNames) {
            inspectTable(conn, metaData, tableName);
        }

        // Show foreign key relationships
        showForeignKeyRelationships(conn, metaData, tableNames);
    }

    private static void inspectTable(Connection conn, DatabaseMetaData metaData, String tableName) throws SQLException {
        System.out.println("\nTABLE: " + SCHEMA_NAME + "." + tableName);
        System.out.println("-".repeat(60));

        // Get column information
        System.out.println("COLUMNS:");
        System.out.printf("%-25s %-20s %-10s %-15s %-30s%n", "COLUMN_NAME", "DATA_TYPE", "NULLABLE", "DEFAULT", "CONSTRAINTS");
        System.out.println("-".repeat(100));

        try (ResultSet columns = metaData.getColumns(null, SCHEMA_NAME, tableName, null)) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                boolean isNullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                String defaultValue = columns.getString("COLUMN_DEF");

                // Format data type with size
                String fullDataType = dataType;
                if (columnSize > 0 && (dataType.equalsIgnoreCase("VARCHAR") || dataType.equalsIgnoreCase("CHAR"))) {
                    fullDataType = dataType + "(" + columnSize + ")";
                }

                System.out.printf("%-25s %-20s %-10s %-15s%n",
                    columnName,
                    fullDataType,
                    isNullable ? "YES" : "NO",
                    defaultValue != null ? defaultValue : "NULL"
                );
            }
        }

        // Get primary keys
        System.out.println("\nPRIMARY KEYS:");
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(null, SCHEMA_NAME, tableName)) {
            while (primaryKeys.next()) {
                String columnName = primaryKeys.getString("COLUMN_NAME");
                String pkName = primaryKeys.getString("PK_NAME");
                System.out.println("• " + columnName + " (constraint: " + pkName + ")");
            }
        }

        // Get indexes
        System.out.println("\nINDEXES:");
        try (ResultSet indexes = metaData.getIndexInfo(null, SCHEMA_NAME, tableName, false, false)) {
            Map<String, List<String>> indexColumns = new HashMap<>();
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                String columnName = indexes.getString("COLUMN_NAME");
                boolean isUnique = !indexes.getBoolean("NON_UNIQUE");

                if (indexName != null && columnName != null) {
                    indexColumns.computeIfAbsent(indexName, k -> new ArrayList<>()).add(columnName);
                    if (indexColumns.get(indexName).size() == 1) { // First column of this index
                        System.out.println("• " + indexName + " (" + (isUnique ? "UNIQUE" : "NON-UNIQUE") + ")");
                    }
                }
            }

            for (Map.Entry<String, List<String>> entry : indexColumns.entrySet()) {
                System.out.println("  Columns: " + String.join(", ", entry.getValue()));
            }
        }

        System.out.println("=".repeat(80));
    }

    private static void showForeignKeyRelationships(Connection conn, DatabaseMetaData metaData, List<String> tableNames) throws SQLException {
        System.out.println("\nFOREIGN KEY RELATIONSHIPS:");
        System.out.println("-".repeat(60));

        boolean foundForeignKeys = false;

        for (String tableName : tableNames) {
            try (ResultSet foreignKeys = metaData.getImportedKeys(null, SCHEMA_NAME, tableName)) {
                while (foreignKeys.next()) {
                    foundForeignKeys = true;
                    String fkTableName = foreignKeys.getString("FKTABLE_NAME");
                    String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
                    String pkTableName = foreignKeys.getString("PKTABLE_NAME");
                    String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
                    String fkName = foreignKeys.getString("FK_NAME");

                    System.out.printf("%s.%s.%s -> %s.%s.%s (FK: %s)%n",
                        SCHEMA_NAME, fkTableName, fkColumnName,
                        SCHEMA_NAME, pkTableName, pkColumnName,
                        fkName
                    );
                }
            }
        }

        if (!foundForeignKeys) {
            System.out.println("No foreign key relationships found.");
        }

        System.out.println("=".repeat(80));
    }
}
