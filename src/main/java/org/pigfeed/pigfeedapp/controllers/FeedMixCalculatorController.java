package org.pigfeed.pigfeedapp.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.converter.NumberStringConverter;
import org.pigfeed.pigfeedapp.model.FeedMixEntry;

import java.sql.*;
import java.io.IOException;

public class FeedMixCalculatorController {

    private static final String DB_URL = "jdbc:sqlite:pigfeed.db";

    // --- Editor pane controls --- (removed TitledPane, now always visible)
    @FXML private ComboBox<String> ingredientDropdown;
    @FXML private TextField ingredientField;
    @FXML private TextField crudeProteinField;
    @FXML private TextField crudeFatField;
    @FXML private TextField crudeFiberField;
    @FXML private TextField lysineField;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> priceUnitCombo;

    // --- Mix table and columns ---
    @FXML private TableView<FeedMixEntry> feedTable;
    @FXML private TableColumn<FeedMixEntry, String> ingredientCol;
    @FXML private TableColumn<FeedMixEntry, Number> weightCol;
    @FXML private TableColumn<FeedMixEntry, Number> proteinCol;
    @FXML private TableColumn<FeedMixEntry, Number> fatCol;
    @FXML private TableColumn<FeedMixEntry, Number> fiberCol;
    @FXML private TableColumn<FeedMixEntry, Number> lysineCol;
    // @FXML private TableColumn<FeedMixEntry, Number> percentCol;
    
    // Totals labels
    @FXML private Label totalWeightLabel;
    @FXML private Label totalProteinLabel;
    @FXML private Label totalFatLabel;
    @FXML private Label totalFiberLabel;
    @FXML private Label totalLysineLabel;

