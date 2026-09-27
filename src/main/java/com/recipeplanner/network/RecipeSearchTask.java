package com.recipeplanner.network;

import com.recipeplanner.model.Recipe;
import javafx.concurrent.Task;

import java.util.List;

/**
 * Common base for every background recipe search in this app. All three
 * concrete searches (plain TheMealDB search, local-first keyword search,
 * and parallel multi-category search) share two things: a MealDbApiService
 * to talk to the network, and a consistent "Found N recipe(s)..." status
 * message format shown to the user via Task's message property. Pulling
 * these into one abstract parent means every subclass reports progress
 * the same way, instead of each one formatting its own status string.
 *
 * Each subclass still implements call() itself, since HOW they search
 * (one request, local-then-remote fallback, or fan-out/fan-in across
 * several categories) is genuinely different between them -- there's
 * no honest way to share that part without forcing an unnatural shape
 * onto all three.
 */
public abstract class RecipeSearchTask extends Task<List<Recipe>> {

    protected final MealDbApiService api = new MealDbApiService();

    /** Reports how many recipes were found, phrased consistently across every search type. */
    protected void reportFound(List<Recipe> results, String sourceDescription) {
        updateMessage("Found " + results.size() + " recipe(s) " + sourceDescription);
    }
}