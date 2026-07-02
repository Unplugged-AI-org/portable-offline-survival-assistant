package ai.unplugged.posa.data.local

import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InstalledMapImporterTest {
    private lateinit var mapFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        mapFile = File.createTempFile("monaco", ".map")
        context.assets.open("maps/monaco.map").use { input ->
            mapFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    @After
    fun tearDown() {
        mapFile.delete()
    }

    @Test
    fun extractViewportReadsCenterZoomAndBoundsFromMapsforgeFile() {
        val viewport = InstalledMapImporter.extractViewport(mapFile)

        assertTrue(viewport.centerLatitude in viewport.boundingBoxMinLatitude..viewport.boundingBoxMaxLatitude)
        assertTrue(viewport.centerLongitude in viewport.boundingBoxMinLongitude..viewport.boundingBoxMaxLongitude)
        assertTrue(viewport.boundingBoxMinLatitude < viewport.boundingBoxMaxLatitude)
        assertTrue(viewport.boundingBoxMinLongitude < viewport.boundingBoxMaxLongitude)
        assertNotNull(viewport.startZoomLevel)
        assertTrue(viewport.startZoomLevel in 0..22)
    }
}
