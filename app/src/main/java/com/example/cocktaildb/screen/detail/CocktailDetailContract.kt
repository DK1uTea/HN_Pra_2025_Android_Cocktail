package com.example.cocktaildb.screen.detail

import com.example.cocktaildb.data.model.Cocktail


interface CocktailDetailContract {

    interface View {

        fun updateBookmarkButtonState(isBookmarked: Boolean)


        fun showError(message: String)
    }


    interface Presenter {

        fun setView(view: View?)


        fun checkBookmarkStatus(cocktailId: String)


        fun toggleBookmark(cocktail: Cocktail)


        fun onStop()
    }
}
