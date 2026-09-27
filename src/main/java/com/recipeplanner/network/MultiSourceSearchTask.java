package com.recipeplanner.network;

import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.Recipe;
import javafx.concurrent.Task;

import java.util.List;

/**
 * Local-first, web-fallback search: checks the user's saved recipes
 * (fast SQLite query) before ever hitting the network. Only calls
 * TheMealDB if nothing local matches the keyword. Both the DB read and
 * the HTTP call happen inside call(), off the JavaFX Application
 * Thread, same as the plain FetchRecipesTask.
 */
public class MultiSourceSearchTask extends RecipeSearchTask {

    public enum SourceUsed { LOCAL, API }

    private final String keyword;
    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final MealDbApiService api = new MealDbApiService();

    private SourceUsed sourceUsed;

    public MultiSourceSearchTask(String keyword) {
        this.keyword = keyword;
    }

    /** Which source the result actually came from -- only meaningful after the task succeeds. */
    public SourceUsed getSourceUsed() {
        return sourceUsed;
    }

    @Override
    protected List<Recipe> call() throws Exception {
        updateMessage("Checking your library...");
        List<Recipe> local = recipeDAO.searchLocal(keyword);
        if (!local.isEmpty()) {
            sourceUsed = SourceUsed.LOCAL;
            reportFound(local, "in your library");
            //updateMessage("Found " + local.size() + " recipe(s) in your library");
            return local;
        }

        updateMessage("Not in your library -- checking TheMealDB...");
        List<Recipe> remote = api.searchByName(keyword);
        sourceUsed = SourceUsed.API;
        reportFound(remote, "on TheMealDB");
        //updateMessage("Found " + remote.size() + " recipe(s) on TheMealDB");
        return remote;
    }
}
