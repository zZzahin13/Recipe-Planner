# Recipe Planner

JavaFX desktop app: search recipes from TheMealDB, save favorites to a
local SQLite library, create your own recipes, see estimated nutrition
from USDA FoodData Central, and assign meals to a weekly planner grid.
Built to line up with a 6-week Java lab syllabus: OOP/syntax, JavaFX GUI,
multithreading, JSON parsing, SQLite/JDBC, and Git workflow.

## Requirements

- JDK 17 or newer
- Maven 3.8+
- Internet access (Maven downloads dependencies the first time, and the
  app calls two free APIs at runtime: TheMealDB and USDA FoodData Central)

## USDA API key (optional but recommended)

Nutrition lookups use USDA FoodData Central. Get a free key at
https://fdc.nal.usda.gov/api-key-signup.html and set it as an
environment variable named `USDA_API_KEY` before running the app.

If the variable is not set, the app falls back to USDA's public
`DEMO_KEY`, which works without signup but is rate-limited
(about 30 requests per hour). Never commit a real key to the repository.

## Run it

```
mvn clean javafx:run
```

This compiles the project and launches the app. A SQLite file
`recipe_planner.db` is created automatically in the working directory on
first launch, using the schema in `src/main/resources/sql/schema.sql`.

## Build a runnable jar

```
mvn clean package
```

Produces `target/recipe-planner-1.0.0.jar` via the shade plugin. JavaFX's
native modules are platform-specific, so a shaded jar built on one OS may
not run on another. For cross-platform distribution, use
`jlink`/`jpackage` instead.

## Project layout

```
src/main/java/com/recipeplanner/
  Main.java                 loads splash.fxml, then main.fxml
  model/                    Recipe, Ingredient, MealPlanEntry, CookingTimer,
                            Micronutrient, NutritionLookupResult
  db/                       DatabaseManager -- JDBC connection + schema init
  dao/                      RecipeDAO, MealPlanDAO -- all SQL lives here
  network/                  MealDbApiService, NutritionApiService (HTTP+JSON),
                            RecipeSearchTask (abstract) + search tasks,
                            TimerManager
  controller/               MainController + one @FXML controller per screen
  ui/                       small reusable widgets embedded INTO screens
src/main/resources/
  css/style.css
  sql/schema.sql
  fxml/                     one .fxml layout per screen (open in Scene Builder)
```

Every screen is a `.fxml` file paired with a `controller/*Controller.java`
class using `@FXML` fields and `onAction="#methodName"` handlers.
`MainController` loads each screen on demand via `FXMLLoader`, caches the
built screen and its controller, and swaps it into `main.fxml`'s content
area. Any controller that needs to navigate implements the `MainAware`
interface, which hands it a reference to `MainController`.

Some widgets stay as plain Java classes in `ui/` because they are
dynamically built content rather than fixed layouts: `RecipeCardFactory`
(one recipe card at a time), `TimerPanelView` and `NutritionChartView`
(rebuilt whenever data changes), and `CookModeView` (its own fullscreen
`Stage`).

## Where each lab concept lives

- **OOP:**
  - `network/RecipeSearchTask` is an abstract class. `FetchRecipesTask`,
    `MultiSourceSearchTask` and `ParallelCategorySearchTask` extend it and
    share the API client and status-message helper.
  - `controller/MainAware` is an interface implemented by screen controllers.
  - `model/` holds encapsulated classes with constructors and
    getters/setters. `Recipe` has a list of `Ingredient` (composition).
  - Enums (`Mode`, `SourceUsed`) keep search options type-safe.
  - The DAO pattern keeps all SQL out of the UI code.
- **Git:** see "Suggested Git workflow" below.
- **JavaFX GUI:** `resources/fxml/*.fxml` + `controller/*Controller.java`.
  Layouts and controls include `BorderPane`, `VBox`, `HBox`, `StackPane`,
  `GridPane`, `TilePane`, `ScrollPane`, `TitledPane`, `TextField`, `Button`,
  `ComboBox`, `Spinner`, `ListView`, `ImageView`, `PieChart`, and
  `ProgressIndicator`, styled in `style.css`.
