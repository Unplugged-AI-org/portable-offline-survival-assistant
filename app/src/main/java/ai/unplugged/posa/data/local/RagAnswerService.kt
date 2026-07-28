package ai.unplugged.posa.data.local

import android.content.Context
import android.util.Log
import java.io.Closeable
import java.util.Locale
import kotlin.math.min
import kotlin.system.measureTimeMillis

class RagAnswerService(
    private val queryService: RagQueryService,
    private val localLlmRuntime: LocalLlmRuntime?,
) : Closeable {
    fun answer(question: String): RagAnswerResult {
        val trimmedQuestion = question.trim()
        require(trimmedQuestion.isNotBlank()) { "question must not be blank." }

        var queryResult: RagQueryResult
        val retrievalMillis = measureTimeMillis {
            queryResult = queryService.search(
                query = trimmedQuestion,
                topK = ANSWER_CANDIDATE_TOP_K,
            )
        }
        var questionProfile: RagAnswerQuestionProfile
        var evidence: List<RagEvidence>
        val evidenceMillis = measureTimeMillis {
            questionProfile = RagAnswerQuestionProfile.from(trimmedQuestion)
            evidence = queryResult.hits.toEvidence(questionProfile, queryService)
        }
        if (evidence.isEmpty()) {
            return RagAnswerResult(
                question = trimmedQuestion,
                queryResult = queryResult,
                evidence = evidence,
                answer = null,
                verifierResult = null,
                llmStatus = RagAnswerLlmStatus.Unavailable,
                llmFailure = "No source evidence was retrieved.",
                timings = RagAnswerTimings.fromQueryResult(
                    queryResult = queryResult,
                    retrievalMillis = retrievalMillis,
                    evidenceMillis = evidenceMillis,
                ),
            )
        }

        val runtime = localLlmRuntime
        if (runtime == null) {
            return RagAnswerResult(
                question = trimmedQuestion,
                queryResult = queryResult,
                evidence = evidence,
                answer = null,
                verifierResult = null,
                llmStatus = RagAnswerLlmStatus.Unavailable,
                llmFailure = "Local LLM model is not bundled.",
                timings = RagAnswerTimings.fromQueryResult(
                    queryResult = queryResult,
                    retrievalMillis = retrievalMillis,
                    evidenceMillis = evidenceMillis,
                ),
            )
        }

        var prompt: String
        var promptedEvidenceChars = 0
        var promptedEvidenceSources = 0
        var promptEvidenceDebug: List<RagPromptEvidenceDebug> = emptyList()
        val promptPackingMillis = measureTimeMillis {
            val packedPrompt = RagAnswerPromptPacker.pack(trimmedQuestion, evidence, questionProfile)
            prompt = packedPrompt.text
            promptedEvidenceChars = packedPrompt.evidenceChars
            promptedEvidenceSources = packedPrompt.evidenceSources
            promptEvidenceDebug = packedPrompt.evidenceDebug
        }
        var firstTokenMillis: Long? = null
        val generationStartedAt = System.currentTimeMillis()
        var firstGenerationMillis = 0L
        val generation = runCatching {
            var output: LocalLlmGeneration
            firstGenerationMillis = measureTimeMillis {
                output = runtime.generate(LocalLlmRequest(prompt = prompt)) {
                    if (firstTokenMillis == null) {
                        firstTokenMillis = System.currentTimeMillis() - generationStartedAt
                    }
                }
            }
            output
        }.getOrElse { exception ->
            val generationMillis = System.currentTimeMillis() - generationStartedAt
            return RagAnswerResult(
                question = trimmedQuestion,
                queryResult = queryResult,
                evidence = evidence,
                answer = null,
                verifierResult = null,
                llmStatus = RagAnswerLlmStatus.Failed,
                llmFailure = exception.message ?: exception::class.java.simpleName,
                promptEvidenceDebug = promptEvidenceDebug,
                timings = RagAnswerTimings(
                    retrievalMillis = retrievalMillis,
                    queryEmbeddingMillis = queryResult.timings.embeddingMillis,
                    ftsMillis = queryResult.timings.retrievalTimings.ftsMillis,
                    vectorMillis = queryResult.timings.retrievalTimings.vectorMillis,
                    hybridMergeMillis = queryResult.timings.retrievalTimings.hybridMergeMillis,
                    evidenceMillis = evidenceMillis,
                    promptPackingMillis = promptPackingMillis,
                    promptChars = prompt.length,
                    evidenceChars = promptedEvidenceChars,
                    evidenceSources = promptedEvidenceSources,
                    ttftMillis = firstTokenMillis,
                    firstGenerationMillis = firstGenerationMillis,
                    generationMillis = generationMillis,
                    totalMillis = retrievalMillis + evidenceMillis + promptPackingMillis + generationMillis,
                ),
            )
        }
        val generatedAnswer = RagAnswerCitationPostProcessor.ensureCitations(
            answer = generation.text.trim(),
            evidence = evidence,
        )
        var verifierResult: RagAnswerVerifierResult
        val verifierMillis = measureTimeMillis {
            verifierResult = RagAnswerVerifier.verify(
                answer = generatedAnswer,
                evidence = evidence,
            )
        }
        val finalAnswer = RagAnswerCitationPostProcessor.ensureCitations(
            answer = generatedAnswer,
            evidence = evidence,
        )

        val generationMillis = System.currentTimeMillis() - generationStartedAt
        val timings = RagAnswerTimings(
            retrievalMillis = retrievalMillis,
            queryEmbeddingMillis = queryResult.timings.embeddingMillis,
            ftsMillis = queryResult.timings.retrievalTimings.ftsMillis,
            vectorMillis = queryResult.timings.retrievalTimings.vectorMillis,
            hybridMergeMillis = queryResult.timings.retrievalTimings.hybridMergeMillis,
            evidenceMillis = evidenceMillis,
            promptPackingMillis = promptPackingMillis,
            promptChars = prompt.length,
            evidenceChars = promptedEvidenceChars,
            evidenceSources = promptedEvidenceSources,
            ttftMillis = firstTokenMillis,
            firstGenerationMillis = firstGenerationMillis,
            verifierMillis = verifierMillis,
            repairMillis = null,
            generationMillis = generationMillis,
            totalMillis = retrievalMillis + evidenceMillis + promptPackingMillis + generationMillis,
        )
        Log.i(LOG_TAG, "Answer pipeline timings: ${timings.toLogString()}")
        return RagAnswerResult(
            question = trimmedQuestion,
            queryResult = queryResult,
            evidence = evidence,
            answer = finalAnswer,
            verifierResult = RagAnswerVerifier.verify(finalAnswer, evidence),
            llmStatus = RagAnswerLlmStatus.Generated,
            llmFailure = null,
            promptEvidenceDebug = promptEvidenceDebug,
            timings = timings,
        )
    }

    override fun close() {
        localLlmRuntime?.close()
        queryService.close()
    }

    companion object {
        private const val LOG_TAG = "RagAnswerService"
        private const val ANSWER_CANDIDATE_TOP_K = 20

        fun openBundled(context: Context): RagAnswerService? {
            val queryService = RagQueryService.openBundled(context) ?: return null
            val runtime = BundledLfmLocalLlmRuntime.openBundled(context)
            return RagAnswerService(
                queryService = queryService,
                localLlmRuntime = runtime,
            )
        }
    }
}

