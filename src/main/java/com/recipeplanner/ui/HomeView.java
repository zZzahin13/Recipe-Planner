package com.recipeplanner.ui;

import com.recipeplanner.dao.MealPlanDAO;
import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.Recipe;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * The app's landing screen: a welcome header, a few at-a-glance stats
 * pulled from SQLite, quick-action buttons into the other screens, and
 * a small strip of recently-saved recipes. This is what appears first
 * after the splash screen closes, instead of dropping straight into Search.
 */
public class HomeView extends ScrollPane {

    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final MealPlanDAO mealPlanDAO = new MealPlanDAO();

    private final Label recipeCountLabel = new Label();
    private final Label plannedMealsLabel = new Label();
    private final TilePane recentPane = new TilePane();
    private final Label recentHint = new Label();

    public HomeView(Runnable onGoSearch, Runnable onGoFavorites, Runnable onGoPlanner,
                     Runnable onGoCustom, Consumer<Recipe> onRecipeSelected) {
        setFitToWidth(true);
        setContent(buildLayout(onGoSearch, onGoFavorites, onGoPlanner, onGoCustom, onRecipeSelected));
    }

    private VBox buildLayout(Runnable onGoSearch, Runnable onGoFavorites, Runnable onGoPlanner,
                              Runnable onGoCustom, Consumer<Recipe> onRecipeSelected) {
        Label welcome = new Label("Welcome to Recipe Planner");
        welcome.setFont(Font.font("System", FontWeight.BOLD, 28));

        Label subtitle = new Label("Search for recipes, build your library, and plan your week.");
        subtitle.getStyleClass().add("status-label");

        VBox header = new VBox(4, welcome, subtitle);

        HBox statsRow = new HBox(20, statCard(recipeCountLabel, "Saved Recipes"),
                statCard(plannedMealsLabel, "Meals Planned This Week"));

        Button searchBtn = quickActionButton("Search Recipes", onGoSearch);
        Button favoritesBtn = quickActionButton("My Library", onGoFavorites);
        Button plannerBtn = quickActionButton("Meal Planner", onGoPlanner);
        Button customBtn = quickActionButton("Create a Recipe", onGoCustom);
        HBox actionsRow = new HBox(12, searchBtn, favoritesBtn, plannerBtn, customBtn);

        recentHint.getStyleClass().add("status-label");
        recentPane.setHgap(14);
        recentPane.setVgap(14);
        recentPane.setPrefColumns(4);

        VBox recentSection = new VBox(8, new Label("Recently Added"), recentHint, recentPane);

        VBox root = new VBox(24, header, statsRow, actionsRow, recentSection);
        root.setPadding(new Insets(24));

        loadStats();
        loadRecent(onRecipeSelected);

        return root;
    }

    private VBox statCard(Label valueLabel, String caption) {
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 30));
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("card-subtitle");

        VBox card = new VBox(4, valueLabel, captionLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16, 28, 16, 28));
        card.getStyleClass().add("recipe-card");
        return card;
    }

    private Button quickActionButton(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("primary-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    /** Called every time Home is shown again, so the counts don't go stale after you save/plan something. */
    public void refresh() {
        loadStats();
    }

    private void loadStats() {
        try {
            int recipeCount = recipeDAO.getAllRecipes().size();
            recipeCountLabel.setText(String.valueOf(recipeCount));
        } catch (SQLException e) {
            recipeCountLabel.setText("--");
        }
        try {
            int plannedCount = mealPlanDAO.getWeek().size();
            plannedMealsLabel.setText(String.valueOf(plannedCount));
        } catch (SQLException e) {
            plannedMealsLabel.setText("--");
        }
    }

    private void loadRecent(Consumer<Recipe> onRecipeSelected) {
        try {
            List<Recipe> all = recipeDAO.getAllRecipes(); // already ordered by created_at DESC
            List<Recipe> recent = all.stream()
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
                recentPane.getChildren().add(RecipeCardFactory.create(recipe, () -> onRecipeSelected.accept(recipe)));
            }
        } catch (SQLException e) {
            recentHint.setText("Could not load recent recipes: " + e.getMessage());
        }
    }
}
