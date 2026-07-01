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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset

@Composable
internal fun GuideCardsSection(
    state: GuideContentState,
    query: String,
    selectedCardId: String?,
    onQueryChange: (String) -> Unit,
    onSelectCard: (String) -> Unit,
    onBackToList: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StateBanners(state)
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
