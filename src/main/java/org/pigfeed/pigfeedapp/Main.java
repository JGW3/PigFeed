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
        // Ensure DB file & tables exist before any UI loads
        DatabaseHelper.initializeDatabase();

        // Load welcome screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("welcome-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        stage.setTitle("Pig Feed App");
        
        // Set application icon
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/pigLogo.png")));
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }
        
        stage.setScene(scene);
        stage.sizeToScene();     // make room for any padding
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
