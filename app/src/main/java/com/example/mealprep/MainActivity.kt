package com.example.mealprep

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Main Activity - entry point of the Meal Prep application.
 * Displays the main menu with buttons for each feature.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

/**
 * Main screen composable with 4 buttons:
 * - Add Meals to DB
 * - Search for Meals By Ingredient
 * - Search for Meals
 * - Search Meals from Web Service
 */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // State to show status message after adding meals
    val statusMessage = rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Meal Prep App",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Button 1: Add Meals to DB
        Button(
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val db = MealDatabase.getDatabase(context)
                        val dao = db.mealDao()
                        dao.insertMeals(getHardcodedMeals())
                    }
                    statusMessage.value = "Meals added to database!"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Meals to DB", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button 2: Search for Meals By Ingredient
        Button(
            onClick = {
                val intent = Intent(context, SearchByIngredientActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search for Meals By Ingredient", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button 3: Search for Meals (local DB)
        Button(
            onClick = {
                val intent = Intent(context, SearchMealsActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search for Meals", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button 4: Search Meals from Web Service
        Button(
            onClick = {
                val intent = Intent(context, SearchWebActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search Meals from Web Service", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status message display
        if (statusMessage.value.isNotEmpty()) {
            Text(
                text = statusMessage.value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Returns a list of hardcoded meals to populate the database.
 * These are sample meals used for initial data.
 */
fun getHardcodedMeals(): List<Meal> {
    return listOf(
        Meal(
            idMeal = "1",
            name = "Sweet and Sour Pork",
            drinkAlternate = null,
            category = "Pork",
            area = "Chinese",
            instructions = "Prepare the pork by cutting into bite sized pieces. " +
                    "Mix together the flour, cornstarch, and salt. " +
                    "Coat the pork pieces in the flour mixture. " +
                    "Deep fry the pork until golden brown. " +
                    "For the sauce, mix vinegar, sugar, ketchup, and soy sauce. " +
                    "Heat the sauce and pour over the fried pork. Serve hot.",
            mealThumb = "https://www.themealdb.com/images/media/meals/1529442316.jpg",
            tags = "Sweet,Pork",
            youtube = "https://www.youtube.com/watch?v=mdaBxXBEJ3w",
            ingredient1 = "Pork", ingredient2 = "Egg", ingredient3 = "Water",
            ingredient4 = "Salt", ingredient5 = "Sugar", ingredient6 = "Soy Sauce",
            ingredient7 = "Starch", ingredient8 = "Tomato Puree", ingredient9 = "Vinegar",
            ingredient10 = "Coriander", ingredient11 = null, ingredient12 = null,
            ingredient13 = null, ingredient14 = null, ingredient15 = null,
            ingredient16 = null, ingredient17 = null, ingredient18 = null,
            ingredient19 = null, ingredient20 = null,
            measure1 = "200g", measure2 = "1", measure3 = "Dash",
            measure4 = "1/2 tsp", measure5 = "1 tsp", measure6 = "10ml",
            measure7 = "10g", measure8 = "30g", measure9 = "10ml",
            measure10 = "Dash", measure11 = null, measure12 = null,
            measure13 = null, measure14 = null, measure15 = null,
            measure16 = null, measure17 = null, measure18 = null,
            measure19 = null, measure20 = null
        ),
        Meal(
            idMeal = "2",
            name = "Chicken Marengo",
            drinkAlternate = null,
            category = "Chicken",
            area = "French",
            instructions = "Season the chicken with salt and pepper. " +
                    "Heat olive oil in a large skillet and brown the chicken. " +
                    "Remove the chicken and saute onion and garlic. " +
                    "Add tomatoes, mushrooms, and wine. " +
                    "Return the chicken to the pan and simmer for 30 minutes. " +
                    "Serve with crusty bread.",
            mealThumb = "https://www.themealdb.com/images/media/meals/qpxvuq1511798906.jpg",
            tags = "Chicken,Tomato",
            youtube = "https://www.youtube.com/watch?v=U33HYUr-0Fw",
            ingredient1 = "Chicken", ingredient2 = "Olive Oil", ingredient3 = "Mushrooms",
            ingredient4 = "Onion", ingredient5 = "Garlic", ingredient6 = "Tomatoes",
            ingredient7 = "White Wine", ingredient8 = "Black Olives", ingredient9 = "Parsley",
            ingredient10 = "Salt", ingredient11 = "Pepper", ingredient12 = null,
            ingredient13 = null, ingredient14 = null, ingredient15 = null,
            ingredient16 = null, ingredient17 = null, ingredient18 = null,
            ingredient19 = null, ingredient20 = null,
            measure1 = "4 pieces", measure2 = "3 tbs", measure3 = "200g",
            measure4 = "1 chopped", measure5 = "3 cloves", measure6 = "400g",
            measure7 = "100ml", measure8 = "50g", measure9 = "Chopped",
            measure10 = "Pinch", measure11 = "Pinch", measure12 = null,
            measure13 = null, measure14 = null, measure15 = null,
            measure16 = null, measure17 = null, measure18 = null,
            measure19 = null, measure20 = null
        ),
        Meal(
            idMeal = "3",
            name = "Beef Stroganoff",
            drinkAlternate = null,
            category = "Beef",
            area = "Russian",
            instructions = "Slice the beef into thin strips. " +
                    "Season with salt and pepper. " +
                    "Heat butter in a pan and cook onions until soft. " +
                    "Add mushrooms and cook for 5 minutes. " +
                    "Add beef strips and cook until browned. " +
                    "Stir in sour cream and mustard. " +
                    "Serve over egg noodles or rice.",
            mealThumb = "https://www.themealdb.com/images/media/meals/svprys1511176755.jpg",
            tags = "Beef,Creamy",
            youtube = "https://www.youtube.com/watch?v=nYtNu4gR5Jg",
            ingredient1 = "Beef", ingredient2 = "Butter", ingredient3 = "Onion",
            ingredient4 = "Mushrooms", ingredient5 = "Sour Cream", ingredient6 = "Mustard",
            ingredient7 = "Flour", ingredient8 = "Beef Stock", ingredient9 = "Paprika",
            ingredient10 = "Salt", ingredient11 = "Pepper", ingredient12 = "Egg Noodles",
            ingredient13 = null, ingredient14 = null, ingredient15 = null,
            ingredient16 = null, ingredient17 = null, ingredient18 = null,
            ingredient19 = null, ingredient20 = null,
            measure1 = "500g", measure2 = "30g", measure3 = "1 large",
            measure4 = "250g", measure5 = "200ml", measure6 = "1 tsp",
            measure7 = "2 tbs", measure8 = "200ml", measure9 = "1 tsp",
            measure10 = "Pinch", measure11 = "Pinch", measure12 = "400g",
            measure13 = null, measure14 = null, measure15 = null,
            measure16 = null, measure17 = null, measure18 = null,
            measure19 = null, measure20 = null
        ),
        Meal(
            idMeal = "4",
            name = "Spaghetti Bolognese",
            drinkAlternate = null,
            category = "Pasta",
            area = "Italian",
            instructions = "Heat olive oil in a large saucepan. " +
                    "Add onion, garlic, and carrot, and cook until soft. " +
                    "Add the minced beef and cook until browned. " +
                    "Add tomatoes, tomato paste, and herbs. " +
                    "Simmer for 30 minutes. " +
                    "Cook the spaghetti according to package instructions. " +
                    "Serve the sauce over the spaghetti with parmesan.",
            mealThumb = "https://www.themealdb.com/images/media/meals/sutysw1468247559.jpg",
            tags = "Pasta,Meat",
            youtube = "https://www.youtube.com/watch?v=wxHqG-IN3pM",
            ingredient1 = "Spaghetti", ingredient2 = "Olive Oil", ingredient3 = "Onion",
            ingredient4 = "Garlic", ingredient5 = "Minced Beef", ingredient6 = "Tomatoes",
            ingredient7 = "Tomato Paste", ingredient8 = "Carrot", ingredient9 = "Basil",
            ingredient10 = "Oregano", ingredient11 = "Salt", ingredient12 = "Pepper",
            ingredient13 = "Parmesan", ingredient14 = null, ingredient15 = null,
            ingredient16 = null, ingredient17 = null, ingredient18 = null,
            ingredient19 = null, ingredient20 = null,
            measure1 = "400g", measure2 = "2 tbs", measure3 = "1 chopped",
            measure4 = "2 cloves", measure5 = "500g", measure6 = "400g tin",
            measure7 = "2 tbs", measure8 = "1 grated", measure9 = "1 tsp",
            measure10 = "1 tsp", measure11 = "Pinch", measure12 = "Pinch",
            measure13 = "Grated", measure14 = null, measure15 = null,
            measure16 = null, measure17 = null, measure18 = null,
            measure19 = null, measure20 = null
        )
    )
}
