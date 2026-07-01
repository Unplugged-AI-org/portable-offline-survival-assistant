package ai.unplugged.posa.ui

import ai.unplugged.posa.data.model.BreadcrumbTrail
import ai.unplugged.posa.data.model.InstalledMap
import ai.unplugged.posa.data.model.Waypoint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class MapActions(
    val onImportMap: (Uri) -> Unit,
    val onSetInstalledMapEnabled: (InstalledMap, Boolean) -> Unit,
    val onDeleteInstalledMap: (InstalledMap) -> Unit,
    val onSaveCurrentWaypoint: (name: String, notes: String?, coordinate: FieldCoordinate) -> Unit,
    val onDeleteWaypoint: (waypoint: Waypoint) -> Unit,
    val onStartBreadcrumb: (coordinate: FieldCoordinate?) -> Unit,
    val onStopBreadcrumb: (trail: BreadcrumbTrail) -> Unit,
    val onRecordBreadcrumbPoint: (trail: BreadcrumbTrail, coordinate: FieldCoordinate) -> Unit,
)

@Composable
internal fun MapSection(
    state: MapContentState,
    selectedWaypointId: String?,
    onSelectWaypoint: (String?) -> Unit,
    actions: MapActions,
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(context.hasLocationPermission())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            context.hasLocationPermission()
    }
    val currentLocation = rememberCurrentLocation(hasLocationPermission)
    val selectedWaypoint = state.waypoints.firstOrNull { it.id == selectedWaypointId }
    val activeTrail = state.activeTrail
    val mapImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(actions.onImportMap)
    }

    LaunchedEffect(activeTrail?.trail?.id, currentLocation?.recordedAtEpochMillis) {
        if (activeTrail != null && currentLocation != null) {
            actions.onRecordBreadcrumbPoint(activeTrail.trail, currentLocation)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MapStateBanners(state)
        MapsforgeMapSurface(
            activeInstalledMap = state.activeInstalledMap,
            mapCenter = selectedWaypoint?.toFieldCoordinate() ?: MONACO_CENTER,
        )
        InstalledMapPanel(
            state = state,
            onImportMap = { mapImportLauncher.launch(arrayOf("*/*")) },
            onSetMapEnabled = actions.onSetInstalledMapEnabled,
            onDeleteMap = actions.onDeleteInstalledMap,
        )
        LocationPanel(
            hasLocationPermission = hasLocationPermission,
            currentLocation = currentLocation,
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
        )
        WaypointPanel(
            state = state,
            currentLocation = currentLocation,
            selectedWaypoint = selectedWaypoint,
            onSelectWaypoint = onSelectWaypoint,
            onSaveCurrentWaypoint = actions.onSaveCurrentWaypoint,
            onDeleteWaypoint = actions.onDeleteWaypoint,
        )
        BreadcrumbPanel(
            state = state,
            currentLocation = currentLocation,
            activeTrail = activeTrail,
            onStartBreadcrumb = actions.onStartBreadcrumb,
            onStopBreadcrumb = actions.onStopBreadcrumb,
        )
    }
}

