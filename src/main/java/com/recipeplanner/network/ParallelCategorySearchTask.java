package com.recipeplanner.network;

import com.recipeplanner.model.Recipe;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.io.IOException;
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
            List<String> failed = new ArrayList<>();
            String lastError = "";
            for (Map.Entry<String, Future<List<Recipe>>> entry : futures.entrySet()) {
                try {
                    for (Recipe recipe : entry.getValue().get(30, TimeUnit.SECONDS)) {
                        merged.putIfAbsent(recipe.getApiId(), recipe);
                    }
                } catch (ExecutionException e) {
                    failed.add(entry.getKey());
                    lastError = String.valueOf(e.getCause());
                } catch (TimeoutException e) {
                    failed.add(entry.getKey());
                    lastError = "timed out";
                }
            }
            if (merged.isEmpty() && !failed.isEmpty()) {
                throw new IOException("Could not load " + String.join(", ", failed) + ": " + lastError);
            }

            updateMessage("Found " + merged.size() + " recipe(s) across " + categories.size() + " categories");
            return new ArrayList<>(merged.values());
        } finally {
            pool.shutdown();
        }
    }
}
