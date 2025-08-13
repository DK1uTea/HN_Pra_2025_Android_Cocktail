package com.example.cocktaildb.screen.myrecipe

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

/**
 * Contract for the My Recipes screen using MVP pattern
 */
interface MyRecipeContract {

    /**
     * View interface for the My Recipes screen
     */
    interface View {
        /**
         * Display a list of user-created recipes
         */
        fun showUserRecipes(cocktails: List<Cocktail>)

        /**
         * Show loading indicator
         */
        fun displayLoading(show: Boolean)

        /**
         * Show error message
         */
        fun displayError(message: String)
    }

    /**
     * Presenter interface for the My Recipes screen
     */
    interface Presenter : BasePresenter<View> {
        /**
         * Load user's created recipes
         */
        fun loadUserRecipes()
    }
}
