# Recipe Planner

JavaFX desktop app: search recipes from TheMealDB, save favorites to a
local SQLite library, create your own recipes, and assign meals to a
weekly planner grid. Built to line up with a 6-week Java lab syllabus:
OOP/syntax, JavaFX GUI, multithreading, JSON parsing, SQLite/JDBC, and
Git workflow.

## Requirements

- JDK 17 or newer
- Maven 3.8+
- Internet access (Maven needs to download dependencies the first time,
  and the app itself calls the free TheMealDB API at runtime)

## Run it

```
mvn clean javafx:run
```

This compiles the project and launches the app directly. A SQLite file
`recipe_planner.db` is created automatically in the working directory
on first launch, with the schema in `src/main/resources/sql/schema.sql`.

## Build a runnable jar

```
mvn clean package
```

Produces `target/recipe-planner-1.0.0.jar` via the shade plugin. Note:
JavaFX's native modules are platform-specific, so a shaded jar built on
one OS may not run on another -- for cross-platform distribution you'd
normally use `jlink`/`jpackage` instead, which is out of scope here but
worth knowing if you ever need to hand someone a double-clickable app.

## Project layout

```
src/main/java/com/recipeplanner/
  Main.java                 application entry point
  model/                    Recipe, Ingredient, MealPlanEntry (POJOs)
  db/                       DatabaseManager -- JDBC connection + schema init
  dao/                      RecipeDAO, MealPlanDAO -- all SQL lives here
  network/                  MealDbApiService (HTTP+JSON), FetchRecipesTask,
                             CookingTimerService (background Task/Service)
  ui/                       MainView (shell/nav) + one class per screen
src/main/resources/
  css/style.css
  sql/schema.sql
```

This is a plain MVC-ish split rather than FXML-based MVC: `ui/` classes
build their own JavaFX node trees in code and wire up event handlers
directly, so there's no separate `.fxml` + `@FXML`-annotated controller
per screen. Functionally equivalent for this app's size; if you want to
practice the FXML+Controller pattern specifically for the syllabus,
each `ui/*View` class is a natural candidate to convert.

## Where each lab concept lives

- **Week 1 (Java/OOP):** `model/` package -- encapsulated POJOs with
  constructors and getters/setters; try-catch around all JDBC and
  network calls.
- **Week 2 (Git):** see "Suggested Git workflow" below.
- **Week 3 (JavaFX GUI):** `ui/` package -- `BorderPane`, `VBox`, `HBox`,
  `GridPane`, `TilePane`, `TextField`, `Button`, `ListView`, `ImageView`,
  CSS in `style.css`, `setOnAction` event handlers throughout.
- **Week 4 (Concurrency):** `network/MultiSourceSearchTask` and
  `network/ParallelCategorySearchTask` (both `Task<List<Recipe>>`
  submitted to an `ExecutorService` in `SearchView`), and
  `network/TimerManager` (`ScheduledExecutorService` driving any number
  of concurrent `CookingTimer`s) -- all updating the UI only via
  `Platform.runLater` or JavaFX's Task callback threading guarantees.
- **Week 6 (SQLite/JDBC):** `db/DatabaseManager` + `dao/` package --
  schema creation, parameterized `PreparedStatement` CRUD, results
  mapped into POJOs for `ListView`/`ComboBox`.
- **Week 7 (JSON/API):** `network/MealDbApiService` -- `HttpClient` GET
  requests to TheMealDB, `org.json` parsing of the flattened
  `strIngredient1..20` fields into `Ingredient` objects.

## Feature additions (round 2)

- **Multi-source search / parallel categories** -- `SearchView` now
  either checks your local library first and falls back to TheMealDB
  only if nothing matches (`MultiSourceSearchTask`), or fires several
  category searches at once via an `ExecutorService`
  (`ParallelCategorySearchTask`) when you multi-select categories.
- **Nutrition chart** -- `ui/NutritionChartView` (a `PieChart`) shows
  calories/protein/carbs/fat for a recipe. **TheMealDB has no
  nutrition fields in its API**, so this is never auto-fetched --
  enter it in `CustomRecipeView` when creating a recipe, or via the
  "Edit Nutrition" button on any recipe's detail screen. Values scale
  with the servings spinner.
- **Portion/servings scaler** -- the "Servings" spinner on the detail
  screen rescales ingredient quantities (`util/QuantityScaler`) and
  the nutrition chart together. Only quantities that start with a
  number get scaled; free-text ones like "a pinch" are left alone and
  marked "(not auto-scaled)".
- **Multi-step timers with sound** -- `network/TimerManager` +
  `ui/TimerPanelView` replace the old single-timer widget. Start as
  many named timers as you want; each beeps (system beep, not an
  embedded audio file) and pops a non-blocking alert when it finishes,
  independently of the others.
- **Cook Mode** -- `ui/CookModeView` opens a fullscreen, one-step-at-a-
  time view (Space/Right = next, Left = back, Enter = mark done, Esc =
  exit) from the "Enter Cook Mode" button on the detail screen.

## Known gaps / things to decide next

- **Shopping list** is implemented as a live query
  (`MealPlanDAO.getShoppingList`) joining the week's planned recipes
  against `recipe_ingredients` -- it does *not* sum quantities with
  different units (e.g. "1 cup" + "200 g" of the same ingredient stay
  as separate lines). Decide whether you need real unit conversion or
  if listing them separately is fine for a first version.
- **Nutrition dashboard** (calories/macros + charts) isn't built --
  there's nowhere in the schema to store nutrition data yet. If you
  want it, that's a new columns-on-`ingredients` (or a separate
  `nutrition` table) plus a JavaFX chart (`PieChart`/`BarChart`)
  screen.
- **`meal_plans.plan_date`** exists in the schema but the app only
  ever writes `day_of_week` (a repeating weekly template). If you
  later want actual calendar-dated planning instead of "every Monday",
  the DAO and planner grid both need to switch to keying off dates.

## Suggested Git workflow

```
git init
git add .
git commit -m "Initial project scaffold"
git checkout -b feature/api-handler     # MealDbApiService + FetchRecipesTask
git checkout -b feature/sqlite-db       # DatabaseManager + dao/
git checkout -b feature/javafx-ui       # ui/ package
git checkout -b feature/meal-planner    # MealPlannerView + MealPlanDAO
```

Merge each feature branch back to `main` as you finish it, and push to
GitHub/GitLab to keep a visible commit history for grading.
