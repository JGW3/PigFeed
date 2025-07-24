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
            Scene costTrackerScene = new Scene(costTrackerRoot);
            stage.setScene(costTrackerScene);
            stage.setTitle("Pig Cost Tracker");
            stage.sizeToScene();  // optional: resize window to fit
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
            Scene feedMixScene = new Scene(feedMixRoot);
            stage.setScene(feedMixScene);
            stage.setTitle("Pig Feed Mix Calculator");
            stage.sizeToScene();  // optional: resize window to fit
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
