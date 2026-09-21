package com.recipeplanner.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * App shell: a BorderPane with a VBox navigation sidebar on the left and
 * the active screen filling the center. Screens are lazily created and
 * cached so switching tabs doesn't re-hit the database/network every time.
 */
public class MainView extends BorderPane {

    private final VBox navBar = new VBox(8);
    private final RecipeDetailView detailView = new RecipeDetailView();

    private HomeView homeView;
    private SearchView searchView;
    private FavoritesView favoritesView;
    private MealPlannerView mealPlannerView;
    private CustomRecipeView customRecipeView;

    public MainView() {
        buildNav();
        setLeft(navBar);
        showHome(); // default screen
    }

    private void buildNav() {
        navBar.setPadding(new Insets(16));
        navBar.setPrefWidth(180);
        navBar.getStyleClass().add("nav-bar");

        Label appTitle = new Label("Recipe Planner");
        appTitle.getStyleClass().add("app-title");

        Button homeBtn = navButton("Home", this::showHome);
        Button searchBtn = navButton("Search", this::showSearch);
        Button favoritesBtn = navButton("Favorites", this::showFavorites);
        Button plannerBtn = navButton("Meal Planner", this::showMealPlanner);
        Button customBtn = navButton("My Recipes", this::showCustomRecipe);

        navBar.getChildren().addAll(appTitle, homeBtn, searchBtn, favoritesBtn, plannerBtn, customBtn);
    }

    private Button navButton(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> action.run());
        VBox.setVgrow(b, Priority.NEVER);
        return b;
    }

    // ---------------------------------------------------------------
    // Navigation -- each screen gets a callback into showDetail so it
    // can hand off to the shared RecipeDetailView.
    // ---------------------------------------------------------------

    private void showHome() {
        if (homeView == null) {
            homeView = new HomeView(this::showSearch, this::showFavorites, this::showMealPlanner,
                    this::showCustomRecipe, this::showDetail);
        }
        homeView.refresh();
        setCenter(homeView);
    }

    private void showSearch() {
        if (searchView == null) {
            searchView = new SearchView(this::showDetail);
        }
        setCenter(searchView);
    }

    private void showFavorites() {
        if (favoritesView == null) {
            favoritesView = new FavoritesView(this::showDetail);
        }
        favoritesView.refresh();
        setCenter(favoritesView);
    }

    private void showMealPlanner() {
        if (mealPlannerView == null) {
            mealPlannerView = new MealPlannerView();
        }
        mealPlannerView.refresh();
        setCenter(mealPlannerView);
    }

    private void showCustomRecipe() {
        if (customRecipeView == null) {
            customRecipeView = new CustomRecipeView(this::showFavoritesAfterSave);
        }
        setCenter(customRecipeView);
    }

    private void showFavoritesAfterSave() {
        showFavorites();
    }

    private void showDetail(com.recipeplanner.model.Recipe recipe) {
        detailView.setRecipe(recipe);
        setCenter(detailView);
    }
}
