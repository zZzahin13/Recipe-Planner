package com.recipeplanner.controller;

import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.Ingredient;
import com.recipeplanner.model.Recipe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Controller for custom_recipe.fxml -- create/edit/delete the user's own recipes. */
public class CustomRecipeController implements MainAware {

    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final ObservableList<Recipe> myRecipes = FXCollections.observableArrayList();
    private Recipe editingRecipe; // null while creating a new recipe
    private MainController mainController;

    @FXML private ListView<Recipe> myRecipesList;
    @FXML private TextField titleField;
    @FXML private TextField categoryField;
    @FXML private Spinner<Integer> prepTimeSpinner;
    @FXML private Spinner<Integer> baseServingsSpinner;
    @FXML private Spinner<Integer> caloriesSpinner;
    @FXML private Spinner<Double> proteinSpinner;
    @FXML private Spinner<Double> carbsSpinner;
    @FXML private Spinner<Double> fatSpinner;
    @FXML private VBox ingredientRows;
    @FXML private TextArea instructionsArea;
    @FXML private Label statusLabel;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        prepTimeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 600, 15));
        baseServingsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 4));
        caloriesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5000, 0));
        proteinSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 500, 0, 1));
        carbsSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 500, 0, 1));
        fatSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 500, 0, 1));

        myRecipesList.setItems(myRecipes);
        myRecipesList.getSelectionModel().selectedItemProperty().addListener((obs, oldR, newR) -> {
            if (newR != null) {
                loadIntoForm(newR);
            }
        });

        loadMyRecipes();
        resetForm();
    }

    private void loadMyRecipes() {
        try {
            List<Recipe> all = recipeDAO.getAllRecipes();
            myRecipes.setAll(all.stream().filter(r -> r.getSource() == Recipe.Source.CUSTOM).toList());
        } catch (SQLException e) {
            statusLabel.setText("Could not load your recipes: " + e.getMessage());
        }
    }

    @FXML
    private void onNewRecipe() {
        myRecipesList.getSelectionModel().clearSelection();
        resetForm();
    }

    @FXML
    private void onDeleteSelected() {
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

    @FXML
    private void onAddIngredient() {
        ingredientRows.getChildren().add(buildIngredientRow(null, null));
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

    @FXML
    private void onSave() {
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
            editingRecipe = recipe; // subsequent Save presses now update this row instead of inserting again
            statusLabel.setText("Saved.");
            loadMyRecipes();
            if (mainController != null) {
                mainController.showFavorites();
            }
        } catch (SQLException e) {
            statusLabel.setText("Could not save: " + e.getMessage());
        }
    }
}
