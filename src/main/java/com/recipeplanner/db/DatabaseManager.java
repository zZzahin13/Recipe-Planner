package com.recipeplanner.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the single SQLite JDBC connection used by the app and makes sure
 * the schema exists on first launch. SQLite handles one writer at a time
 * internally, so a single shared Connection kept open for the app's
 * lifetime is the simplest safe approach for a desktop tool like this.
 */
public final class DatabaseManager {

    private static final String DB_FILE = "recipe_planner.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static Connection connection;

    private DatabaseManager() {
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(URL);
                try (Statement pragma = connection.createStatement()) {
                    pragma.execute("PRAGMA foreign_keys = ON;");
                }
                initializeSchema(connection);
                migrateSchema(connection);
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("Could not open SQLite database at " + DB_FILE, e);
        }
        return connection;
    }

    private static void initializeSchema(Connection conn) throws SQLException {
        String ddl = readSchemaSql();
        // Run the whole schema.sql as a single multi-statement script rather
        // than looping and opening a new Statement per ';'-terminated line.
        // sqlite-jdbc's native layer executes each statement in the string
        // sequentially on its own, and doing it this way avoids a known
        // driver bug where creating/closing several Statement objects in
        // quick succession on a freshly-opened connection throws
        // "the prepared statement has been finalized" (a finalizer race in
        // org.sqlite.core.SafeStmtPtr -- see xerial/sqlite-jdbc issues
        // #183, #217, #731 for background).
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(ddl);
        }
    }

    /**
     * "CREATE TABLE IF NOT EXISTS" in schema.sql only creates recipes on a
     * brand-new database -- it does nothing for a database that already
     * existed before these columns were added. This adds any missing
     * columns to an existing recipes table. ALTER TABLE ADD COLUMN throws
     * if the column is already there, so each one is attempted and any
     * "duplicate column" failure is silently ignored.
     */
    private static void migrateSchema(Connection conn) {
        String[] columns = {
                "ALTER TABLE recipes ADD COLUMN base_servings INTEGER DEFAULT 4",
                "ALTER TABLE recipes ADD COLUMN calories INTEGER DEFAULT 0",
                "ALTER TABLE recipes ADD COLUMN protein_g REAL DEFAULT 0",
                "ALTER TABLE recipes ADD COLUMN carbs_g REAL DEFAULT 0",
                "ALTER TABLE recipes ADD COLUMN fat_g REAL DEFAULT 0"
        };
        for (String alter : columns) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(alter);
            } catch (SQLException alreadyExists) {
                // Column already present -- expected on every launch after the first migration.
            }
        }
    }

    private static String readSchemaSql() {
        try (InputStream in = DatabaseManager.class.getResourceAsStream("/sql/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql not found on classpath at /sql/schema.sql");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schema.sql", e);
        }
    }

    public static synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}
