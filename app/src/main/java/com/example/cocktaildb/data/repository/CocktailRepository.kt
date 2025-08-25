package com.example.cocktaildb.data.repository

import android.content.Context
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.source.CocktailDataSource
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource
import com.example.cocktaildb.data.manager.FavoritesManager
import com.example.cocktaildb.data.service.FavoriteFirebaseService
import com.example.cocktaildb.data.service.CheckmarkFirebaseService

class CocktailRepository(
    private val dataSource: CocktailDataSource = CocktailRemoteDataSource(),
    private val favoriteFirebaseService: FavoriteFirebaseService = FavoriteFirebaseService(),
    private val checkmarkFirebaseService: CheckmarkFirebaseService = CheckmarkFirebaseService()
) {

    fun getCocktails(): List<Cocktail> {
        return dataSource.getAllCocktails()
    }

    fun getAllCocktails(): List<Cocktail> {
        return dataSource.getAllCocktails()
    }

    fun getCocktailById(id: String): Cocktail? {
        return dataSource.getCocktailById(id)
    }

    suspend fun fetchCocktailsFromApi(): List<Cocktail> {
        return dataSource.fetchCocktailsFromApi()
    }

    fun searchCocktails(query: String): List<Cocktail> {
        return dataSource.searchCocktails(query)
    }

    fun filterByCategory(category: String): List<Cocktail> {
        return dataSource.filterByCategory(category)
    }

    fun filterByAlcoholic(alcoholic: String): List<Cocktail> {
        return dataSource.filterByAlcoholic(alcoholic)
    }

    fun getCategories(): List<String> {
        return dataSource.getCategories()
    }

    fun loadMoreCocktails(): List<Cocktail> {
        return dataSource.loadMoreCocktails()
    }
    
    fun getAlcoholicTypes(): List<String> {
        return dataSource.getAlcoholicTypes()
    }

    fun preloadOfflineFavorites(context: Context) {
        FavoritesManager.preloadOfflineFavorites(context)
    }

    fun getOfflineFavorites(context: Context): List<Cocktail> {
        return FavoritesManager.getOfflineFavorites(context)
    }

    fun getHistoryCocktails(context: Context): List<Cocktail> {
        val sharedPreferences = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
        val historyString = sharedPreferences.getString("cocktail_history", "") ?: ""
        return if (historyString.isEmpty()) {
            emptyList()
        } else {
            historyString.split("||").mapNotNull { entry ->
                val fields = entry.split("|:|")
                if (fields.size >= 3) {
                    Cocktail(idDrink = fields[0], strDrink = fields[1], strDrinkThumb = fields[2])
                } else null
            }
        }
    }

    fun getHistoryString(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
        return sharedPreferences.getString("cocktail_history", "") ?: ""
    }

    fun clearHistory(context: Context) {
        val sharedPreferences = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("cocktail_history").apply()
    }
    
    suspend fun toggleFavorite(context: Context, cocktail: Cocktail, callback: () -> Unit) {
        FavoritesManager.toggleFavoriteOfflineAware(context, cocktail) {
            callback()
        }
    }

    fun clearAllFavorites(context: Context) {
        val prefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    suspend fun clearAllFavoritesFromFirebase(uid: String): Result<Unit> {
        return try {
            val result = favoriteFirebaseService.clearUserFavorites(uid)
            if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFavoritesFromFirebase(uid: String): Result<List<com.example.cocktaildb.data.model.Favorite>> {
        return favoriteFirebaseService.getUserFavorites(uid)
    }

    fun getFavoritesFromLocal(context: Context): List<Cocktail> {
        val prefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
        val favoritesString = prefs.getString("favorites_list", "") ?: ""
        if (favoritesString.isEmpty()) return emptyList()
        val SEPARATOR = "|||"
        val FIELD_SEPARATOR = ":::"
        return favoritesString.split(SEPARATOR).mapNotNull { cocktailString ->
            val fields = cocktailString.split(FIELD_SEPARATOR)
            if (fields.size >= 9) {
                Cocktail(
                    idDrink = fields[0],
                    strDrink = fields[1],
                    strDrinkThumb = fields[2].ifBlank { null },
                    strCategory = fields[3].ifBlank { null },
                    strAlcoholic = fields[4].ifBlank { null },
                    strGlass = fields[5].ifBlank { null },
                    strInstructions = fields[6].ifBlank { null },
                    ingredients = if (fields[7].isNotBlank()) listOf(fields[7]) else emptyList(),
                    dateModified = fields[8].ifBlank { null }
                )
            } else null
        }
    }

    fun saveFavoritesToLocal(context: Context, favorites: List<Cocktail>) {
        val prefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
        val SEPARATOR = "|||"
        val FIELD_SEPARATOR = ":::"
        val MAX_FAVORITES_ITEMS = 100
        val favoritesString = favorites.take(MAX_FAVORITES_ITEMS).joinToString(SEPARATOR) { cocktail ->
            listOf(
                cocktail.idDrink,
                cocktail.strDrink,
                cocktail.strDrinkThumb ?: "",
                cocktail.strCategory ?: "",
                cocktail.strAlcoholic ?: "",
                cocktail.strGlass ?: "",
                cocktail.strInstructions ?: "",
                cocktail.ingredients.firstOrNull() ?: "",
                cocktail.dateModified ?: ""
            ).joinToString(FIELD_SEPARATOR)
        }
        prefs.edit().putString("favorites_list", favoritesString).apply()
    }

    // Checkmark functions (similar to favorites)
    suspend fun getUserCheckmarksFromFirebase(uid: String): Result<List<com.example.cocktaildb.data.model.Checkmark>> {
        return checkmarkFirebaseService.getUserCheckmarks(uid)
    }

    fun getCheckmarksFromLocal(context: Context): List<Cocktail> {
        val prefs = context.getSharedPreferences("checkmarks_prefs", Context.MODE_PRIVATE)
        val checkmarksString = prefs.getString("checkmarks_list", "") ?: ""
        if (checkmarksString.isEmpty()) return emptyList()
        val SEPARATOR = "|||"
        val FIELD_SEPARATOR = ":::"
        return checkmarksString.split(SEPARATOR).mapNotNull { cocktailString ->
            val fields = cocktailString.split(FIELD_SEPARATOR)
            if (fields.size >= 9) {
                Cocktail(
                    idDrink = fields[0],
                    strDrink = fields[1],
                    strDrinkThumb = fields[2].ifBlank { null },
                    strCategory = fields[3].ifBlank { null },
                    strAlcoholic = fields[4].ifBlank { null },
                    strGlass = fields[5].ifBlank { null },
                    strInstructions = fields[6].ifBlank { null },
                    ingredients = if (fields[7].isNotBlank()) listOf(fields[7]) else emptyList(),
                    dateModified = fields[8].ifBlank { null }
                )
            } else null
        }
    }

    fun saveCheckmarksToLocal(context: Context, checkmarks: List<Cocktail>) {
        val prefs = context.getSharedPreferences("checkmarks_prefs", Context.MODE_PRIVATE)
        val SEPARATOR = "|||"
        val FIELD_SEPARATOR = ":::"
        val MAX_CHECKMARKS_ITEMS = 100
        val checkmarksString = checkmarks.take(MAX_CHECKMARKS_ITEMS).joinToString(SEPARATOR) { cocktail ->
            listOf(
                cocktail.idDrink,
                cocktail.strDrink,
                cocktail.strDrinkThumb ?: "",
                cocktail.strCategory ?: "",
                cocktail.strAlcoholic ?: "",
                cocktail.strGlass ?: "",
                cocktail.strInstructions ?: "",
                cocktail.ingredients.firstOrNull() ?: "",
                cocktail.dateModified ?: ""
            ).joinToString(FIELD_SEPARATOR)
        }
        prefs.edit().putString("checkmarks_list", checkmarksString).apply()
    }

    suspend fun addCheckmarkToFirebase(uid: String, cocktailId: String): Result<Unit> {
        return try {
            val result = checkmarkFirebaseService.addCheckmark(uid, cocktailId)
            if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCheckmarkFromFirebase(uid: String, cocktailId: String): Result<Unit> {
        return try {
            val result = checkmarkFirebaseService.removeCheckmark(uid, cocktailId)
            if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllCheckmarksFromFirebase(uid: String): Result<Unit> {
        return try {
            val result = checkmarkFirebaseService.clearUserCheckmarks(uid)
            if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearAllCheckmarksFromLocal(context: Context) {
        val prefs = context.getSharedPreferences("checkmarks_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

}
