package com.pigfeedapp.pigfeedapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FeedMixCalculatorController {

    @FXML
    private ComboBox<String> ingredientDropdown;
    @FXML
    private TextField ingredientField;
    @FXML
    private TextField crudeProteinField;
    @FXML
    private TextField crudeFatField;
    @FXML
    private TextField crudeFiberField;
    @FXML
    private TextField lysineField;

    // This method initializes the drop-down values and listens for changes to dynamically load data.
    @FXML
    public void initialize() {
        // Add predefined ingredients to the dropdown
        ingredientDropdown.getItems().addAll("Corn", "Milo", "Alfalfa Pellets", "Custom");

        // Listen for dropdown value changes
        ingredientDropdown.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!"Custom".equals(newValue)) {
                loadIngredientData(newValue);
            }
        });
    }

    // This method gets invoked when the save button is pressed on the UI.
    @FXML
    private void saveIngredientInfo() {
        String ingredient = ingredientDropdown.getValue();
        if ("Custom".equals(ingredient)) {
            ingredient = ingredientField.getText();
        }

        // Convert user input to the respective types.
        // Here, some error handling would be beneficial in case of wrong input types.
        double crudeProtein = Double.parseDouble(crudeProteinField.getText());
        double crudeFat = Double.parseDouble(crudeFatField.getText());
        double crudeFiber = Double.parseDouble(crudeFiberField.getText());
        double lysine = Double.parseDouble(lysineField.getText());

        // Save these data points to the database.
        saveToDatabase(ingredient, crudeProtein, crudeFat, crudeFiber, lysine);
    }

    // This method fetches ingredient data from the database to populate the UI fields.
    private void loadIngredientData(String ingredient) {
        // TODO: Implement fetching logic from SQLite for this ingredient and populate the fields
    }

    // This method saves ingredient data to the database.
    private void saveToDatabase(String ingredient, double crudeProtein, double crudeFat, double crudeFiber, double lysine) {
        // SQL query for inserting or replacing ingredient data in the database.
        String sql = "INSERT OR REPLACE INTO ingredients(name, crudeProtein, crudeFat, crudeFiber, lysine) VALUES(?, ?, ?, ?, ?)";

        try (
                // Establish a connection to the SQLite database.
                Connection conn = DriverManager.getConnection("jdbc:sqlite:pigfeed.db");

                // Prepare the SQL statement with placeholders to prevent SQL injection.
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            // Set the values for the placeholders.
            pstmt.setString(1, ingredient);
            pstmt.setDouble(2, crudeProtein);
            pstmt.setDouble(3, crudeFat);
            pstmt.setDouble(4, crudeFiber);
            pstmt.setDouble(5, lysine);

            // Execute the update to insert the data.
            pstmt.executeUpdate();

        } catch (SQLException e) {
            // Handle any database-related exceptions.
            System.err.println("Failed to save ingredient data to the database.");
            e.printStackTrace();
        }
    }
}
