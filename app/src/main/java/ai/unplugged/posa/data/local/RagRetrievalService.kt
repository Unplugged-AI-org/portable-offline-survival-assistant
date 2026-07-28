package ai.unplugged.posa.data.local

import java.util.Locale
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

class RagRetrievalService(
    private val corpus: RagCorpusDatabase,
    private val config: RagRetrievalConfig = RagRetrievalConfig(),
) {
    private val metadata: RagCorpusMetadata by lazy {
        corpus.metadata()
    }

    val embeddingModelId: String
        get() = metadata.embeddingModelId

    val embeddingDimension: Int
        get() = metadata.embeddingDimension

    private val vectorChunks: List<RagCorpusVectorChunk> by lazy {
        corpus.listVectorChunks()
    }

    fun search(request: RagRetrievalRequest): List<RagRetrievalHit> =
        searchWithTimings(request).hits

    fun searchWithTimings(request: RagRetrievalRequest): RagRetrievalResult {
        require(request.topK > 0) { "topK must be positive." }
        var ftsHits: List<RagRetrievalHit>
        val ftsMillis = measureTimeMillis {
            ftsHits = searchFts(request)
        }
        var vectorHits: List<RagRetrievalHit>
        val vectorMillis = measureTimeMillis {
            vectorHits = request.queryEmbedding?.let { embedding ->
                searchVector(request, embedding)
            }.orEmpty()
        }

        var hits: List<RagRetrievalHit>
        val mergeMillis = measureTimeMillis {
            hits = when {
                ftsHits.isEmpty() && vectorHits.isEmpty() -> emptyList()
                vectorHits.isEmpty() -> ftsHits.take(request.topK)
                ftsHits.isEmpty() -> vectorHits.take(request.topK)
                else -> mergeHybrid(ftsHits, vectorHits, request.topK)
            }
        }

        return RagRetrievalResult(
            hits = hits,
            timings = RagRetrievalTimings(
                ftsMillis = ftsMillis,
                vectorMillis = vectorMillis,
                hybridMergeMillis = mergeMillis,
                totalMillis = ftsMillis + vectorMillis + mergeMillis,
            ),
        )
    }

    fun neighboringChunks(
        hit: RagRetrievalHit,
        radius: Int,
    ): List<RagCorpusFtsMatch> =
        corpus.listNeighboringChunks(
            documentId = hit.chunk.documentId,
            chunkId = hit.chunk.chunkId,
            radius = radius,
        )

    private fun searchFts(request: RagRetrievalRequest): List<RagRetrievalHit> {
        val matchQuery = request.ftsQuery ?: request.query.toFtsMatchQuery()
        if (matchQuery.isBlank()) {
            return emptyList()
        }

        return runCatching {
            corpus.searchFts(matchQuery, request.ftsLimit)
        }.getOrElse {
            val fallback = request.query.toFtsMatchQuery(maxTerms = 8, useOr = false)
            if (fallback.isBlank()) {
                emptyList()
            } else {
                corpus.searchFts(fallback, request.ftsLimit)
            }
        }
            .filter { it.matchesFilters(request) }
            .distinctBy { it.chunkId }
            .sortedWith(
                compareByDescending<RagCorpusFtsMatch> { it.lexicalScore(request.query) }
                    .thenBy { it.documentId }
                    .thenBy { it.chunkId },
            )
            .take(request.ftsLimit)
            .mapIndexed { index, match ->
                RagRetrievalHit(
                    chunk = match,
                    ftsRank = index + 1,
                    vectorRank = null,
                    vectorScore = null,
                    hybridScore = reciprocalRank(index + 1, config.rrfK) * config.ftsWeight,
                    source = RagRetrievalSource.Fts,
                )
            }
    }

    private fun searchVector(
        request: RagRetrievalRequest,
        queryEmbedding: FloatArray,
    ): List<RagRetrievalHit> {
        require(queryEmbedding.size == metadata.embeddingDimension) {
            "Query embedding dimension ${queryEmbedding.size} does not match corpus dimension ${metadata.embeddingDimension}."
        }
        val queryNorm = queryEmbedding.norm()
        if (queryNorm == 0f) {
            return emptyList()
        }

        return vectorChunks
            .asSequence()
            .filter { it.match.matchesFilters(request) }
            .map { vectorChunk ->
                vectorChunk to cosine(queryEmbedding, queryNorm, vectorChunk.embedding.vector)
            }
            .sortedByDescending { (_, score) -> score }
            .take(request.vectorLimit)
            .mapIndexed { index, (vectorChunk, score) ->
                RagRetrievalHit(
                    chunk = vectorChunk.match,
                    ftsRank = null,
                    vectorRank = index + 1,
                    vectorScore = score,
                    hybridScore = reciprocalRank(index + 1, config.rrfK) * config.vectorWeight,
                    source = RagRetrievalSource.Vector,
                )
            }
            .toList()
    }

    private fun mergeHybrid(
        ftsHits: List<RagRetrievalHit>,
        vectorHits: List<RagRetrievalHit>,
        topK: Int,
    ): List<RagRetrievalHit> {
        val merged = linkedMapOf<String, RagRetrievalHit>()

        ftsHits.forEach { hit ->
            merged[hit.chunk.chunkId] = hit.copy(source = RagRetrievalSource.Hybrid)
        }
        vectorHits.forEach { vectorHit ->
            val existing = merged[vectorHit.chunk.chunkId]
            if (existing == null) {
                merged[vectorHit.chunk.chunkId] = vectorHit.copy(source = RagRetrievalSource.Hybrid)
            } else {
                merged[vectorHit.chunk.chunkId] = existing.copy(
                    vectorRank = vectorHit.vectorRank,
                    vectorScore = vectorHit.vectorScore,
                    hybridScore = existing.hybridScore + vectorHit.hybridScore,
                    source = RagRetrievalSource.Hybrid,
                )
            }
        }

        return merged.values
            .sortedWith(
                compareByDescending<RagRetrievalHit> { it.hybridScore }
                    .thenBy { it.bestRank ?: Int.MAX_VALUE }
                    .thenBy { it.chunk.documentId }
                    .thenBy { it.chunk.chunkId },
            )
            .take(topK)
    }

    private fun RagCorpusFtsMatch.matchesFilters(request: RagRetrievalRequest): Boolean =
        (request.categories.isEmpty() || category in request.categories) &&
            (request.urgencies.isEmpty() || urgency in request.urgencies) &&
            request.requiredHazardTags.all { tag -> hazardTags.orEmpty().containsJsonTag(tag) } &&
            request.requiredAudienceTags.all { tag -> audienceTags.orEmpty().containsJsonTag(tag) }

    private fun String.toFtsMatchQuery(
        maxTerms: Int = config.maxFtsTerms,
        useOr: Boolean = true,
    ): String {
        val terms = lowercase(Locale.US)
            .split(Regex("[^a-z0-9]+"))
            .map { it.trim() }
            .filter { it.length >= config.minFtsTermLength && it !in STOP_WORDS }
            .distinct()
            .take(maxTerms)

        return if (useOr) {
            terms.joinToString(" OR ")
        } else {
            terms.joinToString(" ")
        }
    }

    private fun RagCorpusFtsMatch.lexicalScore(query: String): Int {
        val terms = query.lexicalTerms()
        if (terms.isEmpty()) return 0

        val titleText = "$title ${sectionTitle.orEmpty()}".lowercase(Locale.US)
        val contentText = content.lowercase(Locale.US)
        val queryMentionsSpecificContainer = SPECIFIC_WATER_CONTAINER_TERMS.any { it in terms }
        var score = 0

        terms.forEach { term ->
            if (titleText.contains(term)) score += 8
            if (contentText.contains(term)) score += 1
        }

        terms.windowed(size = 2).forEach { pair ->
            val phrase = pair.joinToString(" ")
            if (titleText.contains(phrase)) score += 10
            if (contentText.contains(phrase)) score += 2
        }
        terms.windowed(size = 3).forEach { triple ->
            val phrase = triple.joinToString(" ")
            if (titleText.contains(phrase)) score += 18
            if (contentText.contains(phrase)) score += 4
        }

        if (!queryMentionsSpecificContainer && category == "water") {
            if ("cistern" in titleText) score -= 10
            if ("well" in titleText) score -= 6
            if ("rainwater" in titleText) score -= 6
        }

        return score
    }

    private fun String.lexicalTerms(): List<String> =
        lowercase(Locale.US)
            .split(Regex("[^a-z0-9]+"))
            .map { it.trim() }
            .filter { it.length >= config.minFtsTermLength && it !in LEXICAL_STOP_WORDS }
            .distinct()

    private fun String.containsJsonTag(tag: String): Boolean =
        split(',', '[', ']', '"')
            .map { it.trim() }
            .any { it.equals(tag, ignoreCase = true) }

    private fun reciprocalRank(rank: Int, k: Int): Double = 1.0 / (k + rank)

    private fun cosine(query: FloatArray, queryNorm: Float, candidate: FloatArray): Float {
        val candidateNorm = candidate.norm()
        if (candidateNorm == 0f) {
            return 0f
        }
        var dot = 0f
        for (index in query.indices) {
            dot += query[index] * candidate[index]
        }
        return dot / (queryNorm * candidateNorm)
    }

    private fun FloatArray.norm(): Float {
        var sum = 0f
        forEach { value -> sum += value * value }
        return sqrt(sum)
    }

    private val RagRetrievalHit.bestRank: Int?
        get() = listOfNotNull(ftsRank, vectorRank).minOrNull()

    private companion object {
        val STOP_WORDS = setOf(
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
            "your",
        )
        val LEXICAL_STOP_WORDS = STOP_WORDS - setOf("after", "before")
        val SPECIFIC_WATER_CONTAINER_TERMS = setOf("cistern", "well", "rainwater", "tank")
    }
}

