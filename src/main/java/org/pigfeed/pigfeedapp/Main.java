package org.pigfeed.pigfeedapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.pigfeed.pigfeedapp.database.DatabaseHelper;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Load main tabbed interface first for fast UI display
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main-tabbed-view.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 950, 750);
        stage.setTitle("Pig Feed App");
        
        // Set application icon with multiple sizes
        try {
            stage.getIcons().addAll(
                new Image(getClass().getResourceAsStream("/images/pigLogo.png")),
                new Image(getClass().getResourceAsStream("/images/pigfeedLogoUpscale.png")),
                new Image(getClass().getResourceAsStream("/images/pigfeedLogoUpscale2.png"))
            );
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }
        
        stage.setScene(scene);
        stage.sizeToScene();     // make room for any padding
        stage.show();
        
        // Initialize database in background thread immediately after UI is shown
        new Thread(() -> {
            try {
                DatabaseHelper.initializeDatabase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
