package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.PosaDevelopmentSeed
import ai.unplugged.posa.data.local.repository.repositories
import ai.unplugged.posa.data.model.InstalledMap
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MapContentTest {
    private lateinit var database: PosaDatabase

    @Before
    fun setUp() {
        database = PosaDatabase.createInMemory(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadMapContentIncludesWaypointsTrailsPointsAndActiveTrail() = runTest {
        PosaDevelopmentSeed.install(database, NOW)
        val installedMap = InstalledMap(
            id = "map-area-test",
            displayName = "Local Area",
            fileName = "local-area.map",
            filePath = "/local/maps/local-area.map",
            byteSize = 42_000L,
            isEnabled = true,
            importedAtEpochMillis = NOW,
            updatedAtEpochMillis = NOW,
        )
        database.repositories().installedMaps.save(installedMap)

        val state = loadMapContent(database)

        assertEquals(listOf(installedMap), state.installedMaps)
        assertEquals(installedMap, state.activeInstalledMap)
        assertEquals(listOf(PosaDevelopmentSeed.sampleWaypoint(NOW)), state.waypoints)
        assertEquals(1, state.breadcrumbTrails.size)
        assertEquals(PosaDevelopmentSeed.sampleBreadcrumbTrail(NOW), state.breadcrumbTrails.single().trail)
        assertEquals(listOf(PosaDevelopmentSeed.sampleBreadcrumbPoint(NOW)), state.breadcrumbTrails.single().points)
        assertNotNull(state.activeTrail)
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
