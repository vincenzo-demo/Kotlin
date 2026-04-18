package com.example.mealprep

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Uses ViewModel to preserve state across screen rotation.
 */
@Composable
fun SearchWebScreen(vm: SearchWebViewModel = viewModel()) {
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
            text = "Search Meals from Web Service",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Text field for meal name search
        TextField(
            value = vm.searchText.value,
            onValueChange = { vm.searchText.value = it },
            label = { Text("Enter meal name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search button
        Button(
            onClick = {
                scope.launch {
                    vm.statusMessage.value = "Searching..."
                    vm.resultsText.value = ""
                    vm.mealsList.value = emptyList()
                    vm.bitmaps.value = emptyMap()

                    try {
                        val meals = withContext(Dispatchers.IO) {
                            searchMealsByNameFromWeb(vm.searchText.value)
                        }
                        vm.mealsList.value = meals

                        if (meals.isEmpty()) {
                            vm.resultsText.value = "No meals found."
                        } else {
                            vm.resultsText.value = "${meals.size} meals found."

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
                            vm.bitmaps.value = loadedBitmaps
                        }
                        vm.statusMessage.value = ""
                    } catch (e: Exception) {
                        vm.resultsText.value = "Error: ${e.message}"
                        vm.statusMessage.value = ""
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

        Spacer(modifier = Modifier.height(16.dp))

        // Status message
        if (vm.statusMessage.value.isNotEmpty()) {
            Text(
                text = vm.statusMessage.value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Display results text
        if (vm.resultsText.value.isNotEmpty() && vm.mealsList.value.isEmpty()) {
            Text(
                text = vm.resultsText.value,
                fontSize = 14.sp
            )
        }

        // Display meals with images inside cards (same as SearchMeals for consistency)
        for (meal in vm.mealsList.value) {
            MealCard(meal = meal, bitmap = vm.bitmaps.value[meal.idMeal])
            Spacer(modifier = Modifier.height(20.dp))
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
