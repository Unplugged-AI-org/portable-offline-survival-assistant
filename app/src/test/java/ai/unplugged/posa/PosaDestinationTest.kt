package ai.unplugged.posa

import ai.unplugged.posa.ui.PosaDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosaDestinationTest {
    @Test
    fun destinationsMatchRoadmapTabs() {
        assertEquals(
            listOf("Map", "Ask", "Tools"),
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
