package com.recipeplanner.controller;

import com.recipeplanner.model.Recipe;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for main.fxml (the app shell). Loads each screen's own
 * FXML+Controller pair lazily and caches both the built Parent and its
 * controller, so switching screens doesn't re-hit the database/network
 * every time (same caching behavior the old plain-Java MainView had).
 * Every screen controller gets a back-reference to this one (via
 * MainAware.setMainController) so it can ask to navigate elsewhere --
 * e.g. SearchController calling back into showDetail() when a card is clicked.
 */
public class MainController {

    private final Map<String, Parent> screenCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();

    @FXML
    private StackPane contentArea;

    @FXML
    private void initialize() {
        showHome();
    }

    public void showHome() {
        showScreen("/fxml/home.fxml", "home");
    }

    public void showSearch() {
        showScreen("/fxml/search.fxml", "search");
    }

    public void showFavorites() {
        Object controller = showScreen("/fxml/favorites.fxml", "favorites");
        if (controller instanceof FavoritesController fc) {
            fc.refresh();
        }
    }

    public void showMealPlanner() {
        Object controller = showScreen("/fxml/meal_planner.fxml", "meal_planner");
        if (controller instanceof MealPlannerController mpc) {
            mpc.refresh();
        }
    }

    public void showCustomRecipe() {
        showScreen("/fxml/custom_recipe.fxml", "custom_recipe");
    }

    public void showDetail(Recipe recipe) {
        Object controller = showScreen("/fxml/recipe_detail.fxml", "recipe_detail");
        if (controller instanceof RecipeDetailController rdc) {
            rdc.setRecipe(recipe);
        }
    }

    /** Loads (or reuses) the given FXML, injects this controller into it, and displays it. */
    private Object showScreen(String fxmlPath, String cacheKey) {
        Parent view = screenCache.get(cacheKey);
        Object controller;

        if (view == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                view = loader.load();
                controller = loader.getController();
                if (controller instanceof MainAware mainAware) {
                    mainAware.setMainController(this);
                }
                screenCache.put(cacheKey, view);
                controllerCache.put(cacheKey, controller);
            } catch (IOException e) {
                throw new IllegalStateException("Could not load " + fxmlPath, e);
            }
        } else {
            controller = controllerCache.get(cacheKey);
        }

        contentArea.getChildren().setAll(view);
        return controller;
    }
}
