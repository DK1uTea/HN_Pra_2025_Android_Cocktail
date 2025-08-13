package com.example.cocktaildb.screen.profile

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

/**
 * Contract for the Profile screen using MVP pattern
 */
interface ProfileContract {

    /**
     * View interface for the Profile screen
     */
    interface View {
        /**
         * Display user profile data
         */
        fun showUserProfile(userName: String, userBio: String, profileImageUrl: String?)

        /**
         * Display a list of cocktails for the user
         */
        fun showUserCocktails(cocktails: List<Cocktail>)

        /**
         * Navigate to My Recipes screen
         */
        fun navigateToMyRecipes()

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
     * Presenter interface for the Profile screen
     */
    interface Presenter : BasePresenter<View> {
        /**
         * Load user profile data
         */
        fun loadUserProfile()

        /**
         * Load user's cocktails
         */
        fun loadUserCocktails()

        /**
         * Handle My Cocktails button click
         */
        fun onMyRecipesClicked()

        /**
         * Handle History button click
         */
        fun onHistoryClicked()
    }
}
