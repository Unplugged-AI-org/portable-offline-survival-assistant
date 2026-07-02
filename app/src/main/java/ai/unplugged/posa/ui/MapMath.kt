package ai.unplugged.posa.ui

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal data class FieldCoordinate(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val recordedAtEpochMillis: Long = System.currentTimeMillis(),
)

internal data class DistanceBearing(
    val distanceMeters: Double,
    val bearingDegrees: Double,
)

internal fun distanceAndBearing(
    fromLatitude: Double,
    fromLongitude: Double,
    toLatitude: Double,
    toLongitude: Double,
): DistanceBearing {
    val fromLatRadians = fromLatitude.toRadians()
    val toLatRadians = toLatitude.toRadians()
    val deltaLatRadians = (toLatitude - fromLatitude).toRadians()
    val deltaLonRadians = (toLongitude - fromLongitude).toRadians()

    val haversine = sin(deltaLatRadians / 2).let { it * it } +
        cos(fromLatRadians) * cos(toLatRadians) * sin(deltaLonRadians / 2).let { it * it }
    val centralAngle = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))

    val y = sin(deltaLonRadians) * cos(toLatRadians)
    val x = cos(fromLatRadians) * sin(toLatRadians) -
        sin(fromLatRadians) * cos(toLatRadians) * cos(deltaLonRadians)
    val bearing = (atan2(y, x).toDegrees() + 360) % 360

    return DistanceBearing(
        distanceMeters = EARTH_RADIUS_METERS * centralAngle,
        bearingDegrees = bearing,
    )
}

internal fun formatCoordinate(value: Double): String = "%.6f".format(value)

internal fun formatDistance(meters: Double): String =
    if (meters >= 1000) {
        "%.2f km".format(meters / 1000)
    } else {
        "${meters.roundToInt()} m"
    }

internal fun formatBearing(degrees: Double): String = "${degrees.roundToInt()} deg ${bearingCardinal(degrees)}"

private fun bearingCardinal(degrees: Double): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((degrees + 22.5) / 45).toInt() % directions.size
    return directions[index]
}

private fun Double.toRadians(): Double = this * PI / 180.0

private fun Double.toDegrees(): Double = this * 180.0 / PI

private const val EARTH_RADIUS_METERS = 6_371_000.0
