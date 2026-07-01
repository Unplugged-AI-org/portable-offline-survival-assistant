package ai.unplugged.posa

import ai.unplugged.posa.ui.PosaDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosaDestinationTest {
    @Test
    fun phaseOneDestinationsMatchRoadmapTabs() {
        assertEquals(
            listOf("Map", "Tools", "Guide", "Packs"),
            PosaDestination.entries.map { it.label },
        )
    }

    @Test
    fun destinationsStateOfflineBoundaries() {
        PosaDestination.entries.forEach { destination ->
            assertTrue(destination.offlineState.isNotBlank())
            assertTrue(destination.nextSteps.isNotEmpty())
        }
    }
}