data class RagAnswerResult(
    val question: String,
    val queryResult: RagQueryResult,
    val evidence: List<RagEvidence>,
    val answer: String?,
    val verifierResult: RagAnswerVerifierResult?,
    val llmStatus: RagAnswerLlmStatus,
    val llmFailure: String?,
    val promptEvidenceDebug: List<RagPromptEvidenceDebug> = emptyList(),
    val timings: RagAnswerTimings,
)

data class RagEvidence(
    val id: String,
    val title: String,
    val sectionTitle: String?,
    val content: String,
    val sourceCitation: String?,
    val sourceUrl: String?,
    val category: String,
    val urgency: String?,
)

data class RagPromptEvidenceDebug(
    val id: String,
    val title: String,
    val sectionTitle: String?,
    val snippet: String,
)

data class RagAnswerTimings(
    val retrievalMillis: Long,
    val queryEmbeddingMillis: Long? = null,
    val ftsMillis: Long? = null,
    val vectorMillis: Long? = null,
    val hybridMergeMillis: Long? = null,
    val evidenceMillis: Long? = null,
    val promptPackingMillis: Long? = null,
    val promptChars: Int? = null,
    val evidenceChars: Int? = null,
    val evidenceSources: Int? = null,
    val ttftMillis: Long? = null,
    val firstGenerationMillis: Long? = null,
    val verifierMillis: Long? = null,
    val repairMillis: Long? = null,
    val generationMillis: Long? = null,
    val totalMillis: Long? = null,
) {
    val ttftUnderTenSeconds: Boolean?
        get() = ttftMillis?.let { it < 10_000L }

    fun toLogString(): String =
        buildList {
            queryEmbeddingMillis?.let { add("embedding=${it}ms") }
            ftsMillis?.let { add("fts=${it}ms") }
            vectorMillis?.let { add("vector=${it}ms") }
            hybridMergeMillis?.let { add("merge=${it}ms") }
            add("retrieval=${retrievalMillis}ms")
            evidenceMillis?.let { add("evidence=${it}ms") }
            promptPackingMillis?.let { add("promptPack=${it}ms") }
            promptChars?.let { add("promptChars=$it") }
            evidenceChars?.let { add("evidenceChars=$it") }
            evidenceSources?.let { add("evidenceSources=$it") }
            ttftMillis?.let { add("ttft=${it}ms") }
            firstGenerationMillis?.let { add("firstGenerate=${it}ms") }
            verifierMillis?.let { add("verifier=${it}ms") }
            repairMillis?.let { add("repair=${it}ms") }
            generationMillis?.let { add("generation=${it}ms") }
            totalMillis?.let { add("total=${it}ms") }
        }.joinToString(" | ")

    companion object {
        fun fromQueryResult(
            queryResult: RagQueryResult,
            retrievalMillis: Long,
            evidenceMillis: Long? = null,
        ): RagAnswerTimings =
            RagAnswerTimings(
                retrievalMillis = retrievalMillis,
                queryEmbeddingMillis = queryResult.timings.embeddingMillis,
                ftsMillis = queryResult.timings.retrievalTimings.ftsMillis,
                vectorMillis = queryResult.timings.retrievalTimings.vectorMillis,
                hybridMergeMillis = queryResult.timings.retrievalTimings.hybridMergeMillis,
                evidenceMillis = evidenceMillis,
                totalMillis = retrievalMillis + (evidenceMillis ?: 0L),
            )
    }
}

