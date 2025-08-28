package com.example.cocktaildb.screen.history

import android.content.Context
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.utils.CocktailContextWrapper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HistoryPresenterTest {

    private lateinit var closeable: AutoCloseable
    private lateinit var context: Context

    @Mock lateinit var cocktailRepository: CocktailRepository
    @Mock lateinit var contextWrapper: CocktailContextWrapper
    @Mock lateinit var view: HistoryContract.View

    private lateinit var presenter: HistoryPresenter

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()
        `when`(contextWrapper.context).thenReturn(context)

        presenter = HistoryPresenter(cocktailRepository, contextWrapper)
        presenter.setView(view)

        clearLocalHistory()
    }

    @After
    fun tearDown() {
        if (this::presenter.isInitialized) presenter.onStop()
        clearLocalHistory()
        closeable.close()
    }

    private fun clearLocalHistory() {
        val sp = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
        sp.edit().clear().apply()
    }

    private fun putLocalHistory(vararg cocktails: Cocktail) {
        val sp = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
        val encoded = cocktails.joinToString("||") { c ->
            val ingredients = c.ingredients.joinToString(";;")
            val measures = c.measures.joinToString(";;")
            "${c.idDrink}|:|${c.strDrink}|:|${c.strCategory ?: ""}|:|${c.strAlcoholic ?: ""}|:|${c.strGlass ?: ""}|:|${c.strInstructions ?: ""}|:|${c.strDrinkThumb ?: ""}|:|$ingredients|:|$measures"
        }
        sp.edit().putString("cocktail_history", encoded).apply()
    }

    @Test
    fun loadHistoryCocktails_offline_emptyLocal_showsEmpty() {
        presenter.loadHistoryCocktails()

        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
        verify(view).showEmptyState()
        verify(view, never()).hideEmptyState()
        verify(view, never()).showHistoryCocktails(anyList())
    }

    @Test
    fun loadHistoryCocktails_offline_localHasData_showsList() {
        val c1 = Cocktail(
            idDrink = "11000",
            strDrink = "Mojito",
            strCategory = "Cocktail",
            strAlcoholic = "Alcoholic",
            strGlass = "Highball glass",
            strInstructions = "Mix & serve",
            strDrinkThumb = "",
            ingredients = listOf("Rum", "Mint"),
            measures = listOf("50ml", "5 leaves")
        )
        putLocalHistory(c1)

        presenter.loadHistoryCocktails()

        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
        verify(view).hideEmptyState()

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Cocktail>>
        verify(view).showHistoryCocktails(captor.capture())
        assert(captor.value.isNotEmpty())

        verify(view, never()).showEmptyState()
        verify(view, never()).displayError(anyString())
    }

    @Test
    fun onCocktailClicked_navigatesToDetail() {
        val c = Cocktail(
            idDrink = "99999",
            strDrink = "Test Drink",
            strCategory = null, strAlcoholic = null, strGlass = null,
            strInstructions = null, strDrinkThumb = null,
            ingredients = emptyList(), measures = emptyList()
        )

        presenter.onCocktailClicked(c)

        verify(view).navigateToCocktailDetail(c)
    }

    @Test
    fun clearHistory_clearsLocalAndShowsEmpty() {
        val c = Cocktail(
            idDrink = "11007",
            strDrink = "Margarita",
            strCategory = "Cocktail",
            strAlcoholic = "Alcoholic",
            strGlass = "Cocktail glass",
            strInstructions = "Shake with ice",
            strDrinkThumb = "",
            ingredients = listOf("Tequila"),
            measures = listOf("50ml")
        )
        putLocalHistory(c)

        presenter.clearHistory()

        verify(view).displayLoading(true)
        verify(view, timeout(1500)).displayLoading(false)
        verify(view, timeout(1500)).showEmptyState()
    }
}
