package com.recipeplanner.ui;

import com.recipeplanner.dao.MealPlanDAO;
import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.MealPlanEntry;
import com.recipeplanner.model.Recipe;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weekly meal planner: a GridPane with days of the week as columns and
 * meal slots (Breakfast/Lunch/Dinner/Snack) as rows. Each cell is a
 * ComboBox bound to the user's saved recipe library; picking a recipe
 * writes straight through to the meal_plans table via MealPlanDAO.
 */
public class MealPlannerView extends BorderPane {

    private static final String[] DAYS = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    private static final String[] MEAL_TYPES = {"Breakfast", "Lunch", "Dinner", "Snack"};

    private final MealPlanDAO mealPlanDAO = new MealPlanDAO();
    private final RecipeDAO recipeDAO = new RecipeDAO();

    private final GridPane grid = new GridPane();
    private final Label statusLabel = new Label();
    private final Map<String, ComboBox<Recipe>> cellCombos = new HashMap<>();

    public MealPlannerView() {
        setPadding(new Insets(16));

        Label heading = new Label("Weekly Meal Planner");
        heading.getStyleClass().add("screen-heading");

        Button shoppingListBtn = new Button("Generate Shopping List");
        shoppingListBtn.getStyleClass().add("primary-button");
        shoppingListBtn.setOnAction(e -> showShoppingList());

        HBox top = new HBox(12, heading, shoppingListBtn);
        top.setPadding(new Insets(0, 0, 12, 0));

        grid.setHgap(8);
        grid.setVgap(8);
        buildGridHeaders();

        setTop(new VBox(top, statusLabel));
        setCenter(new ScrollPane(grid));
    }

    private void buildGridHeaders() {
        grid.add(new Label(""), 0, 0);
        for (int col = 0; col < DAYS.length; col++) {
            Label dayLabel = new Label(DAYS[col]);
            dayLabel.getStyleClass().add("card-title");
            grid.add(dayLabel, col + 1, 0);
        }
        for (int row = 0; row < MEAL_TYPES.length; row++) {
            Label mealLabel = new Label(MEAL_TYPES[row]);
            mealLabel.getStyleClass().add("card-title");
            grid.add(mealLabel, 0, row + 1);
        }
    }

    public void refresh() {
        try {
            List<Recipe> savedRecipes = recipeDAO.getAllRecipes();
            List<MealPlanEntry> week = mealPlanDAO.getWeek();

            Map<String, Integer> slotToRecipeId = new HashMap<>();
            for (MealPlanEntry entry : week) {
                slotToRecipeId.put(slotKey(entry.getDayOfWeek(), entry.getMealType()), entry.getRecipeId());
            }

            for (int row = 0; row < MEAL_TYPES.length; row++) {
                for (int col = 0; col < DAYS.length; col++) {
                    String day = DAYS[col];
                    String mealType = MEAL_TYPES[row];
                    ComboBox<Recipe> combo = cellComboFor(day, mealType, savedRecipes);

                    Integer assignedId = slotToRecipeId.get(slotKey(day, mealType));
                    combo.getSelectionModel().clearSelection();
                    if (assignedId != null) {
                        savedRecipes.stream()
                                .filter(r -> r.getRecipeId() == assignedId)
                                .findFirst()
                                .ifPresent(r -> combo.getSelectionModel().select(r));
                    }

                    if (grid.getRowIndex(combo) == null) {
                        grid.add(combo, col + 1, row + 1);
                    }
                }
            }
            statusLabel.setText(savedRecipes.isEmpty()
                    ? "Save some recipes to Favorites first, then assign them here."
                    : "");
        } catch (SQLException e) {
            statusLabel.setText("Could not load meal plan: " + e.getMessage());
        }
    }

    private ComboBox<Recipe> cellComboFor(String day, String mealType, List<Recipe> savedRecipes) {
        String key = slotKey(day, mealType);
        ComboBox<Recipe> combo = cellCombos.get(key);
        if (combo == null) {
            combo = new ComboBox<>();
            combo.setPromptText("-- empty --");
            combo.setPrefWidth(140);
            cellCombos.put(key, combo);

            ComboBox<Recipe> finalCombo = combo;
            combo.setOnAction(e -> assignSlot(day, mealType, finalCombo.getValue()));
        }
        combo.getItems().setAll(savedRecipes);
        return combo;
    }

    private void assignSlot(String day, String mealType, Recipe recipe) {
        try {
            if (recipe == null) {
                mealPlanDAO.clearSlot(day, mealType);
            } else {
                mealPlanDAO.assign(recipe.getRecipeId(), day, mealType);
            }
        } catch (SQLException e) {
            statusLabel.setText("Could not save meal plan slot: " + e.getMessage());
        }
    }

    private void showShoppingList() {
        try {
            List<String> lines = mealPlanDAO.getShoppingList();
            TextArea area = new TextArea(String.join("\n", lines));
            area.setEditable(false);
            area.setPrefSize(360, 420);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Shopping List");
            alert.setHeaderText(lines.isEmpty() ? "No meals planned yet" : "Ingredients for this week's plan");
            alert.getDialogPane().setContent(area);
            alert.showAndWait();
        } catch (SQLException e) {
            statusLabel.setText("Could not build shopping list: " + e.getMessage());
        }
    }

    private static String slotKey(String day, String mealType) {
        return day + "|" + mealType;
    }
}
