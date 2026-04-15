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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
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
    // Store meals and their loaded bitmaps for display
    val mealsList = remember { mutableStateOf<List<Meal>>(emptyList()) }
    val bitmaps = remember { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Search for Meals",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Text field for search input
        TextField(
            value = searchText.value,
            onValueChange = { searchText.value = it },
            label = { Text("Enter search text") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                        resultsText.value = formatMealsForDisplay(meals)

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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display results with images
        if (mealsList.value.isNotEmpty()) {
            for (meal in mealsList.value) {
                // Show meal image if available
                val bitmap = bitmaps.value[meal.idMeal]
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = meal.name,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Show meal details
                Text(
                    text = buildMealDetailString(meal),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
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
 * Builds a formatted string with all details of a single meal.
 */
fun buildMealDetailString(meal: Meal): String {
    val sb = StringBuilder()
    sb.appendLine("\"Meal\":\"${meal.name}\",")
    sb.appendLine("\"DrinkAlternate\":${if (meal.drinkAlternate.isNullOrEmpty()) "null" else "\"${meal.drinkAlternate}\""},")
    sb.appendLine("\"Category\":\"${meal.category ?: ""}\",")
    sb.appendLine("\"Area\":\"${meal.area ?: ""}\",")
    sb.appendLine("\"Instructions\":\"${meal.instructions ?: ""}\",")
    sb.appendLine("\"Tags\":${if (meal.tags.isNullOrEmpty()) "null" else "\"${meal.tags}\""},")
    sb.appendLine("\"Youtube\":\"${meal.youtube ?: ""}\",")

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

    for (j in ingredients.indices) {
        val ing = ingredients[j]
        if (!ing.isNullOrEmpty()) {
            sb.appendLine("\"Ingredient${j + 1}\":\"$ing\",")
        }
    }
    for (j in measures.indices) {
        val meas = measures[j]
        if (!meas.isNullOrEmpty()) {
            sb.appendLine("\"Measure${j + 1}\":\"$meas\",")
        }
    }

    return sb.toString()
}