@Composable
private fun MapsforgeMapSurface(
    activeInstalledMap: InstalledMap?,
    mapCenter: FieldCoordinate,
) {
    val context = LocalContext.current
    var mapFile by remember { mutableStateOf<File?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }
    val mapViewHolder = remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(context, activeInstalledMap?.id, activeInstalledMap?.filePath) {
        try {
            val installedFile = activeInstalledMap?.let { File(it.filePath) }
            if (installedFile == null) {
                mapFile = copyAssetToCache(context, MAP_ASSET_PATH)
                mapError = null
            } else if (installedFile.exists() && installedFile.length() > 0L) {
                mapFile = installedFile
                mapError = null
            } else {
                mapFile = null
                mapError = "Enabled map area file is missing. Disable it or import the file again."
            }
        } catch (exception: Exception) {
            mapFile = null
            mapError = "Map file could not be opened: ${exception.message.orEmpty()}"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewHolder.value?.destroyAll()
            AndroidGraphicFactory.clearResourceMemoryCache()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val localMapFile = mapFile
            if (localMapFile == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = mapError ?: "Loading local Mapsforge test map...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                key(localMapFile.absolutePath) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            AndroidGraphicFactory.createInstance(viewContext.applicationContext)
                            MapView(viewContext).apply {
                                mapViewHolder.value = this
                                isClickable = true
                                mapScaleBar.isVisible = true
                                setBuiltInZoomControls(true)
                                addMapLayer(viewContext, localMapFile)
                                setCenter(LatLong(mapCenter.latitude, mapCenter.longitude))
                                setZoomLevel(14)
                            }
                        },
                        update = { mapView ->
                            mapView.setCenter(LatLong(mapCenter.latitude, mapCenter.longitude))
                        },
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = "Map data (c) OpenStreetMap contributors. Rendered with Mapsforge.",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun InstalledMapPanel(
    state: MapContentState,
    onImportMap: () -> Unit,
    onSetMapEnabled: (InstalledMap, Boolean) -> Unit,
    onDeleteMap: (InstalledMap) -> Unit,
) {
    PanelSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Offline map areas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(onClick = onImportMap) {
                    Icon(
                        imageVector = Icons.Outlined.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Import")
                }
            }
            Text(
                text = "Import Mapsforge .map files from local storage. Enabled areas are available without network access.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!state.isLoading && state.installedMaps.isEmpty()) {
                Text(
                    text = "No user-loaded map areas are installed. The bundled Monaco fixture remains available for renderer checks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            state.installedMaps.forEach { map ->
                InstalledMapRow(
                    map = map,
                    onSetEnabled = { isEnabled -> onSetMapEnabled(map, isEnabled) },
                    onDelete = { onDeleteMap(map) },
                )
            }
        }
    }
}

@Composable
private fun InstalledMapRow(
    map: InstalledMap,
    onSetEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (map.isEnabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (map.isEnabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = map.isEnabled,
                onCheckedChange = onSetEnabled,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = map.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${map.fileName} - ${formatByteSize(map.byteSize)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = if (map.isEnabled) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete map area",
                )
            }
        }
    }
}

@Composable
private fun LocationPanel(
    hasLocationPermission: Boolean,
    currentLocation: FieldCoordinate?,
    onRequestPermission: () -> Unit,
) {
    PanelSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Current GPS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!hasLocationPermission) {
                Text(
                    text = "Location permission is needed to show current coordinates, save a current-position waypoint, and record breadcrumbs.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onRequestPermission) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Allow location")
                }
            } else if (currentLocation == null) {
                Text(
                    text = "Waiting for a device location fix.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                CoordinateRows(currentLocation)
            }
        }
    }
}

@Composable
private fun WaypointPanel(
    state: MapContentState,
    currentLocation: FieldCoordinate?,
    selectedWaypoint: Waypoint?,
    onSelectWaypoint: (String?) -> Unit,
    onSaveCurrentWaypoint: (name: String, notes: String?, coordinate: FieldCoordinate) -> Unit,
    onDeleteWaypoint: (waypoint: Waypoint) -> Unit,
) {
    var waypointName by rememberSaveable { mutableStateOf("") }
    var waypointNotes by rememberSaveable { mutableStateOf("") }

    PanelSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Waypoints",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = waypointName,
                onValueChange = { waypointName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Current location waypoint name") },
            )
            OutlinedTextField(
                value = waypointNotes,
                onValueChange = { waypointNotes = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Notes") },
            )
            Button(
                onClick = {
                    val coordinate = currentLocation ?: return@Button
                    onSaveCurrentWaypoint(
                        waypointName.trim(),
                        waypointNotes.trim().blankToNull(),
                        coordinate,
                    )
                    waypointName = ""
                    waypointNotes = ""
                },
                enabled = currentLocation != null && waypointName.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddLocationAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text("Save current location")
            }

            HorizontalDivider()
            if (!state.isLoading && state.waypoints.isEmpty()) {
                Text(
                    text = "No waypoints are stored locally.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.waypoints.forEach { waypoint ->
                WaypointRow(
                    waypoint = waypoint,
                    currentLocation = currentLocation,
                    isSelected = selectedWaypoint?.id == waypoint.id,
                    onSelect = {
                        onSelectWaypoint(if (selectedWaypoint?.id == waypoint.id) null else waypoint.id)
                    },
                    onDelete = { onDeleteWaypoint(waypoint) },
                )
            }
        }
    }
}

@Composable
private fun WaypointRow(
    waypoint: Waypoint,
    currentLocation: FieldCoordinate?,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val distanceBearing = currentLocation?.let {
        distanceAndBearing(
            fromLatitude = it.latitude,
            fromLongitude = it.longitude,
            toLatitude = waypoint.latitude,
            toLongitude = waypoint.longitude,
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = waypoint.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatCoordinate(waypoint.latitude)}, ${formatCoordinate(waypoint.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                waypoint.notes?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                distanceBearing?.let {
                    Text(
                        text = "${formatDistance(it.distanceMeters)} away at ${formatBearing(it.bearingDegrees)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    text = "Saved ${waypoint.createdAtEpochMillis.toMapDateTimeLabel()}",
                    style = MaterialTheme.typography.labelSmall,
                )
                OutlinedButton(onClick = onSelect) {
                    Text(if (isSelected) "Hide details" else "Details")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete waypoint",
                )
            }
        }
    }
}

@Composable
private fun BreadcrumbPanel(
    state: MapContentState,
    currentLocation: FieldCoordinate?,
    activeTrail: BreadcrumbTrailSummary?,
    onStartBreadcrumb: (coordinate: FieldCoordinate?) -> Unit,
    onStopBreadcrumb: (trail: BreadcrumbTrail) -> Unit,
) {
    PanelSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Breadcrumbs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (activeTrail == null) {
                    Button(onClick = { onStartBreadcrumb(currentLocation) }) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Start")
                    }
                } else {
                    Button(onClick = { onStopBreadcrumb(activeTrail.trail) }) {
                        Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Stop")
                    }
                }
            }
            activeTrail?.let {
                Text(
                    text = "Recording ${it.points.size} breadcrumb points.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (!state.isLoading && state.breadcrumbTrails.isEmpty()) {
                Text(
                    text = "No breadcrumb trails are stored locally.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.breadcrumbTrails.forEach { summary ->
                BreadcrumbTrailRow(summary)
            }
        }
    }
}

