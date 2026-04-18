package com.example.mealprep

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

/**
 * ViewModel for SearchMealsActivity.
 * Holds all UI state so it survives screen rotation.
 * ViewModel is NOT destroyed on configuration changes (unlike remember).
 */
class SearchMealsViewModel : ViewModel() {
    val searchText = mutableStateOf("")
    val resultsText = mutableStateOf("")
    val mealsList = mutableStateOf<List<Meal>>(emptyList())
    val bitmaps = mutableStateOf<Map<String, Bitmap?>>(emptyMap())
}

/**
 * ViewModel for SearchByIngredientActivity.
 * Holds all UI state so it survives screen rotation.
 */
class SearchByIngredientViewModel : ViewModel() {
    val ingredient = mutableStateOf("")
    val statusMessage = mutableStateOf("")
    val retrievedMeals = mutableStateOf<List<Meal>>(emptyList())
    val bitmaps = mutableStateOf<Map<String, Bitmap?>>(emptyMap())
}

/**
 * ViewModel for SearchWebActivity.
 * Holds all UI state so it survives screen rotation.
 */
class SearchWebViewModel : ViewModel() {
    val searchText = mutableStateOf("")
    val resultsText = mutableStateOf("")
    val statusMessage = mutableStateOf("")
    val mealsList = mutableStateOf<List<Meal>>(emptyList())
    val bitmaps = mutableStateOf<Map<String, Bitmap?>>(emptyMap())
}