data class RagRetrievalConfig(
    val rrfK: Int = 60,
    val ftsWeight: Double = 1.35,
    val vectorWeight: Double = 0.85,
    val maxFtsTerms: Int = 24,
    val minFtsTermLength: Int = 3,
)

data class RagRetrievalRequest(
    val query: String,
    val queryEmbedding: FloatArray? = null,
    val topK: Int = 5,
    val ftsLimit: Int = max(20, topK * 8),
    val vectorLimit: Int = max(80, topK * 16),
    val ftsQuery: String? = null,
    val categories: Set<String> = emptySet(),
    val urgencies: Set<String> = emptySet(),
    val requiredHazardTags: Set<String> = emptySet(),
    val requiredAudienceTags: Set<String> = emptySet(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RagRetrievalRequest) return false
        return query == other.query &&
            queryEmbedding.contentEqualsNullable(other.queryEmbedding) &&
            topK == other.topK &&
            ftsLimit == other.ftsLimit &&
            vectorLimit == other.vectorLimit &&
            ftsQuery == other.ftsQuery &&
            categories == other.categories &&
            urgencies == other.urgencies &&
            requiredHazardTags == other.requiredHazardTags &&
            requiredAudienceTags == other.requiredAudienceTags
    }

    override fun hashCode(): Int {
        var result = query.hashCode()
        result = 31 * result + (queryEmbedding?.contentHashCode() ?: 0)
        result = 31 * result + topK
        result = 31 * result + ftsLimit
        result = 31 * result + vectorLimit
        result = 31 * result + (ftsQuery?.hashCode() ?: 0)
        result = 31 * result + categories.hashCode()
        result = 31 * result + urgencies.hashCode()
        result = 31 * result + requiredHazardTags.hashCode()
        result = 31 * result + requiredAudienceTags.hashCode()
        return result
    }
}

data class RagRetrievalHit(
    val chunk: RagCorpusFtsMatch,
    val ftsRank: Int?,
    val vectorRank: Int?,
    val vectorScore: Float?,
    val hybridScore: Double,
    val source: RagRetrievalSource,
)

data class RagRetrievalResult(
    val hits: List<RagRetrievalHit>,
    val timings: RagRetrievalTimings,
)

data class RagRetrievalTimings(
    val ftsMillis: Long,
    val vectorMillis: Long,
    val hybridMergeMillis: Long,
    val totalMillis: Long,
)

enum class RagRetrievalSource {
    Fts,
    Vector,
    Hybrid,
}

private fun FloatArray?.contentEqualsNullable(other: FloatArray?): Boolean = when {
    this == null && other == null -> true
    this == null || other == null -> false
    else -> contentEquals(other)
}
