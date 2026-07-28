package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.RagQueryEmbeddingStatus
import ai.unplugged.posa.data.local.RagQueryResult
import ai.unplugged.posa.data.local.RagAnswerLlmStatus
import ai.unplugged.posa.data.local.RagAnswerResult
import ai.unplugged.posa.data.local.RagAnswerVerifierIssue
import ai.unplugged.posa.data.local.RagRetrievalHit
import ai.unplugged.posa.data.local.RagRetrievalSource
import java.util.Locale

internal data class AskContentState(
    val question: String = "",
    val isSearching: Boolean = false,
    val result: AskSourceResult? = null,
    val errorMessage: String? = null,
) {
    val hasQuestion: Boolean = question.isNotBlank()
}

internal data class AskSourceResult(
    val question: String,
    val sourceMatches: List<AskSourceMatch>,
    val embeddingStatus: RagQueryEmbeddingStatus,
    val embeddingFailure: String?,
    val answer: AskGeneratedAnswer? = null,
) {
    val hasMatches: Boolean = sourceMatches.isNotEmpty()
    val statusText: String
        get() = when {
            question.isBlank() -> "Ask a question to search the source corpus."
            answer?.text?.isNotBlank() == true -> "Showing source-grounded answer and evidence."
            hasMatches -> "Showing source excerpts from the local corpus."
            else -> "I do not know from the local source corpus."
        }

    val retrievalModeLabel: String
        get() = when (embeddingStatus) {
            RagQueryEmbeddingStatus.Used -> "Hybrid"
            RagQueryEmbeddingStatus.Unavailable -> "Keyword"
            RagQueryEmbeddingStatus.Failed -> "Keyword fallback"
    }
}

internal data class AskGeneratedAnswer(
    val text: String?,
    val llmStatus: RagAnswerLlmStatus,
    val llmFailure: String?,
    val verifierPassed: Boolean?,
    val verifierIssues: List<String>,
    val retrievalMillis: Long,
    val queryEmbeddingMillis: Long?,
    val ftsMillis: Long?,
    val vectorMillis: Long?,
    val hybridMergeMillis: Long?,
    val evidenceMillis: Long?,
    val promptPackingMillis: Long?,
    val ttftMillis: Long?,
    val firstGenerationMillis: Long?,
    val verifierMillis: Long?,
    val repairMillis: Long?,
    val generationMillis: Long?,
    val totalMillis: Long?,
) {
    val statusLabel: String
        get() = when (llmStatus) {
            RagAnswerLlmStatus.Generated -> if (verifierPassed == true) "Generated" else "Generated with warnings"
            RagAnswerLlmStatus.Unavailable -> "LLM unavailable"
            RagAnswerLlmStatus.Failed -> "LLM failed"
        }

    val ttftLabel: String
        get() = ttftMillis?.let { "${it}ms" } ?: "n/a"

    val ttftTargetLabel: String
        get() = ttftMillis?.let { if (it < 10_000L) "under 10s" else "over 10s" } ?: "n/a"
}

internal data class AskSourceMatch(
    val rank: Int,
    val title: String,
    val sectionTitle: String?,
    val excerpt: String,
    val category: String,
    val urgency: String?,
    val sourceUrl: String?,
    val sourceCitation: String?,
    val retrievalSource: RagRetrievalSource,
    val ftsRank: Int?,
    val vectorRank: Int?,
    val vectorScore: Float?,
    val hybridScore: Double,
)

internal fun RagQueryResult.toAskSourceResult(): AskSourceResult =
    AskSourceResult(
        question = query,
        sourceMatches = hits.mapIndexed { index, hit -> hit.toAskSourceMatch(index + 1) },
        embeddingStatus = embeddingStatus,
        embeddingFailure = embeddingFailure,
    )

internal fun RagAnswerResult.toAskSourceResult(): AskSourceResult =
    AskSourceResult(
        question = queryResult.query,
        sourceMatches = queryResult.hits
            .take(MAX_GENERATED_ANSWER_SOURCE_MATCHES)
            .mapIndexed { index, hit -> hit.toAskSourceMatch(index + 1) },
        embeddingStatus = queryResult.embeddingStatus,
        embeddingFailure = queryResult.embeddingFailure,
        answer = AskGeneratedAnswer(
            text = answer,
            llmStatus = llmStatus,
            llmFailure = llmFailure,
            verifierPassed = verifierResult?.passed,
            verifierIssues = verifierResult?.issues.orEmpty().map(RagAnswerVerifierIssue::message),
            retrievalMillis = timings.retrievalMillis,
            queryEmbeddingMillis = timings.queryEmbeddingMillis,
            ftsMillis = timings.ftsMillis,
            vectorMillis = timings.vectorMillis,
            hybridMergeMillis = timings.hybridMergeMillis,
            evidenceMillis = timings.evidenceMillis,
            promptPackingMillis = timings.promptPackingMillis,
            ttftMillis = timings.ttftMillis,
            firstGenerationMillis = timings.firstGenerationMillis,
            verifierMillis = timings.verifierMillis,
            repairMillis = timings.repairMillis,
            generationMillis = timings.generationMillis,
            totalMillis = timings.totalMillis,
        ),
    )

private fun RagRetrievalHit.toAskSourceMatch(rank: Int): AskSourceMatch =
    AskSourceMatch(
        rank = rank,
        title = chunk.title,
        sectionTitle = chunk.sectionTitle,
        excerpt = chunk.content.cleanSourceExcerpt(),
        category = chunk.category,
        urgency = chunk.urgency,
        sourceUrl = chunk.sourceUrl,
        sourceCitation = chunk.sourceCitation,
        retrievalSource = source,
        ftsRank = ftsRank,
        vectorRank = vectorRank,
        vectorScore = vectorScore,
        hybridScore = hybridScore,
    )

private fun String.cleanSourceExcerpt(): String =
    lines()
        .map { it.trim().removePrefix("- ").trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .replace(Regex("\\s+"), " ")
        .limitLength(MAX_SOURCE_EXCERPT_CHARS)

private fun String.limitLength(maxLength: Int): String {
    if (length <= maxLength) return this
    val clipped = take(maxLength)
    return clipped.substringBeforeLast(' ', clipped).trimEnd('.', ',', ';', ':') + "..."
}

internal fun Float.toScoreLabel(): String =
    String.format(Locale.US, "%.3f", this)

private const val MAX_SOURCE_EXCERPT_CHARS = 700
private const val MAX_GENERATED_ANSWER_SOURCE_MATCHES = 5
