-- Recipe Planner database schema
-- Loaded automatically by DatabaseManager on first run if tables don't exist.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS recipes (
    recipe_id INTEGER PRIMARY KEY AUTOINCREMENT,
    api_id TEXT UNIQUE,
    title TEXT NOT NULL,
    category TEXT,
    instructions TEXT NOT NULL,
    image_url TEXT,
    is_favorite INTEGER DEFAULT 0 CHECK(is_favorite IN (0, 1)),
    prep_time_minutes INTEGER DEFAULT 0,
    base_servings INTEGER DEFAULT 4,
    calories INTEGER DEFAULT 0,
    protein_g REAL DEFAULT 0,
    carbs_g REAL DEFAULT 0,
    fat_g REAL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ingredients (
    ingredient_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS recipe_ingredients (
    recipe_id INTEGER NOT NULL,
    ingredient_id INTEGER NOT NULL,
    quantity TEXT NOT NULL,
    PRIMARY KEY (recipe_id, ingredient_id),
    FOREIGN KEY (recipe_id) REFERENCES recipes(recipe_id) ON DELETE CASCADE,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(ingredient_id) ON DELETE RESTRICT
);

-- NOTE: day_of_week drives a recurring weekly template (Mon..Sun repeat every week).
-- plan_date is kept nullable for a future "specific calendar date" mode but is not
-- written by the current app code -- see MealPlanDAO. UNIQUE constraint below assumes
-- one recipe per day+slot; remove it if you want to allow multiple options per slot.
CREATE TABLE IF NOT EXISTS meal_plans (
    plan_id INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_id INTEGER NOT NULL,
    day_of_week TEXT NOT NULL CHECK(day_of_week IN ('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')),
    meal_type TEXT NOT NULL CHECK(meal_type IN ('Breakfast', 'Lunch', 'Dinner', 'Snack')),
    plan_date DATE,
    FOREIGN KEY (recipe_id) REFERENCES recipes(recipe_id) ON DELETE CASCADE,
    UNIQUE(day_of_week, meal_type)
);

CREATE INDEX IF NOT EXISTS idx_recipes_title ON recipes(title);
CREATE INDEX IF NOT EXISTS idx_recipes_favorite ON recipes(is_favorite);
CREATE INDEX IF NOT EXISTS idx_meal_plans_day ON meal_plans(day_of_week);
