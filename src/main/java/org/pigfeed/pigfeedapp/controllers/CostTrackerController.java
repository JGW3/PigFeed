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
import javafx.scene.control.Alert;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import java.awt.Desktop;

public class CostTrackerController {

    private static final String DB_URL = "jdbc:sqlite:pigfeed.db";

    // Entry form controls
    @FXML private DatePicker datePicker;
    @FXML private TextField descriptionField;
    @FXML private ComboBox<String> expenseTypeCombo;
    @FXML private Label ingredientLabel;
    @FXML private javafx.scene.layout.HBox feedTypeBox;
    @FXML private ComboBox<String> ingredientCombo;
    @FXML private Button addFeedTypeButton;
    @FXML private Label priceUnitLabel;
    @FXML private ComboBox<String> priceUnitCombo;
    @FXML private TextField costField;
    @FXML private TextField quantityField;

    // Table and columns
    @FXML private TableView<CostEntry> costTable;
    @FXML private TableColumn<CostEntry, LocalDate> dateCol;
    @FXML private TableColumn<CostEntry, String> descriptionCol;
    @FXML private TableColumn<CostEntry, String> categoryCol;
    @FXML private TableColumn<CostEntry, Number> costCol;
    @FXML private TableColumn<CostEntry, Number> quantityCol;
    @FXML private TableColumn<CostEntry, String> unitSizeCol;
    @FXML private TableColumn<CostEntry, Number> pricePerCol;
    @FXML private TableColumn<CostEntry, Number> totalCostCol;

    // Summary labels and edit button
    @FXML private Label totalCostYTDLabel;
    @FXML private Label totalCostLabel;
    @FXML private Label entryCountLabel;
    @FXML private Button editButton;
    
    // Loading spinner and tabs
    @FXML private javafx.scene.layout.VBox costLoadingPane;
    @FXML private javafx.scene.control.TabPane costTabPane;
    @FXML private javafx.scene.control.Tab entryTab;
    @FXML private javafx.scene.control.Tab reportsTab;
    
    // Reports tab controls
    @FXML private javafx.scene.control.ComboBox<String> reportPeriodCombo;
    @FXML private javafx.scene.control.ComboBox<String> chartTypeCombo;
    @FXML private javafx.scene.control.Button exportButton;
    @FXML private javafx.scene.control.TableView<CategoryReportEntry> categoryTable;
    @FXML private javafx.scene.control.TableColumn<CategoryReportEntry, String> categoryNameCol;
    @FXML private javafx.scene.control.TableColumn<CategoryReportEntry, String> categoryAmountCol;
    @FXML private javafx.scene.control.Label totalSpendingLabel;
    @FXML private javafx.scene.control.Label selectedPeriodLabel;
    @FXML private javafx.scene.layout.StackPane chartContainer;
    @FXML private javafx.scene.control.Label detailsHeaderLabel;
    @FXML private javafx.scene.control.TableView<DetailReportEntry> detailsTable;
    @FXML private javafx.scene.control.TableColumn<DetailReportEntry, String> detailPeriodCol;
    @FXML private javafx.scene.control.TableColumn<DetailReportEntry, String> detailCategoryCol;
    @FXML private javafx.scene.control.TableColumn<DetailReportEntry, String> detailAmountCol;
    @FXML private javafx.scene.control.TableColumn<DetailReportEntry, Integer> detailCountCol;

    private final ObservableList<CostEntry> costData = FXCollections.observableArrayList();
    private final ObservableList<CategoryReportEntry> categoryReportData = FXCollections.observableArrayList();
    private final ObservableList<DetailReportEntry> detailReportData = FXCollections.observableArrayList();
    
    @FXML
    private void exportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Cost Data to Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        
        // Set default filename with current date
        String defaultName = "PigFeed_CostData_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
        fileChooser.setInitialFileName(defaultName);
        
        File file = fileChooser.showSaveDialog(costTable.getScene().getWindow());
        if (file != null) {
            try {
                exportCostDataToExcel(file);
                showAlert("Excel file exported successfully to: " + file.getAbsolutePath());
                
                // Ask if user wants to open the file
                Alert openAlert = new Alert(Alert.AlertType.CONFIRMATION);
                openAlert.setTitle("Export Complete");
                openAlert.setHeaderText("Excel file exported successfully!");
                openAlert.setContentText("Would you like to open the file now?");
                
                if (openAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error exporting to Excel: " + e.getMessage());
            }
        }
    }
    
    private void exportCostDataToExcel(File file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("mm/dd/yyyy"));
            
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(createHelper.createDataFormat().getFormat("$#,##0.00"));
            
            // Sheet 1: All Cost Entries
            Sheet entriesSheet = workbook.createSheet("Cost Entries");
            createCostEntriesSheet(entriesSheet, headerStyle, titleStyle, dateStyle, currencyStyle);
            
            // Sheet 2: Category Summary
            Sheet categorySheet = workbook.createSheet("Category Summary");
            createCategorySummarySheet(categorySheet, headerStyle, titleStyle, currencyStyle);
            
