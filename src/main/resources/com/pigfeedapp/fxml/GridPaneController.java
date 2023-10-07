package com.pigfeedapp.com;

import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

public class GridPaneController {
    @FXML
    private GridPane mainGridPane;

    public void initialize() {
        // Create row constraints for the "Add Ingredients" section
        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setVgrow(Priority.ALWAYS);

        // Apply the row constraints to the second row of the mainGridPane
        mainGridPane.getRowConstraints().add(2, rowConstraints);
    }

    @FXML
    private void saveIngredientInfo() {
        // Your code to save the ingredient information goes here
    }
}
