package com.example.mealprep

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Activity for searching meals by ingredient using TheMealDB API.
 * User enters an ingredient, retrieves meals, and can save them to the database.
 */
class SearchByIngredientActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SearchByIngredientScreen()
        }
    }
}

/**
 * Composable screen for searching meals by ingredient.
 * Contains a text field, Retrieve Meals button, and Save meals to Database button.
 * Uses ViewModel to preserve state across screen rotation.
 */
@Composable
fun SearchByIngredientScreen(vm: SearchByIngredientViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Search Meals By Ingredient",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Text field for ingredient input
        TextField(
            value = vm.ingredient.value,
            onValueChange = { vm.ingredient.value = it },
            label = { Text("Enter ingredient") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Row with two buttons
        Row {
            // Retrieve Meals button
            Button(
                onClick = {
                    scope.launch {
                        vm.statusMessage.value = "Searching..."
                        vm.retrievedMeals.value = emptyList()
                        vm.bitmaps.value = emptyMap()
                        try {
                            val meals = withContext(Dispatchers.IO) {
                                fetchMealsByIngredient(vm.ingredient.value)
                            }
                            vm.retrievedMeals.value = meals
                            if (meals.isEmpty()) {
                                vm.statusMessage.value = "No meals found."
                            } else {
                                vm.statusMessage.value = "${meals.size} meals found."
                                // Load images in background
                                val loadedBitmaps = mutableMapOf<String, Bitmap?>()
                                withContext(Dispatchers.IO) {
                                    for (meal in meals) {
                                        if (!meal.mealThumb.isNullOrEmpty()) {
                                            loadedBitmaps[meal.idMeal] = loadBitmapFromUrl(meal.mealThumb)
                                        }
                                    }
                                }
                                vm.bitmaps.value = loadedBitmaps
                            }
                        } catch (e: Exception) {
                            vm.statusMessage.value = "Error: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.padding(end = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retrieve Meals")
            }

            // Save meals to Database button
            Button(
                onClick = {
                    scope.launch {
                        if (vm.retrievedMeals.value.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                val db = MealDatabase.getDatabase(context)
                                db.mealDao().insertMeals(vm.retrievedMeals.value)
                            }
                            vm.statusMessage.value = "Meals saved to database!"
                        } else {
                            vm.statusMessage.value = "No meals to save. Retrieve meals first."
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save meals to Database")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status message
        if (vm.statusMessage.value.isNotEmpty()) {
            Text(
                text = vm.statusMessage.value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Display retrieved meals with Card UI + required text format
        if (vm.retrievedMeals.value.isNotEmpty()) {
            for (meal in vm.retrievedMeals.value) {
                // Pretty Card UI (same as SearchWebActivity) - includes image
                MealCard(meal = meal, bitmap = vm.bitmaps.value[meal.idMeal])

                // REQUIRED TEXT FORMAT (Task 3) - clean, inside a subtle card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Raw Data",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMealsForDisplay(listOf(meal)),
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Fetches meals by ingredient from TheMealDB API.
 * First gets meal IDs from filter endpoint, then gets full details for each meal.
 * Uses HttpURLConnection and JSONObject for parsing as required.
 */
fun fetchMealsByIngredient(ingredient: String): List<Meal> {
    val meals = mutableListOf<Meal>()

    // Step 1: Get meal IDs using the filter endpoint
    val filterUrl = "https://www.themealdb.com/api/json/v1/1/filter.php?i=$ingredient"
    val filterConnection = URL(filterUrl).openConnection() as HttpURLConnection
    filterConnection.requestMethod = "GET"
    filterConnection.connectTimeout = 10000
    filterConnection.readTimeout = 10000

    val filterResponse = StringBuilder()
    val filterReader = BufferedReader(InputStreamReader(filterConnection.inputStream))
    var line: String?
    while (filterReader.readLine().also { line = it } != null) {
        filterResponse.append(line)
    }
    filterReader.close()
    filterConnection.disconnect()

    val filterJson = JSONObject(filterResponse.toString())
    if (filterJson.isNull("meals")) {
        return emptyList()
    }

    val mealsArray = filterJson.getJSONArray("meals")

    // Step 2: For each meal, get full details using lookup endpoint
    for (i in 0 until mealsArray.length()) {
        val mealObj = mealsArray.getJSONObject(i)
        val mealId = mealObj.getString("idMeal")

        // Lookup full meal details
        val lookupUrl = "https://www.themealdb.com/api/json/v1/1/lookup.php?i=$mealId"
        val lookupConnection = URL(lookupUrl).openConnection() as HttpURLConnection
        lookupConnection.requestMethod = "GET"
        lookupConnection.connectTimeout = 10000
        lookupConnection.readTimeout = 10000

        val lookupResponse = StringBuilder()
        val lookupReader = BufferedReader(InputStreamReader(lookupConnection.inputStream))
        while (lookupReader.readLine().also { line = it } != null) {
            lookupResponse.append(line)
        }
        lookupReader.close()
        lookupConnection.disconnect()

        val lookupJson = JSONObject(lookupResponse.toString())
        val detailArray = lookupJson.getJSONArray("meals")
        val detail = detailArray.getJSONObject(0)

        val meal = parseMealFromJson(detail)
        meals.add(meal)
    }

    return meals
}

/**
 * Helper function to safely get a nullable String from a JSONObject.
 * Returns null if the key is missing, the value is JSONObject.NULL, or the string is empty.
 * This avoids the issue where optString returns the string "null" instead of actual null.
 */
fun JSONObject.getNullableString(key: String): String? {
    if (!this.has(key) || this.isNull(key)) return null
    val value = this.getString(key)
    return if (value.isNullOrEmpty() || value == "null") null else value
}

/**
 * Parses a single meal from a JSONObject into a Meal data class.
 * Uses getNullableString helper to properly handle null JSON values.
 */
fun parseMealFromJson(json: JSONObject): Meal {
    return Meal(
        idMeal = json.getString("idMeal"),
        name = json.optString("strMeal", ""),
        drinkAlternate = json.getNullableString("strDrinkAlternate"),
        category = json.getNullableString("strCategory"),
        area = json.getNullableString("strArea"),
        instructions = json.getNullableString("strInstructions"),
        mealThumb = json.getNullableString("strMealThumb"),
        tags = json.getNullableString("strTags"),
        youtube = json.getNullableString("strYoutube"),
        ingredient1 = json.getNullableString("strIngredient1"),
        ingredient2 = json.getNullableString("strIngredient2"),
        ingredient3 = json.getNullableString("strIngredient3"),
        ingredient4 = json.getNullableString("strIngredient4"),
        ingredient5 = json.getNullableString("strIngredient5"),
        ingredient6 = json.getNullableString("strIngredient6"),
        ingredient7 = json.getNullableString("strIngredient7"),
        ingredient8 = json.getNullableString("strIngredient8"),
        ingredient9 = json.getNullableString("strIngredient9"),
        ingredient10 = json.getNullableString("strIngredient10"),
        ingredient11 = json.getNullableString("strIngredient11"),
        ingredient12 = json.getNullableString("strIngredient12"),
        ingredient13 = json.getNullableString("strIngredient13"),
        ingredient14 = json.getNullableString("strIngredient14"),
        ingredient15 = json.getNullableString("strIngredient15"),
        ingredient16 = json.getNullableString("strIngredient16"),
        ingredient17 = json.getNullableString("strIngredient17"),
        ingredient18 = json.getNullableString("strIngredient18"),
        ingredient19 = json.getNullableString("strIngredient19"),
        ingredient20 = json.getNullableString("strIngredient20"),
        measure1 = json.getNullableString("strMeasure1"),
        measure2 = json.getNullableString("strMeasure2"),
        measure3 = json.getNullableString("strMeasure3"),
        measure4 = json.getNullableString("strMeasure4"),
        measure5 = json.getNullableString("strMeasure5"),
        measure6 = json.getNullableString("strMeasure6"),
        measure7 = json.getNullableString("strMeasure7"),
        measure8 = json.getNullableString("strMeasure8"),
        measure9 = json.getNullableString("strMeasure9"),
        measure10 = json.getNullableString("strMeasure10"),
        measure11 = json.getNullableString("strMeasure11"),
        measure12 = json.getNullableString("strMeasure12"),
        measure13 = json.getNullableString("strMeasure13"),
        measure14 = json.getNullableString("strMeasure14"),
        measure15 = json.getNullableString("strMeasure15"),
        measure16 = json.getNullableString("strMeasure16"),
        measure17 = json.getNullableString("strMeasure17"),
        measure18 = json.getNullableString("strMeasure18"),
        measure19 = json.getNullableString("strMeasure19"),
        measure20 = json.getNullableString("strMeasure20")
    )
}

/**
 * Formats a list of meals into a readable string for display.
 * Shows all meal details in the format specified by the assignment.
 */
fun formatMealsForDisplay(meals: List<Meal>): String {
    val sb = StringBuilder()
    for (meal in meals) {
        sb.appendLine("\"Meal\":\"${meal.name}\",")
        sb.appendLine("\"DrinkAlternate\":${if (meal.drinkAlternate.isNullOrEmpty()) "null" else "\"${meal.drinkAlternate}\""},")
        sb.appendLine("\"Category\":\"${meal.category ?: ""}\",")
        sb.appendLine("\"Area\":\"${meal.area ?: ""}\",")
        sb.appendLine("\"Instructions\":\"${meal.instructions ?: ""}\",")
        sb.appendLine("\"Tags\":${if (meal.tags.isNullOrEmpty()) "null" else "\"${meal.tags}\""},")
        sb.appendLine("\"Youtube\":\"${meal.youtube ?: ""}\",")

        // Display all non-empty ingredients
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

        sb.appendLine() // blank line between meals
    }
    return sb.toString()
}

/**
 * Downloads an image from a URL and returns it as a Bitmap.
 * Uses standard HttpURLConnection as required (no Glide/Coil).
 */
fun loadBitmapFromUrl(urlString: String): Bitmap? {
    return try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val inputStream = connection.inputStream
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        connection.disconnect()
        bitmap
    } catch (e: Exception) {
        null
    }
}
