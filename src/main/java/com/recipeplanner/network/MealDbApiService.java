package com.recipeplanner.network;

import com.recipeplanner.model.Ingredient;
import com.recipeplanner.model.Recipe;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin client for TheMealDB's free public API (no key required for the
 * shared test key "1"). Handles the HTTP GET + JSON parsing described
 * in the Week 7 lab: raw JSON -> org.json -> Recipe/Ingredient POJOs.
 *
 * All methods here are blocking network calls -- callers must run them
 * off the JavaFX Application Thread (see FetchRecipesTask).
 */
public class MealDbApiService {

    private static final String BASE_URL = "https://www.themealdb.com/api/json/v1/1";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Search recipes by name/keyword. */
    public List<Recipe> searchByName(String keyword) throws IOException, InterruptedException {
        String url = BASE_URL + "/search.php?s=" + encode(keyword);
        return parseMealsResponse(fetch(url));
    }

    /** Search recipes by category (e.g. "Seafood", "Dessert", "Vegetarian"). */
//    public List<Recipe> searchByCategory(String category) throws IOException, InterruptedException {
//        // filter.php only returns id/title/thumbnail, so we fetch full details per result.
//        String url = BASE_URL + "/filter.php?c=" + encode(category);
//        String json = fetch(url);
//        JSONObject root = new JSONObject(json);
//        JSONArray meals = root.optJSONArray("meals");
//        List<Recipe> results = new ArrayList<>();
//        if (meals == null) {
//            return results;
//        }
//        for (int i = 0; i < meals.length(); i++) {
//            String id = meals.getJSONObject(i).optString("idMeal", null);
//            if (id != null) {
//                Recipe full = getById(id);
//                if (full != null) {
//                    results.add(full);
//                }
//            }
//        }
//        return results;
//    }
    public List<Recipe> searchByCategory(String category) throws IOException, InterruptedException {
        // One fast request: filter.php returns id + title + thumbnail only.
        // Full details are loaded later, when the user clicks a card.
        String url = BASE_URL + "/filter.php?c=" + encode(category);
        JSONObject root = new JSONObject(fetch(url));
        JSONArray meals = root.optJSONArray("meals");
        List<Recipe> results = new ArrayList<>();
        if (meals == null) {
            return results;
        }
        for (int i = 0; i < meals.length(); i++) {
            JSONObject m = meals.getJSONObject(i);
            Recipe r = new Recipe();
            r.setApiId(m.optString("idMeal", null));
            r.setTitle(m.optString("strMeal", "Untitled"));
            r.setImageUrl(m.optString("strMealThumb", null));
            r.setCategory(category);
            r.setInstructions("");
            results.add(r);
        }
        return results;
    }

    /** Search recipes containing a specific main ingredient. */
    public List<Recipe> searchByIngredient(String ingredient) throws IOException, InterruptedException {
        String url = BASE_URL + "/filter.php?i=" + encode(ingredient);
        String json = fetch(url);
        JSONObject root = new JSONObject(json);
        JSONArray meals = root.optJSONArray("meals");
        List<Recipe> results = new ArrayList<>();
        if (meals == null) {
            return results;
        }
        for (int i = 0; i < meals.length(); i++) {
            String id = meals.getJSONObject(i).optString("idMeal", null);
            if (id != null) {
                Recipe full = getById(id);
                if (full != null) {
                    results.add(full);
                }
            }
        }
        return results;
    }

    /** Fetch a single recipe's full detail (ingredients + instructions) by TheMealDB id. */
    public Recipe getById(String mealId) throws IOException, InterruptedException {
        String url = BASE_URL + "/lookup.php?i=" + encode(mealId);
        List<Recipe> parsed = parseMealsResponse(fetch(url));
        return parsed.isEmpty() ? null : parsed.get(0);
    }

    // ---------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------

    private String fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("TheMealDB request failed, HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private List<Recipe> parseMealsResponse(String json) {
        List<Recipe> recipes = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray meals = root.optJSONArray("meals");
        if (meals == null) {
            return recipes; // no results -- TheMealDB returns {"meals": null}
        }
        for (int i = 0; i < meals.length(); i++) {
            recipes.add(parseSingleMeal(meals.getJSONObject(i)));
        }
        return recipes;
    }

    private Recipe parseSingleMeal(JSONObject meal) {
        Recipe recipe = new Recipe();
        recipe.setApiId(meal.optString("idMeal", null));
        recipe.setTitle(meal.optString("strMeal", "Untitled"));
        recipe.setCategory(meal.optString("strCategory", null));
        recipe.setInstructions(meal.optString("strInstructions", ""));
        recipe.setImageUrl(meal.optString("strMealThumb", null));
        recipe.setPrepTimeMinutes(0); // TheMealDB doesn't provide prep time; user can edit after saving

        // TheMealDB has no ingredients array -- it flattens them into
        // strIngredient1..20 / strMeasure1..20 fields. Loop and skip blanks.
        for (int i = 1; i <= 20; i++) {
            String name = meal.optString("strIngredient" + i, "").trim();
            String measure = meal.optString("strMeasure" + i, "").trim();
            if (!name.isEmpty()) {
                recipe.addIngredient(new Ingredient(name, measure));
            }
        }
        return recipe;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
