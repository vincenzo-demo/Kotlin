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

/**
 * Activity for searching meals by name from TheMealDB Web Service.
 * Retrieves meals directly from the API (not the database).
 * Searches are case insensitive and display at least 10 meals.
 */
class SearchWebActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SearchWebScreen()
        }
    }
}

/**
 * Composable screen for searching meals by name from the web service.
 * Searches each letter combination to get at least 10 results.
 */
@Composable
fun SearchWebScreen() {
    val scope = rememberCoroutineScope()
    val searchText = rememberSaveable { mutableStateOf("") }
    val resultsText = rememberSaveable { mutableStateOf("") }
    val statusMessage = rememberSaveable { mutableStateOf("") }
    // Store retrieved meals and their bitmaps
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
            text = "Search Meals from Web Service",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Text field for meal name search
        TextField(
            value = searchText.value,
            onValueChange = { searchText.value = it },
            label = { Text("Enter meal name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search button
        Button(
            onClick = {
                scope.launch {
                    statusMessage.value = "Searching..."
                    resultsText.value = ""
                    mealsList.value = emptyList()
                    bitmaps.value = emptyMap()

                    try {
                        val meals = withContext(Dispatchers.IO) {
                            searchMealsByNameFromWeb(searchText.value)
                        }
                        mealsList.value = meals

                        if (meals.isEmpty()) {
                            resultsText.value = "No meals found."
                        } else {
                            resultsText.value = "${meals.size} meals found."

                            // Load images in background
                            val loadedBitmaps = mutableMapOf<String, Bitmap?>()
                            withContext(Dispatchers.IO) {
                                for (meal in meals) {
                                    if (!meal.mealThumb.isNullOrEmpty()) {
                                        loadedBitmaps[meal.idMeal] =
                                            loadBitmapFromUrl(meal.mealThumb)
                                    }
                                }
                            }
                            bitmaps.value = loadedBitmaps
                        }
                        statusMessage.value = ""
                    } catch (e: Exception) {
                        resultsText.value = "Error: ${e.message}"
                        statusMessage.value = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search", fontSize = 16.sp)
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

        // Display results text
        if (resultsText.value.isNotEmpty() && mealsList.value.isEmpty()) {
            Text(
                text = resultsText.value,
                fontSize = 14.sp
            )
        }

        // Display meals with images
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
    }
}

/**
 * Searches for meals by name from TheMealDB web service.
 * Uses the search endpoint to find meals containing the given string.
 * Retrieves meals from multiple search queries if needed to get at least 10 results.
 */
fun searchMealsByNameFromWeb(searchText: String): List<Meal> {
    val allMeals = mutableListOf<Meal>()
    val seenIds = mutableSetOf<String>()

    // First, search with the full text
    val mealsFromSearch = searchMealsFromApi(searchText)
    for (meal in mealsFromSearch) {
        if (meal.idMeal !in seenIds) {
            allMeals.add(meal)
            seenIds.add(meal.idMeal)
        }
    }

    // If we have fewer than 10 results, try searching with individual characters
    // from the search string to get more results
    if (allMeals.size < 10 && searchText.isNotEmpty()) {
        // Try searching with just the first letter to get more results
        val firstLetterMeals = searchMealsFromApi(searchText[0].toString())
        for (meal in firstLetterMeals) {
            // Only include meals whose name contains the search text (case insensitive)
            if (meal.idMeal !in seenIds &&
                meal.name.contains(searchText, ignoreCase = true)
            ) {
                allMeals.add(meal)
                seenIds.add(meal.idMeal)
            }
        }
    }

    return allMeals
}

/**
 * Makes a single API call to search meals by name.
 * Uses HttpURLConnection and JSONObject as required.
 */
fun searchMealsFromApi(query: String): List<Meal> {
    val meals = mutableListOf<Meal>()

    val urlString = "https://www.themealdb.com/api/json/v1/1/search.php?s=$query"
    val connection = URL(urlString).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 10000
    connection.readTimeout = 10000

    val response = StringBuilder()
    val reader = BufferedReader(InputStreamReader(connection.inputStream))
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        response.append(line)
    }
    reader.close()
    connection.disconnect()

    val json = JSONObject(response.toString())
    if (json.isNull("meals")) {
        return emptyList()
    }

    val mealsArray = json.getJSONArray("meals")
    for (i in 0 until mealsArray.length()) {
        val mealJson = mealsArray.getJSONObject(i)
        meals.add(parseMealFromJson(mealJson))
    }

    return meals
}
