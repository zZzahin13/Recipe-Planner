package com.recipeplanner.ui;

import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.Ingredient;
import com.recipeplanner.model.Recipe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * "My Recipes" screen: lets the user manually create, edit, and delete
 * their own recipes (no API involved). Each ingredient row is a pair of
 * text fields (name, quantity) added/removed dynamically.
 */
public class CustomRecipeView extends BorderPane {

    private final RecipeDAO recipeDAO = new RecipeDAO();

    private final ListView<Recipe> myRecipesList = new ListView<>();
    private final ObservableList<Recipe> myRecipes = FXCollections.observableArrayList();

    private final TextField titleField = new TextField();
    private final TextField categoryField = new TextField();
    private final Spinner<Integer> prepTimeSpinner = new Spinner<>(0, 600, 15);
    private final Spinner<Integer> baseServingsSpinner = new Spinner<>(1, 50, 4);
    private final Spinner<Integer> caloriesSpinner = new Spinner<>(0, 5000, 0);
    private final Spinner<Double> proteinSpinner = new Spinner<>(0, 500, 0, 1);
    private final Spinner<Double> carbsSpinner = new Spinner<>(0, 500, 0, 1);
    private final Spinner<Double> fatSpinner = new Spinner<>(0, 500, 0, 1);
    private final TextArea instructionsArea = new TextArea();
    private final VBox ingredientRows = new VBox(6);
    private final Label statusLabel = new Label();

    private Recipe editingRecipe; // null while creating a new recipe
    private final Runnable onSaved;

    public CustomRecipeView(Runnable onSaved) {
        this.onSaved = onSaved;
        setPadding(new Insets(16));
        setLeft(buildListPanel());
        setCenter(buildFormPanel());
        loadMyRecipes();
        resetForm();
    }

    // ---------------------------------------------------------------
    // Left: list of the user's own recipes
    // ---------------------------------------------------------------

    private VBox buildListPanel() {
        Label heading = new Label("My Custom Recipes");
        heading.getStyleClass().add("card-title");

        myRecipesList.setItems(myRecipes);
        myRecipesList.setPrefWidth(220);
        myRecipesList.setPrefHeight(500);
        myRecipesList.getSelectionModel().selectedItemProperty().addListener((obs, oldR, newR) -> {
            if (newR != null) {
                loadIntoForm(newR);
            }
        });

        Button newBtn = new Button("+ New Recipe");
        newBtn.setMaxWidth(Double.MAX_VALUE);
        newBtn.setOnAction(e -> {
            myRecipesList.getSelectionModel().clearSelection();
            resetForm();
        });

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> deleteSelected());

