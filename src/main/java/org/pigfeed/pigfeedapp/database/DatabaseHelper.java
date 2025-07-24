package org.pigfeed.pigfeedapp.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {
    // JDBC URL: SQLite will create the file if it doesn't exist
    public static final String DB_URL = "jdbc:sqlite:pigfeed.db";

    /**
     * Call this once at startup. It will:
     * 1) create pigfeed.db if missing
     * 2) create the ingredients, feed_purchases, and other_expenses tables if missing
     */
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Ingredients table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ingredients (
                  name TEXT PRIMARY KEY,
                  crudeProtein REAL,
                  crudeFat REAL,
                  crudeFiber REAL,
                  lysine REAL,
                  price REAL DEFAULT 0.0,
                  priceUnit TEXT DEFAULT '50lbs'
                );
            """);

            // Feed purchases table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS feed_purchases (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  ingredientName TEXT NOT NULL,
                  quantity REAL NOT NULL,
                  quantityUnit TEXT NOT NULL DEFAULT '50lbs',
                  pricePerUnit REAL NOT NULL,
                  totalCost REAL NOT NULL,
                  date TEXT NOT NULL,
                  FOREIGN KEY (ingredientName) REFERENCES ingredients(name)
                );
            """);
            
            // Ingredient spending summary (for faster reporting)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ingredient_spending (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  ingredientName TEXT NOT NULL,
                  month TEXT NOT NULL,
                  year INTEGER NOT NULL,
                  totalQuantity REAL NOT NULL,
                  totalCost REAL NOT NULL,
                  FOREIGN KEY (ingredientName) REFERENCES ingredients(name),
                  UNIQUE(ingredientName, month, year)
                );
            """);

            // Other expenses table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS other_expenses (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  type TEXT NOT NULL,
                  description TEXT,
                  cost REAL NOT NULL,
                  date TEXT NOT NULL
                );
            """);

            // Current feed mix table (for persistence)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS current_feed_mix (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  ingredient_name TEXT NOT NULL,
                  weight REAL NOT NULL,
                  protein REAL NOT NULL,
                  fat REAL NOT NULL,
                  fiber REAL NOT NULL,
                  lysine REAL NOT NULL
                );
            """);

        } catch (SQLException e) {
            // Log and/or show an alert
            System.err.println("Failed to initialize database:");
            e.printStackTrace();
        }
    }
}
