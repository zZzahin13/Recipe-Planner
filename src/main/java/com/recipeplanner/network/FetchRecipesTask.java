package com.recipeplanner.network;

import com.recipeplanner.model.Recipe;
import javafx.concurrent.Task;

import java.util.List;

/**
 * Wraps a MealDbApiService call in a JavaFX Task so the network I/O runs
 * on a background thread (via an ExecutorService/Thread started by the
 * caller) and only the final result touches the UI, through the Task's
 * built-in onSucceeded/onFailed callbacks which JavaFX guarantees run
 * back on the Application Thread. Controllers should never touch
 * JavaFX controls from inside call() -- only from onSucceeded/onFailed.
 */
public class FetchRecipesTask extends Task<List<Recipe>> {

    public enum Mode { NAME, CATEGORY, INGREDIENT }

    private final MealDbApiService api = new MealDbApiService();
    private final Mode mode;
    private final String query;

    public FetchRecipesTask(Mode mode, String query) {
        this.mode = mode;
        this.query = query;
    }

    @Override
    protected List<Recipe> call() throws Exception {
        updateMessage("Contacting TheMealDB...");
        List<Recipe> results = switch (mode) {
            case NAME -> api.searchByName(query);
            case CATEGORY -> api.searchByCategory(query);
            case INGREDIENT -> api.searchByIngredient(query);
        };
        updateMessage("Found " + results.size() + " recipe(s)");
        return results;
    }
}
