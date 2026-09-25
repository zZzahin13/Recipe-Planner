package com.recipeplanner.network;

import com.recipeplanner.model.Recipe;
import javafx.concurrent.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Searches several TheMealDB categories at once (e.g. "Vegetarian" +
 * "Dessert" + "Seafood" all in parallel) instead of one at a time, using
 * an ExecutorService to run the HTTP calls concurrently and merging the
 * results once every category has responded. Still just one Task from
 * the JavaFX side -- the fan-out/fan-in happens inside call().
 */
public class ParallelCategorySearchTask extends Task<List<Recipe>> {

    private final List<String> categories;
    private final MealDbApiService api = new MealDbApiService();

    public ParallelCategorySearchTask(List<String> categories) {
        this.categories = categories;
    }

    @Override
    protected List<Recipe> call() throws Exception {
        if (categories.isEmpty()) {
            return List.of();
        }

        updateMessage("Searching " + categories.size() + " categories at once...");

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(categories.size(), 6), r -> {
            Thread t = new Thread(r, "category-search-worker");
            t.setDaemon(true);
            return t;
        });

        try {
            Map<String, Future<List<Recipe>>> futures = new LinkedHashMap<>();
            for (String category : categories) {
                futures.put(category, pool.submit(() -> api.searchByCategory(category)));
            }

            // De-duplicate by apiId in case the same recipe appears under multiple categories.
            Map<String, Recipe> merged = new LinkedHashMap<>();
            Map<String, String> failures = new LinkedHashMap<>();
            for (Map.Entry<String, Future<List<Recipe>>> entry : futures.entrySet()) {
                try {
                    for (Recipe recipe : entry.getValue().get(30, TimeUnit.SECONDS)) {
                        merged.putIfAbsent(recipe.getApiId(), recipe);
                    }
                } catch (ExecutionException | TimeoutException e) {
                    // One category failing (timeout, bad response) shouldn't sink the whole
                    // search if others succeeded -- but silently returning an empty result
                    // when EVERY category fails just looks like "no recipes found", which is
                    // misleading. Track failures and only surface them if nothing came back.
                    Throwable cause = (e instanceof ExecutionException) ? e.getCause() : e;
                    failures.put(entry.getKey(), cause != null ? cause.getMessage() : e.getMessage());
                }
            }

            if (merged.isEmpty() && !failures.isEmpty()) {
                String detail = failures.entrySet().stream()
                        .map(f -> f.getKey() + ": " + f.getValue())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("unknown error");
                throw new IOException("All category searches failed -- " + detail);
            }

            updateMessage("Found " + merged.size() + " recipe(s) across " + categories.size() + " categories"
                    + (failures.isEmpty() ? "" : " (" + failures.size() + " category/categories failed)"));
            return new ArrayList<>(merged.values());
        } finally {
            pool.shutdown();
        }
    }
}
