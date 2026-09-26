package com.recipeplanner.network;

import com.recipeplanner.model.Micronutrient;
import org.json.JSONArray;
import org.json.JSONObject;
import com.recipeplanner.model.NutritionLookupResult;
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
 * Thin client for USDA FoodData Central. TheMealDB has no nutrition data
 * at all, so micronutrients come from this separate, free government API
 * instead. Looks up the best-matching food by name and returns a curated
 * set of micronutrients from it -- this is a per-100g generic-food match,
 * not a true per-ingredient recipe calculation, so treat the numbers as
 * an estimate, not an exact figure for the dish as written.
 */
public class NutritionApiService {

    private static final String BASE_URL = "https://api.nal.usda.gov/fdc/v1";

    // Get a free key at https://fdc.nal.usda.gov/api-key-signup.html
    // DEMO_KEY works with no signup but is rate-limited (~30 requests/hour).
    private static final String API_KEY = "o7Z4Dzt0cRW6b4OPCqtxdor0cb4TKTE2TJQKogrt";

    // Curated list -- FDC returns 50+ nutrients per food; these are the
    // ones worth showing a user. Matched by substring against the API's
    // own nutrient names, which are verbose (e.g. "Vitamin C, total ascorbic acid").
    private static final String[] TRACKED_NUTRIENTS = {
            "Vitamin C", "Vitamin A", "Vitamin D", "Calcium", "Iron",
            "Potassium", "Sodium", "Magnesium", "Zinc"
    };

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Looks up micronutrients for a food by name (e.g. a recipe title).
     * Returns an empty list if nothing usable was found -- callers should
     * treat that as "no data available", not an error.
     */
    public NutritionLookupResult lookup(String foodName) throws IOException, InterruptedException {
        String url = BASE_URL + "/foods/search?query=" + encode(foodName)
                + "&pageSize=1&api_key=" + API_KEY;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("USDA API returned HTTP " + response.statusCode());
        }

        JSONObject root = new JSONObject(response.body());
        JSONArray foods = root.optJSONArray("foods");
        if (foods == null || foods.isEmpty()) {
            return new NutritionLookupResult(0, 0, 0, 0, List.of());
        }

        JSONObject bestMatch = foods.getJSONObject(0);
        JSONArray nutrients = bestMatch.optJSONArray("foodNutrients");
        if (nutrients == null) {
            return new NutritionLookupResult(0, 0, 0, 0, List.of());
        }

        List<Micronutrient> micronutrients = new ArrayList<>();
        int calories = 0;
        double protein = 0, carbs = 0, fat = 0;

        for (int i = 0; i < nutrients.length(); i++) {
            JSONObject n = nutrients.getJSONObject(i);
            String name = n.optString("nutrientName", "");
            double amount = n.optDouble("amount", n.optDouble("value", 0));
            String unit = n.optString("unitName", "");

            if (name.equals("Energy") && unit.equalsIgnoreCase("kcal")) {
                calories = (int) Math.round(amount);
            } else if (name.equals("Protein")) {
                protein = amount;
            } else if (name.contains("Carbohydrate")) {
                carbs = amount;
            } else if (name.equals("Total lipid (fat)")) {
                fat = amount;
            }

            for (String tracked : TRACKED_NUTRIENTS) {
                if (name.contains(tracked)) {
                    micronutrients.add(new Micronutrient(tracked, amount, unit));
                    break;
                }
            }
        }

        return new NutritionLookupResult(calories, protein, carbs, fat, micronutrients);
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}