package com.recipeplanner.ui;

import com.recipeplanner.model.Recipe;
import com.recipeplanner.network.MultiSourceSearchTask;
import com.recipeplanner.network.ParallelCategorySearchTask;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import com.recipeplanner.network.MealDbApiService;
/**
 * Recipe search screen. Two ways to search, both off the UI thread:
 *
 *  - Keyword only: MultiSourceSearchTask checks the local library first
 *    and only calls TheMealDB if nothing local matches (Week 4 fallback
 *    pattern + Week 7 JSON).
 *  - One or more categories selected: ParallelCategorySearchTask fires
 *    all the category requests at once via an ExecutorService and
 *    merges the results (Week 4 concurrency).
 *
 * Picking both narrows the parallel category results by the keyword.
 */
public class SearchView extends BorderPane {

    private static final String[] CATEGORIES = {
            "Beef", "Chicken", "Dessert", "Lamb", "Pasta",
            "Pork", "Seafood", "Side", "Starter", "Vegan", "Vegetarian", "Breakfast"
    };

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "recipe-search-worker");
        t.setDaemon(true);
        return t;
    });

    private final TextField keywordField = new TextField();
    private final ListView<String> categoryList = new ListView<>();
    private final Button searchButton = new Button("Search");
    private final Label statusLabel = new Label();
    private final Label sourceLabel = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final TilePane resultsPane = new TilePane();
    private final Consumer<Recipe> onRecipeSelected;

    public SearchView(Consumer<Recipe> onRecipeSelected) {
        this.onRecipeSelected = recipe -> openRecipe(recipe, onRecipeSelected);
        setPadding(new Insets(16));
        setTop(buildSearchBar());
        setCenter(buildResultsArea());
    }
    /** Category results only have title + image, so load the full recipe when a card is clicked. */
    private void openRecipe(Recipe recipe, Consumer<Recipe> open) {
        boolean needsDetails = recipe.getApiId() != null
                && recipe.getRecipeId() == 0
                && recipe.getIngredients().isEmpty();
        if (!needsDetails) {
            open.accept(recipe);
            return;
        }

        statusLabel.setText("Loading recipe...");
        Task<Recipe> task = new Task<>() {
            @Override
            protected Recipe call() throws Exception {
                return new MealDbApiService().getById(recipe.getApiId());
            }
        };
        task.setOnSucceeded(e -> {
            Recipe full = task.getValue();
            statusLabel.setText("");
            open.accept(full != null ? full : recipe);
        });
        task.setOnFailed(e -> statusLabel.setText(
                "Could not load recipe: " + task.getException().getMessage()));
        executor.submit(task);
    }
    private VBox buildSearchBar() {
        keywordField.setPromptText("Search by recipe name (e.g. \"chicken curry\")...");
        keywordField.setPrefWidth(320);
        keywordField.setOnAction(e -> runSearch());

        categoryList.getItems().addAll(CATEGORIES);
        categoryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        categoryList.setPrefHeight(90);
        categoryList.setPrefWidth(160);
        Label categoryHint = new Label("Ctrl/Cmd-click to pick multiple categories to search at once:");
        categoryHint.getStyleClass().add("status-label");

        searchButton.setOnAction(e -> runSearch());
        searchButton.getStyleClass().add("primary-button");

        progress.setVisible(false);
        progress.setPrefSize(24, 24);

        HBox keywordRow = new HBox(10, keywordField, searchButton, progress);
        Button clearCategoriesBtn = new Button("Clear categories");
        clearCategoriesBtn.setOnAction(e -> categoryList.getSelectionModel().clearSelection());
        HBox categoryRow = new HBox(10, categoryList, clearCategoriesBtn);

        VBox bar = new VBox(6, keywordRow, categoryHint, categoryRow);
        bar.setPadding(new Insets(0, 0, 12, 0));
        return bar;
    }

    private VBox buildResultsArea() {
        resultsPane.setHgap(14);
        resultsPane.setVgap(14);
        resultsPane.setPrefColumns(4);

        statusLabel.getStyleClass().add("status-label");
        sourceLabel.getStyleClass().add("status-label");

        ScrollPane scroll = new ScrollPane(resultsPane);
        scroll.setFitToWidth(true);

        VBox box = new VBox(4, statusLabel, sourceLabel, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return box;
    }

    private void runSearch() {
        String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim();
        List<String> selectedCategories = List.copyOf(categoryList.getSelectionModel().getSelectedItems());

        if (keyword.isEmpty() && selectedCategories.isEmpty()) {
            statusLabel.setText("Enter a keyword or pick at least one category first.");
            return;
        }

        sourceLabel.setText("");
        setLoading(true);

        if (!selectedCategories.isEmpty()) {
            runParallelCategorySearch(selectedCategories, keyword.isEmpty() ? null : keyword);
        } else {
            runLocalFirstSearch(keyword);
        }
    }

    private void runLocalFirstSearch(String keyword) {
        MultiSourceSearchTask task = new MultiSourceSearchTask(keyword);
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            statusLabel.textProperty().unbind();
            setLoading(false);
            sourceLabel.setText(task.getSourceUsed() == MultiSourceSearchTask.SourceUsed.LOCAL
                    ? "Source: your saved library"
                    : "Source: TheMealDB (not in your library yet)");
            renderResults(task.getValue(), null);
        });
        bindFailureHandler(task);
        executor.submit(task);
    }

    private void runParallelCategorySearch(List<String> categories, String keywordFilter) {
        ParallelCategorySearchTask task = new ParallelCategorySearchTask(categories);
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            statusLabel.textProperty().unbind();
            setLoading(false);
            sourceLabel.setText("Source: TheMealDB, " + categories.size() + " categories fetched in parallel");
            renderResults(task.getValue(), keywordFilter);
        });
        bindFailureHandler(task);
        executor.submit(task);
    }

    private void bindFailureHandler(Task<List<Recipe>> task) {
        task.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            setLoading(false);
            Throwable ex = task.getException();
            statusLabel.setText("Search failed: " + (ex != null ? ex.getMessage() : "unknown error"));
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        searchButton.setDisable(loading);
    }

    private void renderResults(List<Recipe> results, String keywordFilter) {
        resultsPane.getChildren().clear();

        List<Recipe> filtered = results;
        if (keywordFilter != null) {
            String needle = keywordFilter.toLowerCase();
            filtered = results.stream()
                    .filter(r -> r.getTitle().toLowerCase().contains(needle))
                    .toList();
        }

        if (filtered.isEmpty()) {
            statusLabel.setText(keywordFilter != null
                    ? "No results for \"" + keywordFilter + "\" inside the selected categories."
                    : "No recipes found.");
            return;
        }
        statusLabel.setText(filtered.size() + " recipe(s) found.");

        for (Recipe recipe : filtered) {
            resultsPane.getChildren().add(RecipeCardFactory.create(recipe, () -> onRecipeSelected.accept(recipe)));
        }
    }
}
