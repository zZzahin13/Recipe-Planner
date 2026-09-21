package com.recipeplanner.ui;

import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.CookingTimer;
import com.recipeplanner.model.Ingredient;
import com.recipeplanner.model.Recipe;
import com.recipeplanner.network.TimerManager;
import com.recipeplanner.util.QuantityScaler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

/**
 * Full recipe detail screen: image, servings-scaled ingredient list,
 * instructions, nutrition chart, a Save-to-Favorites button, a Cook
 * Mode launcher, and a panel of independent background cooking timers.
 */
public class RecipeDetailView extends ScrollPane {

    private final RecipeDAO recipeDAO = new RecipeDAO();
    // One TimerManager for the whole app's lifetime -- RecipeDetailView itself
    // is a single cached instance reused across every recipe (see MainView), so
    // timers keep ticking in the background no matter which screen is showing.
    private final TimerManager timerManager = new TimerManager();

    private final ImageView imageView = new ImageView();
    private final Label titleLabel = new Label();
    private final Label categoryLabel = new Label();
    private final ListView<String> ingredientList = new ListView<>();
    private final TextArea instructionsArea = new TextArea();
    private final Button favoriteButton = new Button();
    private final Button cookModeButton = new Button("Enter Cook Mode");
    private final Label statusLabel = new Label();
    private final NutritionChartView nutritionChartView = new NutritionChartView();

    private final Spinner<Integer> servingsSpinner = new Spinner<>(1, 50, 4);

    private Recipe currentRecipe;

    public RecipeDetailView() {
        setFitToWidth(true);
        setContent(buildLayout());
        timerManager.setOnTimerFinished(this::showTimerFinishedAlert);
    }

    private VBox buildLayout() {
        imageView.setFitWidth(360);
        imageView.setFitHeight(240);
        imageView.setPreserveRatio(false);

        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        categoryLabel.getStyleClass().add("card-subtitle");

        favoriteButton.getStyleClass().add("primary-button");
        favoriteButton.setOnAction(e -> toggleFavorite());

        cookModeButton.getStyleClass().add("primary-button");
        cookModeButton.setOnAction(e -> openCookMode());

        Button editNutritionButton = new Button("Edit Nutrition");
        editNutritionButton.setOnAction(e -> openEditNutritionDialog());

        HBox actionRow = new HBox(10, favoriteButton, cookModeButton, editNutritionButton);

        VBox header = new VBox(6, titleLabel, categoryLabel, actionRow, statusLabel);
        header.setPadding(new Insets(0, 0, 0, 20));

        HBox top = new HBox(20, imageView, header);
        top.setAlignment(Pos.TOP_LEFT);

        VBox servingsBox = buildServingsScaler();

        ingredientList.setPrefHeight(220);
        instructionsArea.setEditable(false);
        instructionsArea.setWrapText(true);
        instructionsArea.setPrefHeight(220);

        VBox ingredientsBox = new VBox(6, new Label("Ingredients"), servingsBox, ingredientList);
        VBox instructionsBox = new VBox(6, new Label("Instructions"), instructionsArea);
        HBox.setHgrow(instructionsBox, Priority.ALWAYS);

        HBox contentRow = new HBox(20, ingredientsBox, instructionsBox);

        TitledPane nutritionPane = new TitledPane("Nutrition", nutritionChartView);
        nutritionPane.setCollapsible(false);

        TimerPanelView timerPanel = new TimerPanelView(timerManager);
        TitledPane timerPane = new TitledPane("Cooking Timers", timerPanel);
        timerPane.setCollapsible(false);

        VBox root = new VBox(18, top, contentRow, nutritionPane, timerPane);
        root.setPadding(new Insets(20));
        return root;
    }

    private VBox buildServingsScaler() {
        servingsSpinner.setEditable(true);
        servingsSpinner.setPrefWidth(90);
        servingsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> refreshScaledDisplay());

