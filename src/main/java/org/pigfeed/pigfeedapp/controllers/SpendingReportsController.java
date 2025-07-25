package org.pigfeed.pigfeedapp.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.pigfeed.pigfeedapp.model.CostEntry;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class SpendingReportsController {

    private static final String DB_URL = "jdbc:sqlite:pigfeed.db";

    // UI Controls
    @FXML private Button weeklyButton;
    @FXML private Button monthlyButton;
    @FXML private Button ytdButton;
    
    @FXML private Label totalSpendingLabel;
    @FXML private Label feedCostsLabel;
    @FXML private Label otherCostsLabel;
    
    @FXML private LineChart<String, Number> spendingChart;
    @FXML private CategoryAxis timeAxis;
    @FXML private NumberAxis amountAxis;
    @FXML private javafx.scene.layout.VBox legendBox;
    
    @FXML private TableView<CostEntry> spendingTable;
    @FXML private TableColumn<CostEntry, LocalDate> dateColumn;
    @FXML private TableColumn<CostEntry, String> categoryColumn;
    @FXML private TableColumn<CostEntry, String> descriptionColumn;
    @FXML private TableColumn<CostEntry, Number> amountColumn;
    
    private final ObservableList<CostEntry> spendingData = FXCollections.observableArrayList();
    
    // Helper class to store period information for chart clicks
    private static class PeriodInfo {
        final String periodLabel;
        final String periodType;
        
        PeriodInfo(String periodLabel, String periodType) {
            this.periodLabel = periodLabel;
            this.periodType = periodType;
        }
    }
    
    // Consistent colors for categories
    private final Map<String, String> CATEGORY_COLORS = Map.of(
        "Feed", "#2E8B57",           // Sea Green
        "Veterinary", "#DC143C",     // Crimson  
        "Equipment", "#4169E1",      // Royal Blue
        "Maintenance", "#FF8C00",    // Dark Orange
        "Supplements", "#9932CC",    // Dark Orchid
        "Other", "#696969"           // Dim Gray
    );

    @FXML
    public void initialize() {
        // Setup button states
        setSelectedButton(weeklyButton);
        
        // Setup table columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        
        // Format amount column as currency
        amountColumn.setCellFactory(column -> new TableCell<CostEntry, Number>() {
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
        
        spendingTable.setItems(spendingData);
        
        // Setup chart
        spendingChart.setTitle("Spending Trends by Category");
        spendingChart.setLegendVisible(false); // We'll use our custom legend
        timeAxis.setLabel("Time Period");
        amountAxis.setLabel("Amount ($)");
        
        // Setup custom legend
        setupLegend();
        
        // Load initial data (weekly by default)
        loadSpendingData("weekly");
    }

    @FXML
    private void showWeeklyReports() {
        setSelectedButton(weeklyButton);
        loadSpendingData("weekly");
    }

    @FXML
    private void showMonthlyReports() {
        setSelectedButton(monthlyButton);
        loadSpendingData("monthly");
    }

    @FXML
    private void showYtdReports() {
        setSelectedButton(ytdButton);
        loadSpendingData("ytd");
    }

    private void setSelectedButton(Button selectedButton) {
        // Reset all button styles
        weeklyButton.setStyle("");
        monthlyButton.setStyle("");
        ytdButton.setStyle("");
        
        // Highlight the selected button
        selectedButton.setStyle("-fx-background-color: lightblue;");
    }

    private void loadSpendingData(String period) {
        try {
            spendingData.clear();
            
            // Get date range based on period
            LocalDate startDate = getStartDate(period);
            LocalDate endDate = LocalDate.now();
            
            // Load cost entries for the table
            String sql = "SELECT * FROM cost_entries WHERE date >= ? AND date <= ? ORDER BY date DESC";
            
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, startDate.toString());
                pstmt.setString(2, endDate.toString());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    double totalSpending = 0.0;
                    double feedCosts = 0.0;
                    
                    // For weekly: store individual entries, for others: aggregate by time period
                    List<CostEntry> entriesForChart = new ArrayList<>();
                    Map<String, Map<String, Double>> timeSeriesData = new LinkedHashMap<>();
                    
                    while (rs.next()) {
                        LocalDate date = LocalDate.parse(rs.getString("date"));
                        String description = rs.getString("description");
                        String category = rs.getString("category");
                        String ingredient = rs.getString("ingredient");
                        double cost = rs.getDouble("cost");
                        double quantity = rs.getDouble("quantity");
                        
                        CostEntry entry = new CostEntry(date, description, category, ingredient, cost, quantity);
                        spendingData.add(entry);
                        
                        totalSpending += entry.getTotal();
                        if ("Feed".equals(category)) {
                            feedCosts += entry.getTotal();
                        }
                        
                        if ("weekly".equals(period)) {
                            // For weekly, store individual entries
                            entriesForChart.add(entry);
                        } else {
                            // For monthly/ytd, aggregate by time period
                            String timePeriod = getTimePeriodLabel(date, period);
                            timeSeriesData.computeIfAbsent(timePeriod, k -> new HashMap<>())
                                         .merge(category, entry.getTotal(), Double::sum);
                        }
                    }
                    
                    // Update summary labels
                    totalSpendingLabel.setText(String.format("Total: $%.2f", totalSpending));
                    feedCostsLabel.setText(String.format("Feed: $%.2f", feedCosts));
                    otherCostsLabel.setText(String.format("Other: $%.2f", totalSpending - feedCosts));
                    
                    // Update chart
                    if ("weekly".equals(period)) {
                        updateWeeklyChart(entriesForChart);
                    } else {
                        updateLineChart(timeSeriesData, period);
                    }
                }
            }
            
        } catch (SQLException e) {
            showError("Database Error", "Could not load spending data: " + e.getMessage());
        }
    }

    private LocalDate getStartDate(String period) {
        LocalDate now = LocalDate.now();
        switch (period) {
            case "weekly":
                return now.minus(4, ChronoUnit.WEEKS); // Last 4 weeks
            case "monthly":
                return now.minus(3, ChronoUnit.MONTHS); // Last 3 months
            case "ytd":
                return LocalDate.of(now.getYear(), 1, 1); // Start of year
            default:
                return now.minus(4, ChronoUnit.WEEKS);
        }
    }
    
    private String getTimePeriodLabel(LocalDate date, String period) {
        switch (period) {
            case "weekly":
                // Return individual date for each entry (e.g., "Dec 18")
                return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
            case "monthly":
                // Return month-year (e.g., "Dec 2024")
                return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
            case "ytd":
                // Return month (e.g., "December")
                return date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM"));
            default:
                return date.toString();
        }
    }
    
    private void updateWeeklyChart(List<CostEntry> entries) {
        spendingChart.getData().clear();
        
        // Group entries by category and sort by date (oldest first for chart display)
        Map<String, List<CostEntry>> categorizedEntries = entries.stream()
            .collect(java.util.stream.Collectors.groupingBy(CostEntry::getCategory));
        
        // Create a series for each category
        for (Map.Entry<String, List<CostEntry>> categoryGroup : categorizedEntries.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            String category = categoryGroup.getKey();
            series.setName(category);
            
            // Sort entries by date (oldest first for proper chart order)
            List<CostEntry> sortedEntries = categoryGroup.getValue().stream()
                .sorted((e1, e2) -> e1.getDate().compareTo(e2.getDate()))
                .collect(java.util.stream.Collectors.toList());
            
            // Add each individual entry as a point
            for (CostEntry entry : sortedEntries) {
                String dateLabel = entry.getDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
                XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(dateLabel, entry.getTotal());
                
                // Store reference to the entry for click handling
                dataPoint.setExtraValue(entry);
                series.getData().add(dataPoint);
            }
            
            spendingChart.getData().add(series);
        }
        
        // Apply consistent colors and click handlers after chart is rendered
        spendingChart.applyCss();
        spendingChart.layout();
        applyChartStyling();
    }
    
    private void updateLineChart(Map<String, Map<String, Double>> timeSeriesData, String period) {
        spendingChart.getData().clear();
        
        // Get all categories across all time periods
        Set<String> allCategories = new HashSet<>();
        timeSeriesData.values().forEach(periodData -> allCategories.addAll(periodData.keySet()));
        
        // Create a series for each category
        for (String category : allCategories) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(category);
            
            // Sort time periods chronologically and add data points
            List<String> sortedPeriods = new ArrayList<>(timeSeriesData.keySet());
            sortedPeriods.sort((p1, p2) -> comparePeriods(p1, p2, period));
            
            for (String timePeriod : sortedPeriods) {
                double amount = timeSeriesData.get(timePeriod).getOrDefault(category, 0.0);
                XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(timePeriod, amount);
                
                // Store period info for click handling
                dataPoint.setExtraValue(new PeriodInfo(timePeriod, period));
                series.getData().add(dataPoint);
            }
            
            spendingChart.getData().add(series);
        }
        
        // Apply consistent colors after chart is rendered
        spendingChart.applyCss();
        spendingChart.layout();
        applyChartStyling();
    }
    
    private int comparePeriods(String p1, String p2, String period) {
        try {
            switch (period) {
                case "monthly":
                    // Parse "Dec 2024" format and compare chronologically
                    java.time.format.DateTimeFormatter monthFormatter = 
                        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");
                    java.time.YearMonth ym1 = java.time.YearMonth.parse(p1, monthFormatter);
                    java.time.YearMonth ym2 = java.time.YearMonth.parse(p2, monthFormatter);
                    return ym1.compareTo(ym2);
                case "ytd":
                    // Parse month names and compare by month order
                    java.time.Month month1 = java.time.Month.valueOf(p1.toUpperCase());
                    java.time.Month month2 = java.time.Month.valueOf(p2.toUpperCase());
                    return month1.compareTo(month2);
                default:
                    return p1.compareTo(p2); // Default string comparison
            }
        } catch (Exception e) {
            return p1.compareTo(p2); // Fallback to string comparison
        }
    }
    
    private void applyChartStyling() {
        // Apply consistent colors to each series
        for (int i = 0; i < spendingChart.getData().size(); i++) {
            XYChart.Series<String, Number> series = spendingChart.getData().get(i);
            String category = series.getName();
            String color = CATEGORY_COLORS.getOrDefault(category, "#808080"); // Default gray
            
            // Apply color to the series line and symbols
            javafx.scene.Node seriesNode = series.getNode();
            if (seriesNode != null) {
                seriesNode.setStyle("-fx-stroke: " + color + "; -fx-background-color: " + color + ";");
            }
            
            // Apply color to individual data points and add click handlers for weekly view
            for (XYChart.Data<String, Number> data : series.getData()) {
                javafx.scene.Node symbol = data.getNode();
                if (symbol != null) {
                    symbol.setStyle("-fx-background-color: " + color + ";");
                    
                    // Add click handlers based on stored data type
                    if (data.getExtraValue() instanceof CostEntry) {
                        // Weekly view: scroll to specific entry
                        CostEntry entry = (CostEntry) data.getExtraValue();
                        symbol.setOnMouseClicked(event -> scrollToTableEntry(entry));
                        symbol.setStyle(symbol.getStyle() + " -fx-cursor: hand;");
                    } else if (data.getExtraValue() instanceof PeriodInfo) {
                        // Monthly/Yearly view: scroll to period start
                        PeriodInfo periodInfo = (PeriodInfo) data.getExtraValue();
                        symbol.setOnMouseClicked(event -> scrollToPeriodStart(periodInfo));
                        symbol.setStyle(symbol.getStyle() + " -fx-cursor: hand;");
                    }
                }
            }
        }
    }
    
    private void scrollToTableEntry(CostEntry targetEntry) {
        // Find the entry in the table and scroll to it
        for (int i = 0; i < spendingData.size(); i++) {
            CostEntry entry = spendingData.get(i);
            if (entry.getDate().equals(targetEntry.getDate()) && 
                entry.getDescription().equals(targetEntry.getDescription()) &&
                entry.getCategory().equals(targetEntry.getCategory()) &&
                Math.abs(entry.getTotal() - targetEntry.getTotal()) < 0.01) {
                
                // Select and scroll to the entry
                spendingTable.getSelectionModel().select(i);
                spendingTable.scrollTo(i);
                break;
            }
        }
    }
    
    private void scrollToPeriodStart(PeriodInfo periodInfo) {
        // Find the earliest entry in the specified period and scroll to it
        LocalDate targetDate = null;
        
        try {
            switch (periodInfo.periodType) {
                case "monthly":
                    // Parse "Dec 2024" format
                    java.time.format.DateTimeFormatter monthFormatter = 
                        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");
                    java.time.YearMonth yearMonth = java.time.YearMonth.parse(periodInfo.periodLabel, monthFormatter);
                    targetDate = yearMonth.atDay(1); // First day of the month
                    break;
                case "ytd":
                    // Parse "December" format 
                    java.time.format.DateTimeFormatter monthOnlyFormatter = 
                        java.time.format.DateTimeFormatter.ofPattern("MMMM");
                    java.time.Month month = java.time.Month.valueOf(
                        periodInfo.periodLabel.toUpperCase()
                    );
                    targetDate = LocalDate.of(LocalDate.now().getYear(), month, 1);
                    break;
            }
            
            if (targetDate != null) {
                scrollToClosestDate(targetDate);
            }
        } catch (Exception e) {
            System.err.println("Error parsing period: " + e.getMessage());
        }
    }
    
    private void scrollToClosestDate(LocalDate targetDate) {
        // Find the entry closest to the target date
        CostEntry closestEntry = null;
        long minDaysDiff = Long.MAX_VALUE;
        
        for (CostEntry entry : spendingData) {
            long daysDiff = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(targetDate, entry.getDate()));
            if (daysDiff < minDaysDiff) {
                minDaysDiff = daysDiff;
                closestEntry = entry;
            }
        }
        
        if (closestEntry != null) {
            // Find the index and scroll to it
            for (int i = 0; i < spendingData.size(); i++) {
                if (spendingData.get(i) == closestEntry) {
                    spendingTable.getSelectionModel().select(i);
                    spendingTable.scrollTo(i);
                    break;
                }
            }
        }
    }
    
    private void setupLegend() {
        legendBox.getChildren().clear();
        
        for (Map.Entry<String, String> entry : CATEGORY_COLORS.entrySet()) {
            javafx.scene.layout.HBox legendItem = new javafx.scene.layout.HBox(5);
            legendItem.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            // Color box
            javafx.scene.shape.Rectangle colorBox = new javafx.scene.shape.Rectangle(12, 12);
            colorBox.setFill(javafx.scene.paint.Color.web(entry.getValue()));
            
            // Category label
            Label categoryLabel = new Label(entry.getKey());
            categoryLabel.setStyle("-fx-font-size: 11px;");
            
            legendItem.getChildren().addAll(colorBox, categoryLabel);
            legendBox.getChildren().add(legendItem);
        }
    }


    @FXML
    private void exportReport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Report");
        alert.setHeaderText("Export Functionality");
        alert.setContentText("Report export feature will be implemented in a future version.");
        alert.showAndWait();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) totalSpendingLabel.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}