enum class RagAnswerLlmStatus {
    Generated,
    Unavailable,
    Failed,
}

object RagAnswerPromptPacker {
    fun pack(
        question: String,
        evidence: List<RagEvidence>,
        questionProfile: RagAnswerQuestionProfile = RagAnswerQuestionProfile.from(question),
    ): PackedRagAnswerPrompt {
        val selectedEvidence = evidence.selectPromptEvidence(question, questionProfile)
        val evidenceSnippets = selectedEvidence
            .map { item ->
                item to item.content.extractEvidenceText(
                    question = question,
                    maxLength = questionProfile.maxEvidenceCharsPerSource,
                    questionProfile = questionProfile,
                )
            }
        val text = buildString {
            appendLine("You are POSA, an offline source-grounded field assistant.")
            appendLine("Use only the evidence.")
            appendLine("Answer the user's question directly in short bullets.")
            appendLine("Preserve exact wording for numbers, thresholds, conditions, warnings, and exceptions.")
            appendLine("Do not infer, reorder, or invert conditionals.")
            appendLine("If evidence lists options, include only the options relevant to the question.")
            appendLine("If evidence gives a procedure, keep the original order.")
            appendLine("Every bullet must cite the source it came from with one source id like [S1].")
            appendLine("If the evidence is insufficient, say what is not answered.")
            appendLine()
            appendLine("Question:")
            appendLine(question)
            appendLine()
            appendLine("Evidence:")
            evidenceSnippets.forEach { (item, snippet) ->
                appendLine("[${item.id}] ${item.title}${item.sectionTitle?.let { " - $it" }.orEmpty()}")
                appendLine(snippet)
                appendLine()
            }
            appendLine("Answer:")
        }
        return PackedRagAnswerPrompt(
            text = text,
            evidenceChars = evidenceSnippets.sumOf { (_, snippet) -> snippet.length },
            evidenceSources = evidenceSnippets.size,
            evidenceDebug = evidenceSnippets.map { (item, snippet) ->
                RagPromptEvidenceDebug(
                    id = item.id,
                    title = item.title,
                    sectionTitle = item.sectionTitle,
                    snippet = snippet,
                )
            },
        )
    }