    private final ObservableList<FeedMixEntry> feedData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1) Initialize price unit dropdown
        priceUnitCombo.getItems().addAll("1lb", "50lbs", "100lbs", "ton");
        priceUnitCombo.setValue("50lbs");
        
        // 2) Load ingredients from DB
        loadAllIngredients();

        // 3) When user selects an ingredient, load its data into the form for editing
        ingredientDropdown.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                loadIngredientDataToForm(newVal);
            }
        });

        // 4) Configure mix table columns
        ingredientCol.setCellValueFactory(cd -> cd.getValue().ingredientProperty());
        weightCol.setCellValueFactory(cd -> cd.getValue().weightProperty());
        proteinCol.setCellValueFactory(cd -> cd.getValue().proteinProperty());
        fatCol.setCellValueFactory(cd -> cd.getValue().fatProperty());
        fiberCol.setCellValueFactory(cd -> cd.getValue().fiberProperty());
        lysineCol.setCellValueFactory(cd -> cd.getValue().lysineProperty());
        // percentCol.setCellValueFactory(cd -> cd.getValue().percentProperty());

        // 4) Make ingredient, weight and percent editable
        feedTable.setEditable(true);
        
        // Set up ingredient column with ComboBox
        ingredientCol.setCellFactory(column -> new TableCell<FeedMixEntry, String>() {
            private ComboBox<String> comboBox = new ComboBox<>();
            
            {
                comboBox.setEditable(false);
                comboBox.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
                comboBox.setOnAction(e -> {
                    FeedMixEntry entry = getTableRow().getItem();
                    if (entry != null && comboBox.getValue() != null) {
                        if ("(remove)".equals(comboBox.getValue())) {
                            // Remove this entry
                            feedData.remove(entry);
                            ensureEmptyRowExists();
                            recalcPercentages();
                        } else if (!"Select ingredient...".equals(comboBox.getValue())) {
                            // Preserve existing weight when changing ingredient
                            double currentWeight = entry.getWeight();
                            entry.setIngredient(comboBox.getValue());
                            loadIngredientDataForEntry(entry, comboBox.getValue());
                            // Restore weight if it was set
                            if (currentWeight > 0) {
                                entry.setWeight(currentWeight);
                            }
                            ensureEmptyRowExists();
                            recalcPercentages();
                        }
                    }
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    // Refresh items from current dropdown
                    comboBox.getItems().clear();
                    comboBox.getItems().add("Select ingredient...");
                    comboBox.getItems().addAll(ingredientDropdown.getItems());
                    comboBox.getItems().add("(remove)");
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                }
            }
        });
        
        // Set up weight column with yellow background and single-click editing
        weightCol.setCellFactory(column -> new TableCell<FeedMixEntry, Number>() {
            private TextField textField = new TextField();
            private boolean editing = false;
            
            {
                textField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
                textField.setOnAction(e -> commitEdit());
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused && editing) {
                        commitEdit();
                    }
                });
                
                // Single click to edit
                setOnMouseClicked(e -> {
                    if (e.getClickCount() == 1 && !isEmpty()) {
                        startEdit();
                    }
                });
            }
            
            private void commitEdit() {
                if (!editing) return;
                
                try {
                    double value = Double.parseDouble(textField.getText());
                    FeedMixEntry entry = getTableRow().getItem();
                    if (entry != null) {
                        entry.setWeight(value);
                        recalcPercentages();
                    }
                } catch (NumberFormatException e) {
                    // Keep the old value if invalid input
                }
                editing = false;
                updateDisplay();
            }
            
            @Override
            public void startEdit() {
                super.startEdit();
                FeedMixEntry entry = getTableRow().getItem();
                if (entry != null && !editing) {
                    editing = true;
                    textField.setText(String.format("%.1f", entry.getWeight()));
                    setGraphic(textField);
                    setText(null);
                    textField.requestFocus();
                    textField.selectAll();
                }
            }
            
            @Override
            public void cancelEdit() {
                super.cancelEdit();
                editing = false;
                updateDisplay();
            }
            
            private void updateDisplay() {
                FeedMixEntry entry = getTableRow().getItem();
                if (entry != null && !editing) {
                    setText(String.format("%.1f", entry.getWeight()));
                    setGraphic(null);
                    setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
                }
            }
            
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else if (!editing) {
                    setText(item == null ? "0.0" : String.format("%.1f", item.doubleValue()));
                    setGraphic(null);
                    setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
                }
            }
        });
        
        // Set up percent column with yellow background  
        // percentCol.setCellFactory(column -> {
        //     TextFieldTableCell<FeedMixEntry, Number> cell = new TextFieldTableCell<>(new NumberStringConverter());
        //     cell.setStyle("-fx-background-color: lightyellow;");
        //     return cell;
        // });
        // percentCol.setOnEditCommit(evt -> {
        //     evt.getRowValue().setPercent(evt.getNewValue().doubleValue());
        //     recalcWeights();
        // });

        feedTable.setItems(feedData);
        
        // Load saved feed mix from database
        loadSavedFeedMix();
        
        // Ensure at least one empty row exists
        ensureEmptyRowExists();
    }

    // Called by "Save New Ingredient" button
    @FXML
    private void saveIngredientInfo() {
        String name = ingredientField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Please enter an ingredient name.");
            return;
        }

        // Check if ingredient already exists
        if (ingredientExists(name)) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Ingredient Exists");
            confirm.setHeaderText("Ingredient '" + name + "' already exists.");
            confirm.setContentText("Do you want to update it with new values?");
            
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        double protein = parseDouble(crudeProteinField.getText());
        double fat     = parseDouble(crudeFatField.getText());
        double fiber   = parseDouble(crudeFiberField.getText());
        double lysine  = parseDouble(lysineField.getText());
        double price   = parseDouble(priceField.getText());
        String priceUnit = priceUnitCombo.getValue();

        String sql = "INSERT OR REPLACE INTO ingredients(name, crudeProtein, crudeFat, crudeFiber, lysine, price, priceUnit) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, protein);
            ps.setDouble(3, fat);
            ps.setDouble(4, fiber);
            ps.setDouble(5, lysine);
            ps.setDouble(6, price);
            ps.setString(7, priceUnit);
            ps.executeUpdate();
            
            showAlert("Ingredient '" + name + "' saved successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error saving ingredient to database.");
            return;
        }

        // Refresh dropdown and clear form
        loadAllIngredients();
        clearIngredientForm();
    }
    
    @FXML
    private void clearIngredientForm() {
        ingredientField.clear();
        crudeProteinField.clear();
        crudeFatField.clear();
        crudeFiberField.clear();
        lysineField.clear();
        priceField.clear();
        priceUnitCombo.setValue("50lbs");
        ingredientDropdown.getSelectionModel().clearSelection();
    }
    
    private boolean ingredientExists(String name) {
        String sql = "SELECT COUNT(*) FROM ingredients WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Load all ingredient names into the dropdown
    private void loadAllIngredients() {
        ingredientDropdown.getItems().clear();
        String sql = "SELECT name FROM ingredients ORDER BY name";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ingredientDropdown.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load nutrition data for an existing ingredient into the form for editing
    private void loadIngredientDataToForm(String name) {
        String sql = "SELECT crudeProtein, crudeFat, crudeFiber, lysine, price, priceUnit FROM ingredients WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ingredientField.setText(name);
                    crudeProteinField.setText(String.format("%.1f", rs.getDouble("crudeProtein")));
                    crudeFatField.setText(String.format("%.1f", rs.getDouble("crudeFat")));
                    crudeFiberField.setText(String.format("%.1f", rs.getDouble("crudeFiber")));
                    lysineField.setText(String.format("%.2f", rs.getDouble("lysine")));
                    priceField.setText(String.format("%.2f", rs.getDouble("price")));
                    String unit = rs.getString("priceUnit");
                    if (unit != null && !unit.isEmpty()) {
                        priceUnitCombo.setValue(unit);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Load nutrition data for a table entry
    private void loadIngredientDataForEntry(FeedMixEntry entry, String name) {
        String sql = "SELECT crudeProtein, crudeFat, crudeFiber, lysine, price, priceUnit FROM ingredients WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    entry.setProtein(rs.getDouble("crudeProtein"));
                    entry.setFat(rs.getDouble("crudeFat"));
                    entry.setFiber(rs.getDouble("crudeFiber"));
                    entry.setLysine(rs.getDouble("lysine"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Recalculate the percent column based on weights and update totals
    private void recalcPercentages() {
        // Filter out empty rows for calculations
        var validEntries = feedData.stream()
            .filter(entry -> !"Select ingredient...".equals(entry.getIngredient()) && 
                           entry.getWeight() > 0)
            .toList();
        
        double totalWeight = validEntries.stream().mapToDouble(FeedMixEntry::getWeight).sum();
        
        // Calculate percentages for each entry
        for (FeedMixEntry e : feedData) {
            if (validEntries.contains(e)) {
                double pct = totalWeight == 0 ? 0 : (e.getWeight() / totalWeight) * 100.0;
                e.setPercent(Math.round(pct * 100.0) / 100.0);
            } else {
                e.setPercent(0.0);
            }
        }
        
        updateTotalsDisplay(validEntries, totalWeight);
        
        // Auto-save the current mix
        saveFeedMix();
    }
    
    private void updateTotalsDisplay(java.util.List<FeedMixEntry> validEntries, double totalWeight) {
        if (totalWeight == 0) {
            // Show zeros when no valid entries
            totalWeightLabel.setText("0.0 lbs");
            totalProteinLabel.setText("0.0%");
            totalFatLabel.setText("0.0%");
            totalFiberLabel.setText("0.0%");
            totalLysineLabel.setText("0.0%");
            return;
        }
        
        // Calculate weighted averages for nutrition
        double totalProtein = validEntries.stream()
            .mapToDouble(e -> (e.getWeight() / totalWeight) * e.getProtein())
            .sum();
        double totalFat = validEntries.stream()
            .mapToDouble(e -> (e.getWeight() / totalWeight) * e.getFat())
            .sum();
        double totalFiber = validEntries.stream()
            .mapToDouble(e -> (e.getWeight() / totalWeight) * e.getFiber())
            .sum();
        double totalLysine = validEntries.stream()
            .mapToDouble(e -> (e.getWeight() / totalWeight) * e.getLysine())
            .sum();
        
        // Update UI labels
        totalWeightLabel.setText(String.format("%.1f lbs", totalWeight));
        totalProteinLabel.setText(String.format("%.1f%%", totalProtein));
        totalFatLabel.setText(String.format("%.1f%%", totalFat));
        totalFiberLabel.setText(String.format("%.1f%%", totalFiber));
        totalLysineLabel.setText(String.format("%.2f%%", totalLysine));
    }

    // Recalculate weights based on percent distribution
    private void recalcWeights() {
        double totalWeight = feedData.stream().mapToDouble(FeedMixEntry::getWeight).sum();
        double totalPct    = feedData.stream().mapToDouble(FeedMixEntry::getPercent).sum();
        for (FeedMixEntry e : feedData) {
            double w = totalPct == 0 ? 0 : (e.getPercent() / totalPct) * totalWeight;
            e.setWeight(Math.round(w * 100.0) / 100.0);
        }
    }

    @FXML
    private void optimizeMix() {
        openNutritionOptimizationDialog();
    }
    
    private void openNutritionOptimizationDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Optimize by Nutrition");
        dialog.setHeaderText("Feed Mix Optimization");
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        // Instructions
        Label instructions = new Label("Recommended: 6 lbs per pig per day");
        instructions.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(instructions, 0, 0, 2, 1);
        
        Label nutritionHeader = new Label("Recommended Nutrition for Growing Pigs:");
        nutritionHeader.setStyle("-fx-font-weight: bold;");
        grid.add(nutritionHeader, 0, 1, 2, 1);
        
        // Target weight
        Label weightLabel = new Label("Total Feed Weight (lbs):");
        TextField weightField = new TextField("6.0");
        weightField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(weightLabel, 0, 2);
        grid.add(weightField, 1, 2);
        
        // Nutritional targets (editable)
        Label proteinLabel = new Label("Crude Protein (%):");
        TextField proteinField = new TextField("16.0");
        proteinField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(proteinLabel, 0, 3);
        grid.add(proteinField, 1, 3);
        
        Label fatLabel = new Label("Fat (%):");
        TextField fatField = new TextField("5.0");
        fatField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(fatLabel, 0, 4);
        grid.add(fatField, 1, 4);
        
        Label fiberLabel = new Label("Fiber (%):");
        TextField fiberField = new TextField("4.5");
        fiberField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(fiberLabel, 0, 5);
        grid.add(fiberField, 1, 5);
        
        Label lysineLabel = new Label("Lysine (%):");
        TextField lysineField = new TextField("0.95");
        lysineField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(lysineLabel, 0, 6);
        grid.add(lysineField, 1, 6);
        
        // Ranges info
        Label rangesInfo = new Label(
            "Recommended ranges:\n" +
            "• Fat: 3.5-6.5%\n" +
            "• Fiber: 3-6%\n" +
            "• Protein: 16% or more\n" +
            "• Lysine: 0.90-1.0% (150lbs to show day)"
        );
        rangesInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        grid.add(rangesInfo, 0, 7, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    double targetWeight = Double.parseDouble(weightField.getText());
                    double targetProtein = Double.parseDouble(proteinField.getText());
                    double targetFat = Double.parseDouble(fatField.getText());
                    double targetFiber = Double.parseDouble(fiberField.getText());
                    double targetLysine = Double.parseDouble(lysineField.getText());
                    
                    optimizeFeedMix(targetWeight, targetProtein, targetFat, targetFiber, targetLysine);
                } catch (NumberFormatException e) {
                    showAlert("Please enter valid numbers for all fields.");
                }
            }
        });
    }
    
    private void optimizeFeedMix(double targetWeight, double targetProtein, double targetFat, 
                               double targetFiber, double targetLysine) {
        // Improved optimization algorithm with better nutrient balancing
        
        var validIngredients = feedData.stream()
            .filter(entry -> !"Select ingredient...".equals(entry.getIngredient()) &&
                           entry.getProtein() > 0)
            .collect(java.util.stream.Collectors.toList());
        
        if (validIngredients.isEmpty()) {
            showAlert("Please add some ingredients to the mix first.");
            return;
        }
        
        if (validIngredients.size() == 1) {
            // If only one ingredient, just set its weight to target
            validIngredients.get(0).setWeight(targetWeight);
        } else {
            // Multi-ingredient optimization using iterative approach
            optimizeMultiIngredient(validIngredients, targetWeight, targetProtein, targetFat, targetFiber, targetLysine);
        }
        
        recalcPercentages();
        feedTable.refresh();
        
        showAlert("Feed mix optimized for target nutrition values!");
    }
    
    private void optimizeMultiIngredient(java.util.List<FeedMixEntry> ingredients, double targetWeight,
                                       double targetProtein, double targetFat, double targetFiber, double targetLysine) {
        
        // Basic approach: Start with typical feed ratios and adjust
        int numIngredients = ingredients.size();
        if (numIngredients == 0) return;
        
        // Identify ingredients by type to use typical ratios
        double[] weights = new double[numIngredients];
        
        // Use typical pig feed ratios as starting point
        for (int i = 0; i < numIngredients; i++) {
            FeedMixEntry entry = ingredients.get(i);
            String name = entry.getIngredient().toLowerCase();
            
            // Assign typical starting weights based on ingredient type
            if (name.contains("corn")) {
                weights[i] = targetWeight * 0.4; // 40% corn typical
            } else if (name.contains("soybean")) {
                weights[i] = targetWeight * 0.2; // 20% soybean typical
            } else if (name.contains("alfalfa")) {
                weights[i] = targetWeight * 0.2; // 20% alfalfa typical
            } else if (name.contains("milo")) {
                weights[i] = targetWeight * 0.15; // 15% milo typical
            } else if (name.contains("egg")) {
                weights[i] = targetWeight * 0.02; // 2% eggs typical
            } else {
                weights[i] = targetWeight * 0.03; // Small amount for others
            }
        }
        
        // Normalize to exact target weight
        double currentTotal = java.util.Arrays.stream(weights).sum();
        if (currentTotal > 0) {
            for (int i = 0; i < numIngredients; i++) {
                weights[i] = (weights[i] / currentTotal) * targetWeight;
            }
        }
        
        // Simple adjustment: If protein is too low, increase high-protein ingredients
        // If protein is too high, increase low-protein ingredients
        for (int iteration = 0; iteration < 5; iteration++) {
            // Calculate current nutrition
            double currentProtein = 0;
            for (int i = 0; i < numIngredients; i++) {
                currentProtein += (weights[i] / targetWeight) * ingredients.get(i).getProtein();
            }
            
            // Stop if close enough
            if (Math.abs(currentProtein - targetProtein) < 1.0) break;
            
            // Find highest and lowest protein ingredients
            int highProteinIdx = 0, lowProteinIdx = 0;
            double highestProtein = ingredients.get(0).getProtein();
            double lowestProtein = ingredients.get(0).getProtein();
            
            for (int i = 1; i < numIngredients; i++) {
                if (ingredients.get(i).getProtein() > highestProtein) {
                    highestProtein = ingredients.get(i).getProtein();
                    highProteinIdx = i;
                }
                if (ingredients.get(i).getProtein() < lowestProtein) {
                    lowestProtein = ingredients.get(i).getProtein();
                    lowProteinIdx = i;
                }
            }
            
            // Adjust weights
            double adjustment = Math.min(targetWeight * 0.1, Math.abs(currentProtein - targetProtein) * 0.5);
            
            if (currentProtein < targetProtein) {
                // Need more protein - increase high protein ingredient, decrease low protein
                weights[highProteinIdx] += adjustment;
                weights[lowProteinIdx] = Math.max(0.1, weights[lowProteinIdx] - adjustment);
            } else {
                // Need less protein - increase low protein ingredient, decrease high protein  
                weights[lowProteinIdx] += adjustment;
                weights[highProteinIdx] = Math.max(0.1, weights[highProteinIdx] - adjustment);
            }
            
            // Renormalize
            currentTotal = java.util.Arrays.stream(weights).sum();
            for (int i = 0; i < numIngredients; i++) {
                weights[i] = (weights[i] / currentTotal) * targetWeight;
            }
        }
        
        // Apply the calculated weights
        for (int i = 0; i < numIngredients; i++) {
            ingredients.get(i).setWeight(Math.round(weights[i] * 100.0) / 100.0);
        }
    }
    
    private double calculateIngredientScore(FeedMixEntry entry, double targetProtein, 
                                          double targetFat, double targetFiber, double targetLysine) {
        // Score based on how close the ingredient is to target values
        // Higher scores for ingredients closer to targets
        double proteinScore = Math.max(0, 100 - Math.abs(entry.getProtein() - targetProtein));
        double fatScore = Math.max(0, 100 - Math.abs(entry.getFat() - targetFat));
        double fiberScore = Math.max(0, 100 - Math.abs(entry.getFiber() - targetFiber));
        double lysineScore = Math.max(0, 100 - Math.abs(entry.getLysine() - targetLysine) * 100);
        
        return (proteinScore + fatScore + fiberScore + lysineScore) / 4.0;
    }

    // Ensure there's always an empty row available for new entries
    private void ensureEmptyRowExists() {
        // Check if there's already an empty row (no ingredient selected)
        boolean hasEmptyRow = feedData.stream()
                .anyMatch(entry -> "Select ingredient...".equals(entry.getIngredient()) || 
                                 entry.getIngredient() == null || 
                                 entry.getIngredient().trim().isEmpty());
        
        if (!hasEmptyRow) {
            FeedMixEntry newEntry = new FeedMixEntry();
            newEntry.setIngredient("Select ingredient...");
            newEntry.setProtein(0.0);
            newEntry.setFat(0.0);
            newEntry.setFiber(0.0);
            newEntry.setLysine(0.0);
            newEntry.setWeight(0.0);
            newEntry.setPercent(0.0);
            
            feedData.add(newEntry);
        }
    }

    @FXML
    private void clearMix() {
        if (!feedData.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Clear Mix");
            alert.setHeaderText("Are you sure?");
            alert.setContentText("This will remove all ingredients from the current mix.");
            
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                feedData.clear();
                ensureEmptyRowExists();
            }
        }
    }

    @FXML
    private void backToWelcome() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/org/pigfeed/pigfeedapp/welcome-view.fxml")
            );
            javafx.scene.Parent welcomeRoot = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) feedTable.getScene().getWindow();
            javafx.scene.Scene welcomeScene = new javafx.scene.Scene(welcomeRoot);
            stage.setScene(welcomeScene);
            stage.setTitle("Pig Feed App");
            stage.sizeToScene();
            
        } catch (java.io.IOException e) {
            showAlert("Could not return to welcome screen: " + e.getMessage());
        }
    }

    // Helpers

    private double parseDouble(String txt) {
        try { return Double.parseDouble(txt); }
        catch (NumberFormatException e) { return 0; }
    }

    private void clearNutritionFields() {
        crudeProteinField.clear();
        crudeFatField.clear();
        crudeFiberField.clear();
        lysineField.clear();
    }


    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.showAndWait();
    }
    
    // Save current feed mix to database
    private void saveFeedMix() {
        // Clear existing saved mix
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM current_feed_mix");
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        
        // Save current mix (excluding empty rows)
        String sql = "INSERT INTO current_feed_mix(ingredient_name, weight, protein, fat, fiber, lysine) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            for (FeedMixEntry entry : feedData) {
                if (!"Select ingredient...".equals(entry.getIngredient()) && 
                    entry.getIngredient() != null && 
                    !entry.getIngredient().trim().isEmpty() &&
                    entry.getWeight() > 0) {
                    
                    ps.setString(1, entry.getIngredient());
                    ps.setDouble(2, entry.getWeight());
                    ps.setDouble(3, entry.getProtein());
                    ps.setDouble(4, entry.getFat());
                    ps.setDouble(5, entry.getFiber());
                    ps.setDouble(6, entry.getLysine());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Load saved feed mix from database
    private void loadSavedFeedMix() {
        feedData.clear();
        
        String sql = "SELECT ingredient_name, weight, protein, fat, fiber, lysine FROM current_feed_mix";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                FeedMixEntry entry = new FeedMixEntry();
                entry.setIngredient(rs.getString("ingredient_name"));
                entry.setWeight(rs.getDouble("weight"));
                entry.setProtein(rs.getDouble("protein"));
                entry.setFat(rs.getDouble("fat"));
                entry.setFiber(rs.getDouble("fiber"));
                entry.setLysine(rs.getDouble("lysine"));
                
                feedData.add(entry);
            }
            
            // Recalculate totals if we loaded data
            if (!feedData.isEmpty()) {
                recalcPercentages();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
