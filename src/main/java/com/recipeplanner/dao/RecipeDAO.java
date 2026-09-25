package com.recipeplanner.dao;

import com.recipeplanner.db.DatabaseManager;
import com.recipeplanner.model.Ingredient;
import com.recipeplanner.model.Recipe;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the recipes / ingredients / recipe_ingredients tables.
 * Every public method opens its own PreparedStatement against the shared
 * connection; callers are expected to invoke these off the JavaFX
 * Application Thread (see network.FetchRecipesTask / ui controllers using Task<T>).
 */
public class RecipeDAO {

    private final Connection conn;

    public RecipeDAO() {
        this.conn = DatabaseManager.getConnection();
    }

    // ---------------------------------------------------------------
    // Create / update a recipe (used both for saving API results to
    // favorites and for the custom recipe creator).
    // ---------------------------------------------------------------

    /**
     * Inserts a recipe and its ingredients. If this Recipe object already
     * has a database id (recipeId > 0 -- i.e. it was loaded from the DB,
     * such as when editing an existing custom recipe), that row is
     * updated directly. Otherwise, for API-sourced recipes with no known
     * database id yet, falls back to looking the row up by api_id so
     * favoriting the same TheMealDB recipe twice doesn't create a
     * duplicate. Without the recipeId check first, editing a custom
     * recipe (which has no api_id at all) always fell through to INSERT
     * and created a new copy on every save.
     */
    public int saveRecipe(Recipe recipe) throws SQLException {
        Integer existingId = recipe.getRecipeId() > 0
                ? Integer.valueOf(recipe.getRecipeId())
                : (recipe.getApiId() != null ? findIdByApiId(recipe.getApiId()) : null);

        String sql = (existingId != null)
                ? "UPDATE recipes SET title=?, category=?, instructions=?, image_url=?, " +
                  "is_favorite=?, prep_time_minutes=?, base_servings=?, calories=?, " +
                  "protein_g=?, carbs_g=?, fat_g=? WHERE recipe_id=?"
                : "INSERT INTO recipes (api_id, title, category, instructions, image_url, " +
                  "is_favorite, prep_time_minutes, base_servings, calories, protein_g, carbs_g, fat_g) " +
                  "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

        int generatedId;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (existingId != null) {
                ps.setString(1, recipe.getTitle());
                ps.setString(2, recipe.getCategory());
                ps.setString(3, recipe.getInstructions());
                ps.setString(4, recipe.getImageUrl());
                ps.setInt(5, recipe.isFavorite() ? 1 : 0);
                ps.setInt(6, recipe.getPrepTimeMinutes());
                ps.setInt(7, recipe.getBaseServings());
                ps.setInt(8, recipe.getCalories());
                ps.setDouble(9, recipe.getProteinGrams());
                ps.setDouble(10, recipe.getCarbsGrams());
                ps.setDouble(11, recipe.getFatGrams());
                ps.setInt(12, existingId);
                ps.executeUpdate();
                generatedId = existingId;
            } else {
                ps.setString(1, recipe.getApiId());
                ps.setString(2, recipe.getTitle());
                ps.setString(3, recipe.getCategory());
                ps.setString(4, recipe.getInstructions());
                ps.setString(5, recipe.getImageUrl());
                ps.setInt(6, recipe.isFavorite() ? 1 : 0);
                ps.setInt(7, recipe.getPrepTimeMinutes());
                ps.setInt(8, recipe.getBaseServings());
                ps.setInt(9, recipe.getCalories());
                ps.setDouble(10, recipe.getProteinGrams());
                ps.setDouble(11, recipe.getCarbsGrams());
                ps.setDouble(12, recipe.getFatGrams());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    generatedId = keys.getInt(1);
                }
            }
        }

        recipe.setRecipeId(generatedId);
        replaceIngredients(generatedId, recipe.getIngredients());
        return generatedId;
    }

    private void replaceIngredients(int recipeId, List<Ingredient> ingredients) throws SQLException {
        try (PreparedStatement clear = conn.prepareStatement(
                "DELETE FROM recipe_ingredients WHERE recipe_id = ?")) {
            clear.setInt(1, recipeId);
            clear.executeUpdate();
        }

        for (Ingredient ing : ingredients) {
            int ingredientId = findOrCreateIngredient(ing.getName());
            try (PreparedStatement link = conn.prepareStatement(
                    "INSERT OR IGNORE INTO recipe_ingredients (recipe_id, ingredient_id, quantity) VALUES (?,?,?)")) {
                link.setInt(1, recipeId);
                link.setInt(2, ingredientId);
                link.setString(3, ing.getQuantity());
                link.executeUpdate();
            }
        }
    }

    /** Normalizes the name (trim/lowercase) before matching, to keep the ingredients table deduplicated. */
    private int findOrCreateIngredient(String rawName) throws SQLException {
        String normalized = rawName.trim();
        try (PreparedStatement find = conn.prepareStatement(
                "SELECT ingredient_id FROM ingredients WHERE LOWER(name) = LOWER(?)")) {
            find.setString(1, normalized);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO ingredients (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, normalized);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private Integer findIdByApiId(String apiId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT recipe_id FROM recipes WHERE api_id = ?")) {
            ps.setString(1, apiId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    public List<Recipe> getFavorites() throws SQLException {
        List<Recipe> result = new ArrayList<>();
        String sql = "SELECT * FROM recipes WHERE is_favorite = 1 ORDER BY title";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRecipeRow(rs, true));
            }
        }
        return result;
    }

    public List<Recipe> getAllRecipes() throws SQLException {
        List<Recipe> result = new ArrayList<>();
        String sql = "SELECT * FROM recipes ORDER BY created_at DESC";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRecipeRow(rs, true));
            }
        }
        return result;
    }

    public Recipe getRecipeById(int recipeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM recipes WHERE recipe_id = ?")) {
            ps.setInt(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRecipeRow(rs, true) : null;
            }
        }
    }

    /** Local search across previously-saved recipes (favorites + custom), by title substring. */
    public List<Recipe> searchLocal(String keyword) throws SQLException {
        List<Recipe> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM recipes WHERE title LIKE ? ORDER BY title")) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRecipeRow(rs, true));
                }
            }
        }
        return result;
    }

    private Recipe mapRecipeRow(ResultSet rs, boolean loadIngredients) throws SQLException {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(rs.getInt("recipe_id"));
        recipe.setApiId(rs.getString("api_id"));
        recipe.setTitle(rs.getString("title"));
        recipe.setCategory(rs.getString("category"));
        recipe.setInstructions(rs.getString("instructions"));
        recipe.setImageUrl(rs.getString("image_url"));
        recipe.setFavorite(rs.getInt("is_favorite") == 1);
        recipe.setPrepTimeMinutes(rs.getInt("prep_time_minutes"));
        recipe.setBaseServings(rs.getInt("base_servings"));
        recipe.setCalories(rs.getInt("calories"));
        recipe.setProteinGrams(rs.getDouble("protein_g"));
        recipe.setCarbsGrams(rs.getDouble("carbs_g"));
        recipe.setFatGrams(rs.getDouble("fat_g"));
        if (loadIngredients) {
            recipe.setIngredients(getIngredientsForRecipe(recipe.getRecipeId()));
        }
        return recipe;
    }

    private List<Ingredient> getIngredientsForRecipe(int recipeId) throws SQLException {
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT i.ingredient_id, i.name, ri.quantity " +
                     "FROM recipe_ingredients ri JOIN ingredients i ON ri.ingredient_id = i.ingredient_id " +
                     "WHERE ri.recipe_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Ingredient(rs.getInt(1), rs.getString(2), rs.getString(3)));
                }
            }
        }
        return list;
    }

    // ---------------------------------------------------------------
    // Update / delete
    // ---------------------------------------------------------------

    public void setFavorite(int recipeId, boolean favorite) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE recipes SET is_favorite = ? WHERE recipe_id = ?")) {
            ps.setInt(1, favorite ? 1 : 0);
            ps.setInt(2, recipeId);
            ps.executeUpdate();
        }
    }

    public void deleteRecipe(int recipeId) throws SQLException {
        // ON DELETE CASCADE handles recipe_ingredients and meal_plans rows.
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recipes WHERE recipe_id = ?")) {
            ps.setInt(1, recipeId);
            ps.executeUpdate();
        }
    }
}
