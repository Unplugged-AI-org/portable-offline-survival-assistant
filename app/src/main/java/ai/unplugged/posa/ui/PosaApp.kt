package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
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
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosaApp(database: PosaDatabase? = null) {
    var selectedDestination by rememberSaveable { mutableStateOf(PosaDestination.Map) }
    var guideSearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedGuideCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var guideContentState by remember {
        mutableStateOf(GuideContentState(isLoading = database != null))
    }
    var bundledGuideInstalled by remember(database) {
        mutableStateOf(database == null)
    }
    val context = LocalContext.current

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
                guideSearchQuery = guideSearchQuery,
                selectedGuideCardId = selectedGuideCardId,
                onGuideSearchChange = {
                    guideSearchQuery = it
                    selectedGuideCardId = null
                },
                onSelectGuideCard = { selectedGuideCardId = it },
                onBackToGuideList = { selectedGuideCardId = null },
            )
        }
    }
}

@Composable
private fun DestinationScreen(
    destination: PosaDestination,
    contentPadding: PaddingValues,
    guideContentState: GuideContentState,
    guideSearchQuery: String,
    selectedGuideCardId: String?,
    onGuideSearchChange: (String) -> Unit,
    onSelectGuideCard: (String) -> Unit,
    onBackToGuideList: () -> Unit,
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
                PosaDestination.Guide -> GuideCardsSection(
                    state = guideContentState,
                    query = guideSearchQuery,
                    selectedCardId = selectedGuideCardId,
                    onQueryChange = onGuideSearchChange,
                    onSelectCard = onSelectGuideCard,
                    onBackToList = onBackToGuideList,
                )
                PosaDestination.Packs -> PacksSection(guideContentState)
                PosaDestination.Map,
                PosaDestination.Tools -> NextSteps(destination.nextSteps)
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
