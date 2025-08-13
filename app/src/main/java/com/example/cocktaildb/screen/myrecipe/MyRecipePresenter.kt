package com.example.cocktaildb.screen.myrecipe

import android.os.Handler
import android.os.Looper
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import java.util.concurrent.Executors

/**
 * Presenter implementation for the My Recipes screen
 */
class MyRecipePresenter(
    private val cocktailRepository: CocktailRepository
) : MyRecipeContract.Presenter {

    private var view: MyRecipeContract.View? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun setView(view: MyRecipeContract.View?) {
        this.view = view
        if (view != null) {
            loadUserRecipes()
        }
    }

    override fun onStart() {
        // Refresh data if needed
    }

    override fun onStop() {
        // Clean up resources
        view = null
    }

    override fun loadUserRecipes() {
        view?.displayLoading(true)

        // Using a simple background thread executor instead of AsyncTask
        executor.execute {
            // In a real app, this would fetch from repository
            // For now, we'll use mock data
            val recipes = createMockUserRecipes()

            // Post results back to main thread
            mainHandler.post {
                view?.displayLoading(false)
                view?.showUserRecipes(recipes)
            }
        }
    }

    /**
     * Creates mock user recipe data
     */
    private fun createMockUserRecipes(): List<Cocktail> {
        return listOf(
            Cocktail(
                id = "101",
                name = "Berry Blast",
                description = "My special berry cocktail",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/xwqvur1468876473.jpg"
            ),
            Cocktail(
                id = "102",
                name = "Mango Tango",
                description = "Sweet mango-based cocktail with a twist",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/bxyyrq1534536248.jpg"
            ),
            Cocktail(
                id = "103",
                name = "Blue Lagoon",
                description = "My take on the blue lagoon - with extra citrus",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/5wm4zo1582579154.jpg"
            ),
            Cocktail(
                id = "104",
                name = "Vanilla Dream",
                description = "Creamy vanilla cocktail perfect for dessert",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/yrhutv1503563730.jpg"
            ),
            Cocktail(
                id = "105",
                name = "Ginger Fizz",
                description = "Spicy ginger cocktail with soda",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/nzlyc81605905755.jpg"
            ),
            Cocktail(
                id = "106",
                name = "Cucumber Cooler",
                description = "Refreshing cucumber-based summer drink",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/hbkfsh1589574990.jpg"
            )
        )
    }
}
