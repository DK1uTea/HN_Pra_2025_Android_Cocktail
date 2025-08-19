package com.example.cocktaildb.screen.favorites

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.Favorite
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.service.FavoriteFirebaseService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class FavoritesPresenter : FavoritesContract.Presenter {

    private var view: FavoritesContract.View? = null
    private val favoriteFirebaseService = FavoriteFirebaseService()
    private val cocktailRepository = CocktailRepository()
    private val auth = FirebaseAuth.getInstance()
    private var presenterJob: Job? = null
    private val TAG = "FavoritesPresenter"

    private var cachedFavorites: List<Cocktail>? = null
    private var isLoading = false

    override fun setView(view: FavoritesContract.View?) {
        this.view = view
        // Show cached data only if we're not currently loading
        if (!isLoading) {
            cachedFavorites?.let { favorites ->
                view?.displayFavorites(favorites)
            }
        }
    }

    override fun onStart() {
        if (cachedFavorites == null) {
            loadFavorites()
        }
    }

    override fun onStop() {
        // Cancel only the current loading job if any
        presenterJob?.cancel()
        presenterJob = null
        view = null
    }

    override fun loadFavorites() {
        // Prevent multiple simultaneous loads
        if (isLoading) return

        isLoading = true
        view?.displayLoading(true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "loadFavorites: No current user found")
            handleNoUser()
            isLoading = false
            return
        }

        Log.d(TAG, "loadFavorites: Loading favorites for user ${currentUser.uid}")

        // Cancel any existing job before starting a new one
        presenterJob?.cancel()

        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val favoritesResult = withContext(Dispatchers.IO) {
                    favoriteFirebaseService.getUserFavorites(currentUser.uid)
                }

                if (favoritesResult.isSuccess) {
                    val favorites: List<Favorite> = favoritesResult.getOrNull() ?: emptyList()
                    val cocktails = withContext(Dispatchers.IO) {
                        favorites.mapNotNull { favorite ->
                            cocktailRepository.getCocktailById(favorite.cocktailId)
                        }
                    }

                    cachedFavorites = cocktails
                    view?.displayLoading(false)
                    if (cocktails.isEmpty()) {
                        view?.displayEmptyState()
                    } else {
                        view?.displayFavorites(cocktails)
                    }
                } else {
                    throw favoritesResult.exceptionOrNull() ?: Exception("Failed to load favorites")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading favorites", e)
                view?.displayLoading(false)
                view?.displayError("Failed to load favorites: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun handleNoUser() {
        view?.displayLoading(false)
        view?.displayEmptyState()
        view?.displayError("Please sign in to view favorites")
    }

    override fun addToFavorites(cocktail: Cocktail) {
        val currentUser = auth.currentUser ?: return

        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Add to favorites directly without checking or saving cocktail
                val result = withContext(Dispatchers.IO) {
                    favoriteFirebaseService.addFavorite(currentUser.uid, cocktail.idDrink)
                }

                if (result.isSuccess) {
                    view?.showFavoriteAdded(cocktail)
                    // Reload favorites to update the UI
                    loadFavorites()
                } else {
                    view?.displayError("Failed to add to favorites")
                }
            } catch (e: Exception) {
                view?.displayError("Error adding to favorites: ${e.message}")
            }
        }
    }

    override fun removeFromFavorites(cocktail: Cocktail) {
        val currentUser = auth.currentUser ?: return

        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Get the favorite document
                val favoriteResult = withContext(Dispatchers.IO) {
                    favoriteFirebaseService.getFavorite(currentUser.uid, cocktail.idDrink)
                }

                if (favoriteResult.isSuccess && favoriteResult.getOrNull() != null) {
                    // Remove from favorites
                    val result = withContext(Dispatchers.IO) {
                        favoriteFirebaseService.removeFavorite(currentUser.uid, cocktail.idDrink)
                    }

                    if (result.isSuccess && result.getOrNull() == true) {
                        view?.showFavoriteRemoved(cocktail)
                        // Reload favorites to update the UI
                        loadFavorites()
                    } else {
                        view?.displayError("Failed to remove from favorites")
                    }
                } else {
                    view?.displayError("Cocktail not found in favorites")
                }
            } catch (e: Exception) {
                view?.displayError("Error removing from favorites: ${e.message}")
            }
        }
    }

    override fun toggleFavorite(cocktail: Cocktail) {
        val currentUser = auth.currentUser ?: return

        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Check if the cocktail is already a favorite
                val isFavorite = withContext(Dispatchers.IO) {
                    val result = favoriteFirebaseService.isFavorite(currentUser.uid, cocktail.idDrink)
                    result.isSuccess && result.getOrNull() == true
                }

                if (isFavorite) {
                    removeFromFavorites(cocktail)
                } else {
                    addToFavorites(cocktail)
                }
            } catch (e: Exception) {
                view?.displayError("Error toggling favorite: ${e.message}")
            }
        }
    }
}