            // Sheet 3: Monthly Details
            Sheet monthlySheet = workbook.createSheet("Monthly Details");
            createMonthlyDetailsSheet(monthlySheet, headerStyle, titleStyle, currencyStyle);
            
            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
        }
    }
    
    private void createCostEntriesSheet(Sheet sheet, CellStyle headerStyle, CellStyle titleStyle, 
                                      CellStyle dateStyle, CellStyle currencyStyle) {
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Pig Feed Cost Tracker - All Entries");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        
        // Export date
        Row exportDateRow = sheet.createRow(rowNum++);
        exportDateRow.createCell(0).setCellValue("Exported on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        
        rowNum++; // Empty row
        
        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Date", "Description", "Category", "Ingredient", "Unit Size", "Cost Per Unit", "Total Quantity", "Total Cost"};
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows
        double totalCost = 0.0;
        for (CostEntry entry : costData) {
            Row row = sheet.createRow(rowNum++);
            
            // Date
            org.apache.poi.ss.usermodel.Cell dateCell = row.createCell(0);
            if (entry.getDate() != null) {
                dateCell.setCellValue(java.sql.Date.valueOf(entry.getDate()));
                dateCell.setCellStyle(dateStyle);
            }
            
            // Description
            row.createCell(1).setCellValue(entry.getDescription());
            
            // Category
            row.createCell(2).setCellValue(entry.getCategory());
            
            // Ingredient
            row.createCell(3).setCellValue(entry.getIngredient() != null ? entry.getIngredient() : "");
            
            // Unit Size
            row.createCell(4).setCellValue(entry.getUnitSize() != null ? entry.getUnitSize() : "");
            
            // Cost Per Unit (calculate from total cost and quantity)
            double costPerUnit = 0.0;
            if ("Feed".equals(entry.getCategory()) && !entry.getUnitSize().isEmpty()) {
                double poundsPerUnit = getPoundsPerUnit(entry.getUnitSize());
                double units = entry.getQuantity() / poundsPerUnit;
                costPerUnit = units > 0 ? entry.getCost() / units : 0.0;
            } else {
                costPerUnit = entry.getQuantity() > 0 ? entry.getCost() / entry.getQuantity() : 0.0;
            }
            org.apache.poi.ss.usermodel.Cell costPerUnitCell = row.createCell(5);
            costPerUnitCell.setCellValue(costPerUnit);
            costPerUnitCell.setCellStyle(currencyStyle);
            
            // Total Quantity
            row.createCell(6).setCellValue(entry.getQuantity());
            
            // Total Cost
            org.apache.poi.ss.usermodel.Cell totalCostCell = row.createCell(7);
            totalCostCell.setCellValue(entry.getCost());
            totalCostCell.setCellStyle(currencyStyle);
            
            totalCost += entry.getCost();
        }
        
        // Calculate YTD total (current year)
        int currentYear = LocalDate.now().getYear();
        double ytdTotal = costData.stream()
            .filter(entry -> entry.getDate() != null && entry.getDate().getYear() == currentYear)
            .mapToDouble(CostEntry::getCost)
            .sum();
        
        // Total rows
        rowNum++; // Empty row
        
        // YTD Total row
        Row ytdRow = sheet.createRow(rowNum++);
        ytdRow.createCell(6).setCellValue("YTD TOTAL (" + currentYear + "):");
        org.apache.poi.ss.usermodel.Cell ytdTotalCell = ytdRow.createCell(7);
        ytdTotalCell.setCellValue(ytdTotal);
        ytdTotalCell.setCellStyle(currencyStyle);
        
        // Grand Total row
        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(6).setCellValue("GRAND TOTAL (All Time):");
        org.apache.poi.ss.usermodel.Cell grandTotalCell = totalRow.createCell(7);
        grandTotalCell.setCellValue(totalCost);
        grandTotalCell.setCellStyle(currencyStyle);
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createCategorySummarySheet(Sheet sheet, CellStyle headerStyle, CellStyle titleStyle, CellStyle currencyStyle) {
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Category Summary");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
        
        rowNum++; // Empty row
        
        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Category");
        headerRow.createCell(1).setCellValue("Total Amount");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);
        
        // Get category data (all time and YTD)
        List<CategoryReportEntry> categories = getCategoryData();
        List<CategoryReportEntry> ytdCategories = getCategoryDataYTD();
        double grandTotal = 0.0;
        double ytdGrandTotal = 0.0;
        
        // Add YTD column header
        headerRow.createCell(2).setCellValue("YTD Amount");
        headerRow.getCell(2).setCellStyle(headerStyle);
        
        // Create a map for easier YTD lookup
        java.util.Map<String, Double> ytdMap = new java.util.HashMap<>();
        for (CategoryReportEntry ytdCategory : ytdCategories) {
            ytdMap.put(ytdCategory.getCategory(), ytdCategory.getAmount());
        }
        
        for (CategoryReportEntry category : categories) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(category.getCategory());
            
            // All time amount
            org.apache.poi.ss.usermodel.Cell amountCell = row.createCell(1);
            amountCell.setCellValue(category.getAmount());
            amountCell.setCellStyle(currencyStyle);
            
            // YTD amount
            double ytdAmount = ytdMap.getOrDefault(category.getCategory(), 0.0);
            org.apache.poi.ss.usermodel.Cell ytdAmountCell = row.createCell(2);
            ytdAmountCell.setCellValue(ytdAmount);
            ytdAmountCell.setCellStyle(currencyStyle);
            
            grandTotal += category.getAmount();
            ytdGrandTotal += ytdAmount;
        }
        
        // Total rows
        rowNum++; // Empty row
        
        // YTD Total row
        Row ytdTotalRow = sheet.createRow(rowNum++);
        ytdTotalRow.createCell(0).setCellValue("YTD TOTAL (" + LocalDate.now().getYear() + "):");
        org.apache.poi.ss.usermodel.Cell ytdTotalCell = ytdTotalRow.createCell(2);
        ytdTotalCell.setCellValue(ytdGrandTotal);
        ytdTotalCell.setCellStyle(currencyStyle);
        
        // Grand Total row
        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(0).setCellValue("GRAND TOTAL (All Time):");
        org.apache.poi.ss.usermodel.Cell totalCell = totalRow.createCell(1);
        totalCell.setCellValue(grandTotal);
        totalCell.setCellStyle(currencyStyle);
        
        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }
    
    private void createMonthlyDetailsSheet(Sheet sheet, CellStyle headerStyle, CellStyle titleStyle, CellStyle currencyStyle) {
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Monthly Details");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        
        rowNum++; // Empty row
        
        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Period", "Category", "Amount", "Entry Count"};
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Get monthly details data
        List<DetailReportEntry> details = getMonthlyDetailsData();
        
        for (DetailReportEntry detail : details) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(detail.getPeriod());
            row.createCell(1).setCellValue(detail.getCategory());
            
            org.apache.poi.ss.usermodel.Cell amountCell = row.createCell(2);
            amountCell.setCellValue(detail.getAmount());
            amountCell.setCellStyle(currencyStyle);
            
            row.createCell(3).setCellValue(detail.getCount());
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private List<CategoryReportEntry> getCategoryData() {
        List<CategoryReportEntry> categories = new ArrayList<>();
        String sql = "SELECT category, SUM(cost) as total FROM cost_entries GROUP BY category ORDER BY total DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                categories.add(new CategoryReportEntry(
                    rs.getString("category"),
                    rs.getDouble("total")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return categories;
    }
    
    private List<CategoryReportEntry> getCategoryDataYTD() {
        List<CategoryReportEntry> categories = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        String sql = "SELECT category, SUM(cost) as total FROM cost_entries WHERE strftime('%Y', date) = ? GROUP BY category ORDER BY total DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, String.valueOf(currentYear));
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                categories.add(new CategoryReportEntry(
                    rs.getString("category"),
                    rs.getDouble("total")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return categories;
    }
    
    private List<DetailReportEntry> getMonthlyDetailsData() {
        List<DetailReportEntry> details = new ArrayList<>();
        String sql = """
            SELECT 
                strftime('%Y-%m', date) as month,
                category,
                SUM(cost) as total,
                COUNT(*) as count
            FROM cost_entries 
            GROUP BY strftime('%Y-%m', date), category 
            ORDER BY month DESC, category
            """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String monthStr = rs.getString("month");
                String displayMonth = monthStr != null ? 
                    LocalDate.parse(monthStr + "-01").format(DateTimeFormatter.ofPattern("MMM yyyy")) : 
                    "Unknown";
                    
                details.add(new DetailReportEntry(
                    displayMonth,
                    rs.getString("category"),
                    rs.getDouble("total"),
                    rs.getInt("count")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return details;
    }
    

    // Report data classes
    public static class CategoryReportEntry {
        private final String category;
        private final double amount;
        
        public CategoryReportEntry(String category, double amount) {
            this.category = category;
            this.amount = amount;
        }
        
        public String getCategory() { return category; }
        public double getAmount() { return amount; }
        public String getAmountFormatted() { return String.format("$%.2f", amount); }
    }
    
    public static class DetailReportEntry {
        private final String period;
        private final String category;
        private final double amount;
        private final int count;
        
        public DetailReportEntry(String period, String category, double amount, int count) {
            this.period = period;
            this.category = category;
            this.amount = amount;
            this.count = count;
        }
        
        public String getPeriod() { return period; }
        public String getCategory() { return category; }
        public double getAmount() { return amount; }
        public String getAmountFormatted() { return String.format("$%.2f", amount); }
        public int getCount() { return count; }
    }

    @FXML
    public void initialize() {
        // Initialize date picker to today
        datePicker.setValue(LocalDate.now());
        
        // Initialize expense type combo
        expenseTypeCombo.getItems().addAll("Feed", "Supplements", "Veterinary", "Equipment", "Maintenance", "Other");
        expenseTypeCombo.setValue("Feed"); // Set Feed as default
        
        // Initialize price unit combo
        priceUnitCombo.getItems().addAll("lb", "50lbs", "100lbs", "ton");
        // Load last selected price unit, default to 50lbs
        String lastPriceUnit = loadLastPriceUnit();
        priceUnitCombo.setValue(lastPriceUnit != null ? lastPriceUnit : "50lbs");
        
        // Save price unit when changed
        priceUnitCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                saveLastPriceUnit(newVal);
            }
        });
        
        // Show/hide feed type selection based on expense type
        expenseTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isFeedExpense = "Feed".equals(newVal);
            ingredientLabel.setVisible(isFeedExpense);
            feedTypeBox.setVisible(isFeedExpense);
            priceUnitLabel.setVisible(isFeedExpense);
            priceUnitCombo.setVisible(isFeedExpense);
            if (isFeedExpense && ingredientCombo.getItems().isEmpty()) {
                new Thread(() -> {
                    javafx.application.Platform.runLater(() -> {
                        loadIngredientsIntoCombo();
                    });
                }).start();
            }
        });
        
        // Initialize with Feed selected
        ingredientLabel.setVisible(true);
        feedTypeBox.setVisible(true);
        priceUnitLabel.setVisible(true);
        priceUnitCombo.setVisible(true);
        
        // Load ingredients in background to avoid blocking startup
        new Thread(() -> {
            javafx.application.Platform.runLater(() -> {
                loadIngredientsIntoCombo();
            });
        }).start();
        
        // Auto-populate previous purchase info when ingredient is selected
        ingredientCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                loadPreviousPurchaseInfo(newVal);
            }
        });

        // Set up table columns
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        
        // Price column shows cost per unit (need to calculate from stored total cost)
        costCol.setCellValueFactory(cellData -> {
            CostEntry entry = cellData.getValue();
            // Calculate cost per unit from total cost and total quantity
            // For feed: if 2 bags at $47 each = $94 total, we want to show $47 per unit
            // We need to reverse the calculation: totalCost ÷ (totalPounds ÷ poundsPerUnit)
            double costPerUnit = entry.getCost(); // This is total cost
            if ("Feed".equals(entry.getCategory()) && !entry.getUnitSize().isEmpty()) {
                double poundsPerUnit = getPoundsPerUnit(entry.getUnitSize());
                double units = entry.getQuantity() / poundsPerUnit; // Convert total pounds back to units
                costPerUnit = units > 0 ? entry.getCost() / units : 0.0;
            } else {
                // For non-feed items, assume cost is already per unit
                costPerUnit = entry.getQuantity() > 0 ? entry.getCost() / entry.getQuantity() : 0.0;
            }
            return new javafx.beans.property.SimpleObjectProperty<Number>(costPerUnit);
        });
        
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitSizeCol.setCellValueFactory(new PropertyValueFactory<>("unitSize"));
        pricePerCol.setCellValueFactory(cellData -> {
            CostEntry entry = cellData.getValue();
            double pricePerLb = entry.getQuantity() > 0 ? entry.getCost() / entry.getQuantity() : 0.0;
            return new javafx.beans.property.SimpleObjectProperty<Number>(pricePerLb);
        });
        
        // Total Cost column shows the total amount paid
        totalCostCol.setCellValueFactory(new PropertyValueFactory<>("cost")); // cost field stores total cost

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

        // Format total cost column as currency
        totalCostCol.setCellFactory(column -> new TableCell<CostEntry, Number>() {
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

        // Format price per lb column as currency
        pricePerCol.setCellFactory(column -> new TableCell<CostEntry, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(String.format("$%.2f/lb", item.doubleValue()));
                }
            }
        });

        // Format quantity column as whole numbers
        quantityCol.setCellFactory(column -> new TableCell<CostEntry, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(String.format("%.0f lbs", item.doubleValue()));
                }
            }
        });

        // Bind table to data
        costTable.setItems(costData);

        // Load existing data in background
        new Thread(() -> {
            try {
                Thread.sleep(100); // Brief delay to show loading
                javafx.application.Platform.runLater(() -> {
                    loadCostEntries();
                    // Hide loading spinner and show table
                    costLoadingPane.setVisible(false);
                    costTable.setVisible(true);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Listen for changes to update summary
        costData.addListener((javafx.collections.ListChangeListener<CostEntry>) change -> {
            updateSummary();
        });
        updateSummary();
        
        // Enable/disable edit button based on selection
        costTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            editButton.setDisable(newSelection == null);
        });
        
        // Initialize reports tab only when first accessed to avoid blocking startup
        setupLazyReportsInitialization();
        
    }

    private boolean reportsInitialized = false;
    
    private void setupLazyReportsInitialization() {
        // Only initialize reports when the Reports tab is first selected
        costTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == reportsTab && !reportsInitialized) {
                initializeReportsTab();
                reportsInitialized = true;
            }
        });
    }

    private void initializeReportsTab() {
        // Initialize report period combo
        reportPeriodCombo.getItems().addAll("Monthly", "Yearly", "All Time");
        reportPeriodCombo.setValue("Monthly");
        
        // Initialize chart type combo
        chartTypeCombo.getItems().addAll("Pie Chart", "Line Chart");
        chartTypeCombo.setValue("Pie Chart");
        
        // Setup category table columns
        categoryNameCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory()));
        categoryAmountCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAmountFormatted()));
        
        // Setup detail table columns
        detailPeriodCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPeriod()));
        detailCategoryCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory()));
        detailAmountCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAmountFormatted()));
        detailCountCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getCount()).asObject());
        
        // Bind data to tables
        categoryTable.setItems(categoryReportData);
        detailsTable.setItems(detailReportData);
        
        // Add listeners for updates
        reportPeriodCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateReports());
        chartTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateChart());
        
        // Add click listener for category table (drill-down)
        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showCategoryDetails(newSelection.getCategory());
            }
        });
        
        // Initial load
        updateReports();
    }

    private void updateReports() {
        if (reportPeriodCombo == null || reportPeriodCombo.getValue() == null) return;
        
        String period = reportPeriodCombo.getValue();
        categoryReportData.clear();
        detailReportData.clear();
        
        // Calculate category totals based on period
        java.util.Map<String, Double> categoryTotals = new java.util.HashMap<>();
        java.util.Map<String, Integer> categoryCounts = new java.util.HashMap<>();
        
        java.time.LocalDate now = java.time.LocalDate.now();
        
        for (CostEntry entry : costData) {
            boolean includeEntry = false;
            
            switch (period) {
                case "Monthly":
                    includeEntry = entry.getDate().getYear() == now.getYear() && 
                                 entry.getDate().getMonth() == now.getMonth();
                    break;
                case "Yearly":
                    includeEntry = entry.getDate().getYear() == now.getYear();
                    break;
                case "All Time":
                    includeEntry = true;
                    break;
            }
            
            if (includeEntry) {
                String category = entry.getCategory();
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + entry.getCost());
                categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
            }
        }
        
        // Populate category table
        double totalSpending = 0.0;
        for (java.util.Map.Entry<String, Double> categoryEntry : categoryTotals.entrySet()) {
            categoryReportData.add(new CategoryReportEntry(categoryEntry.getKey(), categoryEntry.getValue()));
            totalSpending += categoryEntry.getValue();
        }
        
        // Update labels
        totalSpendingLabel.setText(String.format("Total: $%.2f", totalSpending));
        selectedPeriodLabel.setText(period);
        
        // Populate details table with monthly breakdown
        populateDetailsTable(period);
        
        // Update chart
        updateChart();
    }
    
    private void populateDetailsTable(String period) {
        if ("All Time".equals(period)) {
            // Show yearly breakdown
            java.util.Map<String, java.util.Map<String, Double>> yearlyData = new java.util.HashMap<>();
            java.util.Map<String, java.util.Map<String, Integer>> yearlyCounts = new java.util.HashMap<>();
            
            for (CostEntry entry : costData) {
                String year = String.valueOf(entry.getDate().getYear());
                String category = entry.getCategory();
                
                yearlyData.computeIfAbsent(year, k -> new java.util.HashMap<>())
                          .merge(category, entry.getCost(), Double::sum);
                yearlyCounts.computeIfAbsent(year, k -> new java.util.HashMap<>())
                           .merge(category, 1, Integer::sum);
            }
            
            for (String year : yearlyData.keySet()) {
                for (String category : yearlyData.get(year).keySet()) {
                    detailReportData.add(new DetailReportEntry(
                        year, category, 
                        yearlyData.get(year).get(category),
                        yearlyCounts.get(year).getOrDefault(category, 0)
                    ));
                }
            }
            detailsHeaderLabel.setText("Yearly Details");
        } else {
            // Show monthly breakdown
            java.util.Map<String, java.util.Map<String, Double>> monthlyData = new java.util.HashMap<>();
            java.util.Map<String, java.util.Map<String, Integer>> monthlyCounts = new java.util.HashMap<>();
            
            int targetYear = "Yearly".equals(period) ? java.time.LocalDate.now().getYear() : 0;
            
            for (CostEntry entry : costData) {
                if (targetYear > 0 && entry.getDate().getYear() != targetYear) continue;
                
                String monthYear = entry.getDate().getMonth().toString() + " " + entry.getDate().getYear();
                String category = entry.getCategory();
                
                monthlyData.computeIfAbsent(monthYear, k -> new java.util.HashMap<>())
                          .merge(category, entry.getCost(), Double::sum);
                monthlyCounts.computeIfAbsent(monthYear, k -> new java.util.HashMap<>())
                           .merge(category, 1, Integer::sum);
            }
            
            for (String monthYear : monthlyData.keySet()) {
                for (String category : monthlyData.get(monthYear).keySet()) {
                    detailReportData.add(new DetailReportEntry(
                        monthYear, category,
                        monthlyData.get(monthYear).get(category),
                        monthlyCounts.get(monthYear).getOrDefault(category, 0)
                    ));
                }
            }
            detailsHeaderLabel.setText("Monthly Details");
        }
    }
    
    private void showCategoryDetails(String selectedCategory) {
        // Filter details table to show only selected category
        detailReportData.clear();
        String period = reportPeriodCombo.getValue();
        
        java.util.Map<String, Double> periodTotals = new java.util.HashMap<>();
        java.util.Map<String, Integer> periodCounts = new java.util.HashMap<>();
        
        for (CostEntry entry : costData) {
            if (!entry.getCategory().equals(selectedCategory)) continue;
            
            boolean includeEntry = false;
            java.time.LocalDate now = java.time.LocalDate.now();
            
            switch (period) {
                case "Monthly":
                    includeEntry = entry.getDate().getYear() == now.getYear() && 
                                 entry.getDate().getMonth() == now.getMonth();
                    break;
                case "Yearly":
                    includeEntry = entry.getDate().getYear() == now.getYear();
                    break;
                case "All Time":
                    includeEntry = true;
                    break;
            }
            
            if (includeEntry) {
                String monthYear = entry.getDate().getMonth().toString() + " " + entry.getDate().getYear();
                periodTotals.merge(monthYear, entry.getCost(), Double::sum);
                periodCounts.merge(monthYear, 1, Integer::sum);
            }
        }
        
        for (String monthYear : periodTotals.keySet()) {
            detailReportData.add(new DetailReportEntry(
                monthYear, selectedCategory,
                periodTotals.get(monthYear),
                periodCounts.get(monthYear)
            ));
        }
        
        detailsHeaderLabel.setText(selectedCategory + " - Monthly Details");
    }
    
    private void updateChart() {
        if (chartTypeCombo == null || chartTypeCombo.getValue() == null) return;
        
        chartContainer.getChildren().clear();
        
        String chartType = chartTypeCombo.getValue();
        
        if ("Pie Chart".equals(chartType)) {
            createPieChart();
        } else if ("Line Chart".equals(chartType)) {
            createLineChart();
        }
    }
    
    private void createPieChart() {
        javafx.scene.chart.PieChart pieChart = new javafx.scene.chart.PieChart();
        
        for (CategoryReportEntry entry : categoryReportData) {
            javafx.scene.chart.PieChart.Data slice = new javafx.scene.chart.PieChart.Data(
                entry.getCategory(), entry.getAmount());
            pieChart.getData().add(slice);
        }
        
        pieChart.setTitle("Spending by Category");
        chartContainer.getChildren().add(pieChart);
    }
    
    private void createLineChart() {
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        
        javafx.scene.chart.LineChart<String, Number> lineChart = 
            new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        
        xAxis.setLabel("Period");
        yAxis.setLabel("Amount ($)");
        lineChart.setTitle("Spending Over Time");
        
        // Group data by category for line series  
        java.util.Map<String, javafx.scene.chart.XYChart.Series<String, Number>> seriesMap = 
            new java.util.HashMap<>();
        
        for (DetailReportEntry entry : detailReportData) {
            javafx.scene.chart.XYChart.Series<String, Number> series = 
                seriesMap.computeIfAbsent(entry.getCategory(), 
                    k -> new javafx.scene.chart.XYChart.Series<>());
            series.setName(entry.getCategory());
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getPeriod(), entry.getAmount()));
        }
        
        lineChart.getData().addAll(seriesMap.values());
        chartContainer.getChildren().add(lineChart);
    }
    

    @FXML
    private void addCostEntry() {
        try {
            // Validate input
            LocalDate date = datePicker.getValue();
            String description = descriptionField.getText().trim();
            String category = expenseTypeCombo.getValue();
            String ingredient = null;
            if ("Feed".equals(category)) {
                ingredient = ingredientCombo.getValue();
                if (ingredient == null || ingredient.isEmpty()) {
                    showError("Input Error", "Please select an ingredient for feed expenses.");
                    return;
                }
            }
            String costText = costField.getText().trim();
            String quantityText = quantityField.getText().trim();

            if (date == null || description.isEmpty() || category == null || category.isEmpty() || 
                costText.isEmpty() || quantityText.isEmpty()) {
                showError("Input Error", "Please fill in all fields.");
                return;
            }

            double costPerUnit = Double.parseDouble(costText);
            double quantityUnits = Double.parseDouble(quantityText);

            if (costPerUnit < 0 || quantityUnits < 0) {
                showError("Input Error", "Cost and quantity must be positive numbers.");
                return;
            }

            // Calculate total cost = quantity × cost per unit
            double totalCost = quantityUnits * costPerUnit;

            // For feed expenses, convert quantity to total pounds for accurate price per lb calculation
            // Example: 2 bags × 50 lbs/bag = 100 total lbs, so $94 ÷ 100 lbs = $0.94/lb
            double totalPounds = quantityUnits;
            String unitSize = "";
            if ("Feed".equals(category)) {
                String priceUnit = priceUnitCombo.getValue();
                totalPounds = quantityUnits * getPoundsPerUnit(priceUnit);
                unitSize = priceUnit;
            }

            // Create new entry: store total cost and total pounds/units
            CostEntry entry = new CostEntry(date, description, category, ingredient, unitSize, totalCost, totalPounds);
            
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
        String sql = "INSERT INTO cost_entries (date, description, category, ingredient, unitSize, cost, quantity, total) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entry.getDate().toString());
            pstmt.setString(2, entry.getDescription());
            pstmt.setString(3, entry.getCategory());
            pstmt.setString(4, entry.getIngredient());
            pstmt.setString(5, entry.getUnitSize());
            pstmt.setDouble(6, entry.getCost());
            pstmt.setDouble(7, entry.getQuantity());
            pstmt.setDouble(8, entry.getTotal());
            
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
                try {
                    LocalDate date = LocalDate.parse(rs.getString("date"));
                    String description = rs.getString("description");
                    String category = rs.getString("category");
                    String ingredient = rs.getString("ingredient");
                    String unitSize = rs.getString("unitSize");
                    double cost = rs.getDouble("cost");
                    double quantity = rs.getDouble("quantity");
                    
                    CostEntry entry = new CostEntry(date, description, category, ingredient, unitSize != null ? unitSize : "", cost, quantity);
                    costData.add(entry);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            // Force table refresh after loading data
            if (!costData.isEmpty()) {
                costTable.refresh();
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
    private void editSelectedEntry() {
        CostEntry selectedEntry = costTable.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) return;
        
        // Populate form with selected entry data
        datePicker.setValue(selectedEntry.getDate());
        expenseTypeCombo.setValue(selectedEntry.getCategory());
        
        // Handle ingredient selection for feed expenses
        if ("Feed".equals(selectedEntry.getCategory())) {
            ingredientCombo.setValue(selectedEntry.getIngredient());
        }
        
        descriptionField.setText(selectedEntry.getDescription());
        
        // Convert stored values back to input format (cost per unit and quantity in units)
        if ("Feed".equals(selectedEntry.getCategory())) {
            // Assume the most common case (50lb bags) for editing
            // User can adjust the price unit if needed
            priceUnitCombo.setValue("50lbs");
            double originalQuantityUnits = selectedEntry.getQuantity() / 50.0; // Convert from total lbs to bags
            double costPerUnit = selectedEntry.getCost() / originalQuantityUnits; // Convert from total cost to cost per unit
            costField.setText(String.format("%.2f", costPerUnit));
            quantityField.setText(String.format("%.0f", originalQuantityUnits));
        } else {
            // For non-feed items, assume 1:1 quantity mapping
            double originalQuantityUnits = selectedEntry.getQuantity();
            double costPerUnit = selectedEntry.getCost() / originalQuantityUnits;
            costField.setText(String.format("%.2f", costPerUnit));
            quantityField.setText(String.format("%.0f", originalQuantityUnits));
        }
        
        // Remove the selected entry from table and database so it can be re-added
        costData.remove(selectedEntry);
        deleteEntryFromDatabase(selectedEntry);
        
        showInfo("Edit Entry", "Entry loaded for editing. Modify the values and click 'Add Entry' to save changes.");
    }


    @FXML
    private void backToWelcome() {
        try {
            Parent welcomeRoot = FXMLLoader.load(
                getClass().getResource("/org/pigfeed/pigfeedapp/welcome-view.fxml")
            );
            
            Stage stage = (Stage) costTable.getScene().getWindow();
            Scene welcomeScene = new Scene(welcomeRoot, 600, 400);
            stage.setScene(welcomeScene);
            stage.setTitle("Pig Feed App");
            setApplicationIcon(stage);
            stage.centerOnScreen();
            
        } catch (IOException e) {
            showError("Navigation Error", "Could not return to welcome screen: " + e.getMessage());
        }
    }
    
    @FXML
    private void goToFeedMix() {
        try {
            Parent feedMixRoot = FXMLLoader.load(
                getClass().getResource("/org/pigfeed/pigfeedapp/feed-mix-view.fxml")
            );
            
            Stage stage = (Stage) costTable.getScene().getWindow();
            Scene feedMixScene = new Scene(feedMixRoot, 900, 700);
            stage.setScene(feedMixScene);
            stage.setTitle("Feed Mix Calculator");
            setApplicationIcon(stage);
            stage.centerOnScreen();
            
        } catch (IOException e) {
            showError("Navigation Error", "Could not open feed mix calculator: " + e.getMessage());
        }
    }

    private void clearForm() {
        datePicker.setValue(LocalDate.now());
        descriptionField.clear();
        expenseTypeCombo.setValue("Feed"); // Reset to default
        ingredientCombo.getSelectionModel().clearSelection();
        priceUnitCombo.setValue("50lbs");
        // Keep Feed fields visible since it's the default
        ingredientLabel.setVisible(true);
        feedTypeBox.setVisible(true);
        priceUnitLabel.setVisible(true);
        priceUnitCombo.setVisible(true);
        costField.clear();
        quantityField.clear();
    }

    private void updateSummary() {
        double totalCost = costData.stream()
            .mapToDouble(CostEntry::getCost)
            .sum();
        
        // Calculate YTD costs (current year only)
        int currentYear = java.time.LocalDate.now().getYear();
        double ytdCost = costData.stream()
            .filter(entry -> entry.getDate().getYear() == currentYear)
            .mapToDouble(CostEntry::getCost)
            .sum();
        
        totalCostYTDLabel.setText(String.format("Costs YTD: $%.2f", ytdCost));
        totalCostLabel.setText(String.format("Costs Since Starting: $%.2f", totalCost));
        entryCountLabel.setText("Entries: " + costData.size());
    }

    private void loadIngredientsIntoCombo() {
        ingredientCombo.getItems().clear();
        String sql = "SELECT name FROM ingredients ORDER BY name";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                ingredientCombo.getItems().add(rs.getString("name"));
            }
            
        } catch (SQLException e) {
            showError("Database Error", "Could not load ingredients: " + e.getMessage());
        }
    }
    
    private void loadPreviousPurchaseInfo(String ingredientName) {
        // First, always set the description to the ingredient name
        descriptionField.setText(ingredientName);
        
        // Then try to load the most recent purchase info for this ingredient
        String sql = "SELECT cost, quantity FROM cost_entries WHERE ingredient = ? AND category = 'Feed' ORDER BY date DESC LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, ingredientName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Auto-populate with previous purchase info
                    double cost = rs.getDouble("cost");
                    double quantity = rs.getDouble("quantity");
                    
                    costField.setText(String.format("%.2f", cost));
                    quantityField.setText(String.format("%.0f", quantity));
                }
            }
            
        } catch (SQLException e) {
            // If no previous purchase found or error, just keep the description
        }
    }

    private void deleteEntryFromDatabase(CostEntry entry) {
        // Use ROWID to delete the specific entry since SQLite doesn't support LIMIT in DELETE
        String sql = "DELETE FROM cost_entries WHERE ROWID = (SELECT ROWID FROM cost_entries WHERE date = ? AND description = ? AND category = ? AND cost = ? AND quantity = ? AND unitSize = ? LIMIT 1)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entry.getDate().toString());
            pstmt.setString(2, entry.getDescription());
            pstmt.setString(3, entry.getCategory());
            pstmt.setDouble(4, entry.getCost());
            pstmt.setDouble(5, entry.getQuantity());
            pstmt.setString(6, entry.getUnitSize());
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            showError("Database Error", "Could not delete cost entry: " + e.getMessage());
        }
    }

    /**
     * Converts price units to total pounds for accurate price per pound calculation
     * Example: If user enters 1 bag of "50lbs" corn for $15, this returns 50.0
     * So we calculate $15 ÷ 50 lbs = $0.30/lb instead of $15/lb
     */
    private double getPoundsPerUnit(String priceUnit) {
        return switch (priceUnit) {
            case "lb" -> 1.0;        // Individual pounds
            case "50lbs" -> 50.0;    // Standard feed bag size
            case "100lbs" -> 100.0;  // Large feed bag
            case "ton" -> 2000.0;    // Bulk feed purchase (1 ton = 2000 lbs)
            default -> 1.0;          // Default to 1 lb if unknown unit
        };
    }

    @FXML
    private void showAddFeedTypeDialog() {
        // Create a simple input dialog for adding new feed types
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Add New Feed Type");
        dialog.setHeaderText("Enter Feed Type Information");
        dialog.setContentText("Feed Type Name:");
        
        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(feedTypeName -> {
            if (!feedTypeName.trim().isEmpty()) {
                // Add the new feed type to the ingredients table with basic nutrition values
                addNewFeedType(feedTypeName.trim());
                // Refresh the dropdown
                loadIngredientsIntoCombo();
                // Select the newly added feed type
                ingredientCombo.setValue(feedTypeName.trim());
            }
        });
    }
    
    /**
     * Adds a new feed type to the ingredients database with default nutrition values
     * Users can later edit these values in the Feed Mix Calculator
     */
    private void addNewFeedType(String feedTypeName) {
        String sql = "INSERT OR REPLACE INTO ingredients(name, crudeProtein, crudeFat, crudeFiber, lysine) VALUES(?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, feedTypeName);
            // Set default nutrition values - users can edit these later
            ps.setDouble(2, 14.0);  // Default protein %
            ps.setDouble(3, 4.0);   // Default fat %
            ps.setDouble(4, 5.0);   // Default fiber %
            ps.setDouble(5, 0.8);   // Default lysine %
            
            ps.executeUpdate();
            
            showSuccess("Success", "Feed type '" + feedTypeName + "' added successfully!\n" + 
                       "Default nutrition values have been set. You can edit them in the Feed Mix Calculator.");
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database Error", "Could not add feed type: " + e.getMessage());
        }
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Add icon to dialog
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
            setApplicationIcon(stage);
        } catch (Exception e) {
            // Ignore if can't set icon
        }
        alert.showAndWait();
    }
    
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Add icon to dialog
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
            setApplicationIcon(stage);
        } catch (Exception e) {
            // Ignore if can't set icon
        }
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Add icon to dialog
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
            setApplicationIcon(stage);
        } catch (Exception e) {
            // Ignore if can't set icon
        }
        alert.showAndWait();
    }
    
    private String loadLastPriceUnit() {
        String sql = "SELECT value FROM user_preferences WHERE key = 'lastPriceUnit'";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            // Ignore error, will return null to use default
        }
        return null; // Default to null so caller can use "50lbs"
    }
    
    private void saveLastPriceUnit(String priceUnit) {
        String sql = "INSERT OR REPLACE INTO user_preferences (key, value) VALUES ('lastPriceUnit', ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, priceUnit);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silently ignore error
        }
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}