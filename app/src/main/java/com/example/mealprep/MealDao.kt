package com.example.mealprep

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for Meal entity.
 * Provides methods to insert and search meals in the database.
 */
@Dao
interface MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<Meal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal)

    @Query("SELECT * FROM meals")
    suspend fun getAllMeals(): List<Meal>

    /**
     * Search meals by name or any ingredient field.
     * Case insensitive, partial match using LIKE.
     */
    @Query("""
        SELECT * FROM meals WHERE 
        name LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient1 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient2 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient3 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient4 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient5 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient6 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient7 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient8 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient9 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient10 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient11 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient12 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient13 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient14 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient15 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient16 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient17 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient18 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient19 LIKE '%' || :searchText || '%' COLLATE NOCASE
        OR ingredient20 LIKE '%' || :searchText || '%' COLLATE NOCASE
    """)
    suspend fun searchMeals(searchText: String): List<Meal>
}
