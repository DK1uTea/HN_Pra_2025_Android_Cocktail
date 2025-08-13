package com.example.cocktaildb.screen.myrecipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.local.CocktailLocalDataSource
import com.example.cocktaildb.databinding.FragmentMyRecipeBinding
import com.example.cocktaildb.databinding.ItemCocktailCardBinding
import com.example.cocktaildb.utils.base.BaseFragment

/**
 * Fragment for displaying user's created recipes
 */
class MyRecipeFragment : BaseFragment<FragmentMyRecipeBinding>(), MyRecipeContract.View {

    private lateinit var presenter: MyRecipeContract.Presenter
    private val recipeAdapter = RecipeAdapter()

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentMyRecipeBinding {
        return FragmentMyRecipeBinding.inflate(inflater)
    }

    override fun initView() {
        // Set up RecyclerView
        viewBinding.recyclerView.apply {
            adapter = recipeAdapter

            // Add bottom padding to prevent content from being hidden by bottom navigation
            val bottomNavHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
            setPadding(paddingLeft, paddingTop, paddingRight, bottomNavHeight)
            clipToPadding = false  // Allow scrolling into the padding area
        }
    }

    override fun initData() {
        // Initialize presenter with repository
        val dataSource = CocktailLocalDataSource()
        val repository = CocktailRepository(dataSource)
        presenter = MyRecipePresenter(repository)
        presenter.setView(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.onStart()
    }

    override fun onPause() {
        presenter.onStop()
        super.onPause()
    }

    override fun showUserRecipes(cocktails: List<Cocktail>) {
        recipeAdapter.setRecipes(cocktails)
    }

    override fun displayLoading(show: Boolean) {
        viewBinding.loadingView.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun displayError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Adapter for the recipes RecyclerView
     */
    private inner class RecipeAdapter : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

        private val recipes: MutableList<Cocktail> = mutableListOf()

        fun setRecipes(recipes: List<Cocktail>) {
            this.recipes.clear()
            this.recipes.addAll(recipes)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
            val binding = ItemCocktailCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return RecipeViewHolder(binding)
        }

        override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
            holder.bind(recipes[position])
        }

        override fun getItemCount(): Int = recipes.size

        inner class RecipeViewHolder(private val binding: ItemCocktailCardBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(cocktail: Cocktail) {
                binding.cocktailTitleTextView.text = cocktail.name
                binding.cocktailCategoryTextView.text = cocktail.description ?: "Custom Recipe"

                // Load the cocktail image using our custom ImageLoader
                com.example.cocktaildb.utils.ImageLoader.loadImage(
                    url = cocktail.imageUrl,
                    imageView = binding.cocktailImageView,
                    placeholderResId = R.drawable.ic_launcher_background
                )

                // Set rating chip to show this is a user recipe
                binding.ratingChip.text = "★ My Recipe"
            }
        }
    }

    companion object {
        fun newInstance() = MyRecipeFragment()
    }
}
