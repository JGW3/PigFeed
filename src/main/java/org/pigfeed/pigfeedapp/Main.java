package org.pigfeed.pigfeedapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
        stage.setScene(scene);
        stage.sizeToScene();     // make room for any padding
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
