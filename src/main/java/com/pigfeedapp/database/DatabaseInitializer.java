package com.pigfeedapp.pigfeedapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseInitializer {

    private static final String DATABASE_URL = "jdbc:sqlite:pigfeed.db";

    public static void initializeDatabase() {
        // Check for each known ingredient and insert it if it's not present.
        // This ensures user modified values aren't overwritten.
        if (!isIngredientPresent("Corn")) {
            insertIngredient("Corn", 7.00, 3.00, 3.00, 0.30);
        }
        // Repeat for other default ingredients as needed...
    }

    private static boolean isIngredientPresent(String ingredientName) {
        String query = "SELECT count(*) FROM ingredients WHERE name = ?";
        try (
                Connection conn = DriverManager.getConnection(DATABASE_URL);
                PreparedStatement pstmt = conn.prepareStatement(query)
        ) {
            pstmt.setString(1, ingredientName);
            ResultSet rs = pstmt.executeQuery();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void insertIngredient(String name, double crudeProtein, double crudeFat, double crudeFiber, double lysine) {
        String sql = "INSERT INTO ingredients(name, crudeProtein, crudeFat, crudeFiber, lysine) VALUES(?, ?, ?, ?, ?)";
        try (
                Connection conn = DriverManager.getConnection(DATABASE_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, name);
            pstmt.setDouble(2, crudeProtein);
            pstmt.setDouble(3, crudeFat);
            pstmt.setDouble(4, crudeFiber);
            pstmt.setDouble(5, lysine);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
