package com.recipeplanner.controller;

import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.Recipe;
import com.recipeplanner.ui.RecipeCardFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

/** Controller for favorites.fxml -- everything saved to the local library (favorited or custom). */
public class FavoritesController implements MainAware {

    private final RecipeDAO recipeDAO = new RecipeDAO();
    private MainController mainController;

    @FXML private TilePane grid;
    @FXML private Label statusLabel;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void refresh() {
        grid.getChildren().clear();
        try {
            List<Recipe> favorites = recipeDAO.getAllRecipes();
            if (favorites.isEmpty()) {
                statusLabel.setText("No saved recipes yet -- search and favorite one, or create your own.");
                return;
            }
            statusLabel.setText(favorites.size() + " recipe(s) saved.");
            for (Recipe recipe : favorites) {
                grid.getChildren().add(buildCardWithRemove(recipe));
            }
        } catch (SQLException e) {
            statusLabel.setText("Could not load library: " + e.getMessage());
        }
    }

    private VBox buildCardWithRemove(Recipe recipe) {
        VBox card = RecipeCardFactory.create(recipe, () -> mainController.showDetail(recipe));

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("danger-button");
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setOnAction(e -> {
            try {
                recipeDAO.deleteRecipe(recipe.getRecipeId());
                refresh();
            } catch (SQLException ex) {
                statusLabel.setText("Could not remove: " + ex.getMessage());
            }
        });
        card.getChildren().add(removeBtn);
        return card;
    }
}