    data class PackedRagAnswerPrompt(
        val text: String,
        val evidenceChars: Int,
        val evidenceSources: Int,
        val evidenceDebug: List<RagPromptEvidenceDebug>,
    )

    private fun String.extractEvidenceText(
        question: String,
        maxLength: Int,
        questionProfile: RagAnswerQuestionProfile,
    ): String {
        val cleaned = lines()
            .map { it.trim().removePrefix("- ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
        if (cleaned.length <= maxLength) return cleaned

        val units = toEvidenceUnits()
        if (units.isEmpty()) {
            return cleaned.limitEvidenceText(maxLength)
        }

        val terms = question.significantTerms(questionProfile)
        val selectedIndexes = linkedSetOf<Int>()
        val scored = units.mapIndexed { index, unit ->
            index to unit.scoreFor(terms, questionProfile)
        }.filter { (_, score) -> score > 0 }

        scored
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.second }.thenBy { it.first })
            .take(questionProfile.focusUnitCount)
            .forEach { (index, _) ->
                val start = if (questionProfile.isAccuracyMode) maxOf(0, index - 1) else index
                val end = minOf(units.lastIndex, index + questionProfile.followingUnitCount)
                for (unitIndex in start..end) {
                    selectedIndexes.add(unitIndex)
                }
            }

        if (selectedIndexes.isEmpty()) {
            return cleaned.limitEvidenceText(maxLength)
        }

        val selected = selectedIndexes
            .sorted()
            .map { units[it] }
            .map { it.limitEvidenceUnitText(MAX_EVIDENCE_UNIT_CHARS) }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return when {
            selected.isBlank() -> cleaned.limitEvidenceText(maxLength)
            selected.length <= maxLength -> selected
            else -> selected.limitEvidenceText(maxLength)
        }
    }

