package ai.unplugged.posa.ui

import ai.unplugged.posa.data.model.GearItem
import ai.unplugged.posa.data.model.GuideCard
import ai.unplugged.posa.data.model.InstalledMap
import ai.unplugged.posa.data.model.Waypoint
import java.util.Locale

internal data class GuidedQuestionResult(
    val question: String,
    val sourceMatches: List<GuidedQuestionSourceMatch>,
    val gearFacts: List<String>,
    val mapFacts: List<String>,
    val confidence: RetrievalConfidence,
) {
    val hasQuestion: Boolean = question.isNotBlank()
    val hasSourceAnswer: Boolean = sourceMatches.isNotEmpty()
    val statusText: String
        get() = when {
            !hasQuestion -> "Ask a question to search installed guide packs."
            hasSourceAnswer -> "Showing installed-source excerpts only."
            else -> UNKNOWN_FROM_INSTALLED_SOURCES
        }
}

internal data class GuidedQuestionSourceMatch(
    val item: GuideCardItem,
    val excerpt: String,
    val matchedTerms: List<String>,
    val score: Int,
    val confidence: RetrievalConfidence,
)

internal enum class RetrievalConfidence(val label: String) {
    None("No installed-source match"),
    Low("Low"),
    Medium("Medium"),
    High("High"),
}

internal fun answerGuidedQuestion(
    guideState: GuideContentState,
    toolsState: ToolsContentState,
    mapState: MapContentState,
    question: String,
): GuidedQuestionResult {
    val terms = question.toQueryTerms()
    if (terms.isEmpty()) {
        return GuidedQuestionResult(
            question = question,
            sourceMatches = emptyList(),
            gearFacts = emptyList(),
            mapFacts = emptyList(),
            confidence = RetrievalConfidence.None,
        )
    }

    val sourceCards = guideState.allCards.ifEmpty { guideState.cards }
    val matches = sourceCards
        .mapNotNull { item -> item.toSourceMatch(terms) }
        .sortedWith(
            compareByDescending<GuidedQuestionSourceMatch> { it.score }
                .thenBy { it.item.card.sortOrder }
                .thenBy { it.item.card.title.lowercase(Locale.US) },
        )
        .take(MAX_RETRIEVED_SOURCES)

    val contextTerms = (terms + matches.flatMap { it.item.card.contextTerms() }).distinct()
    val gearFacts = toolsState.gear
        .filter { it.matchesAny(contextTerms) }
        .sortedWith(compareBy<GearItem>({ !it.isAvailable }, { it.name.lowercase(Locale.US) }))
        .take(MAX_CONTEXT_FACTS)
        .map { gear ->
            val status = if (gear.isAvailable) "Have" else "Missing"
            "$status: ${gear.name} x${gear.quantity}"
        }

    val mapFacts = if (question.needsMapContext() || matches.any { it.item.card.needsMapContext() }) {
        mapState.retrievalMapFacts()
    } else {
        emptyList()
    }

    val confidence = matches.firstOrNull()?.confidence ?: RetrievalConfidence.None
    return GuidedQuestionResult(
        question = question,
        sourceMatches = matches,
        gearFacts = gearFacts,
        mapFacts = mapFacts,
        confidence = confidence,
    )
}

private fun GuideCardItem.toSourceMatch(terms: List<String>): GuidedQuestionSourceMatch? {
    val sourceCard = this.card
    val score = sourceCard.retrievalScore(terms)
    if (score < MIN_SOURCE_SCORE) {
        return null
    }

    val matchedTerms = terms.filter { term ->
        sourceCard.searchText().contains(term)
    }
    return GuidedQuestionSourceMatch(
        item = this,
        excerpt = sourceCard.bestExcerpt(terms),
        matchedTerms = matchedTerms,
        score = score,
        confidence = score.toConfidence(matchedTerms.size),
    )
}

private fun GuideCard.retrievalScore(terms: List<String>): Int {
    val titleText = title.normalized()
    val categoryText = category.normalized()
    val summaryText = summary.normalized()
    val bodyText = bodyMarkdown.normalized()
    var score = 0

    terms.forEach { term ->
        if (titleText.contains(term)) score += 5
        if (categoryText.contains(term)) score += 4
        if (summaryText.contains(term)) score += 3
        if (bodyText.contains(term)) score += 1
    }
    if (summaryText.contains(terms.joinToString(" "))) score += 3
    if (bodyText.contains(terms.joinToString(" "))) score += 2

    return score
}

private fun GuideCard.bestExcerpt(terms: List<String>): String {
    val candidates = bodyMarkdown.lines()
        .map { it.cleanMarkdownLine() }
        .filter { it.length >= MIN_EXCERPT_LENGTH }

    val bestLines = candidates
        .map { line -> line to line.normalized().termHitCount(terms) }
        .filter { (_, hits) -> hits > 0 }
        .sortedByDescending { (_, hits) -> hits }
        .take(MAX_EXCERPT_LINES)
        .map { (line, _) -> line }

    return (bestLines.ifEmpty { listOf(summary) })
        .joinToString(" ")
        .limitLength(MAX_EXCERPT_CHARS)
}

