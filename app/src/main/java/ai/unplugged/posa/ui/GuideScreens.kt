package ai.unplugged.posa.ui

import ai.unplugged.posa.data.model.Pack
import ai.unplugged.posa.data.model.Provenance
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset

@Composable
internal fun GuideSection(
    state: GuideContentState,
    workflows: List<GuidedWorkflowResult>,
    questionResult: GuidedQuestionResult,
    question: String,
    query: String,
    selectedCardId: String?,
    selectedWorkflowId: GuidedWorkflowId,
    onQuestionChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectCard: (String) -> Unit,
    onSelectWorkflow: (GuidedWorkflowId) -> Unit,
    onBackToList: () -> Unit,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Ask", "Workflows", "Cards")
    LaunchedEffect(selectedCardId) {
        if (selectedCardId != null) {
            selectedTabIndex = 2
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StateBanners(state)
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(label) },
                )
            }
        }
        when (selectedTabIndex) {
            0 -> GuidedQuestionSection(
                question = question,
                result = questionResult,
                onQuestionChange = onQuestionChange,
                onOpenGuideCard = { cardId ->
                    selectedTabIndex = 2
                    onSelectCard(cardId)
                },
            )
            1 -> GuidedWorkflowsSection(
                workflows = workflows,
                selectedWorkflowId = selectedWorkflowId,
                onSelectWorkflow = onSelectWorkflow,
                onOpenGuideCard = { cardId ->
                    selectedTabIndex = 2
                    onSelectCard(cardId)
                },
            )
            else -> GuideCardsSection(
                state = state,
                query = query,
                selectedCardId = selectedCardId,
                onQueryChange = onQueryChange,
                onSelectCard = onSelectCard,
                onBackToList = onBackToList,
            )
        }
    }
}

@Composable
private fun GuidedQuestionSection(
    question: String,
    result: GuidedQuestionResult,
    onQuestionChange: (String) -> Unit,
    onOpenGuideCard: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = question,
            onValueChange = onQuestionChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
            label = { Text("Ask installed packs") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                )
            },
        )
        QuestionPromptSuggestions(onQuestionChange)
        RetrievalStatusPanel(result)
        if (result.sourceMatches.isNotEmpty()) {
            RetrievalSourceCards(
                matches = result.sourceMatches,
                onOpenGuideCard = onOpenGuideCard,
            )
        }
        RetrievalContextPanel(
            gearFacts = result.gearFacts,
            mapFacts = result.mapFacts,
        )
    }
}

@Composable
private fun QuestionPromptSuggestions(onQuestionChange: (String) -> Unit) {
    val prompts = listOf(
        "How should I plan water?",
        "What can help if I am lost?",
        "How can I signal from my location?",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        prompts.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { prompt ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuestionChange(prompt) },
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = prompt,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RetrievalStatusPanel(result: GuidedQuestionResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (result.hasQuestion && !result.hasSourceAnswer) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (result.hasQuestion && !result.hasSourceAnswer) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = result.statusText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            MetadataLine("Confidence", result.confidence.label)
            Text(
                text = "No generated medical or survival claims are added beyond installed-source excerpts.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RetrievalSourceCards(
    matches: List<GuidedQuestionSourceMatch>,
    onOpenGuideCard: (String) -> Unit,
) {
    WorkflowPanel("Retrieved source cards") {
        matches.forEach { match ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGuideCard(match.item.card.id) },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = match.item.card.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = match.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    MetadataLine("Confidence", match.confidence.label)
                    MetadataLine("Matched", match.matchedTerms.joinToString(", "))
                    MetadataLine("Source", match.item.provenance?.sourceTitle)
                    MetadataLine("Citation", match.item.provenance?.citation)
                    MetadataLine("URL", match.item.provenance?.sourceUrl)
                    MetadataLine("Review", match.item.provenance?.reviewStatus ?: match.item.pack?.reviewStatus)
                }
            }
        }
    }
}