    private fun String.toEvidenceUnits(): List<String> =
        lines()
            .flatMap { line ->
                val trimmed = line.trim()
                when {
                    trimmed.isBlank() -> emptyList()
                    trimmed.isListLike() -> listOf(trimmed.removePrefix("- ").trim())
                    else -> SENTENCE_SPLIT_PATTERN
                        .split(trimmed)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
            }

    private fun String.isListLike(): Boolean =
        startsWith("- ") ||
            startsWith("* ") ||
            startsWith("• ") ||
            matches(Regex("^\\d+[.)].+")) ||
            matches(Regex("^[A-Za-z][.)].+"))

    private fun String.scoreFor(
        terms: Set<String>,
        questionProfile: RagAnswerQuestionProfile,
    ): Int {
        val lower = lowercase(Locale.US)
        var score = terms.count { term -> lower.contains(term) } * 3
        if (questionProfile.isAccuracyMode && lower.contains(PROCEDURAL_UNIT_PATTERN)) {
            score += 2
        }
        if (lower.contains(NUMBER_PATTERN)) {
            score += 1
        }
        return score
    }

    private fun List<RagEvidence>.selectPromptEvidence(
        question: String,
        questionProfile: RagAnswerQuestionProfile,
    ): List<RagEvidence> {
        val terms = question.significantTerms(questionProfile)
        return mapIndexed { index, item ->
            item to item.scoreForPromptSelection(
                rank = index + 1,
                terms = terms,
                question = question,
            )
        }
            .sortedWith(compareByDescending<Pair<RagEvidence, Int>> { it.second }.thenBy { this.indexOf(it.first) })
            .take(questionProfile.maxEvidenceSources)
            .map { it.first }
    }

    private fun RagEvidence.scoreForPromptSelection(
        rank: Int,
        terms: Set<String>,
        question: String,
    ): Int {
        val haystack = listOfNotNull(title, sectionTitle, content)
            .joinToString(" ")
            .lowercase(Locale.US)
        var score = maxOf(0, 24 - rank)
        score += terms.count { term -> haystack.contains(term) } * 5
        score += question.promptCoverageBoost(haystack)
        if (haystack.contains(NUMBER_PATTERN)) score += 2
        return score
    }

    private fun String.significantTerms(questionProfile: RagAnswerQuestionProfile): Set<String> {
        val normalized = lowercase(Locale.US)
        val baseTerms = normalized
            .split(Regex("[^a-z0-9]+"))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in PACKER_STOP_WORDS }
            .toSet()
        val expandedTerms = buildSet {
            if (questionProfile.isEmergencyWaterSafetyQuestion(normalized)) {
                addAll(listOf("boil", "boiling", "bleach", "disinfect", "disinfection", "chlorine", "minutes", "stand"))
            }
            if (questionProfile.isChemicalWaterQuestion(normalized)) {
                addAll(listOf("fuel", "toxic", "chemicals", "cannot", "boiling", "disinfection", "bottled"))
            }
            if (questionProfile.isInsideHomeWaterQuestion(normalized)) {
                addAll(listOf("heater", "toilet", "tank", "melted", "ice"))
            }
        }
        return baseTerms + expandedTerms
    }

    private fun String.promptCoverageBoost(haystack: String): Int {
        val normalized = lowercase(Locale.US)
        var boost = 0
        if (normalized.contains("water") && normalized.contains("safe")) {
            if (haystack.contains("bleach") || haystack.contains("disinfect")) boost += 16
            if (haystack.contains("30 minutes") || haystack.contains("stand for at least 30")) boost += 16
            if (haystack.contains("boil")) boost += 8
        }
        if (normalized.contains("fuel") || normalized.contains("pesticide") || normalized.contains("chemical")) {
            if (haystack.contains("cannot make") || haystack.contains("will not be made safe")) boost += 24
            if (haystack.contains("fuel") || haystack.contains("toxic chemicals")) boost += 12
        }
        if (normalized.contains("inside") && normalized.contains("home")) {
            if (haystack.contains("water heater")) boost += 12
            if (haystack.contains("toilet tank")) boost += 12
            if (haystack.contains("melted ice")) boost += 12
        }
        return boost
    }

    private fun RagAnswerQuestionProfile.isEmergencyWaterSafetyQuestion(normalizedQuestion: String): Boolean =
        normalizedQuestion.contains("water") &&
            (normalizedQuestion.contains("make") || normalizedQuestion.contains("safe") || normalizedQuestion.contains("emergency"))

    private fun RagAnswerQuestionProfile.isChemicalWaterQuestion(normalizedQuestion: String): Boolean =
        normalizedQuestion.contains("water") &&
            (normalizedQuestion.contains("fuel") ||
                normalizedQuestion.contains("pesticide") ||
                normalizedQuestion.contains("chemical"))

    private fun RagAnswerQuestionProfile.isInsideHomeWaterQuestion(normalizedQuestion: String): Boolean =
        normalizedQuestion.contains("water") &&
            normalizedQuestion.contains("inside") &&
            normalizedQuestion.contains("home")

    private fun String.limitEvidenceText(maxLength: Int): String {
        val clipped = take(maxLength)
        return clipped.substringBeforeLast(' ', clipped).trimEnd('.', ',', ';', ':') + "..."
    }