private fun MapContentState.retrievalMapFacts(): List<String> = buildList {
    activeInstalledMap?.let { map ->
        add("Enabled map area: ${map.displayName}")
        map.boundsLabel()?.let { add("Map bounds: $it") }
    }
    activeTrail?.let { trail ->
        add("Active breadcrumb: ${trail.trail.name} with ${trail.points.size} recorded points")
    }
    if (waypoints.isNotEmpty()) {
        add("Saved waypoints: ${waypoints.take(3).joinToString { it.retrievalLabel() }}")
    }
    if (isEmpty()) {
        add("No saved waypoint, active breadcrumb, or enabled map area is available.")
    }
}.take(MAX_CONTEXT_FACTS)

private fun String.needsMapContext(): Boolean =
    toQueryTerms().any { it in MAP_CONTEXT_TERMS }

private fun GuideCard.needsMapContext(): Boolean =
    contextTerms().any { it in MAP_CONTEXT_TERMS }

private fun GuideCard.contextTerms(): List<String> =
    toQueryTerms(title, category, summary)

private fun GearItem.matchesAny(terms: List<String>): Boolean =
    toQueryTerms(name, category.orEmpty(), condition.orEmpty(), notes.orEmpty())
        .any { it in terms }

private fun InstalledMap.boundsLabel(): String? {
    val minLatitude = boundingBoxMinLatitude ?: return null
    val minLongitude = boundingBoxMinLongitude ?: return null
    val maxLatitude = boundingBoxMaxLatitude ?: return null
    val maxLongitude = boundingBoxMaxLongitude ?: return null
    return "${minLatitude.coordinateLabel()}, ${minLongitude.coordinateLabel()} to " +
        "${maxLatitude.coordinateLabel()}, ${maxLongitude.coordinateLabel()}"
}

private fun Waypoint.retrievalLabel(): String =
    "$name (${latitude.coordinateLabel()}, ${longitude.coordinateLabel()})"

private fun Int.toConfidence(matchedTermCount: Int): RetrievalConfidence = when {
    this >= 12 && matchedTermCount >= 2 -> RetrievalConfidence.High
    this >= 6 -> RetrievalConfidence.Medium
    else -> RetrievalConfidence.Low
}

private fun String.cleanMarkdownLine(): String =
    trim()
        .removePrefix("## ")
        .removePrefix("- ")
        .trim()

private fun String.termHitCount(terms: List<String>): Int =
    terms.count { contains(it) }

private fun String.limitLength(maxLength: Int): String {
    if (length <= maxLength) return this
    val clipped = take(maxLength)
    return clipped.substringBeforeLast(' ', clipped).trimEnd('.', ',', ';', ':') + "..."
}

private fun GuideCard.searchText(): String =
    toQueryTerms(id, title, category, summary, bodyMarkdown).joinToString(" ")

private fun String.normalized(): String =
    lowercase(Locale.US)

private fun String.toQueryTerms(): List<String> =
    toQueryTerms(this)

private fun toQueryTerms(vararg values: String): List<String> =
    values.joinToString(" ")
        .lowercase(Locale.US)
        .split(Regex("[^a-z0-9]+"))
        .map { it.trim() }
        .filter { it.length >= MIN_TERM_LENGTH && it !in STOP_WORDS }
        .distinct()

private fun Double.coordinateLabel(): String =
    String.format(Locale.US, "%.5f", this)

internal const val UNKNOWN_FROM_INSTALLED_SOURCES = "I do not know from installed sources."

private val STOP_WORDS = setOf(
    "about",
    "after",
    "again",
    "also",
    "before",
    "could",
    "does",
    "from",
    "have",
    "help",
    "how",
    "into",
    "can",
    "may",
    "my",
    "need",
    "should",
    "that",
    "their",
    "there",
    "this",
    "what",
    "when",
    "where",
    "which",
    "with",
    "would",
)

private val MAP_CONTEXT_TERMS = setOf(
    "area",
    "breadcrumb",
    "location",
    "lost",
    "map",
    "navigation",
    "near",
    "route",
    "signal",
    "signaling",
    "waypoint",
    "where",
)

private const val MIN_TERM_LENGTH = 3
private const val MIN_SOURCE_SCORE = 2
private const val MIN_EXCERPT_LENGTH = 12
private const val MAX_RETRIEVED_SOURCES = 4
private const val MAX_EXCERPT_LINES = 2
private const val MAX_EXCERPT_CHARS = 320
private const val MAX_CONTEXT_FACTS = 4
