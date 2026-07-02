package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.InstalledMapImporter
import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.repository.repositories
import ai.unplugged.posa.data.model.BreadcrumbPoint
import ai.unplugged.posa.data.model.BreadcrumbTrail
import ai.unplugged.posa.data.model.InstalledMap
import ai.unplugged.posa.data.model.Waypoint
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Owns the Map tab's content state, load, and mutations. Extracted from [PosaApp]
 * as the first step of the ViewModel migration (workstreams.md, UI Architecture #1).
 *
 * @param onDataChanged invoked after each successful mutation so callers can refresh
 *   cross-domain state that still depends on map data (e.g. tools/field notes). This
 *   is a temporary seam; decoupling it is a later task in the UI Architecture plan.
 */
internal class MapViewModel(
    application: Application,
    private val database: PosaDatabase?,
    private val onDataChanged: () -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MapContentState(isLoading = database != null))
    val state: StateFlow<MapContentState> = _state.asStateFlow()

    private val _selectedWaypointId = MutableStateFlow<String?>(null)
    val selectedWaypointId: StateFlow<String?> = _selectedWaypointId.asStateFlow()

    init {
        reload()
    }

    fun selectWaypoint(id: String?) {
        _selectedWaypointId.value = id
    }

    private fun reload() {
        val localDatabase = database
        if (localDatabase == null) {
            _state.value = MapContentState(
                errorMessage = "Local map database is not connected.",
            )
            return
        }

        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val loadedState = withContext(ioDispatcher) {
                    loadMapContent(localDatabase)
                }
                _state.value = loadedState
                val selected = _selectedWaypointId.value
                if (selected != null && loadedState.waypoints.none { it.id == selected }) {
                    _selectedWaypointId.value = null
                }
            } catch (exception: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Map data could not be loaded: ${exception.message.orEmpty()}",
                )
            }
        }
    }

    private fun mutate(block: suspend (PosaDatabase) -> Unit) {
        val localDatabase = database
        if (localDatabase == null) {
            _state.value = _state.value.copy(
                errorMessage = "Local map database is not connected.",
            )
            return
        }

        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    block(localDatabase)
                }
                reload()
                onDataChanged()
            } catch (exception: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Map data could not be saved: ${exception.message.orEmpty()}",
                )
            }
        }
    }

    fun importMap(uri: Uri) {
        val appContext = getApplication<Application>().applicationContext
        mutate { localDatabase ->
            val importedMap = InstalledMapImporter.importFromUri(
                context = appContext,
                uri = uri,
                id = newLocalId("map-area"),
            )
            val installedMaps = localDatabase.repositories().installedMaps
            installedMaps.list()
                .filter { it.isEnabled }
                .forEach { enabledMap ->
                    installedMaps.save(
                        enabledMap.copy(
                            isEnabled = false,
                            updatedAtEpochMillis = System.currentTimeMillis(),
                        ),
                    )
                }
            installedMaps.save(importedMap)
        }
    }

    fun setInstalledMapEnabled(installedMap: InstalledMap, isEnabled: Boolean) {
        mutate { localDatabase ->
            val installedMaps = localDatabase.repositories().installedMaps
            if (isEnabled) {
                installedMaps.list()
                    .filter { it.id != installedMap.id && it.isEnabled }
                    .forEach { enabledMap ->
                        installedMaps.save(
                            enabledMap.copy(
                                isEnabled = false,
                                updatedAtEpochMillis = System.currentTimeMillis(),
                            ),
                        )
                    }
            }
            installedMaps.save(
                installedMap.copy(
                    isEnabled = isEnabled,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteInstalledMap(installedMap: InstalledMap) {
        mutate { localDatabase ->
            File(installedMap.filePath).delete()
            localDatabase.repositories().installedMaps.delete(installedMap.id)
        }
    }

    fun saveCurrentWaypoint(name: String, notes: String?, coordinate: FieldCoordinate) {
        mutate { localDatabase ->
            val now = System.currentTimeMillis()
            localDatabase.repositories().waypoints.save(
                Waypoint(
                    id = newLocalId("waypoint"),
                    name = name,
                    latitude = coordinate.latitude,
                    longitude = coordinate.longitude,
                    elevationMeters = null,
                    notes = notes,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
        }
    }

    fun deleteWaypoint(waypoint: Waypoint) {
        mutate { localDatabase ->
            localDatabase.repositories().waypoints.delete(waypoint.id)
        }
        if (_selectedWaypointId.value == waypoint.id) {
            _selectedWaypointId.value = null
        }
    }

    fun startBreadcrumb(coordinate: FieldCoordinate?) {
        mutate { localDatabase ->
            val repositories = localDatabase.repositories()
            val now = System.currentTimeMillis()
            val trailId = newLocalId("breadcrumb-trail")
            repositories.breadcrumbs.saveTrail(
                BreadcrumbTrail(
                    id = trailId,
                    name = "Trail ${now.toShortDateTimeLabel()}",
                    startedAtEpochMillis = now,
                    endedAtEpochMillis = null,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            coordinate?.let {
                repositories.breadcrumbs.savePoint(
                    BreadcrumbPoint(
                        id = newLocalId("breadcrumb-point"),
                        trailId = trailId,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        accuracyMeters = it.accuracyMeters,
                        recordedAtEpochMillis = it.recordedAtEpochMillis,
                        sequenceNumber = 0,
                    ),
                )
            }
        }
    }

    fun stopBreadcrumb(trail: BreadcrumbTrail) {
        mutate { localDatabase ->
            val now = System.currentTimeMillis()
            localDatabase.repositories().breadcrumbs.saveTrail(
                trail.copy(
                    endedAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
        }
    }

    fun recordBreadcrumbPoint(trail: BreadcrumbTrail, coordinate: FieldCoordinate) {
        mutate { localDatabase ->
            val repositories = localDatabase.repositories()
            val existingPoints = repositories.breadcrumbs.listPointsForTrail(trail.id)
            val lastPoint = existingPoints.maxByOrNull { it.sequenceNumber }
            if (lastPoint == null || lastPoint.recordedAtEpochMillis != coordinate.recordedAtEpochMillis) {
                repositories.breadcrumbs.savePoint(
                    BreadcrumbPoint(
                        id = newLocalId("breadcrumb-point"),
                        trailId = trail.id,
                        latitude = coordinate.latitude,
                        longitude = coordinate.longitude,
                        accuracyMeters = coordinate.accuracyMeters,
                        recordedAtEpochMillis = coordinate.recordedAtEpochMillis,
                        sequenceNumber = (lastPoint?.sequenceNumber ?: -1) + 1,
                    ),
                )
            }
        }
    }

    companion object {
        fun factory(
            database: PosaDatabase?,
            onDataChanged: () -> Unit = {},
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MapViewModel(
                    application = this[APPLICATION_KEY]!!,
                    database = database,
                    onDataChanged = onDataChanged,
                )
            }
        }
    }
}
