package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.RagRetrievalSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AskSection(
    state: AskContentState,
    onQuestionChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = state.question,
            onValueChange = onQuestionChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
            label = { Text("Ask local sources") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                )
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onSearch,
                enabled = state.hasQuestion && !state.isSearching,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                )
                Text(
                    text = "Answer",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = state.result?.retrievalModeLabel ?: "Local corpus",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        AskPromptSuggestions(onQuestionChange = onQuestionChange)

        if (state.isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.errorMessage?.let { message ->
            AskStatusPanel(
                title = message,
                tone = AskStatusTone.Error,
                details = null,
            )
        }

        state.result?.let { result ->
            AskResultPanel(result)
            result.answer?.let { answer ->
                AskAnswerPanel(answer)
            }
            if (result.sourceMatches.isNotEmpty()) {
                AskSourceMatches(matches = result.sourceMatches)
            }
        }
    }
}

@Composable
private fun AskPromptSuggestions(onQuestionChange: (String) -> Unit) {
    val prompts = listOf(
        "How do I make water safe after a storm?",
        "What should I do if lightning starts near camp?",
        "How should I store food around wildlife?",
        "What should I check before a river crossing?",
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
private fun AskResultPanel(result: AskSourceResult) {
    AskStatusPanel(
        title = result.statusText,
        tone = if (result.hasMatches) AskStatusTone.Info else AskStatusTone.Error,
        details = buildList {
            add("Retrieval: ${result.retrievalModeLabel}")
            add("Sources: ${result.sourceMatches.size}")
            result.answer?.let { answer ->
                add("LLM: ${answer.statusLabel}")
                add("TTFT: ${answer.ttftLabel}")
            }
            result.embeddingFailure?.let { failure ->
                add("Embedding: $failure")
            }
        }.joinToString("  |  "),
    )
}

@Composable
private fun AskStatusPanel(
    title: String,
    tone: AskStatusTone,
    details: String?,
) {
    val isError = tone == AskStatusTone.Error
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (isError) {
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
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            details?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun AskAnswerPanel(answer: AskGeneratedAnswer) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = answer.statusLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (!answer.text.isNullOrBlank()) {
                Text(
                    text = answer.text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            answer.llmFailure?.let { failure ->
                Text(
                    text = failure,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            AskMetadataLine("TTFT", answer.ttftLabel)
            AskMetadataLine("TTFT target", answer.ttftTargetLabel)
            AskMetadataLine("Retrieval", "${answer.retrievalMillis}ms")
            AskMetadataLine("Pipeline", answer.pipelineTimingSummary())
            answer.generationMillis?.let { AskMetadataLine("Generation", "${it}ms") }
            answer.totalMillis?.let { AskMetadataLine("Total", "${it}ms") }
            if (answer.verifierIssues.isNotEmpty()) {
                AskMetadataLine("Verifier", answer.verifierIssues.joinToString("; "))
            } else if (answer.verifierPassed == true) {
                AskMetadataLine("Verifier", "passed")
            }
        }
    }
}

@Composable
private fun AskSourceMatches(matches: List<AskSourceMatch>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        matches.forEach { match ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "#${match.rank}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = match.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            match.sectionTitle?.let { section ->
                                Text(
                                    text = section,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                    Text(
                        text = match.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    HorizontalDivider()
                    AskMetadataLine("Category", match.category)
                    AskMetadataLine("Urgency", match.urgency)
                    AskMetadataLine("Citation", match.sourceCitation)
                    AskMetadataLine("URL", match.sourceUrl)
                    AskMetadataLine("Rank", match.rankSummary())
                }
            }
        }
    }
}

@Composable
private fun AskMetadataLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun AskSourceMatch.rankSummary(): String {
    val parts = buildList {
        add(
            when (retrievalSource) {
                RagRetrievalSource.Fts -> "keyword"
                RagRetrievalSource.Vector -> "vector"
                RagRetrievalSource.Hybrid -> "hybrid"
            },
        )
        ftsRank?.let { add("fts $it") }
        vectorRank?.let { add("vector $it") }
        vectorScore?.let { add("score ${it.toScoreLabel()}") }
    }
    return parts.joinToString(" / ")
}

private fun AskGeneratedAnswer.pipelineTimingSummary(): String =
    buildList {
        queryEmbeddingMillis?.let { add("embed ${it}ms") }
        ftsMillis?.let { add("fts ${it}ms") }
        vectorMillis?.let { add("vector ${it}ms") }
        hybridMergeMillis?.let { add("merge ${it}ms") }
        evidenceMillis?.let { add("evidence ${it}ms") }
        promptPackingMillis?.let { add("pack ${it}ms") }
        firstGenerationMillis?.let { add("llm ${it}ms") }
        verifierMillis?.let { add("verify ${it}ms") }
        repairMillis?.let { add("repair ${it}ms") }
    }.joinToString("  |  ")

private enum class AskStatusTone {
    Info,
    Error,
}
