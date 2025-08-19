package com.example.cocktaildb.data.manager

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.service.CheckmarkFirebaseService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Define the type alias at the top level, not nested inside the object
typealias CheckmarkCallback = (Boolean) -> Unit

object CheckmarkManager {
    private val TAG = "CheckmarkManager"
    private val auth = FirebaseAuth.getInstance()
    private val checkmarkService = CheckmarkFirebaseService()
    private var checkmarkManagerJob: Job? = null

    // Cache of checkmarked cocktail IDs
    private val checkmarkedCocktailIds = mutableSetOf<String>()

    // Flag to track if we've loaded checkmarks from Firestore
    private var initialized = false

    fun isInitialized(): Boolean = initialized

    /**
     * Toggle checkmark status for a cocktail
     */
    fun toggleCheckmark(cocktail: Cocktail, callback: CheckmarkCallback) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User must be logged in to toggle checkmark")
            callback(false)
            return
        }

        val isCheckmarked = isCheckmarked(cocktail.idDrink)

        checkmarkManagerJob?.cancel()
        checkmarkManagerJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = if (isCheckmarked) {
                    // Remove from checkmarks
                    withContext(Dispatchers.IO) {
                        checkmarkService.removeCheckmark(currentUser.uid, cocktail.idDrink)
                    }
                    if (checkmarkedCocktailIds.contains(cocktail.idDrink)) {
                        checkmarkedCocktailIds.remove(cocktail.idDrink)
                    }
                    false
                } else {
                    // Add to checkmarks
                    withContext(Dispatchers.IO) {
                        checkmarkService.addCheckmark(currentUser.uid, cocktail.idDrink)
                    }
                    checkmarkedCocktailIds.add(cocktail.idDrink)
                    true
                }

                callback(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling checkmark", e)
                callback(isCheckmarked) // Return original state on error
            }
        }
    }

    /**
     * Check if a cocktail is checkmarked
     */
    fun isCheckmarked(cocktailId: String): Boolean {
        return checkmarkedCocktailIds.contains(cocktailId)
    }

    /**
     * Load all checkmarks from Firestore
     */
    fun loadCheckmarksFromFirestore(callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User must be logged in to load checkmarks")
            callback(false)
            return
        }

        checkmarkManagerJob?.cancel()
        checkmarkManagerJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Get user's checkmarks from Firestore
                val result = withContext(Dispatchers.IO) {
                    val service = CheckmarkFirebaseService()
                    service.getUserCheckmarks(currentUser.uid)
                }

                if (result.isSuccess) {
                    val checkmarks = result.getOrNull() ?: emptyList()
                    // Clear and rebuild cache
                    checkmarkedCocktailIds.clear()
                    checkmarks.forEach { checkmark ->
                        checkmarkedCocktailIds.add(checkmark.cocktailId)
                    }

                    initialized = true
                    callback(true)
                } else {
                    Log.e(TAG, "Failed to load checkmarks: ${result.exceptionOrNull()}")
                    callback(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading checkmarks", e)
                callback(false)
            }
        }
    }

    /**
     * Check the checkmark status in Firestore and update local cache
     */
    fun checkCheckmarkStatus(cocktailId: String, callback: CheckmarkCallback) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false)
            return
        }

        checkmarkManagerJob?.cancel()
        checkmarkManagerJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    checkmarkService.isCheckmarked(currentUser.uid, cocktailId)
                }

                if (result.isSuccess) {
                    val isCheckmarked = result.getOrNull() ?: false
                    if (isCheckmarked) {
                        checkmarkedCocktailIds.add(cocktailId)
                    } else {
                        checkmarkedCocktailIds.remove(cocktailId)
                    }
                    callback(isCheckmarked)
                } else {
                    callback(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking checkmark status", e)
                callback(false)
            }
        }
    }
}
