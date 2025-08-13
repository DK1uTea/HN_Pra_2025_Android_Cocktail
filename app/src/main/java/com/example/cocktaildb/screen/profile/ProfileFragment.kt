package com.example.cocktaildb.screen.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.local.CocktailLocalDataSource
import com.example.cocktaildb.databinding.FragmentProfileBinding
import com.example.cocktaildb.databinding.ItemCocktailCardBinding
import com.example.cocktaildb.databinding.ItemProfileHeaderBinding
import com.example.cocktaildb.utils.base.BaseFragment

/**
 * Fragment for displaying user profile and cocktail list
 */
class ProfileFragment : BaseFragment<FragmentProfileBinding>(), ProfileContract.View {

    private lateinit var presenter: ProfileContract.Presenter
    private val profileAdapter = ProfileAdapter()

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater)
    }

    override fun initView() {
        // Set up RecyclerView with GridLayoutManager to show 2 items per row
        // The span count for header is full width (2), and for cocktail items is 1 (2 per row)
        val layoutManager = GridLayoutManager(context, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 0) 2 else 1 // Header takes full width, cocktails take half
            }
        }

        viewBinding.profileRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = profileAdapter

            // Add fixed bottom padding to prevent content from being hidden by bottom navigation
            // Using a fixed value that matches the standard bottom nav height (56dp)
            val bottomNavHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
            setPadding(paddingLeft, paddingTop, paddingRight, bottomNavHeight)
            clipToPadding = false  // Allow scrolling into the padding area
        }
    }

    override fun initData() {
        // Initialize presenter with repository
        val dataSource = CocktailLocalDataSource()
        val repository = CocktailRepository(dataSource)
        presenter = ProfilePresenter(repository)
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

    override fun displayLoading(show: Boolean) {
        viewBinding.loadingView.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun displayError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToMyRecipes() {
        // Navigate to the MyRecipe screen using the Navigation Component
        val navController = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main)
        navController.navigate(R.id.navigation_my_recipe)
    }

    /**
     * Adapter for the profile RecyclerView that displays both header and cocktail items
     */
    private inner class ProfileAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_COCKTAIL = 1

        private var userName: String = ""
        private var userBio: String = ""
        private var profileImageUrl: String? = null
        private val cocktails: MutableList<Cocktail> = mutableListOf()

        fun setUserProfile(userName: String, userBio: String, profileImageUrl: String?) {
            this.userName = userName
            this.userBio = userBio
            this.profileImageUrl = profileImageUrl
            notifyItemChanged(0)
        }

        fun setCocktails(cocktails: List<Cocktail>) {
            this.cocktails.clear()
            this.cocktails.addAll(cocktails)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_COCKTAIL
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    val binding = ItemProfileHeaderBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false)
                    HeaderViewHolder(binding)
                }
                else -> {
                    val binding = ItemCocktailCardBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false)
                    CocktailViewHolder(binding)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderViewHolder -> {
                    holder.bind(userName, userBio, profileImageUrl)
                }
                is CocktailViewHolder -> {
                    // Position - 1 because header is at position 0
                    holder.bind(cocktails[position - 1])
                }
            }
        }

        override fun getItemCount(): Int = if (cocktails.isEmpty()) 1 else cocktails.size + 1

        /**
         * ViewHolder for the profile header
         */
        inner class HeaderViewHolder(private val binding: ItemProfileHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.myRecipeButton.setOnClickListener {
                    presenter.onMyRecipesClicked()
                }

                binding.historyButton.setOnClickListener {
                    presenter.onHistoryClicked()
                }
            }

            fun bind(name: String, bio: String, imageUrl: String?) {
                binding.userName.text = name
                binding.userBio.text = bio

                // Use the profile placeholder image instead of the icon
                binding.profileImageView.setImageResource(R.drawable.profile_placeholder)
            }
        }

        /**
         * ViewHolder for cocktail items
         */
        inner class CocktailViewHolder(private val binding: ItemCocktailCardBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bind(cocktail: Cocktail) {
                binding.cocktailTitleTextView.text = cocktail.name
                binding.cocktailCategoryTextView.text = cocktail.description ?: "Cocktail"

                // Load the cocktail image using our custom ImageLoader
                com.example.cocktaildb.utils.ImageLoader.loadImage(
                    url = cocktail.imageUrl,
                    imageView = binding.cocktailImageView,
                    placeholderResId = R.drawable.ic_launcher_background
                )

                // Set rating
                binding.ratingChip.text = "★ 4.5"
            }
        }
    }
}
