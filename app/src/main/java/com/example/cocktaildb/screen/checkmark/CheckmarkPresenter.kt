package com.example.cocktaildb.screen.checkmark

import android.content.Context
import android.util.Log
import com.example.cocktaildb.data.manager.CheckmarkManager
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class CheckmarkPresenter(
    private val context: Context,
    private val cocktailRepository: CocktailRepository
) : CheckmarkContract.Presenter {

    private var view: CheckmarkContract.View? = null
    private val auth = FirebaseAuth.getInstance()
    private var presenterJob: Job? = null
    private val TAG = "CheckmarkPresenter"

    private var cachedCheckmarks: List<Cocktail> = emptyList()
    private var isLoading = false
    private var isProcessingCheckmarks = false

    companion object {
        private const val CHECKMARKS_PREFS = "checkmarks_prefs"
        private const val CHECKMARKS_KEY = "checkmarks_list"
        private const val MAX_CHECKMARKS_ITEMS = 100
        private const val SEPARATOR = "|||"
        private const val FIELD_SEPARATOR = ":::"
    }

    override fun setView(view: CheckmarkContract.View?) {
        this.view = view
    }

    override fun onStart() {
    }

    override fun onStop() {
        presenterJob?.cancel()
    }

    override fun loadCheckmarks() {
        if (isLoading) return
        isLoading = true
        view?.displayLoading(true)

        val currentUser = auth.currentUser
        if (currentUser == null || !isNetworkAvailable()) {
            Log.d(TAG, "loadCheckmarks: No user or no network, loading from local only")
            loadFromLocalOnly()
            return
        }

        Log.d(TAG, "loadCheckmarks: Syncing Firebase with local for user ${currentUser.uid}")
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
                val checkmarksResult = withContext(Dispatchers.IO) {
                    cocktailRepository.getUserCheckmarksFromFirebase(uid)
                }

                if (checkmarksResult.isSuccess) {
                    val firebaseCheckmarks = checkmarksResult.getOrNull() ?: emptyList()
                    val firebaseCocktails = withContext(Dispatchers.IO) {
                        firebaseCheckmarks.mapNotNull { checkmark ->
                            cocktailRepository.getCocktailById(checkmark.cocktailId)
                        }
                    }
                    val localCheckmarks = cocktailRepository.getCheckmarksFromLocal(context)
                    val mergedCheckmarks = mutableListOf<Cocktail>()
                    val firebaseIds = firebaseCocktails.map { it.idDrink }.toSet()
                    mergedCheckmarks.addAll(firebaseCocktails)

                    val localOnlyCheckmarks = localCheckmarks.filter { it.idDrink !in firebaseIds }
                    mergedCheckmarks.addAll(localOnlyCheckmarks)

                    val finalCheckmarks = deduplicateCheckmarks(mergedCheckmarks)
                    cocktailRepository.saveCheckmarksToLocal(context, finalCheckmarks)
                    cachedCheckmarks = finalCheckmarks
                    view?.displayLoading(false)
                    view?.displayCheckmarks(finalCheckmarks)
                } else {
                    Log.e(TAG, "Failed to load checkmarks from Firebase: ${checkmarksResult.exceptionOrNull()}")
                    loadFromLocalOnly()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing checkmarks", e)
                loadFromLocalOnly()
            } finally {
                isLoading = false
            }
        }
    }

    private fun loadFromLocalOnly() {
        val localCheckmarks = cocktailRepository.getCheckmarksFromLocal(context)
        cachedCheckmarks = localCheckmarks
        view?.displayLoading(false)
        view?.displayCheckmarks(localCheckmarks)
        isLoading = false
    }

    private fun deduplicateCheckmarks(checkmarks: List<Cocktail>): List<Cocktail> {
        return checkmarks.distinctBy { it.idDrink }
    }

    override fun addToCheckmarks(cocktail: Cocktail) {
        if (isProcessingCheckmarks) return
        isProcessingCheckmarks = true

        val currentUser = auth.currentUser
        if (currentUser == null) {
            addToLocalCheckmarks(cocktail)
            isProcessingCheckmarks = false
            return
        }

        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    cocktailRepository.addCheckmarkToFirebase(currentUser.uid, cocktail.idDrink)
                }

                if (result.isSuccess) {
                    addToLocalCheckmarks(cocktail)
                    view?.showCheckmarkAdded(cocktail)
                } else {
                    Log.e(TAG, "Failed to add checkmark to Firebase: ${result.exceptionOrNull()}")
                    addToLocalCheckmarks(cocktail)
                    view?.showCheckmarkAdded(cocktail)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding checkmark", e)
                addToLocalCheckmarks(cocktail)
                view?.showCheckmarkAdded(cocktail)
            } finally {
                isProcessingCheckmarks = false
            }
        }
    }

    private fun addToLocalCheckmarks(cocktail: Cocktail) {
        if (!cachedCheckmarks.any { it.idDrink == cocktail.idDrink }) {
            cachedCheckmarks = cachedCheckmarks + cocktail
            cocktailRepository.saveCheckmarksToLocal(context, cachedCheckmarks)
        }
    }

    override fun removeFromCheckmarks(cocktail: Cocktail) {
        if (isProcessingCheckmarks) return
        isProcessingCheckmarks = true

        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    val localCheckmarks = cocktailRepository.getCheckmarksFromLocal(context).toMutableList()
                    localCheckmarks.removeAll { it.idDrink == cocktail.idDrink }
                    cocktailRepository.saveCheckmarksToLocal(context, localCheckmarks)
                    cachedCheckmarks = localCheckmarks
                    if (localCheckmarks.isEmpty()) {
                        view?.displayEmptyState()
                    } else {
                        view?.displayCheckmarks(localCheckmarks)
                    }
                    view?.showCheckmarkRemoved(cocktail)
                    return@launch
                }

                val result = withContext(Dispatchers.IO) {
                    cocktailRepository.removeCheckmarkFromFirebase(currentUser.uid, cocktail.idDrink)
                }

                if (result.isSuccess) {
                    val localCheckmarks = cocktailRepository.getCheckmarksFromLocal(context).toMutableList()
                    localCheckmarks.removeAll { it.idDrink == cocktail.idDrink }
                    cocktailRepository.saveCheckmarksToLocal(context, localCheckmarks)

                    cachedCheckmarks = localCheckmarks
                    if (localCheckmarks.isEmpty()) {
                        view?.displayEmptyState()
                    } else {
                        view?.displayCheckmarks(localCheckmarks)
                    }
                    view?.showCheckmarkRemoved(cocktail)
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to remove from checkmarks")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from checkmarks", e)
                view?.displayError("Error removing from checkmarks: ${e.message}")
            } finally {
                isProcessingCheckmarks = false
            }
        }
    }

    override fun toggleCheckmark(cocktail: Cocktail) {
        if (cachedCheckmarks.any { it.idDrink == cocktail.idDrink }) {
            removeFromCheckmarks(cocktail)
        } else {
            addToCheckmarks(cocktail)
        }
    }

    override fun syncCheckmarksIfNeeded() {
        val currentUser = auth.currentUser
        if (currentUser != null && isNetworkAvailable()) {
            syncFirebaseWithLocal(currentUser.uid)
        }
    }

    override fun clearAllCheckmarks() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            presenterJob?.cancel()
            presenterJob = CoroutineScope(Dispatchers.Main).launch {
                try {
                    val checkmarks = withContext(Dispatchers.IO) {
                        cocktailRepository.getUserCheckmarksFromFirebase(currentUser.uid)
                    }

                    if (checkmarks.isSuccess) {
                        val userCheckmarks = checkmarks.getOrNull() ?: emptyList()
                        userCheckmarks.forEach { checkmark ->
                            withContext(Dispatchers.IO) {
                                cocktailRepository.removeCheckmarkFromFirebase(currentUser.uid, checkmark.cocktailId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing checkmarks", e)
                }
            }
        }

        cachedCheckmarks = emptyList()
        cocktailRepository.saveCheckmarksToLocal(context, emptyList())
        view?.displayCheckmarks(emptyList())
    }
}