- **Layout responsiveness:**
  - Search: the results `TilePane` width is bound to the `ScrollPane`
    width, so recipe cards reflow as the window resizes.
  - Recipe detail: the ingredient list height is bound to 35% of the
    scene height.
  - Meal planner: the `GridPane` uses percentage column widths, and the
    dropdowns grow to fill their columns.
- **Concurrency:**
  - `SearchController` submits search tasks to an `ExecutorService`.
  - `ParallelCategorySearchTask` uses a fixed thread pool to fetch several
    categories at once.
  - `TimerManager` uses a `ScheduledExecutorService` to run any number of
    cooking timers independently.
  - `RecipeDetailController` runs USDA lookups on a dedicated single-thread
    executor.
  - UI updates happen only through JavaFX `Task` callbacks.
- **SQLite/JDBC:** `db/DatabaseManager` + `dao/`. Four tables (`recipes`,
  `ingredients`, `recipe_ingredients`, `meal_plans`) with foreign keys,
  `ON DELETE CASCADE`/`RESTRICT`, a composite primary key, and `CHECK`
  constraints. All queries use `PreparedStatement`.
- **CRUD:** create/edit custom recipes, browse the library and favorites,
  update nutrition or favorite status, delete recipes, and assign or
  clear meal plan slots.
- **JSON/API:** two HTTP integrations using `HttpClient` and `org.json`:
  - `MealDbApiService` (TheMealDB): recipe search, categories, images.
    Flattens `strIngredient1..20` fields into `Ingredient` objects.
  - `NutritionApiService` (USDA FoodData Central): calories, macros and
    micronutrients.

## Features

- **Multi-source search / parallel categories:** checks your local library
  first and falls back to TheMealDB (`MultiSourceSearchTask`), or fetches
  several selected categories at once (`ParallelCategorySearchTask`).
  Category results load fast from one request per category, and full
  recipe details are fetched when you open a card.
- **Nutrition (USDA):** when a recipe has no nutrition data, calories,
  protein, carbs and fat are auto-filled from USDA and shown in a pie
  chart. The "Micronutrients" panel lists Vitamin A, C, D, Calcium, Iron,
  Potassium, Sodium, Magnesium and Zinc. Values are per 100 g of the
  closest USDA match to the recipe title, so treat them as estimates, not
  an exact calculation of the dish. Manually entered values ("Edit
  Nutrition") are never overwritten, and auto-filled values are only
  saved when you save or edit the recipe.
- **Servings scaler:** the "Servings" spinner rescales ingredient
  quantities (`util/QuantityScaler`) and the nutrition chart. Quantities
  like "a pinch" are left alone and marked "(not auto-scaled)".
- **Weekly meal planner:** assign saved recipes to day/meal slots, choose
  "-- empty --" to clear a slot, and generate a shopping list.
- **Multi-step timers with sound:** start any number of named timers; each
  beeps and shows a non-blocking alert when it finishes.
- **Cook Mode:** fullscreen, one-step-at-a-time view (Space/Right = next,
  Left = back, Enter = mark done, Esc = exit).

## Known gaps

- **Shopping list** does not sum quantities with different units (for
  example "1 cup" and "200 g" of the same ingredient stay separate lines).
- **Nutrition is approximate:** USDA is searched by recipe title, not by
  the actual ingredient list and quantities.
- **`meal_plans.plan_date`** exists in the schema, but the app only writes
  `day_of_week` (a repeating weekly template).

## Suggested Git workflow

```
git init
git add .
git commit -m "Initial project scaffold"
git checkout -b feature/api-handler     # MealDbApiService + FetchRecipesTask
git checkout -b feature/sqlite-db       # DatabaseManager + dao/
git checkout -b feature/javafx-ui       # screens and controllers
git checkout -b feature/meal-planner    # MealPlannerController + MealPlanDAO
```

Merge each feature branch back to `main` as you finish it, and push to
GitHub to keep a visible commit history.