package com.example.cocktaildb.screen.myrecipe

import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.RecipeFirebaseService
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
class MainCoroutineRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }
    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MyRecipePresenterTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var closeable: AutoCloseable

    @Mock lateinit var recipeService: RecipeFirebaseService
    @Mock lateinit var authRepository: AuthRepository
    @Mock lateinit var view: MyRecipeContract.View
    @Mock lateinit var firebaseUser: FirebaseUser

    private lateinit var presenter: MyRecipePresenter

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        presenter = MyRecipePresenter(recipeService, authRepository)
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        presenter.onStop()
        closeable.close()
    }

    @Test
    fun `loadUserRecipes shows error when no user logged in`() = runTest {
        `when`(authRepository.getCurrentUser()).thenReturn(null)

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayError("Please log in to view your recipes")
        verify(recipeService, never()).getUserRecipes(anyString())
    }

    @Test
    fun `loadUserRecipes success shows data and cache reused onStart`() = runTest {
        `when`(authRepository.getCurrentUser()).thenReturn(firebaseUser)
        `when`(firebaseUser.uid).thenReturn("uid-123")
        `when`(recipeService.getUserRecipes("uid-123"))
            .thenReturn(Result.success(emptyList()))

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view, timeout(1000)).displayLoading(false)
        verify(view, timeout(1000)).showUserRecipes(emptyList())

        presenter.onStop()
        presenter.setView(view)
        presenter.onStart()

        verify(view, timeout(500).atLeastOnce()).showUserRecipes(emptyList())
        verify(recipeService, times(1)).getUserRecipes("uid-123")
    }

    @Test
    fun `loadUserRecipes failure shows failed message`() = runTest {
        `when`(authRepository.getCurrentUser()).thenReturn(firebaseUser)
        `when`(firebaseUser.uid).thenReturn("uid-123")
        `when`(recipeService.getUserRecipes("uid-123"))
            .thenReturn(Result.failure(RuntimeException("boom!")))

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view, timeout(1000)).displayLoading(false)
        verify(view, timeout(1000)).displayError("Failed to load recipes: boom!")
    }

    @Test
    fun `loadUserRecipes exception shows error message`() = runTest {
        `when`(authRepository.getCurrentUser()).thenReturn(firebaseUser)
        `when`(firebaseUser.uid).thenReturn("uid-123")
        `when`(recipeService.getUserRecipes("uid-123"))
            .thenAnswer { throw IllegalStateException("network down") }

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view, timeout(1000)).displayLoading(false)
        verify(view, timeout(1000)).displayError("Error loading recipes: network down")
    }

    @Test
    fun `refreshUserRecipes forces reload`() = runTest {
        `when`(authRepository.getCurrentUser()).thenReturn(firebaseUser)
        `when`(firebaseUser.uid).thenReturn("uid-123")
        `when`(recipeService.getUserRecipes("uid-123"))
            .thenReturn(Result.success(emptyList()))

        presenter.refreshUserRecipes()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view, timeout(1000)).displayLoading(false)
        verify(view, timeout(1000)).showUserRecipes(emptyList())
        verify(recipeService, times(1)).getUserRecipes("uid-123")
    }
}
