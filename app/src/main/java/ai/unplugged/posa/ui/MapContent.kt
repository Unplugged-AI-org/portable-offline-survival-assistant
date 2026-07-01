package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.repository.repositories
import ai.unplugged.posa.data.model.BreadcrumbPoint
import ai.unplugged.posa.data.model.BreadcrumbTrail
import ai.unplugged.posa.data.model.InstalledMap
import ai.unplugged.posa.data.model.Waypoint

internal data class MapContentState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val installedMaps: List<InstalledMap> = emptyList(),
    val waypoints: List<Waypoint> = emptyList(),
    val breadcrumbTrails: List<BreadcrumbTrailSummary> = emptyList(),
) {
    val activeTrail: BreadcrumbTrailSummary?
        get() = breadcrumbTrails.firstOrNull { it.trail.endedAtEpochMillis == null }

    val activeInstalledMap: InstalledMap?
        get() = installedMaps.firstOrNull { it.isEnabled }
}

internal data class BreadcrumbTrailSummary(
    val trail: BreadcrumbTrail,
    val points: List<BreadcrumbPoint>,
)

internal suspend fun loadMapContent(database: PosaDatabase): MapContentState {
    val repositories = database.repositories()
    val trails = repositories.breadcrumbs.listTrails()

    return MapContentState(
        installedMaps = repositories.installedMaps.list(),
        waypoints = repositories.waypoints.list(),
        breadcrumbTrails = trails.map { trail ->
            BreadcrumbTrailSummary(
                trail = trail,
                points = repositories.breadcrumbs.listPointsForTrail(trail.id),
            )
        },
    )
}
