package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.repository.repositories
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
 * Awaits [ToolsViewModel.state] because Room's suspend DAOs resume on Room's own
 * executor, so mutations complete asynchronously. See [MapViewModelTest] for the
 * dispatcher rationale.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ToolsViewModelTest {
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

    private fun createViewModel(): ToolsViewModel =
        ToolsViewModel(application = application, database = database)

    private suspend fun ToolsViewModel.awaitState(
        predicate: (ToolsContentState) -> Boolean,
    ): ToolsContentState = withTimeout(5_000) { state.first(predicate) }

    @Test
    fun installsStarterChecklistOnInit() = runBlocking {
        val viewModel = createViewModel()

        val state = viewModel.awaitState { !it.isLoading && it.checklists.isNotEmpty() }
        assertNull(state.errorMessage)
    }

    @Test
    fun createChecklistPersistsAndReloads() = runBlocking {
        val viewModel = createViewModel()
        viewModel.awaitState { !it.isLoading }
        val before = viewModel.state.value.checklists.size

        viewModel.createChecklist(title = "Alpine Day Kit", description = "test")

        val state = viewModel.awaitState { s ->
            s.checklists.any { it.checklist.title == "Alpine Day Kit" }
        }
        assertEquals(before + 1, state.checklists.size)
    }

    @Test
    fun createChecklistItemAssignsNextPosition() = runBlocking {
        val viewModel = createViewModel()
        viewModel.awaitState { !it.isLoading }
        viewModel.createChecklist(title = "Pack List", description = null)
        val checklist = viewModel
            .awaitState { s -> s.checklists.any { it.checklist.title == "Pack List" } }
            .checklists.first { it.checklist.title == "Pack List" }.checklist

        // Await the first item before adding the second: position is derived from
        // existing items, so concurrent creates would race (as rapid taps would).
        viewModel.createChecklistItem(checklist.id, label = "Water", details = null)
        viewModel.awaitState { s ->
            s.checklists.first { it.checklist.id == checklist.id }.items.size == 1
        }
        viewModel.createChecklistItem(checklist.id, label = "Map", details = null)

        val items = viewModel.awaitState { s ->
            s.checklists.first { it.checklist.id == checklist.id }.items.size == 2
        }.checklists.first { it.checklist.id == checklist.id }.items
        assertEquals(listOf(0, 1), items.map { it.position })
    }

    @Test
    fun reloadPicksUpExternallyInsertedGear() = runBlocking {
        val viewModel = createViewModel()
        viewModel.awaitState { !it.isLoading }

        // Simulate data written by another owner (e.g. a map mutation seam).
        database.repositories().gear.save(
            ai.unplugged.posa.data.model.GearItem(
                id = "gear-ext",
                name = "Headlamp",
                category = "Lighting",
                quantity = 1,
                condition = null,
                notes = null,
                isAvailable = true,
                createdAtEpochMillis = NOW,
                updatedAtEpochMillis = NOW,
            ),
        )
        viewModel.reload()

        val state = viewModel.awaitState { s -> s.gear.any { it.id == "gear-ext" } }
        assertTrue(state.gear.any { it.name == "Headlamp" })
    }

    @Test
    fun nullDatabaseSurfacesErrorState() = runBlocking {
        val viewModel = ToolsViewModel(application = application, database = null)

        assertEquals(
            "Local tools database is not connected.",
            viewModel.state.value.errorMessage,
        )
    }

    private companion object {
        const val NOW = 1_717_200_000_000L
    }
}
