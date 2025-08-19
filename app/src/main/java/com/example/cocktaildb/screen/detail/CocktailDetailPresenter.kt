package com.example.cocktaildb.screen.detail

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.service.CheckmarkFirebaseService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CocktailDetailPresenter : CocktailDetailContract.Presenter {

    private var view: CocktailDetailContract.View? = null
    private val checkmarkService = CheckmarkFirebaseService()
    private val auth = FirebaseAuth.getInstance()
    private var presenterJob: Job? = null
    private val TAG = "CocktailDetailPresenter"

    override fun setView(view: CocktailDetailContract.View?) {
        this.view = view
    }

    override fun toggleBookmark(cocktail: Cocktail) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            view?.showError("Please sign in to bookmark cocktails")
            return
        }

        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Check current bookmark status
                val isBookmarked = withContext(Dispatchers.IO) {
                    checkmarkService.isCheckmarked(currentUser.uid, cocktail.idDrink)
                }

                if (isBookmarked.isSuccess) {
                    if (isBookmarked.getOrNull() == true) {
                        // Remove bookmark
                        val result = withContext(Dispatchers.IO) {
                            checkmarkService.removeCheckmark(currentUser.uid, cocktail.idDrink)
                        }

                        if (result.isSuccess) {
                            view?.updateBookmarkButtonState(false)
                        } else {
                            view?.showError("Failed to remove bookmark")
                        }
                    } else {
                        // Add bookmark
                        val result = withContext(Dispatchers.IO) {
                            checkmarkService.addCheckmark(currentUser.uid, cocktail.idDrink)
                        }

                        if (result.isSuccess) {
                            view?.updateBookmarkButtonState(true)
                        } else {
                            view?.showError("Failed to add bookmark")
                        }
                    }
                } else {
                    view?.showError("Failed to check bookmark status")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling bookmark", e)
                view?.showError("Error updating bookmark status")
            }
        }
    }

    override fun checkBookmarkStatus(cocktailId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            view?.updateBookmarkButtonState(false)
            return
        }

        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val isBookmarked = withContext(Dispatchers.IO) {
                    checkmarkService.isCheckmarked(currentUser.uid, cocktailId)
                }

                if (isBookmarked.isSuccess) {
                    view?.updateBookmarkButtonState(isBookmarked.getOrNull() == true)
                } else {
                    view?.updateBookmarkButtonState(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking bookmark status", e)
                view?.updateBookmarkButtonState(false)
            }
        }
    }

    override fun onStop() {
        presenterJob?.cancel()
        presenterJob = null
        view = null
    }
}
