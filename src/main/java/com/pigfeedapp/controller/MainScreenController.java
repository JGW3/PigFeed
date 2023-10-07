package com.pigfeedapp.pigfeedapp.controller;

// Necessary imports for the JavaFX components and event handling
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainScreenController {

    // This method is tied to the Feed Cost Tracker button's action
    // It will execute when the button is clicked.
    @FXML
    protected void openFeedCostTracker(ActionEvent event) {
        // This is a placeholder logic for opening the Feed Cost Tracker.
        // More complex logic can be implemented based on your application's requirements.
        System.out.println("Opening Feed Cost Tracker...");
    }

    // This method is tied to the Pig Feed Mix Calculator button's action
    // It will execute when the button is clicked.
    @FXML
    protected void openFeedMixCalculator(ActionEvent event) {
        try {
            // Load the FeedMixCalculator.fxml file using FXMLLoader.
            Parent feedMixCalculatorParent = FXMLLoader.load(getClass().getResource("/fxml/FeedMixCalculator.fxml"));

            // Create a new scene using the loaded FXML layout.
            Scene feedMixCalculatorScene = new Scene(feedMixCalculatorParent);

            // Get the current stage from the ActionEvent's source.
            Stage window = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            // Set the new scene to the current stage and display it.
            window.setScene(feedMixCalculatorScene);
            window.show();
        } catch (Exception e) {
            // Print the exception's stack trace for debugging purposes.
            // You might want to handle exceptions differently in a production environment.
            e.printStackTrace();
        }
    }
}
