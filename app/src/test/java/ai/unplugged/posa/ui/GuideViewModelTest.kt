package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Awaits [GuideViewModel.state] because the bundled-pack install and content load
 * run asynchronously (Room's suspend DAOs resume on Room's own executor). See
 * [MapViewModelTest] for the dispatcher rationale.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GuideViewModelTest {
    private lateinit var application: Application
    private lateinit var database: PosaDatabase

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        application = ApplicationProvider.getApplicationContext()
        database = PosaDatabase.createInMemory(application)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun createViewModel(onGuidePackInstalled: () -> Unit = {}): GuideViewModel =
        GuideViewModel(
            application = application,
            database = database,
            onGuidePackInstalled = onGuidePackInstalled,
        )

    private suspend fun GuideViewModel.awaitState(
        predicate: (GuideContentState) -> Boolean,
    ): GuideContentState = withTimeout(5_000) { state.first(predicate) }

    @Test
    fun installsBundledPackAndLoadsCardsOnInit() = runBlocking {
        var installedNotified = 0
        val viewModel = createViewModel { installedNotified += 1 }

        val state = viewModel.awaitState { !it.isLoading && it.allCards.isNotEmpty() }
        assertNull(state.errorMessage)
        assertTrue(state.packs.any { it.id == "wilderness-basics" })
        assertEquals(1, installedNotified)
    }

    @Test
    fun searchFiltersCardsAndClearsSelection() = runBlocking {
        val viewModel = createViewModel()
        viewModel.awaitState { !it.isLoading && it.allCards.isNotEmpty() }
        viewModel.selectCard(viewModel.state.value.allCards.first().card.id)

        viewModel.setGuideSearch("water")

        val state = viewModel.awaitState { s -> s.cards.any { it.card.title == "Water Planning" } }
        assertNull(viewModel.selectedGuideCardId.value)
        assertTrue(state.cards.any { it.card.title == "Water Planning" })
        // Filtered results are a subset of the full catalogue.
        assertTrue(state.cards.size <= state.allCards.size)
        assertEquals("water", viewModel.guideSearchQuery.value)
    }

    @Test
    fun blankSearchRestoresFullCatalogue() = runBlocking {
        val viewModel = createViewModel()
        val loaded = viewModel.awaitState { !it.isLoading && it.allCards.isNotEmpty() }
        val total = loaded.allCards.size

        viewModel.setGuideSearch("water")
        viewModel.awaitState { it.cards.size < total }
        viewModel.setGuideSearch("")

        val state = viewModel.awaitState { it.cards.size == total }
        assertEquals(total, state.cards.size)
    }

    @Test
    fun questionAndWorkflowSelectionAreHeld() = runBlocking {
        val viewModel = createViewModel()
        viewModel.awaitState { !it.isLoading }

        viewModel.setGuidedQuestion("how do I treat water")
        viewModel.selectWorkflow(GuidedWorkflowId.Signal)

        assertEquals("how do I treat water", viewModel.guidedQuestionQuery.value)
        assertEquals(GuidedWorkflowId.Signal, viewModel.selectedWorkflowId.value)
    }

    @Test
    fun nullDatabaseSurfacesErrorState() = runBlocking {
        val viewModel = GuideViewModel(application = application, database = null)

        assertEquals(
            "Local guide database is not connected.",
            viewModel.state.value.errorMessage,
        )
    }
}
