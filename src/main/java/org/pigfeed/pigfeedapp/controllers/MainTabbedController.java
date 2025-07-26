package org.pigfeed.pigfeedapp.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainTabbedController {
    
    @FXML private TabPane mainTabPane;
    @FXML private Tab welcomeTab;
    @FXML private Tab costTrackerTab;
    @FXML private Tab feedMixTab;
    
    private boolean feedMixLoaded = false;
    private boolean costTrackerLoaded = false;
    
    @FXML
    public void initialize() {
        // Set welcome tab as selected by default
        mainTabPane.getSelectionModel().select(welcomeTab);
        
        // Setup lazy loading for tabs
        setupLazyTabLoading();
        
        // Start background preloading after Welcome screen is shown
        startBackgroundTabPreloading();
    }
    
    private void setupLazyTabLoading() {
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == feedMixTab && !feedMixLoaded) {
                loadFeedMixTab();
            } else if (newTab == costTrackerTab && !costTrackerLoaded) {
                loadCostTrackerTab();
            }
        });
    }
    
    private void loadFeedMixTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/pigfeed/pigfeedapp/feed-mix-view.fxml"));
            feedMixTab.setContent(loader.load());
            feedMixLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadCostTrackerTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/pigfeed/pigfeedapp/cost-tracker-view.fxml"));
            costTrackerTab.setContent(loader.load());
            costTrackerLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void startBackgroundTabPreloading() {
        // Start preloading tabs in background after a short delay to ensure Welcome screen is fully loaded
        new Thread(() -> {
            try {
                // Small delay to let Welcome screen fully render
                Thread.sleep(500);
                
                // Preload Feed Mix Calculator tab first (likely used more often)
                if (!feedMixLoaded) {
                    javafx.application.Platform.runLater(() -> {
                        loadFeedMixTab();
                    });
                }
                
                // Wait a bit more, then preload Cost Tracker
                Thread.sleep(1000);
                if (!costTrackerLoaded) {
                    javafx.application.Platform.runLater(() -> {
                        loadCostTrackerTab();
                    });
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    // Public methods that can be called from other controllers to switch tabs
    public void switchToWelcome() {
        mainTabPane.getSelectionModel().select(welcomeTab);
    }
    
    public void switchToCostTracker() {
        mainTabPane.getSelectionModel().select(costTrackerTab);
    }
    
    public void switchToFeedMix() {
        mainTabPane.getSelectionModel().select(feedMixTab);
    }
}