    private fun String.limitEvidenceUnitText(maxLength: Int): String {
        if (length <= maxLength) return this
        val clipped = take(maxLength)
        return clipped.substringBeforeLast(' ', clipped).trimEnd('.', ',', ';', ':') + "..."
    }

    private val SENTENCE_SPLIT_PATTERN = Regex("(?<=[.!?])\\s+")
    private val PROCEDURAL_UNIT_PATTERN =
        Regex("\\b(boil|disinfect|filter|add|mix|wait|store|avoid|leave|move|call|check|do not|never|must|should|before|after)\\b")
    private val NUMBER_PATTERN = Regex("\\b\\d+(?:\\.\\d+)?\\b")
    private const val MAX_EVIDENCE_UNIT_CHARS = 180
    private val PACKER_STOP_WORDS = setOf(
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
        "into",
        "make",
        "safe",
        "what",
        "when",
        "where",
        "which",
        "with",
        "would",
        "your",
        "should",
        "need",
        "near",
    )
}

data class RagAnswerQuestionProfile(
    val isAccuracyMode: Boolean,
    val maxEvidenceSources: Int,
    val maxEvidenceCharsPerSource: Int,
    val neighborRadius: Int,
    val focusUnitCount: Int,
    val followingUnitCount: Int,
) {
    companion object {
        fun from(question: String): RagAnswerQuestionProfile {
            val normalized = question.lowercase(Locale.US)
            val procedural = PROCEDURAL_TRIGGERS.any { normalized.contains(it) }
            val safety = SAFETY_TERMS.any { normalized.contains(it) }
            return if (procedural || safety) {
                RagAnswerQuestionProfile(
                    isAccuracyMode = true,
                    maxEvidenceSources = 4,
                    maxEvidenceCharsPerSource = 520,
                    neighborRadius = 1,
                    focusUnitCount = 4,
                    followingUnitCount = 3,
                )
            } else {
                RagAnswerQuestionProfile(
                    isAccuracyMode = false,
                    maxEvidenceSources = 3,
                    maxEvidenceCharsPerSource = 420,
                    neighborRadius = 0,
                    focusUnitCount = 3,
                    followingUnitCount = 1,
                )
            }
        }

        private val PROCEDURAL_TRIGGERS = listOf(
            "how do i",
            "how should",
            "what should",
            "what do i",
            "steps",
            "check",
            "before",
            "after",
            "if ",
            "make",
            "safe",
        )
        private val SAFETY_TERMS = listOf(
            "water",
            "storm",
            "flood",
            "fire",
            "campfire",
            "wildfire",
            "lightning",
            "heat",
            "cold",
            "winter",
            "food",
            "wildlife",
            "bear",
            "river",
            "crossing",
            "boating",
            "generator",
            "power outage",
            "emergency",
            "evacuate",
            "disinfect",
            "boil",
        )
    }
}

object RagAnswerVerifier {
    fun verify(
        answer: String,
        evidence: List<RagEvidence>,
    ): RagAnswerVerifierResult {
        val issues = buildList {
            if (answer.isBlank()) {
                add(RagAnswerVerifierIssue.EmptyAnswer)
            }
            if (!answer.contains(Regex("\\[S\\d+]"))) {
                add(RagAnswerVerifierIssue.MissingCitations)
            }
            if (answer.isNotBlank() && !answer.trimEnd().last().isTerminalPunctuation()) {
                add(RagAnswerVerifierIssue.PossiblyCutOff)
            }
            val evidenceNumbers = evidence
                .flatMap { NUMBER_PATTERN.findAll(it.content).map { match -> match.value } }
                .toSet()
            val unsupportedNumbers = NUMBER_PATTERN.findAll(answer)
                .map { it.value }
                .filter { it !in evidenceNumbers }
                .toSet()
            if (unsupportedNumbers.isNotEmpty()) {
                add(RagAnswerVerifierIssue.UnsupportedNumbers(unsupportedNumbers.sorted()))
            }
            val validIds = evidence.map { it.id }.toSet()
            val badCitations = Regex("\\[(S\\d+)]").findAll(answer)
                .map { it.groupValues[1] }
                .filter { it !in validIds }
                .toSet()
            if (badCitations.isNotEmpty()) {
                add(RagAnswerVerifierIssue.InvalidCitations(badCitations.sorted()))
            }
        }
        return RagAnswerVerifierResult(issues = issues)
    }