        VBox box = new VBox(8, heading, myRecipesList, newBtn, deleteBtn);
        box.setPadding(new Insets(0, 16, 0, 0));
        return box;
    }

    private void loadMyRecipes() {
        try {
            List<Recipe> all = recipeDAO.getAllRecipes();
            myRecipes.setAll(all.stream().filter(r -> r.getSource() == Recipe.Source.CUSTOM).toList());
        } catch (SQLException e) {
            statusLabel.setText("Could not load your recipes: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        Recipe selected = myRecipesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            recipeDAO.deleteRecipe(selected.getRecipeId());
            loadMyRecipes();
            resetForm();
        } catch (SQLException e) {
            statusLabel.setText("Could not delete: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Center: create/edit form
    // ---------------------------------------------------------------

    private ScrollPane buildFormPanel() {
        prepTimeSpinner.setEditable(true);
        baseServingsSpinner.setEditable(true);
        caloriesSpinner.setEditable(true);
        proteinSpinner.setEditable(true);
        carbsSpinner.setEditable(true);
        fatSpinner.setEditable(true);
        instructionsArea.setWrapText(true);
        instructionsArea.setPrefHeight(160);

        Button addIngredientBtn = new Button("+ Add Ingredient");
        addIngredientBtn.setOnAction(e -> ingredientRows.getChildren().add(buildIngredientRow(null, null)));

        Button saveBtn = new Button("Save Recipe");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setOnAction(e -> save());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Title:"), titleField);
        form.addRow(1, new Label("Category:"), categoryField);
        form.addRow(2, new Label("Prep time (minutes):"), prepTimeSpinner);
        form.addRow(3, new Label("Base servings:"), baseServingsSpinner);

        Label nutritionHint = new Label(
                "Optional -- powers the Nutrition chart and scales with servings. "
                        + "Leave at 0 if unknown.");
        nutritionHint.getStyleClass().add("status-label");
        nutritionHint.setWrapText(true);

        GridPane nutritionForm = new GridPane();
        nutritionForm.setHgap(10);
        nutritionForm.setVgap(10);
        nutritionForm.addRow(0, new Label("Calories (kcal):"), caloriesSpinner);
        nutritionForm.addRow(1, new Label("Protein (g):"), proteinSpinner);
        nutritionForm.addRow(2, new Label("Carbs (g):"), carbsSpinner);
        nutritionForm.addRow(3, new Label("Fat (g):"), fatSpinner);

        VBox root = new VBox(14,
                new Label("Recipe Details"), form,
                new Label("Nutrition (per full recipe, at base servings)"), nutritionHint, nutritionForm,
                new Label("Ingredients"), ingredientRows, addIngredientBtn,
                new Label("Instructions"), instructionsArea,
                saveBtn, statusLabel);
        root.setPadding(new Insets(0, 0, 0, 16));

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private HBox buildIngredientRow(String name, String quantity) {
        TextField nameField = new TextField(name == null ? "" : name);
        nameField.setPromptText("Ingredient name");
        TextField qtyField = new TextField(quantity == null ? "" : quantity);
        qtyField.setPromptText("Quantity (e.g. 2 cups)");
        qtyField.setPrefWidth(140);

        Button removeBtn = new Button("Remove");
        HBox row = new HBox(8, nameField, qtyField, removeBtn);
        removeBtn.setOnAction(e -> ingredientRows.getChildren().remove(row));
        HBox.setHgrow(nameField, Priority.ALWAYS);
        return row;
    }

    private void resetForm() {
        editingRecipe = null;
        titleField.clear();
        categoryField.clear();
        prepTimeSpinner.getValueFactory().setValue(15);
        baseServingsSpinner.getValueFactory().setValue(4);
        caloriesSpinner.getValueFactory().setValue(0);
        proteinSpinner.getValueFactory().setValue(0.0);
        carbsSpinner.getValueFactory().setValue(0.0);
        fatSpinner.getValueFactory().setValue(0.0);
        instructionsArea.clear();
        ingredientRows.getChildren().clear();
        ingredientRows.getChildren().add(buildIngredientRow(null, null));
        statusLabel.setText("");
    }

    private void loadIntoForm(Recipe recipe) {
        editingRecipe = recipe;
        titleField.setText(recipe.getTitle());
        categoryField.setText(recipe.getCategory());
        prepTimeSpinner.getValueFactory().setValue(recipe.getPrepTimeMinutes());
        baseServingsSpinner.getValueFactory().setValue(Math.max(1, recipe.getBaseServings()));
        caloriesSpinner.getValueFactory().setValue(recipe.getCalories());
        proteinSpinner.getValueFactory().setValue(recipe.getProteinGrams());
        carbsSpinner.getValueFactory().setValue(recipe.getCarbsGrams());
        fatSpinner.getValueFactory().setValue(recipe.getFatGrams());
        instructionsArea.setText(recipe.getInstructions());

        ingredientRows.getChildren().clear();
        for (Ingredient ing : recipe.getIngredients()) {
            ingredientRows.getChildren().add(buildIngredientRow(ing.getName(), ing.getQuantity()));
        }
        if (ingredientRows.getChildren().isEmpty()) {
            ingredientRows.getChildren().add(buildIngredientRow(null, null));
        }
        statusLabel.setText("");
    }

    private void save() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            statusLabel.setText("Title is required.");
            return;
        }

        Recipe recipe = (editingRecipe != null) ? editingRecipe : new Recipe();
        recipe.setTitle(title);
        recipe.setCategory(categoryField.getText());
        recipe.setPrepTimeMinutes(prepTimeSpinner.getValue());
        recipe.setBaseServings(baseServingsSpinner.getValue());
        recipe.setCalories(caloriesSpinner.getValue());
        recipe.setProteinGrams(proteinSpinner.getValue());
        recipe.setCarbsGrams(carbsSpinner.getValue());
        recipe.setFatGrams(fatSpinner.getValue());
        recipe.setInstructions(instructionsArea.getText());

        List<Ingredient> ingredients = new ArrayList<>();
        for (var node : ingredientRows.getChildren()) {
            HBox row = (HBox) node;
            TextField nameField = (TextField) row.getChildren().get(0);
            TextField qtyField = (TextField) row.getChildren().get(1);
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (!name.isEmpty()) {
                ingredients.add(new Ingredient(name, qtyField.getText()));
            }
        }
        recipe.setIngredients(ingredients);

        try {
            recipeDAO.saveRecipe(recipe);
            editingRecipe = recipe;
            statusLabel.setText("Saved.");
            loadMyRecipes();
            if (onSaved != null) {
                onSaved.run();
            }
        } catch (SQLException e) {
            statusLabel.setText("Could not save: " + e.getMessage());
        }
    }
}
