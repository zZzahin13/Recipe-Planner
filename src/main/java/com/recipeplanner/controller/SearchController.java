package com.recipeplanner.controller;

import com.recipeplanner.model.Recipe;
import com.recipeplanner.network.MealDbApiService;
import com.recipeplanner.network.MultiSourceSearchTask;
import com.recipeplanner.network.ParallelCategorySearchTask;
import com.recipeplanner.ui.RecipeCardFactory;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for search.fxml. Two ways to search, both off the UI thread:
 * keyword-only uses MultiSourceSearchTask (local library first, TheMealDB
 * fallback); one or more categories selected uses ParallelCategorySearchTask
 * (all category requests fired at once via an ExecutorService).
 */
public class SearchController implements MainAware {

    private static final String[] CATEGORIES = {
            "Beef", "Chicken", "Dessert", "Lamb", "Pasta",
            "Pork", "Seafood", "Side", "Starter", "Vegan", "Vegetarian", "Breakfast"
    };

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "recipe-search-worker");
        t.setDaemon(true);
        return t;
    });
    private final MealDbApiService api = new MealDbApiService();
    private MainController mainController;

    @FXML private TextField keywordField;
    @FXML private ListView<String> categoryList;
    @FXML private Button searchButton;
    @FXML private ProgressIndicator progress;
    @FXML private Label statusLabel;
    @FXML private Label sourceLabel;
    @FXML private TilePane resultsPane;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        categoryList.getItems().addAll(CATEGORIES);
        categoryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    private void onSearch() {
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

    @FXML
    private void onClearCategories() {
        categoryList.getSelectionModel().clearSelection();
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
            resultsPane.getChildren().add(RecipeCardFactory.create(recipe, () -> openRecipe(recipe)));
        }
    }

    /**
     * Category-search results are lightweight "cards" (title/image/id only
     * -- see MealDbApiService.searchByCategory). If the clicked recipe has
     * no ingredients loaded yet, fetch full detail from TheMealDB first,
     * off the UI thread, before navigating to the detail screen.
     */
    private void openRecipe(Recipe card) {
        boolean needsFullDetail = card.getIngredients().isEmpty() && card.getApiId() != null;
        if (!needsFullDetail) {
            mainController.showDetail(card);
            return;
        }

        statusLabel.setText("Loading \"" + card.getTitle() + "\"...");
        Task<Recipe> loadTask = new Task<>() {
            @Override
            protected Recipe call() throws Exception {
                return api.getById(card.getApiId());
            }
        };
        loadTask.setOnSucceeded(e -> {
            Recipe full = loadTask.getValue();
            statusLabel.setText("");
            mainController.showDetail(full != null ? full : card);
        });
        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            statusLabel.setText("Could not load full recipe: " + (ex != null ? ex.getMessage() : "unknown error"));
        });
        executor.submit(loadTask);
    }
}
