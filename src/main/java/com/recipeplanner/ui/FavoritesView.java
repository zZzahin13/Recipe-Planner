package com.recipeplanner.ui;

import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.Recipe;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Personal recipe library: everything the user has bookmarked from
 * search or created manually (RecipeDAO.getAllRecipes / getFavorites).
 * Reads happen directly here since local SQLite reads are fast enough
 * not to need a background Task, unlike the network calls in SearchView.
 */
public class FavoritesView extends BorderPane {

    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final TilePane grid = new TilePane();
    private final Label statusLabel = new Label();
    private final Consumer<Recipe> onRecipeSelected;

    public FavoritesView(Consumer<Recipe> onRecipeSelected) {
        this.onRecipeSelected = onRecipeSelected;
        setPadding(new Insets(16));

        Label heading = new Label("My Recipe Library");
        heading.getStyleClass().add("screen-heading");

        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPrefColumns(4);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);

        VBox top = new VBox(6, heading, statusLabel);
        setTop(top);
        setCenter(scroll);
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
        VBox card = RecipeCardFactory.create(recipe, () -> onRecipeSelected.accept(recipe));

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
