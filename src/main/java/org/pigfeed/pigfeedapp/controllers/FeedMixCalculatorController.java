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
import java.util.HashMap;
import java.util.Map;

public class FeedMixCalculatorController {

    private static final String DB_URL = "jdbc:sqlite:pigfeed.db";
    
    private boolean updatingComboBox = false; // Flag to prevent recursive updates
    private ObservableList<String> cachedIngredients = FXCollections.observableArrayList(); // Cache ingredients list
    private static boolean dataInitialized = false; // Prevent reloading data when switching screens
    private static ObservableList<String> staticCachedIngredients = FXCollections.observableArrayList();
    private static ObservableList<FeedMixEntry> staticFeedData = FXCollections.observableArrayList();

    // --- Editor pane controls --- (removed TitledPane, now always visible)
    @FXML private ComboBox<String> ingredientModeCombo;
    @FXML private TextField ingredientField;
    @FXML private TextField crudeProteinField;
    @FXML private TextField crudeFatField;
    @FXML private TextField crudeFiberField;
    @FXML private TextField lysineField;

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
    @FXML private Label avgPricePerLbLabel;
    @FXML private Label totalCostLabel;
    
    // Loading and content panes
    @FXML private VBox loadingPane;
    @FXML private VBox contentPane;
    

    private final ObservableList<FeedMixEntry> feedData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Fast initialization - only essential items
        setupBasicTable();
        setupDropdownPromptBehavior();
        