@Composable
private fun RetrievalContextPanel(
    gearFacts: List<String>,
    mapFacts: List<String>,
) {
    if (gearFacts.isEmpty() && mapFacts.isEmpty()) {
        return
    }

    WorkflowPanel("Local context") {
        gearFacts.forEach { fact ->
            Text(
                text = "Gear - $fact",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        mapFacts.forEach { fact ->
            Text(
                text = "Map - $fact",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun GuidedWorkflowsSection(
    workflows: List<GuidedWorkflowResult>,
    selectedWorkflowId: GuidedWorkflowId,
    onSelectWorkflow: (GuidedWorkflowId) -> Unit,
    onOpenGuideCard: (String) -> Unit,
) {
    val selectedWorkflow = workflows.firstOrNull { it.id == selectedWorkflowId } ?: workflows.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (workflows.isEmpty()) {
            EmptyText("No guided workflows can be built until local guide content is loaded.")
            return
        }
        WorkflowSelector(
            workflows = workflows,
            selectedWorkflowId = selectedWorkflow?.id,
            onSelectWorkflow = onSelectWorkflow,
        )
        selectedWorkflow?.let {
            WorkflowDetail(
                workflow = it,
                onOpenGuideCard = onOpenGuideCard,
            )
        }
    }
}

@Composable
private fun WorkflowSelector(
    workflows: List<GuidedWorkflowResult>,
    selectedWorkflowId: GuidedWorkflowId?,
    onSelectWorkflow: (GuidedWorkflowId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        workflows.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { workflow ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectWorkflow(workflow.id) },
                        color = if (workflow.id == selectedWorkflowId) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = if (workflow.id == selectedWorkflowId) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        tonalElevation = 1.dp,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = workflow.id.actionLabel,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WorkflowDetail(
    workflow: GuidedWorkflowResult,
    onOpenGuideCard: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = workflow.id.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Built from installed guide cards, local checklist steps, gear inventory, and saved map context.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (workflow.missingDataWarnings.isNotEmpty()) {
            MissingDataPanel(workflow.missingDataWarnings)
        }
        WorkflowGuideBullets(workflow)
        WorkflowChecklistSteps(workflow)
        WorkflowGearContext(workflow)
        WorkflowLocationContext(workflow.locationFacts)
        WorkflowSourceLinks(workflow.guideCards, onOpenGuideCard)
    }
}

@Composable
private fun WorkflowGuideBullets(workflow: GuidedWorkflowResult) {
    WorkflowPanel("Source-backed steps") {
        if (workflow.guideBullets.isEmpty()) {
            EmptyText("No guide-card steps are available for this workflow.")
        }
        workflow.guideBullets.forEach { bullet ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text("-", style = MaterialTheme.typography.bodyMedium)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = bullet.text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Source: ${bullet.sourceCard.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkflowChecklistSteps(workflow: GuidedWorkflowResult) {
    WorkflowPanel("Matching checklist steps") {
        if (workflow.checklistSteps.isEmpty()) {
            EmptyText("No matching local checklist items.")
        }
        workflow.checklistSteps.forEach { step ->
            val status = if (step.item.isChecked) "Done" else "Open"
            Text(
                text = "$status - ${step.item.label}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = step.checklistTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            step.item.details?.let { details ->
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun WorkflowGearContext(workflow: GuidedWorkflowResult) {
    WorkflowPanel("Gear context") {
        if (workflow.availableGear.isEmpty() && workflow.missingGear.isEmpty()) {
            EmptyText("No matching gear inventory items.")
        }
        workflow.availableGear.forEach { item ->
            GearLine("Have", item)
        }
        workflow.missingGear.forEach { item ->
            GearLine("Missing", item)
        }
    }
}

@Composable
private fun GearLine(
    status: String,
    item: ai.unplugged.posa.data.model.GearItem,
) {
    Text(
        text = "$status - ${item.name} x${item.quantity}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
    )
    item.category?.let { category ->
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    item.notes?.let { notes ->
        Text(
            text = notes,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun WorkflowLocationContext(locationFacts: List<String>) {
    WorkflowPanel("Location context") {
        if (locationFacts.isEmpty()) {
            EmptyText("No saved map context is attached to this workflow.")
        }
        locationFacts.forEach { fact ->
            Text(
                text = fact,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun WorkflowSourceLinks(
    guideCards: List<GuideCardItem>,
    onOpenGuideCard: (String) -> Unit,
) {
    WorkflowPanel("Sources") {
        if (guideCards.isEmpty()) {
            EmptyText("No source cards attached.")
        }
        guideCards.forEach { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGuideCard(item.card.id) },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.card.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    MetadataLine("Source", item.provenance?.sourceTitle)
                    MetadataLine("Citation", item.provenance?.citation)
                    MetadataLine("URL", item.provenance?.sourceUrl)
                    MetadataLine("Review", item.provenance?.reviewStatus ?: item.pack?.reviewStatus)
                }
            }
        }
    }
}

@Composable
private fun MissingDataPanel(warnings: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "Missing local data",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            warnings.forEach { warning ->
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun WorkflowPanel(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun GuideCardsSection(
    state: GuideContentState,
    query: String,
    selectedCardId: String?,
    onQueryChange: (String) -> Unit,
    onSelectCard: (String) -> Unit,
    onBackToList: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search guide cards") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                )
            },
        )

        val selectedCard = selectedCardId?.let { id ->
            state.cards.firstOrNull { it.card.id == id }
        }

        if (selectedCard != null) {
            GuideCardDetail(
                item = selectedCard,
                onBackToList = onBackToList,
            )
        } else {
            GuideCardList(
                cards = state.cards,
                query = query,
                onSelectCard = onSelectCard,
            )
        }
    }
}

@Composable
internal fun PacksSection(state: GuideContentState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StateBanners(state)
        if (!state.isLoading && state.packs.isEmpty()) {
            EmptyText("No local guide packs are installed.")
        }
        state.packs.forEach { pack ->
            PackPanel(
                pack = pack,
                cardCount = state.packCardCounts[pack.id] ?: 0,
            )
        }
    }
}

@Composable
private fun GuideCardList(
    cards: List<GuideCardItem>,
    query: String,
    onSelectCard: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (cards.isEmpty() && query.isNotBlank()) {
            EmptyText("No guide cards match this search.")
        }
        cards.forEach { item ->
            GuideCardListRow(
                item = item,
                onSelectCard = onSelectCard,
            )
        }
    }
}

@Composable
private fun GuideCardListRow(
    item: GuideCardItem,
    onSelectCard: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectCard(item.card.id) },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.card.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = item.card.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            MetadataLine("Category", item.card.category)
            MetadataLine("Source", item.provenance?.sourceTitle)
            MetadataLine("Review", item.provenance?.reviewStatus ?: item.pack?.reviewStatus)
            MetadataLine("License", item.provenance?.license ?: item.pack?.license)
        }
    }
}

@Composable
private fun GuideCardDetail(
    item: GuideCardItem,
    onBackToList: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToList) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to guide list",
                )
            }
            Text(
                text = "Guide list",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.card.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = item.card.summary,
                style = MaterialTheme.typography.bodyLarge,
            )
            MetadataLine("Category", item.card.category)
        }

        item.card.warnings?.let { warning ->
            WarningPanel(warning)
        }

        HorizontalDivider()
        MarkdownBody(item.card.bodyMarkdown)
        HorizontalDivider()
        ProvenancePanel(
            pack = item.pack,
            provenance = item.provenance,
        )
    }
}

@Composable
private fun WarningPanel(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProvenancePanel(
    pack: Pack?,
    provenance: Provenance?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Source and provenance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        MetadataLine("Source", provenance?.sourceTitle)
        MetadataLine("Citation", provenance?.citation)
        MetadataLine("URL", provenance?.sourceUrl)
        MetadataLine("License", provenance?.license ?: pack?.license)
        MetadataLine("Review", provenance?.reviewStatus ?: pack?.reviewStatus)
        MetadataLine("Reviewed", provenance?.reviewedAtEpochMillis.toIsoDateLabel())
        MetadataLine("Pack", pack?.let { "${it.title} ${it.version}" })
        MetadataLine("Pack status", pack?.reviewStatus)
        MetadataLine("Notes", provenance?.notes)
    }
}

@Composable
private fun PackPanel(
    pack: Pack,
    cardCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = pack.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            pack.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            MetadataLine("Version", pack.version)
            MetadataLine("Cards", cardCount.toString())
            MetadataLine("Type", pack.packType)
            MetadataLine("Review", pack.reviewStatus)
            MetadataLine("Last reviewed", pack.lastReviewedEpochMillis.toIsoDateLabel())
            MetadataLine("Source", pack.sourceName)
            MetadataLine("URL", pack.sourceUrl)
            MetadataLine("License", pack.license)
        }
    }
}

@Composable
private fun MarkdownBody(markdown: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        markdown.lines().forEach { line ->
            when {
                line.isBlank() -> Spacer(modifier = Modifier.height(2.dp))
                line.startsWith("## ") -> Text(
                    text = line.removePrefix("## "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                line.startsWith("- ") -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = line.removePrefix("- "),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun StateBanners(state: GuideContentState) {
    if (state.isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    state.errorMessage?.let { message ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MetadataLine(
    label: String,
    value: String?,
) {
    if (value.isNullOrBlank()) {
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

private fun Long?.toIsoDateLabel(): String? =
    this?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .toString()
    }
