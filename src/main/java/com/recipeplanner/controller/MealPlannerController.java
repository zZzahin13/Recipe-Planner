package com.recipeplanner.controller;

import com.recipeplanner.dao.MealPlanDAO;
import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.MealPlanEntry;
import com.recipeplanner.model.Recipe;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controller for meal_planner.fxml -- the day x meal-slot grid and shopping list generator. */
public class MealPlannerController implements MainAware {

    private static final String[] DAYS = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    private static final String[] MEAL_TYPES = {"Breakfast", "Lunch", "Dinner", "Snack"};

    private final MealPlanDAO mealPlanDAO = new MealPlanDAO();
    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final Map<String, ComboBox<Recipe>> cellCombos = new HashMap<>();

    private MainController mainController;

    @FXML private GridPane grid;
    @FXML private Label statusLabel;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        buildGridHeaders();
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
            combo.setConverter(new javafx.util.StringConverter<Recipe>() {
                @Override
                public String toString(Recipe recipe) {
                    return recipe == null ? "-- empty --" : recipe.getTitle();
                }
                @Override
                public Recipe fromString(String string) {
                    return null; // not used -- combo isn't editable
                }
            });
            cellCombos.put(key, combo);

            ComboBox<Recipe> finalCombo = combo;
            combo.setOnAction(e -> assignSlot(day, mealType, finalCombo.getValue()));
        }
        combo.getItems().setAll(savedRecipes);
        combo.getItems().add(0, null); // selectable "clear this slot" option
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

    @FXML
    private void onShoppingList() {
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
