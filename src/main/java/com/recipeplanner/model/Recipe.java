package com.recipeplanner.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Core Recipe model. Used both for recipes fetched from TheMealDB
 * (apiId set, source = API) and user-created custom recipes
 * (apiId null, source = CUSTOM).
 */
public class Recipe {

    public enum Source { API, CUSTOM }

    private int recipeId;      // 0 until persisted in SQLite
    private String apiId;      // null for custom recipes
    private String title;
    private String category;
    private String instructions;
    private String imageUrl;
    private boolean favorite;
    private int prepTimeMinutes;
    private int baseServings = 4;
    private int calories;
    private double proteinGrams;
    private double carbsGrams;
    private double fatGrams;
    private Source source;
    private final List<Ingredient> ingredients = new ArrayList<>();

    public Recipe() {
        this.source = Source.CUSTOM;
    }

    public Recipe(String title, String category, String instructions,
                   String imageUrl, int prepTimeMinutes) {
        this.title = title;
        this.category = category;
        this.instructions = instructions;
        this.imageUrl = imageUrl;
        this.prepTimeMinutes = prepTimeMinutes;
        this.source = Source.CUSTOM;
    }

    // ----- getters / setters -----

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
        this.source = (apiId != null && !apiId.isBlank()) ? Source.API : Source.CUSTOM;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public void setPrepTimeMinutes(int prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public Source getSource() {
        return source;
    }

    public int getBaseServings() {
        return baseServings;
    }

    public void setBaseServings(int baseServings) {
        this.baseServings = baseServings <= 0 ? 1 : baseServings;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public double getProteinGrams() {
        return proteinGrams;
    }

    public void setProteinGrams(double proteinGrams) {
        this.proteinGrams = proteinGrams;
    }

    public double getCarbsGrams() {
        return carbsGrams;
    }

    public void setCarbsGrams(double carbsGrams) {
        this.carbsGrams = carbsGrams;
    }

    public double getFatGrams() {
        return fatGrams;
    }

    public void setFatGrams(double fatGrams) {
        this.fatGrams = fatGrams;
    }

    /** True once any nutrition value has actually been entered (vs. all-zero defaults). */
    public boolean hasNutritionData() {
        return calories > 0 || proteinGrams > 0 || carbsGrams > 0 || fatGrams > 0;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void addIngredient(Ingredient ingredient) {
        if (ingredient != null && ingredient.getName() != null && !ingredient.getName().isBlank()) {
            this.ingredients.add(ingredient);
        }
    }

    public void setIngredients(List<Ingredient> newIngredients) {
        this.ingredients.clear();
        if (newIngredients != null) {
            this.ingredients.addAll(newIngredients);
        }
    }

    @Override
    public String toString() {
        return title; // used as the default label in ListView/ComboBox cells
    }
}
