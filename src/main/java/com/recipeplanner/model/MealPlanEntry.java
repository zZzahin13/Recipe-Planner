package com.recipeplanner.model;

/**
 * Represents one assignment of a recipe to a day-of-week + meal slot.
 * Carries the recipe's title/id (denormalized via JOIN) so the JavaFX
 * TableView / GridPane cells don't need a second lookup per row.
 */
public class MealPlanEntry {

    private int planId;
    private int recipeId;
    private String recipeTitle;
    private String dayOfWeek;
    private String mealType;

    public MealPlanEntry(int planId, int recipeId, String recipeTitle,
                          String dayOfWeek, String mealType) {
        this.planId = planId;
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.dayOfWeek = dayOfWeek;
        this.mealType = mealType;
    }

    public int getPlanId() {
        return planId;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public String getRecipeTitle() {
        return recipeTitle;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public String getMealType() {
        return mealType;
    }
}