        HBox row = new HBox(8, new Label("Servings:"), servingsSpinner);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(row);
    }

    public void setRecipe(Recipe recipe) {
        this.currentRecipe = recipe;

        titleLabel.setText(recipe.getTitle());
        categoryLabel.setText(recipe.getCategory() != null ? recipe.getCategory() : "");
        instructionsArea.setText(recipe.getInstructions() != null ? recipe.getInstructions() : "");
        statusLabel.setText("");

        // Reset the spinner to this recipe's own base servings rather than
        // leaving whatever value was left over from the previous recipe.
        servingsSpinner.getValueFactory().setValue(Math.max(1, recipe.getBaseServings()));

        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isBlank()) {
            imageView.setImage(new Image(recipe.getImageUrl(), 360, 240, false, true, true));
        } else {
            imageView.setImage(null);
        }

        updateFavoriteButtonLabel();
        refreshScaledDisplay();
    }

    /** Recomputes ingredient quantities and the nutrition chart for the current servings value. */
    private void refreshScaledDisplay() {
        if (currentRecipe == null) {
            return;
        }
        int desiredServings = servingsSpinner.getValue();
        int baseServings = Math.max(1, currentRecipe.getBaseServings());
        double factor = (double) desiredServings / baseServings;

        ingredientList.getItems().clear();
        for (Ingredient ing : currentRecipe.getIngredients()) {
            String scaledQuantity = QuantityScaler.isScalable(ing.getQuantity())
                    ? QuantityScaler.scale(ing.getQuantity(), factor)
                    : ing.getQuantity(); // left as-is: e.g. "a pinch", "to taste"
            String suffix = QuantityScaler.isScalable(ing.getQuantity()) ? "" : "  (not auto-scaled)";
            ingredientList.getItems().add((scaledQuantity + " " + ing.getName()).trim() + suffix);
        }

        nutritionChartView.setRecipe(currentRecipe, desiredServings);
    }

    private void updateFavoriteButtonLabel() {
        favoriteButton.setText(currentRecipe.isFavorite() ? "\u2605 Saved to Favorites" : "\u2606 Save to Favorites");
    }

    private void toggleFavorite() {
        try {
            currentRecipe.setFavorite(!currentRecipe.isFavorite());
            recipeDAO.saveRecipe(currentRecipe); // INSERT-or-UPDATE by api_id, then favorite flag
            updateFavoriteButtonLabel();
            statusLabel.setText(currentRecipe.isFavorite() ? "Saved to your library." : "Removed from favorites.");
        } catch (SQLException e) {
            statusLabel.setText("Could not update favorites: " + e.getMessage());
        }
    }

    private void openCookMode() {
        if (currentRecipe == null) {
            return;
        }
        Stage owner = (getScene() != null) ? (Stage) getScene().getWindow() : null;
        CookModeView.open(currentRecipe, owner);
    }

    private void showTimerFinishedAlert(CookingTimer timer) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Timer Finished");
        alert.setHeaderText("\"" + timer.getLabel() + "\" is done!");
        alert.setContentText("Your " + (timer.getTotalSeconds() / 60) + "-minute timer has finished.");
        alert.show(); // non-blocking -- doesn't freeze the app if another timer is also running
    }

    private void openEditNutritionDialog() {
        if (currentRecipe == null) {
            return;
        }

        Spinner<Integer> caloriesSpinner = new Spinner<>(0, 5000, currentRecipe.getCalories());
        Spinner<Double> proteinSpinner = new Spinner<>(0, 500, currentRecipe.getProteinGrams(), 1);
        Spinner<Double> carbsSpinner = new Spinner<>(0, 500, currentRecipe.getCarbsGrams(), 1);
        Spinner<Double> fatSpinner = new Spinner<>(0, 500, currentRecipe.getFatGrams(), 1);
        for (Spinner<?> s : List.of(caloriesSpinner, proteinSpinner, carbsSpinner, fatSpinner)) {
            s.setEditable(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Calories (kcal):"), caloriesSpinner);
        grid.addRow(1, new Label("Protein (g):"), proteinSpinner);
        grid.addRow(2, new Label("Carbs (g):"), carbsSpinner);
        grid.addRow(3, new Label("Fat (g):"), fatSpinner);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Nutrition -- " + currentRecipe.getTitle());
        dialog.setHeaderText("TheMealDB doesn't provide nutrition data, so enter it manually "
                + "(values are for " + currentRecipe.getBaseServings() + " base servings). "
                + "Saving here also adds this recipe to your library if it isn't already saved.");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            currentRecipe.setCalories(caloriesSpinner.getValue());
            currentRecipe.setProteinGrams(proteinSpinner.getValue());
            currentRecipe.setCarbsGrams(carbsSpinner.getValue());
            currentRecipe.setFatGrams(fatSpinner.getValue());
            try {
                recipeDAO.saveRecipe(currentRecipe);
                statusLabel.setText("Nutrition updated.");
                refreshScaledDisplay();
            } catch (SQLException e) {
                statusLabel.setText("Could not save nutrition: " + e.getMessage());
            }
        });
    }
}
