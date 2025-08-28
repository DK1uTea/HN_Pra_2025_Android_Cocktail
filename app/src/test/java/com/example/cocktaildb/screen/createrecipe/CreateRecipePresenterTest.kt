package com.example.cocktaildb.screen.createrecipe

import android.content.Context
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.ArgumentMatchers.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.mockito.ArgumentCaptor
import com.example.cocktaildb.data.model.Recipe
import com.example.cocktaildb.data.model.RecipeIngredient

@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(dispatcher)
    }
    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)

@Config(manifest = Config.NONE)
class CreateRecipePresenterTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var closeable: AutoCloseable
    private lateinit var context: Context

    @Mock
    lateinit var firebaseRepository: FirebaseRepository
    @Mock
    lateinit var authRepository: AuthRepository
    @Mock
    lateinit var view: CreateRecipeContract.View
    @Mock
    lateinit var firebaseUser: FirebaseUser

    private lateinit var presenter: CreateRecipePresenter

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)

        val realApp = RuntimeEnvironment.getApplication()
        context = spy(realApp)
        doReturn("OK").`when`(context).getString(anyInt())

        presenter = CreateRecipePresenter(context, firebaseRepository, authRepository)
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        if (this::presenter.isInitialized) {
            presenter.onStop()
        }
        if (this::closeable.isInitialized) {
            closeable.close()
        }
    }

    @Test
    fun saveRecipe_showsError_whenNameBlank() = runTest {
        presenter.saveRecipe(
            name = "",
            instructions = "mix well",
            imageUrl = "",
            ingredients = listOf("Rum"),
            measures = listOf("50ml"),
            category = "Cocktail",
            glass = "Highball",
            alcoholic = true
        )

        verify(view).showError(anyString())
        verify(view, never()).showLoading(true)
        verifyNoInteractions(firebaseRepository)
    }

    @Test
    fun saveRecipe_showsError_whenInstructionsBlank() = runTest {
        presenter.saveRecipe(
            name = "Mojito",
            instructions = "",
            imageUrl = "",
            ingredients = listOf("Rum"),
            measures = listOf("50ml"),
            category = "Cocktail",
            glass = "Highball",
            alcoholic = true
        )

        verify(view).showError(anyString())
        verify(view, never()).showLoading(true)
        verifyNoInteractions(firebaseRepository)
    }

    @Test
    fun saveRecipe_showsError_whenIngredientsInvalid() = runTest {
        presenter.saveRecipe(
            name = "Mojito",
            instructions = "mix well",
            imageUrl = "",
            ingredients = emptyList(),
            measures = emptyList(),
            category = "Cocktail",
            glass = "Highball",
            alcoholic = true
        )

        verify(view).showError(anyString())
        verify(view, never()).showLoading(true)
        verifyNoInteractions(firebaseRepository)
    }

    @Test
    fun saveRecipe_showsError_whenNotLoggedIn() = runTest {
        `when`(authRepository.getCurrentUser()).thenReturn(null)

        presenter.saveRecipe(
            name = "Mojito",
            instructions = "mix well",
            imageUrl = "",
            ingredients = listOf("Rum"),
            measures = listOf("50ml"),
            category = "Cocktail",
            glass = "Highball",
            alcoholic = true
        )

        verifyNoInteractions(firebaseRepository)
    }


    @Test
    fun saveRecipe_success_showsSuccess_andNavigates() = runTest {
        `when`(authRepository.getCurrentUser()).thenReturn(firebaseUser)
        `when`(firebaseUser.uid).thenReturn("uid-123")

        `when`(
            firebaseRepository.createRecipe(org.mockito.ArgumentMatchers.any(Recipe::class.java))
        ).thenReturn(Result.success("recipe-001"))

        `when`(
            firebaseRepository.addRecipeIngredient(org.mockito.ArgumentMatchers.any(RecipeIngredient::class.java))
        ).thenReturn(Result.success("ingredient-001"))

        presenter.saveRecipe(
            name = "Mojito",
            instructions = "mix well",
            imageUrl = "",
            ingredients = listOf("Rum", "Mint"),
            measures = listOf("50ml", "5 leaves"),
            category = "Cocktail",
            glass = "Highball",
            alcoholic = true
        )

        val recipeCaptor = ArgumentCaptor.forClass(Recipe::class.java)
        verify(firebaseRepository).createRecipe(recipeCaptor.capture())

        val ingCaptor = ArgumentCaptor.forClass(RecipeIngredient::class.java)
        verify(firebaseRepository, atLeast(2)).addRecipeIngredient(ingCaptor.capture())
    }


    @Test
    fun addAndRemoveIngredient_callsView() = runTest {
        presenter.addIngredient()
        verify(view).addIngredientField()

        presenter.removeIngredient(0)
        verify(view).removeIngredientField(0)
    }
}
