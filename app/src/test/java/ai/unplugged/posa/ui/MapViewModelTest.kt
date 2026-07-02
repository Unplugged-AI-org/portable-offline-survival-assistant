package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.PosaDevelopmentSeed
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
 * Room's suspend DAOs resume on Room's own executor, so mutations complete
 * asynchronously; tests await the [MapViewModel.state] StateFlow rather than
 * assuming synchronous completion. The ViewModel's viewModelScope runs on
 * Dispatchers.Main.immediate (Robolectric's main looper).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MapViewModelTest {
    private lateinit var application: Application
    private lateinit var database: PosaDatabase

    @Before
    fun setUp() {
        // viewModelScope dispatches on Dispatchers.Main; an unconfined test
        // dispatcher lets those launches resume eagerly off Room's executor
        // threads instead of a paused Robolectric main looper.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        application = ApplicationProvider.getApplicationContext()
        database = PosaDatabase.createInMemory(application)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun createViewModel(onDataChanged: () -> Unit = {}): MapViewModel =
        MapViewModel(
            application = application,
            database = database,
            onDataChanged = onDataChanged,
        )

    private suspend fun MapViewModel.awaitState(
        predicate: (MapContentState) -> Boolean,
    ): MapContentState = withTimeout(5_000) { state.first(predicate) }

    @Test
    fun loadsSeededWaypointsOnInit() = runBlocking {
        PosaDevelopmentSeed.install(database, NOW)

        val viewModel = createViewModel()

        val state = viewModel.awaitState { !it.isLoading && it.waypoints.isNotEmpty() }
        assertTrue(state.waypoints.any { it.id == PosaDevelopmentSeed.sampleWaypoint(NOW).id })
        assertNull(state.errorMessage)
    }

    @Test
    fun saveWaypointPersistsReloadsAndNotifies() = runBlocking {
        var dataChangedCount = 0
        val viewModel = createViewModel { dataChangedCount += 1 }
        viewModel.awaitState { !it.isLoading }
        val before = viewModel.state.value.waypoints.size

        viewModel.saveWaypoint(
            name = "Camp",
            notes = "near the ridge",
            coordinate = FieldCoordinate(latitude = 35.6, longitude = -82.5),
        )

        val state = viewModel.awaitState { it.waypoints.any { wp -> wp.name == "Camp" } }
        assertEquals(before + 1, state.waypoints.size)
        assertEquals(1, dataChangedCount)
    }

    @Test
    fun deleteWaypointClearsSelection() = runBlocking {
        PosaDevelopmentSeed.install(database, NOW)
        val viewModel = createViewModel()
        val waypoint = viewModel.awaitState { it.waypoints.isNotEmpty() }.waypoints.first()
        viewModel.selectWaypoint(waypoint.id)
        assertEquals(waypoint.id, viewModel.selectedWaypointId.value)

        viewModel.deleteWaypoint(waypoint)

        viewModel.awaitState { it.waypoints.none { wp -> wp.id == waypoint.id } }
        assertNull(viewModel.selectedWaypointId.value)
    }

    @Test
    fun enablingMapDisablesOtherInstalledMaps() = runBlocking {
        val installedMaps = database.repositories().installedMaps
        installedMaps.save(installedMap("map-a", isEnabled = true))
        installedMaps.save(installedMap("map-b", isEnabled = false))
        val viewModel = createViewModel()
        val mapB = viewModel.awaitState { it.installedMaps.size == 2 }
            .installedMaps.first { it.id == "map-b" }

        viewModel.setInstalledMapEnabled(mapB, isEnabled = true)

        val state = viewModel.awaitState { s -> s.installedMaps.first { it.id == "map-b" }.isEnabled }
        assertEquals(listOf("map-b"), state.installedMaps.filter { it.isEnabled }.map { it.id })
    }

    @Test
    fun nullDatabaseSurfacesErrorState() = runBlocking {
        val viewModel = MapViewModel(application = application, database = null)

        assertEquals(
            "Local map database is not connected.",
            viewModel.state.value.errorMessage,
        )
    }

    private fun installedMap(id: String, isEnabled: Boolean) =
        ai.unplugged.posa.data.model.InstalledMap(
            id = id,
            displayName = id,
            fileName = "$id.map",
            filePath = "/local/maps/$id.map",
            byteSize = 1_000L,
            isEnabled = isEnabled,
            centerLatitude = 39.7,
            centerLongitude = -104.9,
            startZoomLevel = 11,
            boundingBoxMinLatitude = 39.5,
            boundingBoxMinLongitude = -105.2,
            boundingBoxMaxLatitude = 39.9,
            boundingBoxMaxLongitude = -104.7,
            importedAtEpochMillis = NOW,
            updatedAtEpochMillis = NOW,
        )

    private companion object {
        const val NOW = 1_717_200_000_000L
    }
}
