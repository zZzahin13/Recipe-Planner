package com.recipeplanner.model;

import java.util.List;

/** Combined result of one USDA FoodData Central lookup: macros + micronutrients, from a single API call. */
public class NutritionLookupResult {
    private final int calories;
    private final double proteinGrams;
    private final double carbsGrams;
    private final double fatGrams;
    private final List<Micronutrient> micronutrients;

    public NutritionLookupResult(int calories, double proteinGrams, double carbsGrams,
                                 double fatGrams, List<Micronutrient> micronutrients) {
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
        this.micronutrients = micronutrients;
    }

    public int getCalories() { return calories; }
    public double getProteinGrams() { return proteinGrams; }
    public double getCarbsGrams() { return carbsGrams; }
    public double getFatGrams() { return fatGrams; }
    public List<Micronutrient> getMicronutrients() { return micronutrients; }

    public boolean hasAnyMacros() {
        return calories > 0 || proteinGrams > 0 || carbsGrams > 0 || fatGrams > 0;
    }
}