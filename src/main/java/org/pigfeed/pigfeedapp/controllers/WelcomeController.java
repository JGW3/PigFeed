package org.pigfeed.pigfeedapp.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.image.ImageView;
import java.io.IOException;

public class WelcomeController {

    @FXML private Button costTrackerButton;
    @FXML private Button feedMixButton;
    @FXML private Button aboutButton;
    @FXML private ImageView pigLogo;

    @FXML
    private void onCostTracker() {
        try {
            // Load the cost-tracker FXML
            Parent costTrackerRoot = FXMLLoader.load(
                    getClass().getResource("/org/pigfeed/pigfeedapp/cost-tracker-view.fxml")
            );

            // Grab the stage from any control in the current scene
            Stage stage = (Stage) costTrackerButton.getScene().getWindow();

            // Create & show the new scene
            Scene costTrackerScene = new Scene(costTrackerRoot, 800, 600);
            stage.setScene(costTrackerScene);
            stage.setTitle("Pig Cost Tracker");
            // Add icon
            try {
                stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/pigLogo.png")));
            } catch (Exception e) {
                System.err.println("Could not load application icon: " + e.getMessage());
            }
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onFeedMix() {
        try {
            // Load the feed‑mix FXML
            Parent feedMixRoot = FXMLLoader.load(
                    getClass().getResource("/org/pigfeed/pigfeedapp/feed-mix-view.fxml")
            );

            // Grab the stage from any control in the current scene
            Stage stage = (Stage) feedMixButton.getScene().getWindow();

            // Create & show the new scene
            Scene feedMixScene = new Scene(feedMixRoot, 900, 700);
            stage.setScene(feedMixScene);
            stage.setTitle("Pig Feed Mix Calculator");
            // Add icon
            try {
                stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/pigLogo.png")));
            } catch (Exception e) {
                System.err.println("Could not load application icon: " + e.getMessage());
            }
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void onAbout() {
        // Create an About dialog with clickable GitHub link
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("About Pig Feed App");
        alert.setHeaderText("Pig Feed Application");
        
        // Create the content with clickable link
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox();
        content.setSpacing(10);
        
        javafx.scene.control.Label description = new javafx.scene.control.Label(
            "A comprehensive pig feed management application for tracking costs and calculating optimal feed mixes.\n\n" +
            "Features:\n" +
            "• Feed mix calculator with nutrition optimization\n" +
            "• Cost tracking with price per pound calculations\n" +
            "• Spending reports and analytics\n" +
            "• Feed type management\n\n" +
            "Visit our GitHub repository for updates and support:");
        description.setWrapText(true);
        
        javafx.scene.control.Hyperlink githubLink = new javafx.scene.control.Hyperlink("https://github.com/JGW3/PigFeed");
        githubLink.setOnAction(e -> {
            // Copy to clipboard and show notification
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent clipboardContent = new javafx.scene.input.ClipboardContent();
            clipboardContent.putString("https://github.com/JGW3/PigFeed");
            clipboard.setContent(clipboardContent);
            
            javafx.scene.control.Alert linkAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            linkAlert.setTitle("GitHub Link");
            linkAlert.setHeaderText("Link Copied to Clipboard");
            linkAlert.setContentText("GitHub link has been copied to your clipboard!\nPaste it into your web browser to visit the repository.");
            
            // Add icon to dialog
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) linkAlert.getDialogPane().getScene().getWindow(); 
                stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/pigLogo.png")));
            } catch (Exception ex) {
                // Ignore if can't set icon
            }
            
            linkAlert.showAndWait();
        });
        
        content.getChildren().addAll(description, githubLink);
        alert.getDialogPane().setContent(content);
        
        // Set dialog size and make it resizable for proper word wrapping
        alert.getDialogPane().setPrefSize(500, 400);
        alert.setResizable(true);
        
        // Add icon to dialog
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow(); 
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/pigLogo.png")));
        } catch (Exception e) {
            // Ignore if can't set icon
        }
        
        alert.showAndWait();
    }
}
