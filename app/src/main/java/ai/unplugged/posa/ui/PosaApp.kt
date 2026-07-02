package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.StarterChecklistInstaller
import ai.unplugged.posa.data.local.InstalledMapImporter
import ai.unplugged.posa.data.local.repository.repositories
import ai.unplugged.posa.data.model.BreadcrumbPoint
import ai.unplugged.posa.data.model.BreadcrumbTrail
import ai.unplugged.posa.data.model.Checklist
import ai.unplugged.posa.data.model.ChecklistItem
import ai.unplugged.posa.data.model.FieldNote
import ai.unplugged.posa.data.model.GearItem
import ai.unplugged.posa.data.model.Waypoint
import ai.unplugged.posa.data.pack.BundledPackInstaller
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.unplugged.posa.ui.theme.PosaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosaApp(database: PosaDatabase? = null) {
    var selectedDestination by rememberSaveable { mutableStateOf(PosaDestination.Map) }
    var guidedQuestionQuery by rememberSaveable { mutableStateOf("") }
    var guideSearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedGuideCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedWorkflowId by rememberSaveable { mutableStateOf(GuidedWorkflowId.Water) }
    var guideContentState by remember {
        mutableStateOf(GuideContentState(isLoading = database != null))
    }
    var toolsContentState by remember {
        mutableStateOf(ToolsContentState(isLoading = database != null))
    }
    var mapContentState by remember {
        mutableStateOf(MapContentState(isLoading = database != null))
    }
    var toolsReloadToken by remember { mutableStateOf(0) }
    var mapReloadToken by remember { mutableStateOf(0) }
    var selectedWaypointId by rememberSaveable { mutableStateOf<String?>(null) }
    var bundledGuideInstalled by remember(database) {
        mutableStateOf(database == null)
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun runToolsMutation(block: suspend (PosaDatabase) -> Unit) {
        val localDatabase = database
        if (localDatabase == null) {
            toolsContentState = toolsContentState.copy(
                errorMessage = "Local tools database is not connected.",
            )
            return
        }

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    block(localDatabase)
                }
                toolsReloadToken += 1
            } catch (exception: Exception) {
                toolsContentState = toolsContentState.copy(
                    isLoading = false,
                    errorMessage = "Tools data could not be saved: ${exception.message.orEmpty()}",
                )
            }
        }
    }

    fun runMapMutation(block: suspend (PosaDatabase) -> Unit) {
        val localDatabase = database
        if (localDatabase == null) {
            mapContentState = mapContentState.copy(
                errorMessage = "Local map database is not connected.",
            )
            return
        }

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    block(localDatabase)
                }
                mapReloadToken += 1
                toolsReloadToken += 1
            } catch (exception: Exception) {
                mapContentState = mapContentState.copy(
                    isLoading = false,
                    errorMessage = "Map data could not be saved: ${exception.message.orEmpty()}",
                )
            }
        }
    }

    val mapActions = MapActions(
        onImportMap = { uri ->
            runMapMutation { localDatabase ->
                val importedMap = InstalledMapImporter.importFromUri(
                    context = context.applicationContext,
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
        },
        onSetInstalledMapEnabled = { installedMap, isEnabled ->
            runMapMutation { localDatabase ->
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
        },
        onDeleteInstalledMap = { installedMap ->
            runMapMutation { localDatabase ->
                File(installedMap.filePath).delete()
                localDatabase.repositories().installedMaps.delete(installedMap.id)
            }
        },
        onSaveCurrentWaypoint = { name, notes, coordinate ->
            runMapMutation { localDatabase ->
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
        },
        onDeleteWaypoint = { waypoint ->
            runMapMutation { localDatabase ->
                localDatabase.repositories().waypoints.delete(waypoint.id)
            }
            if (selectedWaypointId == waypoint.id) {
                selectedWaypointId = null
            }
        },
        onStartBreadcrumb = { coordinate ->
            runMapMutation { localDatabase ->
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
        },
        onStopBreadcrumb = { trail ->
            runMapMutation { localDatabase ->
                val now = System.currentTimeMillis()
                localDatabase.repositories().breadcrumbs.saveTrail(
                    trail.copy(
                        endedAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )
            }
        },
        onRecordBreadcrumbPoint = { trail, coordinate ->
            runMapMutation { localDatabase ->
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
        },
    )

    val toolsActions = ToolsActions(
        onCreateChecklist = { title, description ->
            runToolsMutation { localDatabase ->
                val now = System.currentTimeMillis()
                localDatabase.repositories().checklists.saveChecklist(
                    Checklist(
                        id = newLocalId("checklist"),
                        title = title,
                        description = description,
                        isArchived = false,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )
            }
        },
        onUpdateChecklist = { checklist, title, description, isArchived ->
            runToolsMutation { localDatabase ->
                localDatabase.repositories().checklists.saveChecklist(
                    checklist.copy(
                        title = title,
                        description = description,
                        isArchived = isArchived,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }
        },
        onDeleteChecklist = { checklist ->
            runToolsMutation { localDatabase ->
                localDatabase.repositories().checklists.deleteChecklist(checklist.id)
            }
        },
        onCreateChecklistItem = { checklistId, label, details ->
            runToolsMutation { localDatabase ->
                val repositories = localDatabase.repositories()
                val now = System.currentTimeMillis()
                val nextPosition = repositories.checklists
                    .listItemsForChecklist(checklistId)
                    .maxOfOrNull { it.position + 1 } ?: 0
                repositories.checklists.saveItem(
                    ChecklistItem(
                        id = newLocalId("checklist-item"),
                        checklistId = checklistId,
                        label = label,
                        details = details,
                        position = nextPosition,
                        isChecked = false,
                        updatedAtEpochMillis = now,
                    ),
                )
            }
        },
        onUpdateChecklistItem = { item, label, details, isChecked ->
            runToolsMutation { localDatabase ->
                localDatabase.repositories().checklists.saveItem(
                    item.copy(
                        label = label,
                        details = details,
                        isChecked = isChecked,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }
        },
        onDeleteChecklistItem = { item ->
            runToolsMutation { localDatabase ->
                localDatabase.repositories().checklists.deleteItem(item.id)
            }
        },
        onCreateGearItem = { draft ->
            runToolsMutation { localDatabase ->
                val now = System.currentTimeMillis()
                localDatabase.repositories().gear.save(
                    GearItem(
                        id = newLocalId("gear"),
                        name = draft.name,
                        category = draft.category,
                        quantity = draft.quantity,
                        condition = draft.condition,
                        notes = draft.notes,
                        isAvailable = draft.isAvailable,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    ),
                )
            }
        },
        onUpdateGearItem = { item, draft ->
            runToolsMutation { localDatabase ->
                localDatabase.repositories().gear.save(
                    item.copy(
                        name = draft.name,
                        category = draft.category,
                        quantity = draft.quantity,
                        condition = draft.condition,
                        notes = draft.notes,
                        isAvailable = draft.isAvailable,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }
        },
        onDeleteGearItem = { item ->
            runToolsMutation { localDatabase ->
                localDatabase.repositories().gear.delete(item.id)
            }
        },
        onCreateFieldNote = { draft ->
            runToolsMutation { localDatabase ->
                val now = System.currentTimeMillis()
                localDatabase.repositories().fieldNotes.save(
                    FieldNote(
                        id = newLocalId("field-note"),
                        title = draft.title,
                        body = draft.body,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                        latitude = draft.latitude,
                        longitude = draft.longitude,
                        waypointId = draft.waypointId,
                        checklistId = draft.checklistId,
                        guideCardId = draft.guideCardId,
                        gearItemId = draft.gearItemId,
                    ),
                )
            }
        },
        onUpdateFieldNote = { note, draft ->
            runToolsMutation { localDatabase ->
                localDatabase.repositories().fieldNotes.save(
                    note.copy(
                        title = draft.title,
                        body = draft.body,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                        latitude = draft.latitude,
                        longitude = draft.longitude,
                        waypointId = draft.waypointId,
                        checklistId = draft.checklistId,
                        guideCardId = draft.guideCardId,
                        gearItemId = draft.gearItemId,
                    ),
                )
            }
        },
        onDeleteFieldNote = { note ->
            runToolsMutation { localDatabase ->
                localDatabase.repositories().fieldNotes.delete(note.id)
            }
        },
    )

    LaunchedEffect(database) {
        val localDatabase = database
        if (localDatabase == null) {
            guideContentState = GuideContentState(
                errorMessage = "Local guide database is not connected.",
            )
            return@LaunchedEffect
        }

        guideContentState = guideContentState.copy(isLoading = true, errorMessage = null)
        try {
            withContext(Dispatchers.IO) {
                BundledPackInstaller.installAll(context, localDatabase)
            }
            bundledGuideInstalled = true
        } catch (exception: Exception) {
            bundledGuideInstalled = false
            guideContentState = guideContentState.copy(
                isLoading = false,
                errorMessage = "Bundled guide pack could not be loaded: ${exception.message.orEmpty()}",
            )
        }
    }

    LaunchedEffect(database, bundledGuideInstalled, toolsReloadToken) {
        val localDatabase = database
        if (localDatabase == null) {
            toolsContentState = ToolsContentState(
                errorMessage = "Local tools database is not connected.",
            )
            return@LaunchedEffect
        }

        toolsContentState = toolsContentState.copy(isLoading = true, errorMessage = null)
        try {
            val loadedState = withContext(Dispatchers.IO) {
                StarterChecklistInstaller.installIfNeeded(context, localDatabase)
                loadToolsContent(localDatabase)
            }
            toolsContentState = loadedState
        } catch (exception: Exception) {
            toolsContentState = toolsContentState.copy(
                isLoading = false,
                errorMessage = "Tools data could not be loaded: ${exception.message.orEmpty()}",
            )
        }
    }

    LaunchedEffect(database, mapReloadToken) {
        val localDatabase = database
        if (localDatabase == null) {
            mapContentState = MapContentState(
                errorMessage = "Local map database is not connected.",
            )
            return@LaunchedEffect
        }

        mapContentState = mapContentState.copy(isLoading = true, errorMessage = null)
        try {
            val loadedState = withContext(Dispatchers.IO) {
                loadMapContent(localDatabase)
            }
            mapContentState = loadedState
            if (selectedWaypointId != null && loadedState.waypoints.none { it.id == selectedWaypointId }) {
                selectedWaypointId = null
            }
        } catch (exception: Exception) {
            mapContentState = mapContentState.copy(
                isLoading = false,
                errorMessage = "Map data could not be loaded: ${exception.message.orEmpty()}",
            )
        }
    }

    LaunchedEffect(database, bundledGuideInstalled, guideSearchQuery) {
        val localDatabase = database
        if (localDatabase == null || !bundledGuideInstalled) {
            return@LaunchedEffect
        }

        guideContentState = guideContentState.copy(isLoading = true, errorMessage = null)
        try {
            val loadedState = withContext(Dispatchers.IO) {
                loadGuideContent(localDatabase, guideSearchQuery)
            }
            guideContentState = loadedState
            if (selectedGuideCardId != null && loadedState.cards.none { it.card.id == selectedGuideCardId }) {
                selectedGuideCardId = null
            }
        } catch (exception: Exception) {
            guideContentState = guideContentState.copy(
                isLoading = false,
                errorMessage = "Guide cards could not be loaded: ${exception.message.orEmpty()}",
            )
        }
    }

    PosaTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "POSA",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Portable Offline Survival Assistant",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
            bottomBar = {
                NavigationBar {
                    PosaDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selectedDestination == destination,
                            onClick = { selectedDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            DestinationScreen(
                destination = selectedDestination,
                contentPadding = innerPadding,
                guideContentState = guideContentState,
                guidedWorkflows = buildGuidedWorkflows(
                    guideState = guideContentState,
                    toolsState = toolsContentState,
                    mapState = mapContentState,
                ),
                guidedQuestionResult = answerGuidedQuestion(
                    guideState = guideContentState,
                    toolsState = toolsContentState,
                    mapState = mapContentState,
                    question = guidedQuestionQuery,
                ),
                mapContentState = mapContentState,
                toolsContentState = toolsContentState,
                mapActions = mapActions,
                toolsActions = toolsActions,
                guidedQuestionQuery = guidedQuestionQuery,
                guideSearchQuery = guideSearchQuery,
                selectedGuideCardId = selectedGuideCardId,
                selectedWorkflowId = selectedWorkflowId,
                selectedWaypointId = selectedWaypointId,
                onGuidedQuestionChange = { guidedQuestionQuery = it },
                onGuideSearchChange = {
                    guideSearchQuery = it
                    selectedGuideCardId = null
                },
                onSelectGuideCard = { selectedGuideCardId = it },
                onSelectWorkflow = { selectedWorkflowId = it },
                onBackToGuideList = { selectedGuideCardId = null },
                onSelectWaypoint = { selectedWaypointId = it },
            )
        }
    }
}

@Composable
private fun DestinationScreen(
    destination: PosaDestination,
    contentPadding: PaddingValues,
    guideContentState: GuideContentState,
    guidedWorkflows: List<GuidedWorkflowResult>,
    guidedQuestionResult: GuidedQuestionResult,
    mapContentState: MapContentState,
    toolsContentState: ToolsContentState,
    mapActions: MapActions,
    toolsActions: ToolsActions,
    guidedQuestionQuery: String,
    guideSearchQuery: String,
    selectedGuideCardId: String?,
    selectedWorkflowId: GuidedWorkflowId,
    selectedWaypointId: String?,
    onGuidedQuestionChange: (String) -> Unit,
    onGuideSearchChange: (String) -> Unit,
    onSelectGuideCard: (String) -> Unit,
    onSelectWorkflow: (GuidedWorkflowId) -> Unit,
    onBackToGuideList: () -> Unit,
    onSelectWaypoint: (String?) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DestinationHeader(destination)
            OfflineStatus(destination.offlineState)
            when (destination) {
                PosaDestination.Guide -> GuideSection(
                    state = guideContentState,
                    workflows = guidedWorkflows,
                    questionResult = guidedQuestionResult,
                    question = guidedQuestionQuery,
                    query = guideSearchQuery,
                    selectedCardId = selectedGuideCardId,
                    selectedWorkflowId = selectedWorkflowId,
                    onQuestionChange = onGuidedQuestionChange,
                    onQueryChange = onGuideSearchChange,
                    onSelectCard = onSelectGuideCard,
                    onSelectWorkflow = onSelectWorkflow,
                    onBackToList = onBackToGuideList,
                )
                PosaDestination.Packs -> PacksSection(guideContentState)
                PosaDestination.Tools -> ToolsSection(
                    state = toolsContentState,
                    actions = toolsActions,
                )
                PosaDestination.Map -> MapSection(
                    state = mapContentState,
                    selectedWaypointId = selectedWaypointId,
                    onSelectWaypoint = onSelectWaypoint,
                    actions = mapActions,
                )
            }
        }
    }
}

@Composable
private fun DestinationHeader(destination: PosaDestination) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = destination.headline,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = destination.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun OfflineStatus(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Offline-first status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun NextSteps(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Reserved for upcoming phases",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider()
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(4.dp),
                    content = {},
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

private val PosaDestination.icon: ImageVector
    get() = when (this) {
        PosaDestination.Map -> Icons.Outlined.Map
        PosaDestination.Tools -> Icons.Outlined.Build
        PosaDestination.Guide -> Icons.AutoMirrored.Outlined.MenuBook
        PosaDestination.Packs -> Icons.Outlined.Inventory2
    }

private fun newLocalId(prefix: String): String = "$prefix-${UUID.randomUUID()}"

private fun Long.toShortDateTimeLabel(): String =
    java.time.format.DateTimeFormatter.ofPattern("MMM d h:mm a")
        .format(java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()))
