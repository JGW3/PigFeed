package org.pigfeed.pigfeedapp.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.List;

public class SavedMix {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> createdDate = new SimpleObjectProperty<>();
    private final DoubleProperty totalWeight = new SimpleDoubleProperty();
    private final DoubleProperty totalProtein = new SimpleDoubleProperty();
    private final DoubleProperty totalFat = new SimpleDoubleProperty();
    private final DoubleProperty totalFiber = new SimpleDoubleProperty();
    private final DoubleProperty totalLysine = new SimpleDoubleProperty();
    private final DoubleProperty totalCost = new SimpleDoubleProperty();
    
    // Store the mix entries as a transient field (not bound to UI)
    private List<FeedMixEntry> mixEntries;

    // Constructors
    public SavedMix() {}
    
    public SavedMix(String name, List<FeedMixEntry> mixEntries) {
        setName(name);
        this.mixEntries = mixEntries;
        calculateTotals();
        setCreatedDate(LocalDateTime.now());
    }

    // Getters and setters
    public int getId() { return id.get(); }
    public void setId(int val) { id.set(val); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String val) { name.set(val); }
    public StringProperty nameProperty() { return name; }

    public LocalDateTime getCreatedDate() { return createdDate.get(); }
    public void setCreatedDate(LocalDateTime val) { createdDate.set(val); }
    public ObjectProperty<LocalDateTime> createdDateProperty() { return createdDate; }

    public double getTotalWeight() { return totalWeight.get(); }
    public void setTotalWeight(double val) { totalWeight.set(val); }
    public DoubleProperty totalWeightProperty() { return totalWeight; }

    public double getTotalProtein() { return totalProtein.get(); }
    public void setTotalProtein(double val) { totalProtein.set(val); }
    public DoubleProperty totalProteinProperty() { return totalProtein; }

    public double getTotalFat() { return totalFat.get(); }
    public void setTotalFat(double val) { totalFat.set(val); }
    public DoubleProperty totalFatProperty() { return totalFat; }

    public double getTotalFiber() { return totalFiber.get(); }
    public void setTotalFiber(double val) { totalFiber.set(val); }
    public DoubleProperty totalFiberProperty() { return totalFiber; }

    public double getTotalLysine() { return totalLysine.get(); }
    public void setTotalLysine(double val) { totalLysine.set(val); }
    public DoubleProperty totalLysineProperty() { return totalLysine; }

    public double getTotalCost() { return totalCost.get(); }
    public void setTotalCost(double val) { totalCost.set(val); }
    public DoubleProperty totalCostProperty() { return totalCost; }

    public List<FeedMixEntry> getMixEntries() { return mixEntries; }
    public void setMixEntries(List<FeedMixEntry> mixEntries) { 
        this.mixEntries = mixEntries;
        calculateTotals();
    }

    private void calculateTotals() {
        if (mixEntries == null || mixEntries.isEmpty()) return;
        
        var validEntries = mixEntries.stream()
            .filter(entry -> !"Select ingredient...".equals(entry.getIngredient()) && 
                           entry.getWeight() > 0)
            .toList();
        
        double totalWt = validEntries.stream().mapToDouble(FeedMixEntry::getWeight).sum();
        setTotalWeight(totalWt);
        
        if (totalWt > 0) {
            double weightedProtein = 0, weightedFat = 0, weightedFiber = 0, weightedLysine = 0;
            for (FeedMixEntry entry : validEntries) {
                double proportion = entry.getWeight() / totalWt;
                weightedProtein += proportion * entry.getProtein();
                weightedFat += proportion * entry.getFat();
                weightedFiber += proportion * entry.getFiber();
                weightedLysine += proportion * entry.getLysine();
            }
            
            setTotalProtein(weightedProtein);
            setTotalFat(weightedFat);
            setTotalFiber(weightedFiber);
            setTotalLysine(weightedLysine);
        }
    }
    
    @Override
    public String toString() {
        return getName(); // For display in ComboBox
    }
}