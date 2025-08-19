package com.example.cocktaildb.screen.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.FragmentCocktailDetailBinding
import com.example.cocktaildb.screen.search.SearchActivity
import com.example.cocktaildb.utils.ImageLoader
import com.example.cocktaildb.data.manager.FavoritesManager
import com.example.cocktaildb.screen.history.HistoryPresenter

class CocktailDetailFragment : Fragment(), CocktailDetailContract.View {

    companion object {
        const val KEY_COCKTAIL_ID = "cocktail_id"
        const val KEY_COCKTAIL_NAME = "cocktail_name"
        const val KEY_COCKTAIL_CATEGORY = "cocktail_category"
        const val KEY_COCKTAIL_ALCOHOLIC = "cocktail_alcoholic"
        const val KEY_COCKTAIL_GLASS = "cocktail_glass"
        const val KEY_COCKTAIL_INSTRUCTIONS = "cocktail_instructions"
        const val KEY_COCKTAIL_IMAGE = "cocktail_image"
        const val KEY_COCKTAIL_INGREDIENTS = "cocktail_ingredients"
        const val KEY_COCKTAIL_MEASURES = "cocktail_measures"
    }

    private var _binding: FragmentCocktailDetailBinding? = null
    private val binding get() = _binding!!
    private var cocktail: Cocktail? = null
    private val TAG = "CocktailDetailFragment"

    private val presenter = CocktailDetailPresenter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCocktailDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        loadCocktailData()

        // Set view for presenter
        presenter.setView(this)

        setupClickListeners()

        // Ensure favorites are loaded before checking status
        ensureFavoritesLoaded()

