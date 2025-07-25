package org.pigfeed.pigfeedapp.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class CostEntry {
    private final ObjectProperty<LocalDate> date;
    private final StringProperty description;
    private final StringProperty category;
    private final StringProperty ingredient;
    private final StringProperty unitSize; // e.g., "50lbs", "100lbs", "ton"
    private final DoubleProperty cost;
    private final DoubleProperty quantity;
    private final DoubleProperty total;

    public CostEntry() {
        this.date = new SimpleObjectProperty<>();
        this.description = new SimpleStringProperty("");
        this.category = new SimpleStringProperty("");
        this.ingredient = new SimpleStringProperty("");
        this.unitSize = new SimpleStringProperty("");
        this.cost = new SimpleDoubleProperty(0.0);
        this.quantity = new SimpleDoubleProperty(0.0);
        this.total = new SimpleDoubleProperty(0.0);
    }

    public CostEntry(LocalDate date, String description, String category, String ingredient, String unitSize, double cost, double quantity) {
        this();
        setDate(date);
        setDescription(description);
        setCategory(category);
        setIngredient(ingredient);
        setUnitSize(unitSize);
        setCost(cost);
        setQuantity(quantity);
        // Cost parameter is now total cost, quantity is total pounds/units
        // Total equals the total cost paid
        setTotal(cost);
    }
    
    // Backward compatibility constructor without unitSize
    public CostEntry(LocalDate date, String description, String category, String ingredient, double cost, double quantity) {
        this(date, description, category, ingredient, "", cost, quantity);
    }
    
    // Backward compatibility constructor
    public CostEntry(LocalDate date, String description, String category, double cost, double quantity) {
        this(date, description, category, null, cost, quantity);
    }

    // Date property
    public ObjectProperty<LocalDate> dateProperty() { return date; }
    public LocalDate getDate() { return date.get(); }
    public void setDate(LocalDate date) { this.date.set(date); }

    // Description property
    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }

    // Category property
    public StringProperty categoryProperty() { return category; }
    public String getCategory() { return category.get(); }
    public void setCategory(String category) { this.category.set(category); }

    // Ingredient property
    public StringProperty ingredientProperty() { return ingredient; }
    public String getIngredient() { return ingredient.get(); }
    public void setIngredient(String ingredient) { this.ingredient.set(ingredient != null ? ingredient : ""); }

    // UnitSize property
    public StringProperty unitSizeProperty() { return unitSize; }
    public String getUnitSize() { return unitSize.get(); }
    public void setUnitSize(String unitSize) { this.unitSize.set(unitSize != null ? unitSize : ""); }

    // Cost property
    public DoubleProperty costProperty() { return cost; }
    public double getCost() { return cost.get(); }
    public void setCost(double cost) { 
        this.cost.set(cost);
        updateTotal();
    }

    // Quantity property
    public DoubleProperty quantityProperty() { return quantity; }
    public double getQuantity() { return quantity.get(); }
    public void setQuantity(double quantity) { 
        this.quantity.set(quantity);
        updateTotal();
    }

    // Total property (calculated)
    public DoubleProperty totalProperty() { return total; }
    public double getTotal() { return total.get(); }
    public void setTotal(double total) { this.total.set(total); }

    private void updateTotal() {
        setTotal(getCost() * getQuantity());
    }
}