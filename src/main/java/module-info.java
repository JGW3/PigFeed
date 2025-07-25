module org.pigfeed.pigfeedapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // Allow reflective access by javafx.fxml to the controllers package
    opens org.pigfeed.pigfeedapp.controllers to javafx.fxml;
    // Allow JavaFX to access model classes for TableView property binding
    opens org.pigfeed.pigfeedapp.model to javafx.base;
    // Optionally, if other packages need to be open or exported, add them here
    exports org.pigfeed.pigfeedapp;
}
