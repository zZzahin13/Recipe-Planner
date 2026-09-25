package com.recipeplanner.controller;

/**
 * Implemented by any screen controller that needs to navigate to another
 * screen (e.g. opening the recipe detail view from a clicked card).
 * MainController calls setMainController() right after loading a
 * screen's FXML, before it's shown.
 */
public interface MainAware {
    void setMainController(MainController mainController);
}
