package com.recipeplanner.model;

/**
 * Represents an ingredient, optionally with the quantity used in a
 * specific recipe (quantity is only meaningful when this object is
 * attached to a Recipe's ingredient list).
 */
public class Ingredient {

    private int ingredientId; // 0 if not yet persisted
    private String name;
    private String quantity;  // e.g. "2 cups" -- context-specific to a recipe

    public Ingredient(String name, String quantity) {
        this.name = name == null ? "" : name.trim();
        this.quantity = quantity == null ? "" : quantity.trim();
    }

    public Ingredient(int ingredientId, String name, String quantity) {
        this(name, quantity);
        this.ingredientId = ingredientId;
    }

    public int getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(int ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** Lowercased, trimmed form used for de-duplication lookups against the DB. */
    public String getNormalizedName() {
        return name == null ? "" : name.trim().toLowerCase();
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        if (quantity == null || quantity.isBlank()) {
            return name;
        }
        return quantity + " " + name;
    }
}
