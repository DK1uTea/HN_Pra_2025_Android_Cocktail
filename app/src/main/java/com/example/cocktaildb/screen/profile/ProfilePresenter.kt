package com.example.cocktaildb.screen.profile

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.content.Context
import android.content.SharedPreferences
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.User
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import androidx.core.content.edit


class ProfilePresenter(
    private val cocktailRepository: CocktailRepository,
    private val authRepository: AuthRepository,
    private val context: Context
) : ProfileContract.Presenter {

    private var view: ProfileContract.View? = null
    private val prefs: SharedPreferences by lazy { context.getSharedPreferences("user_profile_cache", Context.MODE_PRIVATE) }
    private val gson = Gson()

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
        view?.displayLoading(true)

        // Get the current logged-in user
        val currentUser = authRepository.getCurrentUser()

        if (currentUser != null) {
            try {
                // Get uid of the current user
                val uid = currentUser.uid

                // Fetch user profile data from Firestore
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        view?.displayLoading(false)
                        if (document != null && document.exists()) {
                            val user = document.toObject(User::class.java)
                            if (user != null) {
                                // Cache the user profile locally
                                try {
                                    prefs.edit { putString(uid, gson.toJson(user)) }
                                } catch (_: Exception) {}

                                view?.showUserProfile(
                                    userName = user.name.ifBlank { "User" },
                                    userBio = user.email,
                                    profileImageUrl = user.profileImage
                                )
                            } else {
                                showDefaultProfile()
                            }
                        } else {
                            showDefaultProfile()
                        }
                    }
                    .addOnFailureListener { e ->
                        view?.displayLoading(false)
                        // Try to load from cache on failure/offline
                        val cachedJson = prefs.getString(uid, null)
                        if (cachedJson != null) {
                            try {
                                val cachedUser = gson.fromJson(cachedJson, User::class.java)
                                view?.showUserProfile(
                                    userName = cachedUser.name.ifBlank { "User" },
                                    userBio = cachedUser.email,
                                    profileImageUrl = cachedUser.profileImage
                                )
                                return@addOnFailureListener
                            } catch (_: Exception) {}
                        }
                        view?.displayError("Error loading profile: ${e.message}")
                        showDefaultProfile()
                    }
            } catch (e: Exception) {
                view?.displayLoading(false)
                view?.displayError("Error accessing user data: ${e.message}")
                // Display default profile information on exception
                view?.showUserProfile(
                    userName = "User",
                    userBio = "Error loading profile",
                    profileImageUrl = null
                )
            }
        } else {
            // No user is logged in
            view?.displayLoading(false)
            view?.showUserProfile(
                userName = "Guest User",
                userBio = "Please sign in to see your profile",
                profileImageUrl = null
            )
        }
    }

    override fun loadUserCocktails() {
        view?.displayLoading(true)
        @Suppress("DEPRECATION")
        CocktailLoadTask().execute()
    }

    override fun onMyRecipesClicked() {
        // Navigate to My Recipes screen
        view?.navigateToMyRecipes()
    }

    override fun onHistoryClicked() {
        // Navigate to History screen
        view?.navigateToHistory()
    }

    override fun onLogoutClicked() {
        // Call the AuthRepository's signOut method to log the user out
        authRepository.signOut()

        // Navigate to login screen
        view?.navigateToLogin()
    }

    private fun loadData() {
        loadUserProfile()
        loadUserCocktails()
    }


    @SuppressLint("StaticFieldLeak")
    @Suppress("DEPRECATION")
    private inner class CocktailLoadTask : AsyncTask<Void, Void, List<Cocktail>>() {

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): List<Cocktail> {
            // Get cocktails from repository (currently returns empty list)
            val repoList = cocktailRepository.getCocktails()

            // Since the repository returns empty list, use mock data instead
            return if (repoList.isEmpty()) createMockCocktails() else repoList
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: List<Cocktail>) {
            view?.displayLoading(false)
            view?.showUserCocktails(result)
        }

        @Deprecated("Deprecated in Java")
        override fun onCancelled() {
            view?.displayLoading(false)
            view?.displayError("Failed to load cocktails")
        }
    }


    private fun showDefaultProfile() {
        view?.showUserProfile(
            userName = "User",
            userBio = "No profile information",
            profileImageUrl = null
        )
    }


    private fun createMockCocktails(): List<Cocktail> {
        return listOf(
            Cocktail(
                idDrink = "1",
                strDrink = "Mojito",
                strCategory = "Cocktail",
                strAlcoholic = "Alcoholic",
                strGlass = "Highball glass",
                strInstructions = "Muddle mint leaves with sugar and lime juice. Add a splash of soda water and fill the glass with cracked ice. Pour the rum and top with soda water. Garnish and serve with straws.",
                strDrinkThumb = "https://www.thecocktaildb.com/images/media/drink/metwgh1606770327.jpg",
                ingredients = listOf("White rum", "Lime juice", "Sugar", "Mint", "Soda water"),
                measures = listOf("2 oz", "1 oz", "1 tsp", "6 leaves", "Top up")
            ),
            Cocktail(
                idDrink = "2",
                strDrink = "Margarita",
                strCategory = "Cocktail",
                strAlcoholic = "Alcoholic",
                strGlass = "Cocktail glass",
                strInstructions = "Rub the rim of the glass with the lime slice to make the salt stick to it. Take care to moisten only the outer rim and sprinkle the salt on it. The salt should present to the lips of the imbiber and never mix into the cocktail. Shake the other ingredients with ice, then carefully pour into the glass.",
                strDrinkThumb = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg",
                ingredients = listOf("Tequila", "Triple sec", "Lime juice", "Salt"),
                measures = listOf("1 1/2 oz", "1/2 oz", "1 oz", "For rim")
            ),
            Cocktail(
                idDrink = "3",
                strDrink = "Piña Colada",
                strCategory = "Cocktail",
                strAlcoholic = "Alcoholic",
                strGlass = "Hurricane glass",
                strInstructions = "Mix all ingredients in a blender until smooth. Pour into a chilled glass and garnish with a pineapple slice and cherry.",
                strDrinkThumb = "https://www.thecocktaildb.com/images/media/drink/cpf4j51504371346.jpg",
                ingredients = listOf("White rum", "Coconut cream", "Pineapple juice"),
                measures = listOf("2 oz", "2 oz", "3 oz")
            ),
            Cocktail(
                idDrink = "4",
                strDrink = "Virgin Mojito",
                strCategory = "Cocktail",
                strAlcoholic = "Non alcoholic",
                strGlass = "Highball glass",
                strInstructions = "Muddle mint leaves with sugar and lime juice. Add a splash of soda water and fill the glass with cracked ice. Top with soda water. Garnish and serve with straws.",
                strDrinkThumb = "https://www.thecocktaildb.com/images/media/drink/vr6kle1504886114.jpg",
                ingredients = listOf("Lime juice", "Sugar", "Mint", "Soda water"),
                measures = listOf("1 oz", "1 tsp", "6 leaves", "Top up")
            )
        )
    }
}
