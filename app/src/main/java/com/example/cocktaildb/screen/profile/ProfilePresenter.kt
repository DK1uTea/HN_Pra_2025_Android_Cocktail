package com.example.cocktaildb.screen.profile

import android.os.AsyncTask
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository

/**
 * Presenter implementation for the Profile screen
 */
class ProfilePresenter(
    private val cocktailRepository: CocktailRepository
) : ProfileContract.Presenter {

    private var view: ProfileContract.View? = null

    override fun setView(view: ProfileContract.View?) {
        this.view = view
        if (view != null) {
            loadData()
        }
    }

    override fun onStart() {
        // This method is called when the view starts
        // If needed, refresh data here
    }

    override fun onStop() {
        // Clean up resources when the view stops
        this.view = null
    }

    override fun loadUserProfile() {
        // In a real app, this would fetch user data from a repository
        // For now, we'll use mock data
        view?.showUserProfile(
            userName = "John Doe",
            userBio = "Cocktail enthusiast and mixologist",
            profileImageUrl = null // Use default placeholder
        )
    }

    override fun loadUserCocktails() {
        view?.displayLoading(true)

        // Using AsyncTask instead of Coroutines
        CocktailLoadTask().execute()
    }

    override fun onMyRecipesClicked() {
        // Handle My Cocktails button click
        // This would typically navigate to a user's created cocktails screen
    }

    override fun onHistoryClicked() {
        // Handle History button click
        // This would typically navigate to a history screen
    }

    private fun loadData() {
        loadUserProfile()
        loadUserCocktails()
    }

    /**
     * AsyncTask to load cocktails in background
     */
    private inner class CocktailLoadTask : AsyncTask<Void, Void, List<Cocktail>>() {

        override fun doInBackground(vararg params: Void?): List<Cocktail> {
            // Get cocktails from repository (currently returns empty list)
            val repoList = cocktailRepository.getCocktails()

            // Since the repository returns empty list, use mock data instead
            return if (repoList.isEmpty()) createMockCocktails() else repoList
        }

        override fun onPostExecute(result: List<Cocktail>) {
            view?.displayLoading(false)
            view?.showUserCocktails(result)
        }

        override fun onCancelled() {
            view?.displayLoading(false)
            view?.displayError("Failed to load cocktails")
        }
    }

    /**
     * Creates mock cocktail data for display when repository is empty
     */
    private fun createMockCocktails(): List<Cocktail> {
        return listOf(
            Cocktail(
                id = "1",
                name = "Mojito",
                description = "Refreshing rum cocktail with mint and lime",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/metwgh1606770327.jpg"
            ),
            Cocktail(
                id = "2",
                name = "Margarita",
                description = "Classic tequila cocktail with lime",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg"
            ),
            Cocktail(
                id = "3",
                name = "Piña Colada",
                description = "Tropical rum cocktail with coconut and pineapple",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/cpf4j51504371346.jpg"
            ),
            Cocktail(
                id = "4",
                name = "Virgin Mojito",
                description = "Non-alcoholic version of the classic Mojito",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/vr6kle1504886114.jpg"
            )
        )
    }
}