    private fun Char.isTerminalPunctuation(): Boolean = this == '.' || this == '!' || this == '?' || this == ']'

    private val NUMBER_PATTERN = Regex("\\b\\d+(?:\\.\\d+)?(?:\\s?(?:%|degrees?|hours?|minutes?|days?|gallons?|liters?|feet|miles))?\\b")
}

object RagAnswerCitationPostProcessor {
    fun ensureCitations(
        answer: String,
        evidence: List<RagEvidence>,
    ): String {
        if (answer.isBlank()) {
            return answer
        }
        val normalized = answer.normalizeParenthesizedCitations(evidence.map { it.id }.toSet())
        if (normalized.contains(Regex("\\[S\\d+]"))) {
            return normalized
        }
        val citedIds = evidence
            .take(DEFAULT_FALLBACK_CITATION_COUNT)
            .joinToString(", ") { "[${it.id}]" }
        if (citedIds.isBlank()) return answer
        return normalized.trimEnd() + "\n\nSources: $citedIds"
    }

    private fun String.normalizeParenthesizedCitations(validIds: Set<String>): String =
        PARENTHESIZED_CITATION_PATTERN.replace(this) { match ->
            val ids = CITATION_ID_PATTERN.findAll(match.value)
                .map { it.value }
                .filter { it in validIds }
                .toList()
            if (ids.isEmpty()) {
                match.value
            } else {
                ids.joinToString(" ") { "[$it]" }
            }
        }

    private val PARENTHESIZED_CITATION_PATTERN = Regex("\\((?:\\s*S\\d+\\s*,?\\s*)+\\)")
    private val CITATION_ID_PATTERN = Regex("S\\d+")
    private const val DEFAULT_FALLBACK_CITATION_COUNT = 3
}

data class RagAnswerVerifierResult(
    val issues: List<RagAnswerVerifierIssue>,
) {
    val passed: Boolean = issues.isEmpty()
    val shouldRepair: Boolean = issues.isNotEmpty()
}

sealed class RagAnswerVerifierIssue(val message: String) {
    data object EmptyAnswer : RagAnswerVerifierIssue("answer is empty")
    data object MissingCitations : RagAnswerVerifierIssue("citations are missing")
    data object PossiblyCutOff : RagAnswerVerifierIssue("answer may be cut off")
    data class UnsupportedNumbers(val numbers: List<String>) :
        RagAnswerVerifierIssue("unsupported numbers: ${numbers.joinToString()}")

    data class InvalidCitations(val citations: List<String>) :
        RagAnswerVerifierIssue("invalid citations: ${citations.joinToString()}")
}

private fun List<RagRetrievalHit>.toEvidence(
    questionProfile: RagAnswerQuestionProfile,
    queryService: RagQueryService,
): List<RagEvidence> =
    mapIndexed { index, hit ->
        hit.toEvidence(
            index = index + 1,
            neighbors = if (index < questionProfile.maxEvidenceSources && questionProfile.neighborRadius > 0) {
                queryService.neighboringChunks(hit, questionProfile.neighborRadius)
            } else {
                emptyList()
            },
        )
    }

private fun RagRetrievalHit.toEvidence(
    index: Int,
    neighbors: List<RagCorpusFtsMatch>,
): RagEvidence {
    val relatedContent = listOf(chunk.content)
        .plus(
            neighbors
                .filter { it.documentId == chunk.documentId }
                .map { it.content },
        )
        .distinct()
        .joinToString("\n\n")

    return RagEvidence(
        id = "S${min(index, 99)}",
        title = chunk.title,
        sectionTitle = chunk.sectionTitle,
        content = relatedContent,
        sourceCitation = chunk.sourceCitation,
        sourceUrl = chunk.sourceUrl,
        category = chunk.category,
        urgency = chunk.urgency,
    )
}
