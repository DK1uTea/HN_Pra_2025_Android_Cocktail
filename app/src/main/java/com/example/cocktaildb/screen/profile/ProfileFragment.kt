package com.example.cocktaildb.screen.profile

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.local.CocktailLocalDataSource
import com.example.cocktaildb.databinding.FragmentProfileBinding
import com.example.cocktaildb.screen.auth.SignInActivity
import com.example.cocktaildb.utils.base.BaseFragment


class ProfileFragment : BaseFragment<FragmentProfileBinding>(), ProfileContract.View, ProfileAdapter.HeaderClickListener, ProfileAdapter.CocktailClickListener {

    private lateinit var presenter: ProfileContract.Presenter
    private lateinit var profileAdapter: ProfileAdapter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply window insets to handle navigation bar correctly
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { _, insets ->
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomNavHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_height)

            // Apply bottom margin to account for both navigation bar and bottom nav
            viewBinding.profileRecyclerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navigationBarInsets.bottom + bottomNavHeight
            }

            insets
        }
    }

    override fun initView() {
        // Initialize the adapter with this fragment as both click listeners
        profileAdapter = ProfileAdapter(this, this)

        // Set up RecyclerView with GridLayoutManager showing 2 items per row
        val layoutManager = GridLayoutManager(context, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 0) 2 else 1 // Header takes full width, cocktails take half
            }
        }

        // Apply proper item spacing decoration
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.dp_8)
        viewBinding.profileRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (position > 0) { // Skip header
                    // Apply spacing to all items except header
                    outRect.left = spacingInPixels
                    outRect.right = spacingInPixels
                    outRect.bottom = spacingInPixels

                    // Determine if this is an item in the left or right column
                    val isLeftColumn = (position - 1) % 2 == 0

                    // Add more space on the left for left column items and on the right for right column items
                    if (isLeftColumn) {
                        outRect.left = spacingInPixels * 2
                    } else {
                        outRect.right = spacingInPixels * 2
                    }

                    // Add top margin only for the first row items
                    if (position == 1 || position == 2) {
                        outRect.top = spacingInPixels
                    }
                }
            }
        })

        viewBinding.profileRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = profileAdapter
            clipToPadding = false  // Allow scrolling into the padding area
            setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_nav_height))
        }

        // Set up loading view
        viewBinding.loadingView.visibility = View.GONE
    }

    override fun initData() {
        // Initialize presenter with repositories
        val dataSource = CocktailLocalDataSource()
        val repository = CocktailRepository(dataSource)
        val authRepository = AuthRepository(requireContext())
        presenter = ProfilePresenter(repository, authRepository, requireContext())
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

    override fun showUserProfile(userName: String, userBio: String, profileImageUrl: String?) {
        profileAdapter.setUserProfile(userName, userBio, profileImageUrl)
    }

    override fun showUserCocktails(cocktails: List<Cocktail>) {
        profileAdapter.setCocktails(cocktails)
    }

    override fun navigateToMyRecipes() {
        try {
            // Navigate to the MyRecipe screen using the Navigation Component
            val navController = androidx.navigation.Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment_activity_main
            )
            navController.navigate(R.id.navigation_my_recipe)
        } catch (_: Exception) {
            // Fallback in case navigation fails
            Toast.makeText(
                context,
                "My Recipes feature coming soon!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun navigateToHistory() {
        try {
            // Navigate to the History screen using the Navigation Component
            val navController = androidx.navigation.Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment_activity_main
            )
            navController.navigate(R.id.navigation_history)
        } catch (_: Exception) {
            // Fallback in case navigation fails
            Toast.makeText(
                context,
                "History feature coming soon!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun navigateToLogin() {
        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun displayLoading(show: Boolean) {
        viewBinding.loadingView.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun displayError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Implement HeaderClickListener interface methods
    override fun onMyRecipesClicked() {
        presenter.onMyRecipesClicked()
    }

    override fun onHistoryClicked() {
        presenter.onHistoryClicked()
    }

    override fun onLogoutClicked() {
        presenter.onLogoutClicked()
    }

    // Implement CocktailClickListener interface method
    override fun onCocktailClicked(cocktail: Cocktail) {
        // Add to history first
        try {
            com.example.cocktaildb.screen.history.HistoryPresenter.addToHistory(requireContext(), cocktail)
        } catch (_: Exception) {
            // Handle error silently
        }
        
        // Navigate to cocktail detail fragment using Navigation Component
        val bundle = Bundle().apply {
            putString("cocktail_id", cocktail.idDrink)
            putString("cocktail_name", cocktail.strDrink)
            putString("cocktail_category", cocktail.strCategory ?: "")
            putString("cocktail_alcoholic", cocktail.strAlcoholic ?: "")
            putString("cocktail_glass", cocktail.strGlass ?: "")
            putString("cocktail_instructions", cocktail.strInstructions ?: "")
            putString("cocktail_image", cocktail.strDrinkThumb ?: "")
            putStringArray("cocktail_ingredients", cocktail.ingredients.toTypedArray())
            putStringArray("cocktail_measures", cocktail.measures.toTypedArray())
        }
        findNavController().navigate(R.id.navigation_cocktail_detail, bundle)
    }
}
