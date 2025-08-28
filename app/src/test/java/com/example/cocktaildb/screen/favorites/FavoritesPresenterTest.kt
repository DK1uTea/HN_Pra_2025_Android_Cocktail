package com.example.cocktaildb.screen.favorites

import android.content.Context
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FavoritesPresenterTest {

    private lateinit var context: Context
    private lateinit var presenter: FavoritesPresenter
    private lateinit var closeable: AutoCloseable

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Mock lateinit var repo: CocktailRepository
    @Mock lateinit var view: FavoritesContract.View
    @Mock lateinit var mockAuth: FirebaseAuth

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(mainDispatcher)

        context = RuntimeEnvironment.getApplication()

        `when`(mockAuth.currentUser).thenReturn(null)

        presenter = FavoritesPresenter(
            context = context,
            cocktailRepository = repo,
            firebaseAuth = mockAuth
        )
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        closeable.close()
        Dispatchers.resetMain()
    }

    @Test
    fun loadFavorites_offline_localHasItems_showsList() = runTest {
        val fakeList = listOf(
            Cocktail(
                idDrink = "1",
                strDrink = "A",
                strCategory = "C",
                strAlcoholic = "Alcoholic",
                strGlass = "Glass",
                strInstructions = "Do it",
                strDrinkThumb = null,
                ingredients = listOf("x"),
                measures = listOf("1")
            )
        )
        `when`(repo.getFavoritesFromLocal(context)).thenReturn(fakeList)

        presenter.loadFavorites()

        verify(view).displayLoading(true)
        verify(view).displayFavorites(fakeList)
        verify(view, atLeastOnce()).displayLoading(false)
    }

    @Test
    fun loadFavorites_offline_localEmpty_showsEmpty() = runTest {
        `when`(repo.getFavoritesFromLocal(context)).thenReturn(emptyList())

        presenter.loadFavorites()

        verify(view).displayLoading(true)
        verify(view).displayEmptyState()
        verify(view, atLeastOnce()).displayLoading(false)
    }

    @Test
    fun toggleFavorite_togglesAndReloadsLocal() = runTest {
        val c = Cocktail(
            idDrink = "9",
            strDrink = "Toggle Me",
            strCategory = "Test",
            strAlcoholic = "Non alcoholic",
            strGlass = "Glass",
            strInstructions = "Mix",
            strDrinkThumb = null,
            ingredients = listOf("i1"),
            measures = listOf("m1")
        )

        `when`(repo.toggleFavorite(eq(context), eq(c))).thenReturn(true)
        `when`(repo.getFavoritesFromLocal(context)).thenReturn(listOf(c))

        presenter.toggleFavorite(c)

        verify(repo).toggleFavorite(eq(context), eq(c))
        verify(view).displayFavorites(listOf(c))
    }

    @Test
    fun addToFavorites_success_showsStatusAndReloadsLocal() = runTest {
        val c = Cocktail(
            idDrink = "2",
            strDrink = "Add Me",
            strCategory = "Cat",
            strAlcoholic = "Alcoholic",
            strGlass = "Glass",
            strInstructions = "Shake",
            strDrinkThumb = null,
            ingredients = listOf("i"),
            measures = listOf("m")
        )

        doAnswer { invocation ->
            val cb = invocation.arguments[2] as (Boolean) -> Unit
            cb(true)
            null
        }.`when`(repo).addToFavorites(eq(context), eq(c), any())

        `when`(repo.getFavoritesFromLocal(context)).thenReturn(listOf(c))

        presenter.addToFavorites(c)

        verify(view).showSyncStatus("Added to favorites")
        verify(view).displayFavorites(listOf(c))
    }

    @Test
    fun clearAllFavorites_offline_clearsLocalAndShowsEmpty() = runTest {
        doNothing().`when`(repo).clearAllFavorites(context)

        presenter.clearAllFavorites()

        verify(repo).clearAllFavorites(context)
        verify(view).showSyncStatus("Local favorites cleared")
        verify(view).displayEmptyState()
    }
}
