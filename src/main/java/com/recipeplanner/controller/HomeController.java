package com.recipeplanner.controller;

import com.recipeplanner.dao.MealPlanDAO;
import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.Recipe;
import com.recipeplanner.ui.RecipeCardFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

/** Controller for home.fxml -- the app's landing screen. */
public class HomeController implements MainAware {

    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final MealPlanDAO mealPlanDAO = new MealPlanDAO();
    private MainController mainController;

    @FXML private Label recipeCountLabel;
    @FXML private Label plannedMealsLabel;
    @FXML private Label recentHint;
    @FXML private TilePane recentPane;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        refresh();
    }

    /** Called by MainController every time Home is shown, so stats/recent list don't go stale. */
    public void refresh() {
        loadStats();
        loadRecent();
    }

    private void loadStats() {
        try {
            recipeCountLabel.setText(String.valueOf(recipeDAO.getAllRecipes().size()));
        } catch (SQLException e) {
            recipeCountLabel.setText("--");
        }
        try {
            plannedMealsLabel.setText(String.valueOf(mealPlanDAO.getWeek().size()));
        } catch (SQLException e) {
            plannedMealsLabel.setText("--");
        }
    }

    private void loadRecent() {
        try {
            List<Recipe> recent = recipeDAO.getAllRecipes().stream()
                    .sorted(Comparator.comparingInt(Recipe::getRecipeId).reversed())
                    .limit(4)
                    .toList();

            recentPane.getChildren().clear();
            if (recent.isEmpty()) {
                recentHint.setText("Nothing saved yet -- search for a recipe and favorite one to get started.");
                return;
            }
            recentHint.setText("");
            for (Recipe recipe : recent) {
                recentPane.getChildren().add(RecipeCardFactory.create(recipe, () -> mainController.showDetail(recipe)));
            }
        } catch (SQLException e) {
            recentHint.setText("Could not load recent recipes: " + e.getMessage());
        }
    }

    @FXML
    private void onGoSearch() {
        mainController.showSearch();
    }

    @FXML
    private void onGoFavorites() {
        mainController.showFavorites();
    }

    @FXML
    private void onGoPlanner() {
        mainController.showMealPlanner();
    }

    @FXML
    private void onGoCustom() {
        mainController.showCustomRecipe();
    }
}
