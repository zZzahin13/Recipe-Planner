package com.recipeplanner.model;

/** One nutrient value returned by the USDA FoodData Central API. */
public class Micronutrient {
    private final String name;
    private final double amount;
    private final String unit;

    public Micronutrient(String name, double amount, String unit) {
        this.name = name;
        this.amount = amount;
        this.unit = unit;
    }

    public String getName() { return name; }
    public double getAmount() { return amount; }
    public String getUnit() { return unit; }

    @Override
    public String toString() {
        return String.format("%s: %.1f %s", name, amount, unit);
    }
}