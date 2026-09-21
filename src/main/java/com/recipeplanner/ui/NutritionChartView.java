package com.recipeplanner.ui;

import com.recipeplanner.model.Recipe;
import javafx.collections.FXCollections;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * PieChart breakdown of protein/carbs/fat (by calorie contribution:
 * protein and carbs are ~4 kcal/g, fat is ~9 kcal/g). TheMealDB's API
 * doesn't provide nutrition data, so this only shows real numbers for
 * recipes where calories/macros were entered manually (see
 * CustomRecipeView); otherwise it shows a placeholder message instead
 * of a misleading empty/zero chart.
 */
public class NutritionChartView extends VBox {

    private final PieChart pieChart = new PieChart();
    private final Label caloriesLabel = new Label();
    private final Label placeholderLabel = new Label(
            "No nutrition data for this recipe yet -- edit it to add calories and macros.");

    public NutritionChartView() {
        super(6);
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);
        pieChart.setPrefSize(260, 220);
        placeholderLabel.setWrapText(true);
        placeholderLabel.getStyleClass().add("status-label");
        getChildren().add(placeholderLabel);
    }

    /** currentServings/baseServings scales the displayed totals to match the portion scaler. */
    public void setRecipe(Recipe recipe, int currentServings) {
        getChildren().clear();
        if (recipe == null || !recipe.hasNutritionData()) {
            getChildren().add(placeholderLabel);
            return;
        }

        double factor = recipe.getBaseServings() <= 0
                ? 1.0
                : (double) currentServings / recipe.getBaseServings();

        double protein = recipe.getProteinGrams() * factor;
        double carbs = recipe.getCarbsGrams() * factor;
        double fat = recipe.getFatGrams() * factor;
        int calories = (int) Math.round(recipe.getCalories() * factor);

        pieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data(String.format("Protein (%.0fg)", protein), protein * 4),
                new PieChart.Data(String.format("Carbs (%.0fg)", carbs), carbs * 4),
                new PieChart.Data(String.format("Fat (%.0fg)", fat), fat * 9)
        ));
        pieChart.setTitle("Macronutrients (% of calories)");

        caloriesLabel.setText(calories + " kcal for " + currentServings + " serving(s)");
        caloriesLabel.getStyleClass().add("card-title");

        getChildren().addAll(caloriesLabel, pieChart);
    }
}
