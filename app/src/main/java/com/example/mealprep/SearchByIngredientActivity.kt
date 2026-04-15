package com.example.mealprep

import android.graphics.BitmapFactory
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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.remember

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
 */
@Composable
fun SearchByIngredientScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ingredient = rememberSaveable { mutableStateOf("") }
    val mealsText = rememberSaveable { mutableStateOf("") }
    val statusMessage = rememberSaveable { mutableStateOf("") }
    // Store retrieved meals for saving to DB (not saveable, will be re-fetched on rotation if needed)
    val retrievedMeals = remember { mutableStateOf<List<Meal>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Search Meals By Ingredient",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Text field for ingredient input
        TextField(
            value = ingredient.value,
            onValueChange = { ingredient.value = it },
            label = { Text("Enter ingredient") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row with two buttons
        Row {
            // Retrieve Meals button
            Button(
                onClick = {
                    scope.launch {
                        statusMessage.value = "Searching..."
                        mealsText.value = ""
                        try {
                            val meals = withContext(Dispatchers.IO) {
                                fetchMealsByIngredient(ingredient.value)
                            }
                            retrievedMeals.value = meals
                            if (meals.isEmpty()) {
                                mealsText.value = "No meals found."
                            } else {
                                mealsText.value = formatMealsForDisplay(meals)
                            }
                            statusMessage.value = ""
                        } catch (e: Exception) {
                            mealsText.value = "Error: ${e.message}"
                            statusMessage.value = ""
                        }
                    }
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Retrieve Meals")
            }

            // Save meals to Database button
            Button(
                onClick = {
                    scope.launch {
                        if (retrievedMeals.value.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                val db = MealDatabase.getDatabase(context)
                                db.mealDao().insertMeals(retrievedMeals.value)
                            }
                            statusMessage.value = "Meals saved to database!"
                        } else {
                            statusMessage.value = "No meals to save. Retrieve meals first."
                        }
                    }
                }
            ) {
                Text("Save meals to Database")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status message
        if (statusMessage.value.isNotEmpty()) {
            Text(
                text = statusMessage.value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Display retrieved meals
        if (mealsText.value.isNotEmpty()) {
            Text(
                text = mealsText.value,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
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
 * Parses a single meal from a JSONObject into a Meal data class.
 * Handles null values using optString.
 */
fun parseMealFromJson(json: JSONObject): Meal {
    return Meal(
        idMeal = json.getString("idMeal"),
        name = json.optString("strMeal", ""),
        drinkAlternate = json.optString("strDrinkAlternate", null),
        category = json.optString("strCategory", null),
        area = json.optString("strArea", null),
        instructions = json.optString("strInstructions", null),
        mealThumb = json.optString("strMealThumb", null),
        tags = json.optString("strTags", null),
        youtube = json.optString("strYoutube", null),
        ingredient1 = json.optString("strIngredient1", null),
        ingredient2 = json.optString("strIngredient2", null),
        ingredient3 = json.optString("strIngredient3", null),
        ingredient4 = json.optString("strIngredient4", null),
        ingredient5 = json.optString("strIngredient5", null),
        ingredient6 = json.optString("strIngredient6", null),
        ingredient7 = json.optString("strIngredient7", null),
        ingredient8 = json.optString("strIngredient8", null),
        ingredient9 = json.optString("strIngredient9", null),
        ingredient10 = json.optString("strIngredient10", null),
        ingredient11 = json.optString("strIngredient11", null),
        ingredient12 = json.optString("strIngredient12", null),
        ingredient13 = json.optString("strIngredient13", null),
        ingredient14 = json.optString("strIngredient14", null),
        ingredient15 = json.optString("strIngredient15", null),
        ingredient16 = json.optString("strIngredient16", null),
        ingredient17 = json.optString("strIngredient17", null),
        ingredient18 = json.optString("strIngredient18", null),
        ingredient19 = json.optString("strIngredient19", null),
        ingredient20 = json.optString("strIngredient20", null),
        measure1 = json.optString("strMeasure1", null),
        measure2 = json.optString("strMeasure2", null),
        measure3 = json.optString("strMeasure3", null),
        measure4 = json.optString("strMeasure4", null),
        measure5 = json.optString("strMeasure5", null),
        measure6 = json.optString("strMeasure6", null),
        measure7 = json.optString("strMeasure7", null),
        measure8 = json.optString("strMeasure8", null),
        measure9 = json.optString("strMeasure9", null),
        measure10 = json.optString("strMeasure10", null),
        measure11 = json.optString("strMeasure11", null),
        measure12 = json.optString("strMeasure12", null),
        measure13 = json.optString("strMeasure13", null),
        measure14 = json.optString("strMeasure14", null),
        measure15 = json.optString("strMeasure15", null),
        measure16 = json.optString("strMeasure16", null),
        measure17 = json.optString("strMeasure17", null),
        measure18 = json.optString("strMeasure18", null),
        measure19 = json.optString("strMeasure19", null),
        measure20 = json.optString("strMeasure20", null)
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
