package org.pigfeed.pigfeedapp.model;

import javafx.beans.property.*;

public class FeedMixEntry {
    private final StringProperty ingredient = new SimpleStringProperty();
    private final DoubleProperty weight = new SimpleDoubleProperty();
    private final DoubleProperty protein = new SimpleDoubleProperty();
    private final DoubleProperty fat = new SimpleDoubleProperty();
    private final DoubleProperty fiber = new SimpleDoubleProperty();
    private final DoubleProperty lysine = new SimpleDoubleProperty();
    private final DoubleProperty percent = new SimpleDoubleProperty();

    // Constructors, getters & setters

    public String getIngredient() { return ingredient.get(); }
    public void setIngredient(String val) { ingredient.set(val); }
    public StringProperty ingredientProperty() { return ingredient; }

    public double getWeight() { return weight.get(); }
    public void setWeight(double val) { weight.set(val); }
    public DoubleProperty weightProperty() { return weight; }

    public double getProtein() { return protein.get(); }
    public void setProtein(double val) { protein.set(val); }
    public DoubleProperty proteinProperty() { return protein; }

    public double getFat() { return fat.get(); }
    public void setFat(double val) { fat.set(val); }
    public DoubleProperty fatProperty() { return fat; }

    public double getFiber() { return fiber.get(); }
    public void setFiber(double val) { fiber.set(val); }
    public DoubleProperty fiberProperty() { return fiber; }

    public double getLysine() { return lysine.get(); }
    public void setLysine(double val) { lysine.set(val); }
    public DoubleProperty lysineProperty() { return lysine; }

    public double getPercent() { return percent.get(); }
    public void setPercent(double val) { percent.set(val); }
    public DoubleProperty percentProperty() { return percent; }
}
