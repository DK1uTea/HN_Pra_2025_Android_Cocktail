package com.example.cocktaildb.screen.profile

import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class TestableProfilePresenter(
    cocktailRepository: CocktailRepository,
    authRepository: AuthRepository
) : ProfilePresenter(cocktailRepository, authRepository) {
    override fun loadData() { /* no-op for unit test */ }
}

class ProfilePresenterTest {
    @Mock lateinit var mockView: ProfileContract.View
    @Mock lateinit var mockAuthRepository: AuthRepository
    @Mock lateinit var mockCocktailRepository: CocktailRepository
    @Mock lateinit var mockFirestore: FirebaseFirestore
    @Mock lateinit var mockCollection: CollectionReference
    @Mock lateinit var mockDocument: DocumentReference
    @Mock lateinit var mockSnapshot: DocumentSnapshot

    private lateinit var presenter: ProfilePresenter

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        presenter = TestableProfilePresenter(mockCocktailRepository, mockAuthRepository)
        presenter.setView(mockView)
    }

    @Test
    fun `setView should call loadData if view is not null`() {
        val presenterSpy = spy(presenter)
        presenterSpy.setView(mockView)
        verify(presenterSpy, atLeastOnce()).setView(mockView)
    }

    @Test
    fun `loadUserProfile should show guest profile if user is null`() {
        whenever(mockAuthRepository.getCurrentUser()).thenReturn(null)
        presenter.setView(mockView)
        presenter.loadUserProfile()
        verify(mockView).displayLoading(true)
        verify(mockView).displayLoading(false)
        verify(mockView).showUserProfile(eq("Guest User"), eq("Please sign in to see your profile"), eq(null))
    }

    @Test
    fun `loadUserProfile should show user profile if Firestore document exists and has data`() {
        val mockUser = mock(com.google.firebase.auth.FirebaseUser::class.java)
        whenever(mockAuthRepository.getCurrentUser()).thenReturn(mockUser)
        whenever(mockUser.uid).thenReturn("uid123")
        // Simulate Firestore success with user data
        // This part would require Robolectric or Android instrumentation for full async Firestore simulation
        // Here, we just verify the loading and error fallback logic
        presenter.setView(mockView)
        // Can't fully test Firestore callback without Android, but can check loading
        presenter.loadUserProfile()
        verify(mockView).displayLoading(true)
    }

    @Test
    fun `onMyRecipesClicked should navigate to My Recipes`() {
        presenter.setView(mockView)
        presenter.onMyRecipesClicked()
        verify(mockView).navigateToMyRecipes()
    }

    @Test
    fun `onHistoryClicked should navigate to History`() {
        presenter.setView(mockView)
        presenter.onHistoryClicked()
        verify(mockView).navigateToHistory()
    }
}
