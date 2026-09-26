package com.recipeplanner.controller;
import com.recipeplanner.model.Micronutrient;
import com.recipeplanner.network.NutritionApiService;
import javafx.concurrent.Task;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.recipeplanner.dao.RecipeDAO;
import com.recipeplanner.model.CookingTimer;
import com.recipeplanner.model.Ingredient;
import com.recipeplanner.model.Recipe;
import com.recipeplanner.network.TimerManager;
import com.recipeplanner.ui.CookModeView;
import com.recipeplanner.ui.NutritionChartView;
import com.recipeplanner.ui.TimerPanelView;
import com.recipeplanner.util.QuantityScaler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.recipeplanner.model.NutritionLookupResult;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for recipe_detail.fxml. MainController caches this
 * controller instance (like every other screen), so the TimerManager
 * created here -- and any timers running on it -- persists across
 * navigation, not just across recipes.
 */
public class RecipeDetailController implements MainAware {

    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final TimerManager timerManager = new TimerManager();
    private final NutritionChartView nutritionChartView = new NutritionChartView();
    private final NutritionApiService nutritionApi = new NutritionApiService();
    private final ExecutorService nutritionExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nutrition-lookup-worker");
        t.setDaemon(true);
        return t;
    });
    private MainController mainController;
    private Recipe currentRecipe;

    @FXML private ImageView imageView;
    @FXML private Label titleLabel;
    @FXML private Label categoryLabel;
    @FXML private Button favoriteButton;
    @FXML private Label statusLabel;
    @FXML private Spinner<Integer> servingsSpinner;
    @FXML private ListView<String> ingredientList;
    @FXML private TextArea instructionsArea;
    @FXML private VBox nutritionContainer;
    @FXML private VBox timerContainer;
    @FXML private Label micronutrientStatusLabel;
    @FXML private ListView<String> micronutrientList;
    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        servingsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 4));
        servingsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> refreshScaledDisplay());

        nutritionContainer.getChildren().add(nutritionChartView);
        timerContainer.getChildren().add(new TimerPanelView(timerManager));
        timerManager.setOnTimerFinished(this::showTimerFinishedAlert);

        ingredientList.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                ingredientList.prefHeightProperty().bind(newScene.heightProperty().multiply(0.35));
            }
        });
    }
    private void loadNutritionFromApi(Recipe recipe) {
        micronutrientList.getItems().clear();
        micronutrientStatusLabel.setText("Looking up nutrition data...");

        Task<NutritionLookupResult> task = new Task<>() {
            @Override
            protected NutritionLookupResult call() throws Exception {
                return nutritionApi.lookup(recipe.getTitle());
            }
        };
        task.setOnSucceeded(e -> {
            NutritionLookupResult result = task.getValue();

            // Only auto-fill macros if none were entered by hand -- never
            // overwrite something the user typed via Edit Nutrition.
            if (!recipe.hasNutritionData() && result.hasAnyMacros()) {
                recipe.setCalories(result.getCalories());
                recipe.setProteinGrams(result.getProteinGrams());
                recipe.setCarbsGrams(result.getCarbsGrams());
                recipe.setFatGrams(result.getFatGrams());
                nutritionChartView.setRecipe(recipe, servingsSpinner.getValue());
            }

            List<Micronutrient> micros = result.getMicronutrients();
            if (micros.isEmpty()) {
                micronutrientStatusLabel.setText("No USDA match found for \"" + recipe.getTitle() + "\".");
            } else {
                micronutrientStatusLabel.setText("Estimated per 100g, closest USDA match for \"" + recipe.getTitle() + "\":");
                for (Micronutrient m : micros) {
                    micronutrientList.getItems().add(m.toString());
                }
            }
        });
        task.setOnFailed(e -> micronutrientStatusLabel.setText(
                "Could not fetch nutrition data: " + task.getException().getMessage()));

        nutritionExecutor.submit(task);
    }
    public void setRecipe(Recipe recipe) {
        this.currentRecipe = recipe;

        titleLabel.setText(recipe.getTitle());
        categoryLabel.setText(recipe.getCategory() != null ? recipe.getCategory() : "");
        instructionsArea.setText(recipe.getInstructions() != null ? recipe.getInstructions() : "");
        statusLabel.setText("");

        servingsSpinner.getValueFactory().setValue(Math.max(1, recipe.getBaseServings()));

        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isBlank()) {
            imageView.setImage(new Image(recipe.getImageUrl(), 360, 240, false, true, true));
        } else {
            imageView.setImage(null);
        }

        updateFavoriteButtonLabel();
        refreshScaledDisplay();
        loadNutritionFromApi(recipe);
    }

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
                    : ing.getQuantity();
            String suffix = QuantityScaler.isScalable(ing.getQuantity()) ? "" : "  (not auto-scaled)";
            ingredientList.getItems().add((scaledQuantity + " " + ing.getName()).trim() + suffix);
        }

        nutritionChartView.setRecipe(currentRecipe, desiredServings);
    }

    private void updateFavoriteButtonLabel() {
        favoriteButton.setText(currentRecipe.isFavorite() ? "\u2605 Saved to Favorites" : "\u2606 Save to Favorites");
    }

    @FXML
    private void onToggleFavorite() {
        try {
            currentRecipe.setFavorite(!currentRecipe.isFavorite());
            recipeDAO.saveRecipe(currentRecipe);
            updateFavoriteButtonLabel();
            statusLabel.setText(currentRecipe.isFavorite() ? "Saved to your library." : "Removed from favorites.");
        } catch (SQLException e) {
            statusLabel.setText("Could not update favorites: " + e.getMessage());
        }
    }

    @FXML
    private void onEnterCookMode() {
        if (currentRecipe == null) {
            return;
        }
        Stage owner = (imageView.getScene() != null) ? (Stage) imageView.getScene().getWindow() : null;
        CookModeView.open(currentRecipe, owner);
    }

    @FXML
    private void onEditNutrition() {
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

        GridPaneQuick grid = new GridPaneQuick();
        grid.addRow("Calories (kcal):", caloriesSpinner);
        grid.addRow("Protein (g):", proteinSpinner);
        grid.addRow("Carbs (g):", carbsSpinner);
        grid.addRow("Fat (g):", fatSpinner);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Nutrition -- " + currentRecipe.getTitle());
        dialog.setHeaderText("TheMealDB doesn't provide nutrition data, so enter it manually "
                + "(values are for " + currentRecipe.getBaseServings() + " base servings). "
                + "Saving here also adds this recipe to your library if it isn't already saved.");
        dialog.getDialogPane().setContent(grid.pane);
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

    private void showTimerFinishedAlert(CookingTimer timer) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Timer Finished");
        alert.setHeaderText("\"" + timer.getLabel() + "\" is done!");
        alert.setContentText("Your " + (timer.getTotalSeconds() / 60) + "-minute timer has finished.");
        alert.show();
    }

    /** Tiny local helper so onEditNutrition doesn't need a GridPane import juggling row indices by hand. */
    private static class GridPaneQuick {
        private final javafx.scene.layout.GridPane pane = new javafx.scene.layout.GridPane();
        private int row = 0;

        GridPaneQuick() {
            pane.setHgap(10);
            pane.setVgap(10);
        }

        void addRow(String label, javafx.scene.Node field) {
            pane.addRow(row++, new Label(label), field);
        }
    }
}
