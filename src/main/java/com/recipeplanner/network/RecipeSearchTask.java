package com.recipeplanner.network;

import com.recipeplanner.model.Recipe;
import javafx.concurrent.Task;

import java.util.List;

public abstract class RecipeSearchTask extends Task<List<Recipe>> {

    protected final MealDbApiService api = new MealDbApiService();

    /** Reports how many recipes were found, phrased consistently across every search type. */
    protected void reportFound(List<Recipe> results, String sourceDescription) {
        updateMessage("Found " + results.size() + " recipe(s) " + sourceDescription);
    }
}