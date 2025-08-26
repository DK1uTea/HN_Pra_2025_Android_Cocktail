package com.example.cocktaildb.screen.myrecipe

import com.example.cocktaildb.data.model.Recipe
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.RecipeFirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MyRecipePresenter(
    private val recipeFirebaseService: RecipeFirebaseService,
    private val authRepository: AuthRepository
) : MyRecipeContract.Presenter {

    private var view: MyRecipeContract.View? = null
    private var job: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isDataLoaded = false
    private var cachedRecipes: List<Recipe> = emptyList()

    override fun setView(view: MyRecipeContract.View?) {
        this.view = view
        if (view != null && isDataLoaded && cachedRecipes.isNotEmpty()) {
            view.showUserRecipes(cachedRecipes)
        }
    }

    override fun onStart() {
        if (isDataLoaded && cachedRecipes.isNotEmpty()) {
            view?.showUserRecipes(cachedRecipes)
        }
    }

    override fun onStop() {
        job?.cancel()
        view = null
    }

    override fun loadUserRecipes() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            view?.displayError("Please log in to view your recipes")
            return
        }

        job?.cancel()

        view?.displayLoading(true)

        job = coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    recipeFirebaseService.getUserRecipes(currentUser.uid)
                }

                result.fold(
                    onSuccess = { recipes ->
                        view?.displayLoading(false)
                        view?.showUserRecipes(recipes)
                        cachedRecipes = recipes
                        isDataLoaded = true
                    },
                    onFailure = { exception ->
                        view?.displayLoading(false)
                        view?.displayError("Failed to load recipes: ${exception.message}")
                        cachedRecipes = emptyList()
                        isDataLoaded = false
                    }
                )
            } catch (e: Exception) {
                view?.displayLoading(false)
                view?.displayError("Error loading recipes: ${e.message}")
                cachedRecipes = emptyList()
                isDataLoaded = false
            }
        }
    }

    override fun refreshUserRecipes() {
        isDataLoaded = false
        cachedRecipes = emptyList()
        loadUserRecipes()
    }
}
