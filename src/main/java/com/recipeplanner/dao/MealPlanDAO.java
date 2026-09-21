package com.recipeplanner.dao;

import com.recipeplanner.db.DatabaseManager;
import com.recipeplanner.model.MealPlanEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO for the meal_plans table -- assigning saved recipes to day/meal-type slots. */
public class MealPlanDAO {

    private final Connection conn;

    public MealPlanDAO() {
        this.conn = DatabaseManager.getConnection();
    }

    /** Assigns a recipe to a slot. Overwrites whatever was already in that slot (UNIQUE day+meal_type). */
    public void assign(int recipeId, String dayOfWeek, String mealType) throws SQLException {
        String sql = "INSERT INTO meal_plans (recipe_id, day_of_week, meal_type) VALUES (?,?,?) " +
                     "ON CONFLICT(day_of_week, meal_type) DO UPDATE SET recipe_id = excluded.recipe_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipeId);
            ps.setString(2, dayOfWeek);
            ps.setString(3, mealType);
            ps.executeUpdate();
        }
    }

    public void clearSlot(String dayOfWeek, String mealType) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM meal_plans WHERE day_of_week = ? AND meal_type = ?")) {
            ps.setString(1, dayOfWeek);
            ps.setString(2, mealType);
            ps.executeUpdate();
        }
    }

    /** Full week, joined with recipe titles, for populating the planner grid in one query. */
    public List<MealPlanEntry> getWeek() throws SQLException {
        List<MealPlanEntry> result = new ArrayList<>();
        String sql = "SELECT mp.plan_id, mp.recipe_id, r.title, mp.day_of_week, mp.meal_type " +
                     "FROM meal_plans mp JOIN recipes r ON mp.recipe_id = r.recipe_id";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new MealPlanEntry(
                        rs.getInt("plan_id"),
                        rs.getInt("recipe_id"),
                        rs.getString("title"),
                        rs.getString("day_of_week"),
                        rs.getString("meal_type")));
            }
        }
        return result;
    }

    /**
     * Shopping list for the current week: every ingredient + quantity across all
     * planned recipes, grouped by ingredient name. Quantities are stored as free-text
     * (e.g. "2 cups"), so identical-unit amounts are summed textually where possible;
     * otherwise each distinct quantity is listed separately under the ingredient.
     */
    public List<String> getShoppingList() throws SQLException {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT i.name, ri.quantity " +
                     "FROM meal_plans mp " +
                     "JOIN recipe_ingredients ri ON mp.recipe_id = ri.recipe_id " +
                     "JOIN ingredients i ON ri.ingredient_id = i.ingredient_id " +
                     "ORDER BY i.name";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lines.add(rs.getString(1) + " -- " + rs.getString(2));
            }
        }
        return lines;
    }
}
