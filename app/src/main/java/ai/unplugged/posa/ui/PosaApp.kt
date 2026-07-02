package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.unplugged.posa.ui.theme.PosaTheme
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosaApp(database: PosaDatabase? = null) {
    var selectedDestination by rememberSaveable { mutableStateOf(PosaDestination.Map) }

    val toolsViewModel: ToolsViewModel = viewModel(
        factory = ToolsViewModel.factory(database),
    )

    // Tools content reflects guide cards and waypoints owned by the other VMs, so
    // both wire a reload seam to refresh tools after their mutations/install.
    val mapViewModel: MapViewModel = viewModel(
        factory = MapViewModel.factory(database) { toolsViewModel.reload() },
    )
    val guideViewModel: GuideViewModel = viewModel(
        factory = GuideViewModel.factory(database) { toolsViewModel.reload() },
    )

    PosaTheme {
        Scaffold(
            // The Map tab is a full-screen, ATAK-style map: no top app bar, its own
            // in-map menu button provides the tools.
            topBar = {
                if (selectedDestination != PosaDestination.Map) {
                    PosaTopBar()
                }
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
                guideViewModel = guideViewModel,
                toolsViewModel = toolsViewModel,
                mapViewModel = mapViewModel,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosaTopBar() {
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
}

@Composable
private fun DestinationScreen(
    destination: PosaDestination,
    contentPadding: PaddingValues,
    guideViewModel: GuideViewModel,
    toolsViewModel: ToolsViewModel,
    mapViewModel: MapViewModel,
) {
    // Map is full-bleed: it manages its own layout and insets rather than the
    // header + scrolling-column wrapper the other tabs use.
    if (destination == PosaDestination.Map) {
        MapDestination(mapViewModel, contentPadding)
        return
    }

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
            when (destination) {
                PosaDestination.Guide -> GuideDestination(
                    guideViewModel = guideViewModel,
                    toolsViewModel = toolsViewModel,
                    mapViewModel = mapViewModel,
                )
                PosaDestination.Packs -> {
                    val guideContentState by guideViewModel.state.collectAsState()
                    PacksSection(guideContentState)
                }
                PosaDestination.Tools -> ToolsDestination(toolsViewModel)
                PosaDestination.Map -> Unit // handled full-screen above
            }
        }
    }
}

@Composable
private fun GuideDestination(
    guideViewModel: GuideViewModel,
    toolsViewModel: ToolsViewModel,
    mapViewModel: MapViewModel,
) {
    // The Guide tab aggregates all three domains: workflows and answers are derived
    // from guide + tools + map state together.
    val guideContentState by guideViewModel.state.collectAsState()
    val toolsContentState by toolsViewModel.state.collectAsState()
    val mapContentState by mapViewModel.state.collectAsState()
    val guidedQuestionQuery by guideViewModel.guidedQuestionQuery.collectAsState()
    val guideSearchQuery by guideViewModel.guideSearchQuery.collectAsState()
    val selectedGuideCardId by guideViewModel.selectedGuideCardId.collectAsState()
    val selectedWorkflowId by guideViewModel.selectedWorkflowId.collectAsState()

    GuideSection(
        state = guideContentState,
        workflows = buildGuidedWorkflows(
            guideState = guideContentState,
            toolsState = toolsContentState,
            mapState = mapContentState,
        ),
        questionResult = answerGuidedQuestion(
            guideState = guideContentState,
            toolsState = toolsContentState,
            mapState = mapContentState,
            question = guidedQuestionQuery,
        ),
        question = guidedQuestionQuery,
        query = guideSearchQuery,
        selectedCardId = selectedGuideCardId,
        selectedWorkflowId = selectedWorkflowId,
        onQuestionChange = guideViewModel::setGuidedQuestion,
        onQueryChange = guideViewModel::setGuideSearch,
        onSelectCard = guideViewModel::selectCard,
        onSelectWorkflow = guideViewModel::selectWorkflow,
        onBackToList = guideViewModel::clearSelectedCard,
    )
}

@Composable
private fun ToolsDestination(toolsViewModel: ToolsViewModel) {
    val toolsContentState by toolsViewModel.state.collectAsState()
    ToolsSection(
        state = toolsContentState,
        actions = ToolsActions(
            onCreateChecklist = toolsViewModel::createChecklist,
            onUpdateChecklist = toolsViewModel::updateChecklist,
            onDeleteChecklist = toolsViewModel::deleteChecklist,
            onCreateChecklistItem = toolsViewModel::createChecklistItem,
            onUpdateChecklistItem = toolsViewModel::updateChecklistItem,
            onDeleteChecklistItem = toolsViewModel::deleteChecklistItem,
            onCreateGearItem = toolsViewModel::createGearItem,
            onUpdateGearItem = toolsViewModel::updateGearItem,
            onDeleteGearItem = toolsViewModel::deleteGearItem,
            onCreateFieldNote = toolsViewModel::createFieldNote,
            onUpdateFieldNote = toolsViewModel::updateFieldNote,
            onDeleteFieldNote = toolsViewModel::deleteFieldNote,
        ),
    )
}

@Composable
private fun MapDestination(mapViewModel: MapViewModel, contentPadding: PaddingValues) {
    val mapContentState by mapViewModel.state.collectAsState()
    val selectedWaypointId by mapViewModel.selectedWaypointId.collectAsState()
    MapSection(
        state = mapContentState,
        selectedWaypointId = selectedWaypointId,
        contentPadding = contentPadding,
        onSelectWaypoint = mapViewModel::selectWaypoint,
        actions = MapActions(
            onImportMap = mapViewModel::importMap,
            onSetInstalledMapEnabled = mapViewModel::setInstalledMapEnabled,
            onDeleteInstalledMap = mapViewModel::deleteInstalledMap,
            onSaveWaypoint = mapViewModel::saveWaypoint,
            onUpdateWaypoint = mapViewModel::updateWaypoint,
            onDeleteWaypoint = mapViewModel::deleteWaypoint,
            onStartBreadcrumb = mapViewModel::startBreadcrumb,
            onStopBreadcrumb = mapViewModel::stopBreadcrumb,
            onRecordBreadcrumbPoint = mapViewModel::recordBreadcrumbPoint,
        ),
    )
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

internal fun newLocalId(prefix: String): String = "$prefix-${UUID.randomUUID()}"

internal fun Long.toShortDateTimeLabel(): String =
    java.time.format.DateTimeFormatter.ofPattern("MMM d h:mm a")
        .format(java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()))
