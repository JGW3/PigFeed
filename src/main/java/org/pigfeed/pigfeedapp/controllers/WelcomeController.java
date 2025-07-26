package org.pigfeed.pigfeedapp.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.scene.image.ImageView;
import org.pigfeed.pigfeedapp.controllers.FeedMixCalculatorController;
import java.io.IOException;

public class WelcomeController {

    @FXML private Button costTrackerButton;
    @FXML private Button feedMixButton;
    @FXML private Button aboutButton;
    @FXML private ImageView pigLogo;
    
    @FXML
    public void initialize() {
        // Pre-load Feed Mix Calculator data in background to improve performance
        FeedMixCalculatorController.preloadData();
    }

    @FXML
    private void onCostTracker() {
        // Find the parent TabPane and switch to Cost Tracker tab
        TabPane tabPane = findParentTabPane();
        if (tabPane != null) {
            // Cost Tracker is index 1 (Welcome=0, Cost Tracker=1, Feed Mix=2)
            tabPane.getSelectionModel().select(1);
        }
    }

    @FXML
    private void onFeedMix() {
        // Find the parent TabPane and switch to Feed Mix tab
        TabPane tabPane = findParentTabPane();
        if (tabPane != null) {
            // Feed Mix is index 2 (Welcome=0, Cost Tracker=1, Feed Mix=2)
            tabPane.getSelectionModel().select(2);
        }
    }
    
    private TabPane findParentTabPane() {
        // Walk up the scene graph to find the TabPane
        javafx.scene.Node node = costTrackerButton;
        while (node != null) {
            if (node instanceof TabPane) {
                return (TabPane) node;
            }
            node = node.getParent();
        }
        return null;
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
            String url = "https://github.com/JGW3/PigFeed";
            boolean browserOpened = false;
            
            // Try to open in default browser first
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                        desktop.browse(new java.net.URI(url));
                        browserOpened = true;
                    }
                }
            } catch (Exception ex) {
                // Browser opening failed, will fall back to clipboard
            }
            
            if (!browserOpened) {
                // Fallback: Copy to clipboard and show notification
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent clipboardContent = new javafx.scene.input.ClipboardContent();
                clipboardContent.putString(url);
                clipboard.setContent(clipboardContent);
                
                javafx.scene.control.Alert linkAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                linkAlert.setTitle("GitHub Link");
                linkAlert.setHeaderText("Link Copied to Clipboard");
                linkAlert.setContentText("Could not open browser automatically.\nGitHub link has been copied to your clipboard!\nPaste it into your web browser to visit the repository.");
                
                // Add icon to dialog
                try {
                    javafx.stage.Stage stage = (javafx.stage.Stage) linkAlert.getDialogPane().getScene().getWindow(); 
                    stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/pigLogo.png")));
                } catch (Exception ex) {
                    // Ignore if can't set icon
                }
                
                linkAlert.showAndWait();
            }
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
