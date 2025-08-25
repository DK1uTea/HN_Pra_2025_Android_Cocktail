package com.example.cocktaildb.screen.favorites

import android.content.Context
import android.util.Log
import com.example.cocktaildb.data.manager.FavoritesManager
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.Favorite
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.service.FavoriteFirebaseService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class FavoritesPresenter(
    private val context: Context,
    private val cocktailRepository: CocktailRepository
) : FavoritesContract.Presenter {

    private var view: FavoritesContract.View? = null
    private val favoriteFirebaseService = FavoriteFirebaseService()
    private val auth = FirebaseAuth.getInstance()
    private var presenterJob: Job? = null
    private val TAG = "FavoritesPresenter"

    private var cachedFavorites: List<Cocktail> = emptyList()
    private var isLoading = false
    private var isProcessingFavorites = false

    companion object {
        private const val FAVORITES_PREFS = "favorites_prefs"
        private const val FAVORITES_KEY = "favorites_list"
        private const val MAX_FAVORITES_ITEMS = 100
        private const val SEPARATOR = "|||"
        private const val FIELD_SEPARATOR = ":::"
    }

    override fun setView(view: FavoritesContract.View?) {
        this.view = view
    }

    override fun onStart() {
    }

    override fun onStop() {
        presenterJob?.cancel()
    }

    override fun loadFavorites() {
        if (isLoading) return
        isLoading = true
        view?.displayLoading(true)

        val currentUser = auth.currentUser
        if (currentUser == null || !isNetworkAvailable()) {
            Log.d(TAG, "loadFavorites: No user or no network, loading from local only")
            loadFromLocalOnly()
            return
        }

        Log.d(TAG, "loadFavorites: Syncing Firebase with local for user ${currentUser.uid}")
        syncFirebaseWithLocal(currentUser.uid)
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as? android.net.ConnectivityManager
            val networkInfo = connectivityManager?.activeNetworkInfo
            networkInfo?.isConnected == true
        } catch (e: Exception) {
            false
        }
    }

    private fun syncFirebaseWithLocal(uid: String) {
        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val favoritesResult = withContext(Dispatchers.IO) {
                    cocktailRepository.getUserFavoritesFromFirebase(uid)
                }

                if (favoritesResult.isSuccess) {
                    val firebaseFavorites = favoritesResult.getOrNull() ?: emptyList()
                    val firebaseCocktails = withContext(Dispatchers.IO) {
                        firebaseFavorites.mapNotNull { favorite ->
                            cocktailRepository.getCocktailById(favorite.cocktailId)
                        }
                    }
                    val localFavorites = cocktailRepository.getFavoritesFromLocal(context)
                    val mergedFavorites = mutableListOf<Cocktail>()
                    val firebaseIds = firebaseCocktails.map { it.idDrink }.toSet()
                    mergedFavorites.addAll(firebaseCocktails)

                    val localOnlyFavorites = localFavorites.filter { it.idDrink !in firebaseIds }
                    mergedFavorites.addAll(localOnlyFavorites)

                    val finalFavorites = deduplicateFavorites(mergedFavorites)
                    cocktailRepository.saveFavoritesToLocal(context, finalFavorites)
                    cachedFavorites = finalFavorites
                    view?.displayLoading(false)
                    if (finalFavorites.isEmpty()) {
                        view?.displayEmptyState()
                    } else {
                        view?.displayFavorites(finalFavorites)
                    }

                    Log.d(TAG, "Synced ${firebaseCocktails.size} Firebase + ${localOnlyFavorites.size} local → ${finalFavorites.size} total favorites")
                } else {
                    loadFromLocalOnly()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing favorites, falling back to local", e)
                loadFromLocalOnly()
            } finally {
                isLoading = false
            }
        }
    }

    private fun loadFromLocalOnly() {
        try {
            val favorites = cocktailRepository.getFavoritesFromLocal(context)
            cachedFavorites = favorites
            view?.displayLoading(false)
            if (favorites.isEmpty()) {
                view?.displayEmptyState()
            } else {
                view?.displayFavorites(favorites)
            }
        } catch (e: Exception) {
            view?.displayLoading(false)
            view?.displayError("Error loading favorites: ${e.message}")
            view?.displayEmptyState()
        } finally {
            isLoading = false
        }
    }

    private fun saveFavoritesToLocal(favorites: List<Cocktail>) {
        try {
            val prefs = context.getSharedPreferences(FAVORITES_PREFS, Context.MODE_PRIVATE)
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
            prefs.edit().putString(FAVORITES_KEY, favoritesString).apply()
            Log.d(TAG, "Saved ${favorites.size} favorites to local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorites to local storage", e)
        }
    }

    private fun getFavoritesFromLocal(): List<Cocktail> {
        return try {
            val prefs = context.getSharedPreferences(FAVORITES_PREFS, Context.MODE_PRIVATE)
            val favoritesString = prefs.getString(FAVORITES_KEY, "") ?: ""

            if (favoritesString.isEmpty()) {
                Log.d(TAG, "No local favorites found")
                return emptyList()
            }

            val favorites = favoritesString.split(SEPARATOR).mapNotNull { cocktailString ->
                try {
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
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing cocktail from local storage: $cocktailString", e)
                    null
                }
            }

            Log.d(TAG, "Loaded ${favorites.size} favorites from local storage")
            favorites
        } catch (e: Exception) {
            Log.e(TAG, "Error loading favorites from local storage", e)
            emptyList()
        }
    }

    private fun deduplicateFavorites(favorites: List<Cocktail>): List<Cocktail> {
        val seen = mutableSetOf<String>()
        return favorites.filter { cocktail ->
            val id = cocktail.idDrink ?: return@filter false
            if (seen.contains(id)) {
                Log.d(TAG, "Removing duplicate favorite: ${cocktail.strDrink} (ID: $id)")
                false
            } else {
                seen.add(id)
                true
            }
        }
    }

    override fun addToFavorites(cocktail: Cocktail) {
        if (isProcessingFavorites) return
        isProcessingFavorites = true

        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    val localFavorites = getFavoritesFromLocal().toMutableList()
                    if (!localFavorites.any { it.idDrink == cocktail.idDrink }) {
                        localFavorites.add(cocktail)
                        saveFavoritesToLocal(localFavorites)
                        cachedFavorites = localFavorites
                        view?.displayFavorites(localFavorites)
                        view?.showSyncStatus("Added to local favorites")
                    }
                    return@launch
                }

                val result = withContext(Dispatchers.IO) {
                    favoriteFirebaseService.addFavorite(currentUser.uid, cocktail.idDrink)
                }

                if (result.isSuccess) {
                    val localFavorites = getFavoritesFromLocal().toMutableList()
                    if (!localFavorites.any { it.idDrink == cocktail.idDrink }) {
                        localFavorites.add(cocktail)
                        saveFavoritesToLocal(localFavorites)
                    }

                    cachedFavorites = localFavorites
                    view?.displayFavorites(localFavorites)
                    view?.showSyncStatus("Added to favorites")
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to add to favorites")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding to favorites", e)
                view?.displayError("Error adding to favorites: ${e.message}")
            } finally {
                isProcessingFavorites = false
            }
        }
    }

    override fun removeFromFavorites(cocktail: Cocktail) {
        if (isProcessingFavorites) return
        isProcessingFavorites = true

        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    val localFavorites = getFavoritesFromLocal().toMutableList()
                    localFavorites.removeAll { it.idDrink == cocktail.idDrink }
                    saveFavoritesToLocal(localFavorites)
                    cachedFavorites = localFavorites
                    if (localFavorites.isEmpty()) {
                        view?.displayEmptyState()
                    } else {
                        view?.displayFavorites(localFavorites)
                    }
                    view?.showSyncStatus("Removed from local favorites")
                    return@launch
                }

                val result = withContext(Dispatchers.IO) {
                    favoriteFirebaseService.removeFavorite(currentUser.uid, cocktail.idDrink)
                }

                if (result.isSuccess) {
                    val localFavorites = getFavoritesFromLocal().toMutableList()
                    localFavorites.removeAll { it.idDrink == cocktail.idDrink }
                    saveFavoritesToLocal(localFavorites)

                    cachedFavorites = localFavorites
                    if (localFavorites.isEmpty()) {
                        view?.displayEmptyState()
                    } else {
                        view?.displayFavorites(localFavorites)
                    }
                    view?.showSyncStatus("Removed from favorites")
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to remove from favorites")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from favorites", e)
                view?.displayError("Error removing from favorites: ${e.message}")
            } finally {
                isProcessingFavorites = false
            }
        }
    }

    override fun toggleFavorite(cocktail: Cocktail) {
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            cocktailRepository.toggleFavorite(context, cocktail) {
                loadFavorites()
            }
        }
    }

    override fun clearAllFavorites() {
        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentUser = auth.currentUser
                cocktailRepository.clearAllFavorites(context)

                if (currentUser != null) {
                    val result = cocktailRepository.clearAllFavoritesFromFirebase(currentUser.uid)
                    if (result.isSuccess) {
                        Log.d(TAG, "Cleared all favorites from Firebase and local storage")
                        view?.showSyncStatus("All favorites cleared")
                    } else {
                        Log.w(TAG, "Failed to clear Firebase favorites, but local cleared")
                        view?.showSyncStatus("Local favorites cleared")
                    }
                } else {
                    Log.d(TAG, "Cleared local favorites only (no user)")
                    view?.showSyncStatus("Local favorites cleared")
                }
                cachedFavorites = emptyList()
                view?.displayEmptyState()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing favorites", e)
                view?.displayError("Error clearing favorites: ${e.message}")
            }
        }
    }

    override fun syncFavoritesIfNeeded() {
        val currentUser = auth.currentUser
        if (currentUser != null && isNetworkAvailable()) {
            view?.showSyncStatus("Syncing favorites...")
            syncFirebaseWithLocal(currentUser.uid)
        } else {
            view?.showSyncStatus("Offline mode - showing local favorites")
        }
    }
}
