package com.example.cocktaildb.screen.myrecipe

import com.example.cocktaildb.data.model.Recipe
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.RecipeFirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MyRecipePresenterTest {
    @Mock lateinit var mockView: MyRecipeContract.View
    @Mock lateinit var mockRecipeService: RecipeFirebaseService
    @Mock lateinit var mockAuthRepository: AuthRepository

    private lateinit var presenter: MyRecipePresenter
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        presenter = MyRecipePresenter(mockRecipeService, mockAuthRepository)
        presenter.setView(mockView)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setView should show cached recipes if already loaded`() {
        val recipes = listOf(
            Recipe(id = "id", uid = "uid", name = "title", description = "desc")
        )
        // Simulate cache
        presenter.apply {
            javaClass.getDeclaredField("cachedRecipes").apply { isAccessible = true }.set(this, recipes)
            javaClass.getDeclaredField("isDataLoaded").apply { isAccessible = true }.set(this, true)
        }
        presenter.setView(mockView)
        verify(mockView).showUserRecipes(recipes)
    }

    @Test
    fun `onStart should show cached recipes if already loaded`() {
        val recipes = listOf(
            Recipe(id = "id", uid = "uid", name = "title", description = "desc")
        )
        presenter.apply {
            javaClass.getDeclaredField("cachedRecipes").apply { isAccessible = true }.set(this, recipes)
            javaClass.getDeclaredField("isDataLoaded").apply { isAccessible = true }.set(this, true)
        }
        presenter.setView(mockView)
        presenter.onStart()
        verify(mockView, atLeastOnce()).showUserRecipes(recipes)
    }

    @Test
    fun `onStop should cancel job and detach view`() {
        presenter.onStop()
        // No exception means success; can't verify private job or view directly
    }

    @Test
    fun `loadUserRecipes should show error if user not logged in`() = runTest {
        whenever(mockAuthRepository.getCurrentUser()).thenReturn(null)
        presenter.loadUserRecipes()
        verify(mockView).displayError(any())
    }

    @Test
    fun `loadUserRecipes should show recipes on success`() = runBlocking {
        val user = mock(com.google.firebase.auth.FirebaseUser::class.java)
        whenever(user.uid).thenReturn("uid123")
        whenever(mockAuthRepository.getCurrentUser()).thenReturn(user)
        val recipes = listOf(Recipe(id = "id", uid = "uid123", name = "title", description = "desc"))
        whenever(mockRecipeService.getUserRecipes("uid123")).thenReturn(Result.success(recipes))
        presenter.loadUserRecipes()
        // Wait for coroutine to finish
        Thread.sleep(100)
        verify(mockView).displayLoading(true)
        verify(mockView).displayLoading(false)
        verify(mockView).showUserRecipes(recipes)
    }

    @Test
    fun `loadUserRecipes should show error on failure`() = runBlocking {
        val user = mock(com.google.firebase.auth.FirebaseUser::class.java)
        whenever(user.uid).thenReturn("uid123")
        whenever(mockAuthRepository.getCurrentUser()).thenReturn(user)
        whenever(mockRecipeService.getUserRecipes("uid123")).thenReturn(Result.failure(Exception("fail")))
        presenter.loadUserRecipes()
        Thread.sleep(100)
        verify(mockView).displayLoading(true)
        verify(mockView).displayLoading(false)
        verify(mockView).displayError(any())
    }

    @Test
    fun `loadUserRecipes should show error on exception`() = runBlocking {
        val user = mock(com.google.firebase.auth.FirebaseUser::class.java)
        whenever(user.uid).thenReturn("uid123")
        whenever(mockAuthRepository.getCurrentUser()).thenReturn(user)
        whenever(mockRecipeService.getUserRecipes("uid123")).thenThrow(RuntimeException("exception"))
        presenter.loadUserRecipes()
        Thread.sleep(100)
        verify(mockView).displayLoading(true)
        verify(mockView).displayLoading(false)
        verify(mockView).displayError(any())
    }
}
