package com.recipeplanner.ui;

import com.recipeplanner.model.Recipe;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/** Builds the clickable recipe thumbnail card shown in grid/tile layouts. */
final class RecipeCardFactory {

    private static final double CARD_WIDTH = 220;
    private static final double IMAGE_HEIGHT = 140;

    private RecipeCardFactory() {
    }

    static VBox create(Recipe recipe, Runnable onClick) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(CARD_WIDTH - 20);
        imageView.setFitHeight(IMAGE_HEIGHT);
        imageView.setPreserveRatio(false);

        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isBlank()) {
            // background=true loads the image off the UI thread so scrolling a
            // full results grid doesn't stutter while thumbnails come in.
            imageView.setImage(new Image(recipe.getImageUrl(), CARD_WIDTH - 20, IMAGE_HEIGHT, false, true, true));
        }

        Label title = new Label(recipe.getTitle());
        title.setWrapText(true);
        title.getStyleClass().add("card-title");

        Label category = new Label(recipe.getCategory() != null ? recipe.getCategory() : "");
        category.getStyleClass().add("card-subtitle");

        VBox card = new VBox(6, imageView, title, category);
        card.setPadding(new Insets(10));
        card.setPrefWidth(CARD_WIDTH);
        card.getStyleClass().add("recipe-card");
        card.setOnMouseClicked(e -> onClick.run());
        return card;
    }
}