        // Check bookmark status for current cocktail
        cocktail?.let {
            presenter.checkBookmarkStatus(it.idDrink)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorite button state when returning to this screen
        cocktail?.let {
            updateFavoriteButtonState(it)
            presenter.checkBookmarkStatus(it.idDrink)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onStop()
        _binding = null
    }

    private fun ensureFavoritesLoaded() {
        // Make sure favorites are loaded before checking favorite status
        if (!FavoritesManager.isInitialized()) {
            Log.d(TAG, "Favorites not initialized, loading them now")
            FavoritesManager.loadFavoritesFromFirestore { success ->
                if (success) {
                    Log.d(TAG, "Favorites loaded successfully")
                    // Update button state after favorites are loaded
                    activity?.runOnUiThread {
                        cocktail?.let {
                            updateFavoriteButtonState(it)
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to load favorites")
                }
            }
        } else {
            Log.d(TAG, "Favorites already initialized")
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            // Check if we're in SearchActivity context
            if (activity is SearchActivity) {
                val searchActivity = activity as SearchActivity
                if (searchActivity.intent.getBooleanExtra("from_today_drink", false)) {
                    // If opened from TodayDrinkActivity, finish SearchActivity to go back to TodayDrinkActivity
                    searchActivity.finish()
                } else {
                    // Normal SearchActivity back press - trigger the back handler
                    searchActivity.onBackPressedDispatcher.onBackPressed()
                }
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun loadCocktailData() {
        // Get data from arguments using Navigation component's safe args
        val args = arguments
        val cocktailId = args?.getString(KEY_COCKTAIL_ID) ?: ""
        val cocktailName = args?.getString(KEY_COCKTAIL_NAME) ?: "Cocktail"
        val cocktailCategory = args?.getString(KEY_COCKTAIL_CATEGORY) ?: "Cocktail"
        val alcoholic = args?.getString(KEY_COCKTAIL_ALCOHOLIC) ?: ""
        val glass = args?.getString(KEY_COCKTAIL_GLASS) ?: ""
        val instructions = args?.getString(KEY_COCKTAIL_INSTRUCTIONS) ?: "No instructions available"
        val imageUrl = args?.getString(KEY_COCKTAIL_IMAGE)
        val ingredients = args?.getStringArray(KEY_COCKTAIL_INGREDIENTS) ?: emptyArray()
        val measures = args?.getStringArray(KEY_COCKTAIL_MEASURES) ?: emptyArray()

        // Create cocktail object and add to history
        val cocktail = Cocktail(
            idDrink = cocktailId,
            strDrink = cocktailName,
            strCategory = cocktailCategory,
            strAlcoholic = alcoholic,
            strGlass = glass,
            strInstructions = instructions,
            strDrinkThumb = imageUrl,
            ingredients = ingredients.toList(),
            measures = measures.toList()
        )

        this.cocktail = cocktail

        try {
            // Add to history
            HistoryPresenter.addToHistory(requireContext(), cocktail)
        } catch (e: Exception) {
            // Handle error silently
            Log.e(TAG, "Error adding to history", e)
        }

        // Set cocktail name
        binding.tvCocktailName.text = cocktailName

        // Set description with category, alcoholic status, and glass type
        val description = buildString {
            append("A delicious $cocktailCategory cocktail")
            if (alcoholic.isNotEmpty()) {
                append(" ($alcoholic)")
            }
            if (glass.isNotEmpty()) {
                append(" served in a $glass")
            }
            append(" with carefully selected ingredients.")
        }
        binding.tvDescription.text = description

        // Load image
        ImageLoader.loadImage(imageUrl, binding.ivCocktail, R.drawable.imgstart)

        // Set instructions
        setupInstructions(instructions)

        // Set ingredients (only non-null ingredients)
        setupIngredients(ingredients, measures)
    }

    private fun setupInstructions(instructions: String) {
        val instructionLines = instructions.split(". ")
        val instructionsContainer = binding.llInstructions

        // Clear existing instructions
        instructionsContainer.removeAllViews()

        instructionLines.forEachIndexed { index, instruction ->
            if (instruction.isNotBlank()) {
                val instructionView = TextView(requireContext()).apply {
                    text = "${index + 1}. $instruction"
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.dark_gray, null))
                    setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.dp_8))
                }
                instructionsContainer.addView(instructionView)
            }
        }
    }

    private fun setupIngredients(ingredients: Array<String>, measures: Array<String>) {
        val ingredientsContainer = binding.llIngredients

        // Clear existing ingredients
        ingredientsContainer.removeAllViews()

        // Filter out null/empty ingredients
        val validIngredients = ingredients.filterIndexed { index, ingredient ->
            ingredient.isNotBlank() && ingredient != "null"
        }

        // Update ingredients count in header
        val ingredientsCount = validIngredients.size
        binding.tvIngredientsHeader.text = getString(R.string.ingredients) + " ($ingredientsCount)"

        validIngredients.forEachIndexed { index, ingredient ->
            val ingredientView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_ingredient, ingredientsContainer, false)

            val ingredientName = ingredientView.findViewById<TextView>(R.id.tvIngredientName)
            val ingredientMeasure = ingredientView.findViewById<TextView>(R.id.tvIngredientMeasure)

            ingredientName.text = ingredient

            // Find corresponding measure
            val originalIndex = ingredients.indexOf(ingredient)
            val measure = if (originalIndex < measures.size && originalIndex >= 0) {
                measures[originalIndex]?.takeIf { it.isNotBlank() && it != "null" } ?: ""
            } else ""

            ingredientMeasure.text = measure

            ingredientsContainer.addView(ingredientView)
        }
    }

    private fun setupClickListeners() {
        cocktail?.let { currentCocktail ->
            // Update favorite button based on current state
            updateFavoriteButtonState(currentCocktail)

            binding.btnFavorite.setOnClickListener {
                // Show loading indicator on the button
                binding.btnFavorite.isEnabled = false

                // Toggle favorite with Firebase
                FavoritesManager.toggleFavorite(currentCocktail) { isFavorite ->
                    // Update UI on the main thread
                    activity?.runOnUiThread {
                        binding.btnFavorite.isEnabled = true

                        // Update button state
                        if (isFavorite) {
                            binding.btnFavorite.setColorFilter(resources.getColor(R.color.pink_primary, null))
                            // Show toast notification when adding to favorites
                            Toast.makeText(
                                requireContext(),
                                "Added ${currentCocktail.strDrink} to favorites",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            binding.btnFavorite.setColorFilter(resources.getColor(R.color.red, null))
                            // Show toast notification when removing from favorites
                            Toast.makeText(
                                requireContext(),
                                "Removed ${currentCocktail.strDrink} from favorites",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            binding.btnBookmark.setOnClickListener {
                // Show loading indicator on the button
                binding.btnBookmark.isEnabled = false

                // Toggle bookmark using presenter
                presenter.toggleBookmark(currentCocktail)

                // Nút sẽ được enable lại trong updateBookmarkButtonState
            }
        }
    }

    // Implement CocktailDetailContract.View methods
    override fun updateBookmarkButtonState(isBookmarked: Boolean) {
        binding.btnBookmark.isEnabled = true

        if (isBookmarked) {
            binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
            binding.btnBookmark.setColorFilter(resources.getColor(R.color.pink_primary, null))

            // Hiển thị thông báo nếu đang trong quá trình thêm bookmark
            cocktail?.let { currentCocktail ->
                Toast.makeText(
                    requireContext(),
                    "Added ${currentCocktail.strDrink} to bookmarks",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            binding.btnBookmark.setImageResource(R.drawable.ic_bookmark)
            binding.btnBookmark.setColorFilter(resources.getColor(R.color.red, null))

            // Hiển thị thông báo nếu đang trong quá trình xóa bookmark
            cocktail?.let { currentCocktail ->
                Toast.makeText(
                    requireContext(),
                    "Removed ${currentCocktail.strDrink} from bookmarks",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun showError(message: String) {
        binding.btnBookmark.isEnabled = true
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun updateFavoriteButtonState(cocktail: Cocktail) {
        Log.d(TAG, "Updating favorite button state for cocktail ${cocktail.idDrink}")
        val isFavorite = FavoritesManager.isFavorite(cocktail.idDrink)
        Log.d(TAG, "Is favorite: $isFavorite")

        if (isFavorite) {
            binding.btnFavorite.setColorFilter(resources.getColor(R.color.pink_primary, null))
        } else {
            binding.btnFavorite.setColorFilter(resources.getColor(R.color.red, null))
        }
    }
}
