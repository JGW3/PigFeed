package org.pigfeed.pigfeedapp;

/**
 * Launcher class to work around module path issues with newer Java versions
 * when running JavaFX applications from IDEs.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}