@Composable
private fun BreadcrumbTrailRow(summary: BreadcrumbTrailSummary) {
    val trail = summary.trail
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Save,
            contentDescription = null,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = trail.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (trail.endedAtEpochMillis == null) {
                    "Active since ${trail.startedAtEpochMillis.toMapDateTimeLabel()}"
                } else {
                    "Ended ${trail.endedAtEpochMillis.toMapDateTimeLabel()}"
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${summary.points.size} points",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun CoordinateRows(coordinate: FieldCoordinate) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Latitude ${formatCoordinate(coordinate.latitude)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Longitude ${formatCoordinate(coordinate.longitude)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        coordinate.accuracyMeters?.let {
            Text(
                text = "Accuracy about ${formatDistance(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun PanelSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun MapStateBanners(state: MapContentState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.errorMessage?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun rememberCurrentLocation(enabled: Boolean): FieldCoordinate? {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<FieldCoordinate?>(null) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            return@DisposableEffect onDispose {}
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { location ->
            currentLocation = location.toFieldCoordinate()
        }

        try {
            locationManager.bestLastKnownLocation()?.let { location ->
                currentLocation = location.toFieldCoordinate()
            }
            locationManager.enabledFieldProviders().forEach { provider ->
                locationManager.requestLocationUpdates(
                    provider,
                    LOCATION_UPDATE_INTERVAL_MILLIS,
                    LOCATION_UPDATE_DISTANCE_METERS,
                    listener,
                    Looper.getMainLooper(),
                )
            }
        } catch (_: SecurityException) {
            currentLocation = null
        } catch (_: IllegalArgumentException) {
            currentLocation = null
        }

        onDispose {
            locationManager.removeUpdates(listener)
        }
    }

    return currentLocation
}

private fun MapView.addMapLayer(context: Context, mapFile: File) {
    val tileCache: TileCache = AndroidUtil.createTileCache(
        context,
        "posa-map-cache",
        model.displayModel.tileSize,
        1f,
        model.frameBufferModel.overdrawFactor,
    )
    val mapDataStore: MapDataStore = MapFile(FileInputStream(mapFile))
    val tileRendererLayer = TileRendererLayer(
        tileCache,
        mapDataStore,
        model.mapViewPosition,
        AndroidGraphicFactory.INSTANCE,
    )
    tileRendererLayer.setXmlRenderTheme(MapsforgeThemes.DEFAULT)
    layerManager.layers.add(tileRendererLayer)
}

private fun copyAssetToCache(context: Context, assetPath: String): File {
    val file = File(context.cacheDir, assetPath.substringAfterLast('/'))
    context.assets.open(assetPath).use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun LocationManager.bestLastKnownLocation(): Location? =
    enabledFieldProviders().mapNotNull { provider ->
        try {
            getLastKnownLocation(provider)
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }.maxByOrNull { it.time }

private fun LocationManager.enabledFieldProviders(): List<String> =
    listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { provider ->
            try {
                isProviderEnabled(provider)
            } catch (_: IllegalArgumentException) {
                false
            }
        }

private fun Location.toFieldCoordinate(): FieldCoordinate = FieldCoordinate(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else null,
    recordedAtEpochMillis = if (time > 0) time else System.currentTimeMillis(),
)

private fun Waypoint.toFieldCoordinate(): FieldCoordinate = FieldCoordinate(
    latitude = latitude,
    longitude = longitude,
    recordedAtEpochMillis = updatedAtEpochMillis,
)

private fun Long.toMapDateTimeLabel(): String =
    MAP_DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

private fun String.blankToNull(): String? = takeIf { it.isNotBlank() }

private fun formatByteSize(bytes: Long): String =
    when {
        bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000L -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000L -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }

private val MONACO_CENTER = FieldCoordinate(latitude = 43.7384, longitude = 7.4246)
private const val MAP_ASSET_PATH = "maps/monaco.map"
private const val LOCATION_UPDATE_INTERVAL_MILLIS = 10_000L
private const val LOCATION_UPDATE_DISTANCE_METERS = 10f

private val MAP_DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
