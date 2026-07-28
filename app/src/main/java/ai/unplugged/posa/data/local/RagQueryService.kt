package ai.unplugged.posa.data.local

import android.content.Context
import java.io.Closeable
import kotlin.system.measureTimeMillis

class RagQueryService(
    private val retrievalService: RagRetrievalService,
    private val queryEmbeddingProvider: QueryEmbeddingProvider?,
    private val ownedResources: List<Closeable> = emptyList(),
) : Closeable {
    fun search(
        query: String,
        topK: Int = DEFAULT_TOP_K,
        categories: Set<String> = emptySet(),
        urgencies: Set<String> = emptySet(),
        requiredHazardTags: Set<String> = emptySet(),
        requiredAudienceTags: Set<String> = emptySet(),
    ): RagQueryResult {
        var embeddingResult: Result<FloatArray>? = null
        val embeddingMillis = queryEmbeddingProvider?.let { provider ->
            measureTimeMillis {
                embeddingResult = runCatching {
                    require(provider.modelId == retrievalService.embeddingModelId) {
                        "Query embedding model ${provider.modelId} does not match corpus model ${retrievalService.embeddingModelId}."
                    }
                    require(provider.dimension == retrievalService.embeddingDimension) {
                        "Query embedding dimension ${provider.dimension} does not match corpus dimension ${retrievalService.embeddingDimension}."
                    }
                    provider.embedQuery(query)
                }
            }
        }
        val queryEmbedding = embeddingResult?.getOrNull()
        val retrievalResult = retrievalService.searchWithTimings(
            RagRetrievalRequest(
                query = query,
                queryEmbedding = queryEmbedding,
                topK = topK,
                categories = categories,
                urgencies = urgencies,
                requiredHazardTags = requiredHazardTags,
                requiredAudienceTags = requiredAudienceTags,
            ),
        )

        return RagQueryResult(
            query = query,
            hits = retrievalResult.hits,
            embeddingStatus = when {
                queryEmbedding != null -> RagQueryEmbeddingStatus.Used
                queryEmbeddingProvider == null -> RagQueryEmbeddingStatus.Unavailable
                else -> RagQueryEmbeddingStatus.Failed
            },
            embeddingFailure = embeddingResult?.exceptionOrNull()?.message,
            timings = RagQueryTimings(
                embeddingMillis = embeddingMillis,
                retrievalTimings = retrievalResult.timings,
                totalMillis = (embeddingMillis ?: 0L) + retrievalResult.timings.totalMillis,
            ),
        )
    }

    fun neighboringChunks(
        hit: RagRetrievalHit,
        radius: Int,
    ): List<RagCorpusFtsMatch> = retrievalService.neighboringChunks(hit, radius)

    override fun close() {
        queryEmbeddingProvider?.close()
        ownedResources.forEach { it.close() }
    }

    companion object {
        const val DEFAULT_TOP_K = 5

        fun openBundled(context: Context): RagQueryService? {
            val corpus = RagCorpusDatabase.openBundled(context) ?: return null
            return try {
                val retrievalService = RagRetrievalService(corpus)
                val queryEmbeddingProvider = runCatching {
                    BgeOnnxQueryEmbeddingProvider.create(context)
                }.getOrNull()
                RagQueryService(
                    retrievalService = retrievalService,
                    queryEmbeddingProvider = queryEmbeddingProvider,
                    ownedResources = listOf(corpus),
                )
            } catch (exception: Exception) {
                corpus.close()
                throw exception
            }
        }
    }
}

data class RagQueryResult(
    val query: String,
    val hits: List<RagRetrievalHit>,
    val embeddingStatus: RagQueryEmbeddingStatus,
    val embeddingFailure: String?,
    val timings: RagQueryTimings = RagQueryTimings(),
)

data class RagQueryTimings(
    val embeddingMillis: Long? = null,
    val retrievalTimings: RagRetrievalTimings = RagRetrievalTimings(
        ftsMillis = 0,
        vectorMillis = 0,
        hybridMergeMillis = 0,
        totalMillis = 0,
    ),
    val totalMillis: Long = (embeddingMillis ?: 0L) + retrievalTimings.totalMillis,
)

enum class RagQueryEmbeddingStatus {
    Used,
    Unavailable,
    Failed,
}
