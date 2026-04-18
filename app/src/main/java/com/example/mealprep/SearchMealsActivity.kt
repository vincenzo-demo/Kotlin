package com.example.mealprep

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for searching meals from the local Room database.
 * Supports case insensitive, partial match search on meal names and ingredients.
 * Also displays meal thumbnail images.
 */
class SearchMealsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SearchMealsScreen()
        }
    }
}

/**
 * Composable screen for searching meals in the local database.
 * Displays matching meals with their images.
 */
@Composable
fun SearchMealsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val searchText = rememberSaveable { mutableStateOf("") }
    val resultsText = rememberSaveable { mutableStateOf("") }
    // Store meals and their loaded bitmaps for display (meals survive rotation)
    val mealsList = rememberSaveable(saver = MealListSaver) { mutableStateOf(emptyList()) }
    val bitmaps = remember { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }

    // Reload bitmaps after rotation (meals restored via rememberSaveable, bitmaps lost)
    LaunchedEffect(Unit) {
        if (mealsList.value.isNotEmpty() && bitmaps.value.isEmpty()) {
            val loadedBitmaps = mutableMapOf<String, Bitmap?>()
            withContext(Dispatchers.IO) {
                for (meal in mealsList.value) {
                    if (!meal.mealThumb.isNullOrEmpty()) {
                        loadedBitmaps[meal.idMeal] = loadBitmapFromUrl(meal.mealThumb)
                    }
                }
            }
            bitmaps.value = loadedBitmaps
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Search for Meals",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Text field for search input
        TextField(
            value = searchText.value,
            onValueChange = { searchText.value = it },
            label = { Text("Enter search text") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search button
        Button(
            onClick = {
                scope.launch {
                    val db = MealDatabase.getDatabase(context)
                    val meals = withContext(Dispatchers.IO) {
                        db.mealDao().searchMeals(searchText.value)
                    }
                    mealsList.value = meals

                    if (meals.isEmpty()) {
                        resultsText.value = "No meals found."
                        bitmaps.value = emptyMap()
                    } else {
                        resultsText.value = "${meals.size} meals found."

                        // Load images for each meal in background
                        val loadedBitmaps = mutableMapOf<String, Bitmap?>()
                        withContext(Dispatchers.IO) {
                            for (meal in meals) {
                                if (!meal.mealThumb.isNullOrEmpty()) {
                                    loadedBitmaps[meal.idMeal] = loadBitmapFromUrl(meal.mealThumb)
                                }
                            }
                        }
                        bitmaps.value = loadedBitmaps
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Search", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Display results with images inside cards
        if (mealsList.value.isNotEmpty()) {
            for (meal in mealsList.value) {
                MealCard(meal = meal, bitmap = bitmaps.value[meal.idMeal])
                Spacer(modifier = Modifier.height(20.dp))
            }
        } else if (resultsText.value.isNotEmpty()) {
            Text(
                text = resultsText.value,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Shared composable that displays a single meal inside a styled Card.
 * Used by both SearchMealsScreen and SearchWebScreen for consistency.
 */
@Composable
fun MealCard(meal: Meal, bitmap: Bitmap?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Meal image - centered with rounded corners
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = meal.name,
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Meal title - large and bold
            Text(
                text = meal.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Category and Area
            if (!meal.category.isNullOrEmpty() || !meal.area.isNullOrEmpty()) {
                Text(
                    text = "${meal.category ?: ""} | ${meal.area ?: ""}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Instructions section header
            if (!meal.instructions.isNullOrEmpty()) {
                Text(
                    text = "Instructions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Format instructions as numbered steps
                val steps = formatInstructionSteps(meal.instructions)
                for (step in steps) {
                    Text(
                        text = step,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Ingredients section header
            Text(
                text = "Ingredients",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Format ingredients as bullet list - each on new line
            val ingredientLines = getIngredientsList(meal)
            for (line in ingredientLines) {
                Text(
                    text = line,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }

            // Tags
            if (!meal.tags.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tags: ${meal.tags}",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Normalizes instructions text into numbered steps.
 * Splits on sentences and returns a list like ["Step 1: ...", "Step 2: ...", ...].
 */
fun formatInstructionSteps(instructions: String?): List<String> {
    if (instructions.isNullOrBlank()) return emptyList()
    var text = instructions
    // Remove existing step numbering patterns
    text = text.replace(Regex("(?i)step\\s*\\d+[:\\-.]?\\s*"), "")
    text = text.replace(Regex("^\\d+\\.\\s*", RegexOption.MULTILINE), "")
    // Split on period followed by space or newline
    val steps = text.split(Regex("(?<=\\.)\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.length > 2 }
    return steps.mapIndexed { index, step ->
        "Step ${index + 1}: $step"
    }
}

/**
 * Extracts non-empty ingredients with their measures as a bullet list.
 * Returns a list like ["- Pork (200g)", "- Egg (1)", ...].
 */
fun getIngredientsList(meal: Meal): List<String> {
    val ingredients = listOf(
        meal.ingredient1, meal.ingredient2, meal.ingredient3, meal.ingredient4,
        meal.ingredient5, meal.ingredient6, meal.ingredient7, meal.ingredient8,
        meal.ingredient9, meal.ingredient10, meal.ingredient11, meal.ingredient12,
        meal.ingredient13, meal.ingredient14, meal.ingredient15, meal.ingredient16,
        meal.ingredient17, meal.ingredient18, meal.ingredient19, meal.ingredient20
    )
    val measures = listOf(
        meal.measure1, meal.measure2, meal.measure3, meal.measure4,
        meal.measure5, meal.measure6, meal.measure7, meal.measure8,
        meal.measure9, meal.measure10, meal.measure11, meal.measure12,
        meal.measure13, meal.measure14, meal.measure15, meal.measure16,
        meal.measure17, meal.measure18, meal.measure19, meal.measure20
    )
    val result = mutableListOf<String>()
    for (i in ingredients.indices) {
        val ing = ingredients[i]
        if (!ing.isNullOrEmpty()) {
            val meas = measures[i]
            if (!meas.isNullOrEmpty()) {
                result.add("- $ing ($meas)")
            } else {
                result.add("- $ing")
            }
        }
    }
    return result
}
