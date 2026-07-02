package ai.unplugged.posa.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MapMathTest {
    @Test
    fun distanceAndBearingCalculatesEastboundEquatorLeg() {
        val result = distanceAndBearing(
            fromLatitude = 0.0,
            fromLongitude = 0.0,
            toLatitude = 0.0,
            toLongitude = 1.0,
        )

        assertEquals(111_195.0, result.distanceMeters, 1.0)
        assertEquals(90.0, result.bearingDegrees, 0.1)
    }

    @Test
    fun formattingKeepsCoordinatesDistanceAndBearingScannable() {
        assertEquals("35.600950", formatCoordinate(35.60095))
        assertEquals("850 m", formatDistance(850.4))
        assertEquals("1.50 km", formatDistance(1500.0))
        assertEquals("225 deg SW", formatBearing(225.0))
    }
}
