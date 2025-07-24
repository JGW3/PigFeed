package org.pigfeed.pigfeedapp.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.pigfeed.pigfeedapp.model.CostEntry;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class CostTrackerController {

    private static final String DB_URL = "jdbc:sqlite:pigfeed.db";

    // Entry form controls
    @FXML private TitledPane entryPane;
    @FXML private DatePicker datePicker;
    @FXML private TextField descriptionField;
    @FXML private TextField categoryField;
    @FXML private TextField costField;
    @FXML private TextField quantityField;

    // Table and columns
    @FXML private TableView<CostEntry> costTable;
    @FXML private TableColumn<CostEntry, LocalDate> dateCol;
    @FXML private TableColumn<CostEntry, String> descriptionCol;
    @FXML private TableColumn<CostEntry, String> categoryCol;
    @FXML private TableColumn<CostEntry, Number> costCol;
    @FXML private TableColumn<CostEntry, Number> quantityCol;
    @FXML private TableColumn<CostEntry, Number> totalCol;

    // Summary labels
    @FXML private Label totalCostLabel;
    @FXML private Label entryCountLabel;

    private final ObservableList<CostEntry> costData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize date picker to today
        datePicker.setValue(LocalDate.now());

        // Set up table columns
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        costCol.setCellValueFactory(new PropertyValueFactory<>("cost"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        // Format currency columns
        costCol.setCellFactory(column -> new TableCell<CostEntry, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(String.format("$%.2f", item.doubleValue()));
                }
            }
        });

        totalCol.setCellFactory(column -> new TableCell<CostEntry, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(String.format("$%.2f", item.doubleValue()));
                }
            }
        });

        // Bind table to data
        costTable.setItems(costData);

        // Initialize database table
        initializeCostTable();

        // Load existing data
        loadCostEntries();

        // Listen for changes to update summary
        costData.addListener((javafx.collections.ListChangeListener<CostEntry>) change -> updateSummary());
        updateSummary();
    }

    private void initializeCostTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS cost_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    cost REAL NOT NULL,
                    quantity REAL NOT NULL,
                    total REAL NOT NULL
                )
                """;
            stmt.execute(createTableSQL);
            
        } catch (SQLException e) {
            showError("Database Error", "Could not initialize cost tracking table: " + e.getMessage());
        }
    }

    @FXML
    private void addCostEntry() {
        try {
            // Validate input
            LocalDate date = datePicker.getValue();
            String description = descriptionField.getText().trim();
            String category = categoryField.getText().trim();
            String costText = costField.getText().trim();
            String quantityText = quantityField.getText().trim();

            if (date == null || description.isEmpty() || category.isEmpty() || 
                costText.isEmpty() || quantityText.isEmpty()) {
                showError("Input Error", "Please fill in all fields.");
                return;
            }

            double cost = Double.parseDouble(costText);
            double quantity = Double.parseDouble(quantityText);

            if (cost < 0 || quantity < 0) {
                showError("Input Error", "Cost and quantity must be positive numbers.");
                return;
            }

            // Create new entry
            CostEntry entry = new CostEntry(date, description, category, cost, quantity);
            
            // Save to database
            saveCostEntry(entry);
            
            // Add to table
            costData.add(entry);
            
            // Clear form
            clearForm();
            
        } catch (NumberFormatException e) {
            showError("Input Error", "Cost and quantity must be valid numbers.");
        }
    }

    private void saveCostEntry(CostEntry entry) {
        String sql = "INSERT INTO cost_entries (date, description, category, cost, quantity, total) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entry.getDate().toString());
            pstmt.setString(2, entry.getDescription());
            pstmt.setString(3, entry.getCategory());
            pstmt.setDouble(4, entry.getCost());
            pstmt.setDouble(5, entry.getQuantity());
            pstmt.setDouble(6, entry.getTotal());
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            showError("Database Error", "Could not save cost entry: " + e.getMessage());
        }
    }

    private void loadCostEntries() {
        costData.clear();
        String sql = "SELECT * FROM cost_entries ORDER BY date DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                LocalDate date = LocalDate.parse(rs.getString("date"));
                String description = rs.getString("description");
                String category = rs.getString("category");
                double cost = rs.getDouble("cost");
                double quantity = rs.getDouble("quantity");
                
                CostEntry entry = new CostEntry(date, description, category, cost, quantity);
                costData.add(entry);
            }
            
        } catch (SQLException e) {
            showError("Database Error", "Could not load cost entries: " + e.getMessage());
        }
    }

    @FXML
    private void clearAllEntries() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear All Entries");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("This will permanently delete all cost entries. This action cannot be undone.");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {
                
                stmt.execute("DELETE FROM cost_entries");
                costData.clear();
                
            } catch (SQLException e) {
                showError("Database Error", "Could not clear entries: " + e.getMessage());
            }
        }
    }

    @FXML
    private void backToWelcome() {
        try {
            Parent welcomeRoot = FXMLLoader.load(
                getClass().getResource("/org/pigfeed/pigfeedapp/welcome-view.fxml")
            );
            
            Stage stage = (Stage) costTable.getScene().getWindow();
            Scene welcomeScene = new Scene(welcomeRoot);
            stage.setScene(welcomeScene);
            stage.setTitle("Pig Feed App");
            stage.sizeToScene();
            
        } catch (IOException e) {
            showError("Navigation Error", "Could not return to welcome screen: " + e.getMessage());
        }
    }

    private void clearForm() {
        datePicker.setValue(LocalDate.now());
        descriptionField.clear();
        categoryField.clear();
        costField.clear();
        quantityField.clear();
    }

    private void updateSummary() {
        double totalCost = costData.stream()
            .mapToDouble(CostEntry::getTotal)
            .sum();
        
        totalCostLabel.setText(String.format("Total Cost: $%.2f", totalCost));
        entryCountLabel.setText("Entries: " + costData.size());
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}