package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.PosaDevelopmentSeed
import ai.unplugged.posa.data.local.repository.repositories
import ai.unplugged.posa.data.model.InstalledMap
import androidx.test.core.app.ApplicationProvider
import java.io.File
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
            centerLatitude = 39.7392,
            centerLongitude = -104.9903,
            startZoomLevel = 11,
            boundingBoxMinLatitude = 39.5,
            boundingBoxMinLongitude = -105.2,
            boundingBoxMaxLatitude = 39.9,
            boundingBoxMaxLongitude = -104.7,
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

    @Test
    fun activeInstalledMapUsesFirstEnabledMapAndFallsBackWhenNoneEnabled() = runTest {
        val disabledMap = InstalledMap(
            id = "map-area-disabled",
            displayName = "Disabled Area",
            fileName = "disabled.map",
            filePath = "/local/maps/disabled.map",
            byteSize = 1_000L,
            isEnabled = false,
            centerLatitude = 34.05,
            centerLongitude = -118.24,
            startZoomLevel = 10,
            boundingBoxMinLatitude = 33.9,
            boundingBoxMinLongitude = -118.5,
            boundingBoxMaxLatitude = 34.2,
            boundingBoxMaxLongitude = -118.0,
            importedAtEpochMillis = NOW,
            updatedAtEpochMillis = NOW,
        )
        val enabledMap = disabledMap.copy(
            id = "map-area-enabled",
            displayName = "Enabled Area",
            fileName = "enabled.map",
            filePath = "/local/maps/enabled.map",
            isEnabled = true,
            centerLatitude = 36.1699,
            centerLongitude = -115.1398,
        )
        database.repositories().installedMaps.save(disabledMap)
        database.repositories().installedMaps.save(enabledMap)

        assertEquals(enabledMap, loadMapContent(database).activeInstalledMap)

        database.repositories().installedMaps.save(enabledMap.copy(isEnabled = false))

        assertEquals(null, loadMapContent(database).activeInstalledMap)
    }

    @Test
    fun loadMapContentRepairsMissingInstalledMapViewportMetadata() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val mapFile = File.createTempFile("monaco", ".map")
        context.assets.open("maps/monaco.map").use { input ->
            mapFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val legacyMap = InstalledMap(
            id = "map-area-legacy",
            displayName = "Legacy Area",
            fileName = "legacy.map",
            filePath = mapFile.absolutePath,
            byteSize = mapFile.length(),
            isEnabled = true,
            centerLatitude = null,
            centerLongitude = null,
            startZoomLevel = null,
            boundingBoxMinLatitude = null,
            boundingBoxMinLongitude = null,
            boundingBoxMaxLatitude = null,
            boundingBoxMaxLongitude = null,
            importedAtEpochMillis = NOW,
            updatedAtEpochMillis = NOW,
        )
        database.repositories().installedMaps.save(legacyMap)

        val activeMap = loadMapContent(database).activeInstalledMap
        val persistedMap = database.repositories().installedMaps.get(legacyMap.id)

        assertNotNull(activeMap?.centerLatitude)
        assertNotNull(activeMap?.centerLongitude)
        assertEquals(activeMap?.centerLatitude, persistedMap?.centerLatitude)
        assertEquals(activeMap?.centerLongitude, persistedMap?.centerLongitude)

        mapFile.delete()
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