        // If data is already pre-loaded AND we have actual data, restore everything immediately
        if (dataInitialized && !staticFeedData.isEmpty()) {
            // Restore cached data immediately for instant display
            cachedIngredients.setAll(staticCachedIngredients);
            ingredientModeCombo.getItems().setAll(staticCachedIngredients);
            ingredientModeCombo.setValue("");
            feedData.setAll(staticFeedData);
            
            // Show content immediately, no spinner needed
            loadingPane.setVisible(false);
            contentPane.setVisible(true);
            
            // Still need to setup cell factories, but data is already visible
            javafx.application.Platform.runLater(() -> {
                setupTableCellFactories();
                setupIngredientModeListener();
                ensureEmptyRowExists();
                // Ensure prompt text is shown after all initialization
                ingredientModeCombo.setPromptText("Edit Existing Ingredient");
            });
        } else {
            // First time loading - show spinner and load data
            javafx.application.Platform.runLater(() -> {
                loadAllIngredients();
                setupTableCellFactories();
                setupIngredientModeListener();
                loadSavedFeedMix();
                ensureEmptyRowExists();
                dataInitialized = true;
                
                // Hide loading spinner and show content
                loadingPane.setVisible(false);
                contentPane.setVisible(true);
                
                // Ensure prompt text is shown after all initialization
                ingredientModeCombo.setPromptText("Edit Existing Ingredient");
            });
        }
    }
    
    private void setupDropdownPromptBehavior() {
        // Set initial prompt text
        ingredientModeCombo.setPromptText("Edit Existing Ingredient");
        
        // Hide prompt text when item is selected, show when cleared
        ingredientModeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Use Platform.runLater to ensure the prompt text is updated after the value change
            javafx.application.Platform.runLater(() -> {
                if (newVal != null && !newVal.trim().isEmpty()) {
                    ingredientModeCombo.setPromptText(""); // Hide prompt when something is selected
                } else {
                    ingredientModeCombo.setPromptText("Edit Existing Ingredient"); // Show prompt when nothing selected
                }
            });
        });
    }
    
    private void setupBasicTable() {
        // Basic table configuration without expensive cell factories
        ingredientCol.setCellValueFactory(cd -> cd.getValue().ingredientProperty());
        weightCol.setCellValueFactory(cd -> cd.getValue().weightProperty());
        proteinCol.setCellValueFactory(cd -> cd.getValue().proteinProperty());
        fatCol.setCellValueFactory(cd -> cd.getValue().fatProperty());
        fiberCol.setCellValueFactory(cd -> cd.getValue().fiberProperty());
        lysineCol.setCellValueFactory(cd -> cd.getValue().lysineProperty());
        feedTable.setItems(feedData);
        feedTable.setEditable(true);
    }
    
    private void setupIngredientModeListener() {
        ingredientModeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingComboBox && newVal != null && !newVal.isEmpty()) {
                try {
                    loadIngredientDataToForm(newVal);
                } catch (Exception e) {
                    System.err.println("Error loading ingredient data for: " + newVal);
                    e.printStackTrace();
                    updatingComboBox = true;
                    clearIngredientForm();
                    updatingComboBox = false;
                }
            }
        });
    }
    
    private void setupTableCellFactories() {
        
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
                    // Use cached ingredients list for better performance
                    comboBox.getItems().clear();
                    comboBox.getItems().add("Select ingredient...");
                    for (String ingredientName : cachedIngredients) {
                        if (ingredientName != null && !ingredientName.trim().isEmpty()) {
                            comboBox.getItems().add(ingredientName);
                        }
                    }
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

    }
    
    // Static method to pre-load data from Welcome screen
    public static void preloadData() {
        if (!dataInitialized) {
            // Run directly without Platform.runLater to avoid blocking UI thread
                staticCachedIngredients.clear();
                staticCachedIngredients.add(""); // Blank option
                
                String sql = "SELECT name FROM ingredients ORDER BY name";
                try (Connection conn = DriverManager.getConnection(DB_URL);
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        staticCachedIngredients.add(rs.getString("name"));
                    }
                    dataInitialized = true;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }
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
            
            // Add favicon to dialog
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) confirm.getDialogPane().getScene().getWindow();
                setApplicationIcon(stage);
            } catch (Exception e) {
                // Ignore if can't set icon
            }
            
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        double protein = parseDouble(crudeProteinField.getText());
        double fat     = parseDouble(crudeFatField.getText());
        double fiber   = parseDouble(crudeFiberField.getText());
        double lysine  = parseDouble(lysineField.getText());

        String sql = "INSERT OR REPLACE INTO ingredients(name, crudeProtein, crudeFat, crudeFiber, lysine) VALUES(?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, protein);
            ps.setDouble(3, fat);
            ps.setDouble(4, fiber);
            ps.setDouble(5, lysine);
            ps.executeUpdate();
            
            showAlert("Ingredient '" + name + "' saved successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error saving ingredient to database.");
            return;
        }

        // Refresh dropdown, table data, and clear form
        loadAllIngredients();
        refreshTableComboBoxes();
        refreshIngredientDataInTable(name);
        clearIngredientForm();
    }
    
    @FXML
    private void deleteSelectedIngredient() {
        String selectedIngredient = ingredientModeCombo.getValue();
        if (selectedIngredient == null || selectedIngredient.isEmpty()) {
            showAlert("Please select an ingredient to delete.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Ingredient");
        confirm.setHeaderText("Delete ingredient '" + selectedIngredient + "'?");
        confirm.setContentText("This will permanently remove this ingredient from the database. This action cannot be undone.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String sql = "DELETE FROM ingredients WHERE name = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, selectedIngredient);
                int rowsAffected = ps.executeUpdate();
                
                if (rowsAffected > 0) {
                    showAlert("Ingredient '" + selectedIngredient + "' has been deleted successfully.");
                    // Remove from feed mix table if present
                    feedData.removeIf(entry -> selectedIngredient.equals(entry.getIngredient()));
                    // Refresh the dropdown and clear the form
                    loadAllIngredients();
                    refreshTableComboBoxes();
                    clearIngredientForm();
                } else {
                    showAlert("Failed to delete ingredient. It may not exist in the database.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error deleting ingredient: " + e.getMessage());
            }
        }
    }

    @FXML
    private void clearIngredientForm() {
        clearIngredientFormFieldsOnly();
        
        // Safely reset selection to blank without triggering listener
        updatingComboBox = true;
        try {
            ingredientModeCombo.setValue("");
        } catch (Exception e) {
            // Ignore any errors during combobox update
        }
        updatingComboBox = false;
    }
    
    private void clearIngredientFormFieldsOnly() {
        ingredientField.clear();
        crudeProteinField.clear();
        crudeFatField.clear();
        crudeFiberField.clear();
        lysineField.clear();
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
        updatingComboBox = true;
        try {
            // Clear both the main combo and cache
            ingredientModeCombo.getItems().clear();
            cachedIngredients.clear();
            staticCachedIngredients.clear();
            
            ingredientModeCombo.getItems().add(""); // Blank option at top to clear back to Add New
            cachedIngredients.add(""); // Keep cache in sync
            staticCachedIngredients.add(""); // Keep static cache in sync
            
            String sql = "SELECT name FROM ingredients ORDER BY name";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    String ingredientName = rs.getString("name");
                    ingredientModeCombo.getItems().add(ingredientName);
                    cachedIngredients.add(ingredientName);
                    staticCachedIngredients.add(ingredientName);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            // Set default selection to blank (which will show as "Add New Ingredient" mode)
            ingredientModeCombo.setValue("");
        } finally {
            updatingComboBox = false;
        }
    }

    // Load nutrition data for an existing ingredient into the form for editing
    private void loadIngredientDataToForm(String name) {
        if (name == null || name.trim().isEmpty()) {
            // Blank selection - clear form for new ingredient mode
            clearIngredientFormFieldsOnly(); // Don't reset the dropdown, just clear fields
            return;
        }
        
        String sql = "SELECT crudeProtein, crudeFat, crudeFiber, lysine FROM ingredients WHERE name = ?";
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
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Load nutrition data for a table entry
    private void loadIngredientDataForEntry(FeedMixEntry entry, String name) {
        String sql = "SELECT crudeProtein, crudeFat, crudeFiber, lysine FROM ingredients WHERE name = ?";
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
    /**
     * Recalculates mix percentages and updates nutrition/cost totals
     * Called whenever feed mix entries are modified
     */
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
            avgPricePerLbLabel.setText("$0.00/lb");
            totalCostLabel.setText("Total: $0.00");
            return;
        }
        
        // Calculate weighted averages for nutrition
        double totalProtein = 0, totalFat = 0, totalFiber = 0, totalLysine = 0;
        for (FeedMixEntry e : validEntries) {
            double proportion = e.getWeight() / totalWeight;
            
            totalProtein += proportion * e.getProtein();
            totalFat += proportion * e.getFat();
            totalFiber += proportion * e.getFiber();
            totalLysine += proportion * e.getLysine();
        }
        
        // Calculate cost information
        double totalCost = 0.0;
        double totalWeightWithPrices = 0.0;
        
        for (FeedMixEntry entry : validEntries) {
            double pricePerLb = getIngredientPricePerLb(entry.getIngredient());
            if (pricePerLb > 0) {
                double entryCost = entry.getWeight() * pricePerLb;
                totalCost += entryCost;
                totalWeightWithPrices += entry.getWeight();
            }
        }
        
        double avgPricePerLb = totalWeightWithPrices > 0 ? totalCost / totalWeightWithPrices : 0.0;
        
        // Update UI labels
        totalWeightLabel.setText(String.format("%.1f lbs", totalWeight));
        totalProteinLabel.setText(String.format("%.1f%%", totalProtein));
        totalFatLabel.setText(String.format("%.1f%%", totalFat));
        totalFiberLabel.setText(String.format("%.1f%%", totalFiber));
        totalLysineLabel.setText(String.format("%.2f%%", totalLysine));
        avgPricePerLbLabel.setText(String.format("$%.2f/lb", avgPricePerLb));
        totalCostLabel.setText(String.format("Total: $%.2f", totalCost));
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
        
        // Number of pigs input
        Label pigsLabel = new Label("Number of pigs:");
        TextField numberOfPigsField = new TextField(loadNumberOfPigs());
        numberOfPigsField.setPrefWidth(60);
        numberOfPigsField.setMaxWidth(60);
        numberOfPigsField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(pigsLabel, 0, 0);
        grid.add(numberOfPigsField, 1, 0);
        
        // Pig stage dropdown
        Label stageLabel = new Label("Recommended for:");
        ComboBox<String> pigStageCombo = new ComboBox<>();
        pigStageCombo.getItems().addAll("Weaner", "Grower", "Finisher");
        pigStageCombo.setValue("Grower"); // Default to Grower
        pigStageCombo.setPrefWidth(120);
        pigStageCombo.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(stageLabel, 0, 1);
        grid.add(pigStageCombo, 1, 1);
        
        // Instructions (will be updated based on number of pigs and stage)
        Label instructions = new Label("Recommended: 3-5 lbs per pig per day (1 pig = 6.0 lbs total)");
        instructions.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(instructions, 0, 2, 2, 1);
        
        // Adjustment caption
        Label adjustmentCaption = new Label("(You may adjust these amounts to your pigs' individual needs)");
        adjustmentCaption.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888; -fx-font-style: italic;");
        grid.add(adjustmentCaption, 0, 2, 2, 1);
        adjustmentCaption.setTranslateY(18); // Move it below the instructions
        
        // Nutrition header (will be updated based on pig stage)
        Label nutritionHeader = new Label("Recommended Nutrition for Grower Pigs:");
        nutritionHeader.setStyle("-fx-font-weight: bold;");
        grid.add(nutritionHeader, 0, 3, 2, 1);
        
        // Target weight
        Label weightLabel = new Label("Total Feed Weight (lbs):");
        TextField weightField = new TextField("6.0");
        weightField.setPrefWidth(60);
        weightField.setMaxWidth(60);
        weightField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(weightLabel, 0, 4);
        grid.add(weightField, 1, 4);
        
        // Nutritional targets (editable) - will be updated based on pig stage
        Label proteinLabel = new Label("Crude Protein (%):");
        TextField proteinField = new TextField("16.0"); // Default for Grower
        proteinField.setPrefWidth(60);
        proteinField.setMaxWidth(60);
        proteinField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(proteinLabel, 0, 5);
        grid.add(proteinField, 1, 5);
        
        Label fatLabel = new Label("Fat (%):");
        TextField fatField = new TextField("4.0"); // Default for Grower
        fatField.setPrefWidth(60);
        fatField.setMaxWidth(60);
        fatField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(fatLabel, 0, 6);
        grid.add(fatField, 1, 6);
        
        Label fiberLabel = new Label("Fiber (%):");
        TextField fiberField = new TextField("5.0"); // Default for Grower
        fiberField.setPrefWidth(60);
        fiberField.setMaxWidth(60);
        fiberField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(fiberLabel, 0, 7);
        grid.add(fiberField, 1, 7);
        
        Label lysineLabel = new Label("Lysine (%):");
        TextField lysineField = new TextField("1.05"); // Default for Grower
        lysineField.setPrefWidth(60);
        lysineField.setMaxWidth(60);
        lysineField.setStyle("-fx-background-color: lightyellow; -fx-border-color: gray; -fx-border-width: 1px;");
        grid.add(lysineLabel, 0, 8);
        grid.add(lysineField, 1, 8);
        
        // Helper method to get feed amount per pig based on stage
        java.util.function.Function<String, Double> getFeedAmountPerPig = (stage) -> {
            switch (stage) {
                case "Weaner": return 3.0; // 1-3 lbs per day, default to 3 lbs
                case "Finisher": return 5.5; // 5-6 lbs per day, using middle value
                default: return 6.0; // Grower: 3-5 lbs per day, default to 6 lbs
            }
        };
        
        // Helper method to update instructions and weight
        Runnable updateInstructionsAndWeight = () -> {
            try {
                int pigs = Integer.parseInt(numberOfPigsField.getText().trim());
                String stage = pigStageCombo.getValue();
                if (pigs > 0 && stage != null) {
                    double feedPerPig = getFeedAmountPerPig.apply(stage);
                    double totalWeight = feedPerPig * pigs;
                    
                    String rangeText;
                    switch (stage) {
                        case "Weaner": rangeText = "1-3 lbs"; break;
                        case "Finisher": rangeText = "5-6 lbs"; break;
                        default: rangeText = "4-6 lbs"; // Grower
                    }
                    
                    instructions.setText(String.format("Recommended: %s per pig per day (%d pigs = %.1f lbs total)", 
                                                     rangeText, pigs, totalWeight));
                    weightField.setText(String.format("%.1f", totalWeight));
                }
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        };
        
        // Update instructions when number of pigs changes
        numberOfPigsField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateInstructionsAndWeight.run();
        });
        
        // Update nutrition values and feed amounts when pig stage changes
        pigStageCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                nutritionHeader.setText("Recommended Nutrition for " + newValue + " Pigs:");
                
                // Update nutrition field defaults based on stage
                switch (newValue) {
                    case "Weaner":
                        proteinField.setText("20.0"); fatField.setText("4.5"); 
                        fiberField.setText("4.0"); lysineField.setText("1.25");
                        break;
                    case "Finisher":
                        proteinField.setText("15.0"); fatField.setText("4.0"); 
                        fiberField.setText("6.0"); lysineField.setText("0.85");
                        break;
                    default: // Grower
                        proteinField.setText("16.0"); fatField.setText("4.0"); 
                        fiberField.setText("5.0"); lysineField.setText("1.05");
                }
                
                // Update feed amount and total weight
                updateInstructionsAndWeight.run();
            }
        });
        
        // Trigger initial weight calculation after setting up all listeners
        updateInstructionsAndWeight.run();
        
        // Ranges info
        Label rangesInfo = new Label(
            "Recommended ranges by stage:\n" +
            "• Weaner (11-55 lbs): Protein 18-22%, Fat 3-6%, Fiber 3-5%, Lysine 1.15-1.35%\n" +
            "• Grower (55-130 lbs): Protein 16-18%, Fat 3-5%, Fiber 4-6%, Lysine 0.95-1.15%\n" +
            "• Finisher (130-240 lbs): Protein 14-16%, Fat 3-5%, Fiber 5-7%, Lysine 0.75-0.95%"
        );
        rangesInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        grid.add(rangesInfo, 0, 9, 2, 1);
        
        // Price optimization checkbox
        CheckBox priceOptimizationCheckBox = new CheckBox("💰 Price Optimization");
        priceOptimizationCheckBox.setSelected(true); // Default to enabled
        priceOptimizationCheckBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #0066CC; -fx-font-weight: bold;");
        grid.add(priceOptimizationCheckBox, 0, 10, 2, 1);
        
        // Price optimization info
        Label priceInfo = new Label(
            "Uses recent purchase prices from cost tracker to optimize cost effectiveness.\n" +
            "Note: Prices must be input into the cost tracker for price optimization to work."
        );
        priceInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666; -fx-padding: 5 0 0 20;");
        grid.add(priceInfo, 0, 10, 2, 1);
        priceInfo.setTranslateY(20); // Move it below the checkbox
        
        // Trace minerals warning
        Label mineralWarning = new Label(
            "Many commercial feeds don't include adequate trace minerals -\n" +
            "you may want to add supplements like kelp."
        );
        mineralWarning.setStyle("-fx-font-size: 12px; -fx-text-fill: #cc0000;");
        grid.add(mineralWarning, 0, 12, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    // Save the number of pigs preference
                    saveNumberOfPigs(numberOfPigsField.getText().trim());
                    
                    // Validate number of pigs
                    int numberOfPigs = Integer.parseInt(numberOfPigsField.getText().trim());
                    if (numberOfPigs <= 0) {
                        showAlert("Number of pigs must be a positive number.");
                        return;
                    }
                    
                    double targetWeight = Double.parseDouble(weightField.getText());
                    double targetProtein = Double.parseDouble(proteinField.getText());
                    double targetFat = Double.parseDouble(fatField.getText());
                    double targetFiber = Double.parseDouble(fiberField.getText());
                    double targetLysine = Double.parseDouble(lysineField.getText());
                    
                    boolean usePriceOptimization = priceOptimizationCheckBox.isSelected();
                    optimizeFeedMix(targetWeight, targetProtein, targetFat, targetFiber, targetLysine, usePriceOptimization);
                } catch (NumberFormatException e) {
                    showAlert("Please enter valid numbers for all fields.");
                }
            }
        });
    }
    
    private void optimizeFeedMix(double targetWeight, double targetProtein, double targetFat, 
                               double targetFiber, double targetLysine, boolean usePriceOptimization) {
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
            optimizeMultiIngredient(validIngredients, targetWeight, targetProtein, targetFat, targetFiber, targetLysine, usePriceOptimization);
        }
        
        recalcPercentages();
        feedTable.refresh();
        
        showAlert("Feed mix optimized for target nutrition values!");
    }
    
    /**
     * Optimizes feed mix using multiple ingredients to meet target nutrition values
     * Uses a proportion-based approach that gives consistent results regardless of total weight
     * 
     * @param ingredients List of available feed ingredients with nutrition data
     * @param targetWeight Total weight of feed mix in pounds
     * @param targetProtein Target crude protein percentage
     * @param targetFat Target fat percentage  
     * @param targetFiber Target fiber percentage
     * @param targetLysine Target lysine percentage
     * @param usePriceOptimization Whether to consider cost in optimization (currently informational only)
     */
    private void optimizeMultiIngredient(java.util.List<FeedMixEntry> ingredients, double targetWeight,
                                       double targetProtein, double targetFat, double targetFiber, double targetLysine, 
                                       boolean usePriceOptimization) {
        
        // Weight-independent approach: Work with proportions (0-1) first, then scale to target weight
        // This ensures consistent nutrition percentages regardless of whether optimizing for 6 lbs or 600 lbs
        int numIngredients = ingredients.size();
        if (numIngredients == 0) return;
        
        // Work with proportions (0-1) instead of absolute weights
        double[] proportions = new double[numIngredients];
        
        
        // Use typical pig feed proportions as starting point, modified by price optimization if enabled
        double[] pricesPerLb = new double[numIngredients];
        double maxPrice = 0;
        double minPrice = Double.MAX_VALUE;
        
        // Get prices for all ingredients if price optimization is enabled
        if (usePriceOptimization) {
            for (int i = 0; i < numIngredients; i++) {
                pricesPerLb[i] = getIngredientPricePerLb(ingredients.get(i).getIngredient());
                if (pricesPerLb[i] > 0) {
                    maxPrice = Math.max(maxPrice, pricesPerLb[i]);
                    minPrice = Math.min(minPrice, pricesPerLb[i]);
                }
            }
        }
        
        for (int i = 0; i < numIngredients; i++) {
            FeedMixEntry entry = ingredients.get(i);
            String name = entry.getIngredient().toLowerCase();
            
            // Assign starting proportions based on ingredient's nutritional profile
            // Be very conservative with high-lysine ingredients
            double protein = entry.getProtein();
            double lysine = entry.getLysine();
            double fat = entry.getFat();
            
            double baseProportion;
            if (lysine > 10) {
                // Very high lysine ingredients (eggs, fish meal) - extremely limited
                baseProportion = 0.01 + (Math.random() * 0.02); // 1-3%
            } else if (protein > 25) {
                // High protein ingredients (soybean meal, etc.) - use sparingly
                baseProportion = 0.05 + (Math.random() * 0.05); // 5-10%
            } else if (protein > 15) {
                // Medium protein ingredients (alfalfa, etc.) - moderate amount
                baseProportion = 0.15 + (Math.random() * 0.10); // 15-25%
            } else if (protein < 10 && fat < 5) {
                // Energy grains (corn, milo, etc.) - can be major component
                baseProportion = 0.30 + (Math.random() * 0.20); // 30-50%
            } else {
                // Other ingredients - moderate amount
                baseProportion = 0.08 + (Math.random() * 0.07); // 8-15%
            }
            
            // Apply price optimization: favor cheaper ingredients
            if (usePriceOptimization && pricesPerLb[i] > 0 && maxPrice > minPrice) {
                // Create price factor: expensive ingredients get reduced proportion, cheap ones get increased
                double priceRange = maxPrice - minPrice;
                double priceNormalized = (pricesPerLb[i] - minPrice) / priceRange; // 0 = cheapest, 1 = most expensive
                double priceFactor = 1.5 - priceNormalized; // 1.5 for cheapest, 0.5 for most expensive
                
                // Don't let price completely override nutrition needs, but strongly influence proportions
                proportions[i] = baseProportion * priceFactor;
                
                // Extra penalty for very expensive ingredients like eggs
                if (pricesPerLb[i] > 5.0) { // $5+ per lb is very expensive
                    proportions[i] *= 0.1; // Reduce by 90% (should give ~0.1-0.3 lbs for eggs)
                } else if (pricesPerLb[i] > 3.0) { // $3+ per lb is expensive
                    proportions[i] *= 0.4; // Reduce by 60%
                }
            } else {
                proportions[i] = baseProportion;
            }
        }
        
        // Normalize proportions to sum to 1.0
        double currentTotal = java.util.Arrays.stream(proportions).sum();
        if (currentTotal > 0) {
            for (int i = 0; i < numIngredients; i++) {
                proportions[i] = proportions[i] / currentTotal;
            }
        }
        
        // Optimize proportions using multi-nutrient balancing with more iterations for precision
        for (int iteration = 0; iteration < 100; iteration++) {
            // Calculate current nutrition using proportions
            double currentProtein = 0, currentFat = 0, currentFiber = 0, currentLysine = 0;
            for (int i = 0; i < numIngredients; i++) {
                currentProtein += proportions[i] * ingredients.get(i).getProtein();
                currentFat += proportions[i] * ingredients.get(i).getFat();
                currentFiber += proportions[i] * ingredients.get(i).getFiber();
                currentLysine += proportions[i] * ingredients.get(i).getLysine();
            }
            
            // Calculate errors for all nutrients with very tight tolerances for protein and lysine
            double proteinError = Math.abs(currentProtein - targetProtein);
            double fatError = Math.abs(currentFat - targetFat);
            double fiberError = Math.abs(currentFiber - targetFiber);
            double lysineError = Math.abs(currentLysine - targetLysine);
            
            // Stop if protein and lysine are very close to targets (tight tolerances)
            if (proteinError < 0.1 && lysineError < 0.05) {
                break;
            }
            
            // Prioritize protein and lysine adjustments
            double adjustment = 0.002; // Even smaller 0.2% adjustment per iteration for higher precision
            
            if (proteinError > 0.1) {
                // Protein is top priority
                adjustProportionsForNutrient(proportions, ingredients, currentProtein, targetProtein, 
                                           (entry) -> entry.getProtein(), adjustment);
            } else if (lysineError > 0.05) {
                // Lysine is second priority (very small adjustments)
                adjustProportionsForNutrient(proportions, ingredients, currentLysine, targetLysine, 
                                           (entry) -> entry.getLysine(), adjustment * 0.1);
            } else if (fatError > 0.5) {
                // Fat is third priority
                adjustProportionsForNutrient(proportions, ingredients, currentFat, targetFat, 
                                           (entry) -> entry.getFat(), adjustment);
            } else if (fiberError > 0.5) {
                // Fiber is lowest priority
                adjustProportionsForNutrient(proportions, ingredients, currentFiber, targetFiber, 
                                           (entry) -> entry.getFiber(), adjustment);
            }
            
            // Ensure no negative or excessive proportions with ingredient-specific limits
            for (int i = 0; i < numIngredients; i++) {
                FeedMixEntry ingredient = ingredients.get(i);
                
                if (proportions[i] < 0.01) {
                    proportions[i] = 0.01; // Minimum 1%
                } else {
                    // Apply ingredient-specific maximum limits
                    double maxProportion = 0.65; // Default maximum
                    
                    if (ingredient.getLysine() > 10) {
                        maxProportion = 0.05; // Max 5% for very high lysine ingredients (eggs, fish meal)
                    } else if (ingredient.getProtein() > 25) {
                        maxProportion = 0.25; // Max 25% for high protein ingredients
                    } else if (ingredient.getFiber() > 25) {
                        maxProportion = 0.30; // Max 30% for high fiber ingredients
                    }
                    
                    if (proportions[i] > maxProportion) {
                        proportions[i] = maxProportion;
                    }
                }
            }
            
            // Re-normalize after constraints
            double total = java.util.Arrays.stream(proportions).sum();
            for (int i = 0; i < numIngredients; i++) {
                proportions[i] = proportions[i] / total;
            }
        }
        
        // Apply the calculated proportions scaled to target weight
        for (int i = 0; i < numIngredients; i++) {
            double finalWeight = proportions[i] * targetWeight;
            ingredients.get(i).setWeight(Math.round(finalWeight * 100.0) / 100.0);
        }
    }
    
    // Helper method to adjust proportions for a specific nutrient
    private void adjustProportionsForNutrient(double[] proportions, java.util.List<FeedMixEntry> ingredients, 
                                            double currentValue, double targetValue, 
                                            java.util.function.Function<FeedMixEntry, Double> getNutrient,
                                            double adjustment) {
        int numIngredients = ingredients.size();
        
        // Find ingredient with highest and lowest value for this nutrient
        int highIdx = 0, lowIdx = 0;
        double highest = getNutrient.apply(ingredients.get(0));
        double lowest = getNutrient.apply(ingredients.get(0));
        
        for (int i = 1; i < numIngredients; i++) {
            double value = getNutrient.apply(ingredients.get(i));
            if (value > highest) {
                highest = value;
                highIdx = i;
            }
            if (value < lowest) {
                lowest = value;
                lowIdx = i;
            }
        }
        
        // Adjust proportions based on whether we need more or less of this nutrient
        if (currentValue < targetValue) {
            // Need more: increase high-nutrient ingredient, decrease low-nutrient ingredient
            proportions[highIdx] += adjustment;
            proportions[lowIdx] -= adjustment;
        } else {
            // Need less: decrease high-nutrient ingredient, increase low-nutrient ingredient
            proportions[highIdx] -= adjustment;
            proportions[lowIdx] += adjustment;
        }
    }
    
    /**
     * Gets the most recent price per pound for a feed ingredient from the cost tracker
     * This is used in the feed mix calculator to show cost information
     * 
     * @param ingredientName The name of the feed ingredient (e.g., "Corn", "Soybean Meal")
     * @return Price per pound in dollars, or 0.0 if no recent price data found
     */
    private double getIngredientPricePerLb(String ingredientName) {
        // Query the most recent cost entry for this ingredient
        String sql = """
            SELECT cost, quantity, date FROM cost_entries 
            WHERE ingredient = ? AND category = 'Feed' AND cost > 0 AND quantity > 0
            ORDER BY date DESC LIMIT 1
            """;
            
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, ingredientName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double cost = rs.getDouble("cost");
                    double quantity = rs.getDouble("quantity"); // This is already in total pounds from cost tracker
                    
                    if (quantity > 0) {
                        double pricePerPound = cost / quantity;
                        
                        // Sanity check - reject unrealistic prices for animal feed
                        if (pricePerPound > 10.0) { // More than $10/lb seems wrong for feed
                            return 0.0;
                        }
                        
                        return pricePerPound;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting price for " + ingredientName + ": " + e.getMessage());
        }
        
        return 0.0; // No price data found
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
                staticFeedData.clear(); // Also clear static cache
                ensureEmptyRowExists();
            }
        }
    }
    
    @FXML
    private void zeroWeights() {
        for (FeedMixEntry entry : feedData) {
            if (!"Select ingredient...".equals(entry.getIngredient())) {
                entry.setWeight(0.0);
            }
        }
        recalcPercentages();
        feedTable.refresh();
    }

    @FXML
    private void backToWelcome() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/org/pigfeed/pigfeedapp/welcome-view.fxml")
            );
            javafx.scene.Parent welcomeRoot = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) feedTable.getScene().getWindow();
            javafx.scene.Scene welcomeScene = new javafx.scene.Scene(welcomeRoot, 600, 400);
            stage.setScene(welcomeScene);
            stage.setTitle("Pig Feed App");
            stage.centerOnScreen();
            
        } catch (java.io.IOException e) {
            showAlert("Could not return to welcome screen: " + e.getMessage());
        }
    }
    
    @FXML
    private void goToCostTracker() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/org/pigfeed/pigfeedapp/cost-tracker-view.fxml")
            );
            javafx.scene.Parent costTrackerRoot = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) feedTable.getScene().getWindow();
            javafx.scene.Scene costTrackerScene = new javafx.scene.Scene(costTrackerRoot, 800, 600);
            stage.setScene(costTrackerScene);
            stage.setTitle("Pig Cost Tracker");
            setApplicationIcon(stage);
            stage.centerOnScreen();
            
        } catch (java.io.IOException e) {
            showAlert("Could not open cost tracker: " + e.getMessage());
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


    private void refreshTableComboBoxes() {
        // Force refresh of the table to update combo box items
        feedTable.refresh();
    }
    
    private void refreshIngredientDataInTable(String ingredientName) {
        // Update any entries in the table that use this ingredient
        for (FeedMixEntry entry : feedData) {
            if (ingredientName.equals(entry.getIngredient())) {
                loadIngredientDataForEntry(entry, ingredientName);
            }
        }
        recalcPercentages();
        feedTable.refresh();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.showAndWait();
    }
    
    /**
     * Sets the application icon for any stage/window
     */
    private void setApplicationIcon(javafx.stage.Stage stage) {
        try {
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/pigLogo.png")));
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }
    }
    
    private String loadNumberOfPigs() {
        String sql = "SELECT value FROM user_preferences WHERE key = 'numberOfPigs'";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load number of pigs preference: " + e.getMessage());
        }
        return "1"; // Default value
    }
    
    private void saveNumberOfPigs(String numberOfPigs) {
        String sql = "INSERT OR REPLACE INTO user_preferences (key, value) VALUES ('numberOfPigs', ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, numberOfPigs);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Could not save number of pigs preference: " + e.getMessage());
        }
    }
    
    // Utility method to scale the entire mix to a target weight while maintaining proportions
    private void scaleMixToTargetWeight(double targetWeight) {
        var validEntries = feedData.stream()
            .filter(entry -> !"Select ingredient...".equals(entry.getIngredient()) && 
                           entry.getWeight() > 0)
            .collect(java.util.stream.Collectors.toList());
        
        if (validEntries.isEmpty()) return;
        
        double currentTotalWeight = validEntries.stream().mapToDouble(FeedMixEntry::getWeight).sum();
        if (currentTotalWeight == 0) return;
        
        double scaleFactor = targetWeight / currentTotalWeight;
        
        for (FeedMixEntry entry : validEntries) {
            double newWeight = entry.getWeight() * scaleFactor;
            entry.setWeight(Math.round(newWeight * 100.0) / 100.0);
        }
        
        recalcPercentages();
        feedTable.refresh();
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
        staticFeedData.clear(); // Also clear static cache
        
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
                
                // Also add to static cache
                FeedMixEntry staticEntry = new FeedMixEntry();
                staticEntry.setIngredient(entry.getIngredient());
                staticEntry.setWeight(entry.getWeight());
                staticEntry.setProtein(entry.getProtein());
                staticEntry.setFat(entry.getFat());
                staticEntry.setFiber(entry.getFiber());
                staticEntry.setLysine(entry.getLysine());
                staticFeedData.add(staticEntry);
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
