package ai.unplugged.posa.data.local

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RagQueryServiceTest {
    private lateinit var tempDb: File
    private lateinit var corpus: RagCorpusDatabase
    private lateinit var retrievalService: RagRetrievalService

    @Before
    fun setUp() {
        tempDb = File.createTempFile("posa-rag-query-test", ".db")
        createRetrievalCorpus(tempDb)
        corpus = RagCorpusDatabase.openReadOnly(tempDb)
        retrievalService = RagRetrievalService(corpus)
    }

    @After
    fun tearDown() {
        if (::corpus.isInitialized) {
            corpus.close()
        }
        tempDb.delete()
    }

    @Test
    fun searchUsesQueryEmbeddingWhenProviderMatchesCorpus() {
        val queryService = RagQueryService(
            retrievalService = retrievalService,
            queryEmbeddingProvider = FakeQueryEmbeddingProvider(
                embedding = floatArrayOf(0f, 1f, 0f, 0f),
            ),
        )

        val result = queryService.search("terms without keyword match", topK = 1)

        assertEquals(RagQueryEmbeddingStatus.Used, result.embeddingStatus)
        assertEquals("chunk-campfire", result.hits.single().chunk.chunkId)
        assertEquals(RagRetrievalSource.Vector, result.hits.single().source)
    }

    @Test
    fun searchFallsBackToFtsWhenProviderIsUnavailable() {
        val queryService = RagQueryService(
            retrievalService = retrievalService,
            queryEmbeddingProvider = null,
        )

        val result = queryService.search("make water safe", topK = 1)

        assertEquals(RagQueryEmbeddingStatus.Unavailable, result.embeddingStatus)
        assertEquals("chunk-water", result.hits.single().chunk.chunkId)
        assertEquals(RagRetrievalSource.Fts, result.hits.single().source)
    }

    @Test
    fun searchFallsBackToFtsWhenProviderDoesNotMatchCorpus() {
        val queryService = RagQueryService(
            retrievalService = retrievalService,
            queryEmbeddingProvider = FakeQueryEmbeddingProvider(
                modelId = "other-model",
                embedding = floatArrayOf(0f, 1f, 0f, 0f),
            ),
        )

        val result = queryService.search("make water safe", topK = 1)

        assertEquals(RagQueryEmbeddingStatus.Failed, result.embeddingStatus)
        assertNotNull(result.embeddingFailure)
        assertTrue(result.embeddingFailure!!.contains("does not match corpus model"))
        assertEquals("chunk-water", result.hits.single().chunk.chunkId)
        assertEquals(RagRetrievalSource.Fts, result.hits.single().source)
    }

    private class FakeQueryEmbeddingProvider(
        override val modelId: String = "BAAI/bge-small-en-v1.5",
        override val dimension: Int = 4,
        private val embedding: FloatArray,
    ) : QueryEmbeddingProvider {
        override fun embedQuery(query: String): FloatArray = embedding
        override fun close() = Unit
    }

    private fun createRetrievalCorpus(file: File) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE rag_metadata (
                    key TEXT NOT NULL PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """.trimIndent(),
            )
            listOf(
                "corpus_id" to "test-corpus",
                "corpus_version" to "private-test",
                "created_at" to "2026-07-13T00:00:00+00:00",
                "document_count" to "3",
                "chunk_count" to "3",
                "embedding_count" to "3",
                "embedding_dimension" to "4",
                "embedding_model_id" to "BAAI/bge-small-en-v1.5",
                "distance_metric" to "cosine",
                "vector_format" to "float32",
                "normalized" to "true",
                "schema_version" to "1",
                "source_hash" to "hash",
            ).forEach { (key, value) ->
                db.execSQL("INSERT INTO rag_metadata(key, value) VALUES (?, ?)", arrayOf(key, value))
            }
            db.execSQL(
                """
                CREATE TABLE retrieval_documents (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    source_url TEXT,
                    source_citation TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE retrieval_chunk_embeddings (
                    chunk_id TEXT NOT NULL PRIMARY KEY,
                    embedding_model_id TEXT NOT NULL,
                    embedding_dimension INTEGER NOT NULL,
                    vector_format TEXT NOT NULL,
                    content_hash TEXT NOT NULL,
                    embedding BLOB NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE retrieval_chunks (
                    chunk_id TEXT NOT NULL PRIMARY KEY,
                    document_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    source_url TEXT,
                    source_citation TEXT,
                    page_start INTEGER,
                    page_end INTEGER,
                    section_path TEXT,
                    category TEXT NOT NULL,
                    urgency TEXT NOT NULL,
                    hazard_tags TEXT,
                    audience_tags TEXT,
                    text TEXT NOT NULL,
                    token_count INTEGER,
                    content_hash TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE VIRTUAL TABLE retrieval_chunks_fts USING fts4(
                    chunk_id,
                    document_id,
                    title,
                    section_title,
                    heading_path,
                    content,
                    category,
                    hazard_tags,
                    audience_tags,
                    urgency,
                    tokenize=unicode61
                )
                """.trimIndent(),
            )

            insertChunk(
                db = db,
                chunkId = "chunk-water",
                documentId = "doc-water",
                title = "Make Water Safe",
                category = "water",
                urgency = "urgent",
                hazardTags = """["water"]""",
                audienceTags = """["preparedness"]""",
                text = "Boil water to make it safe during an emergency.",
                embedding = floatArrayOf(1f, 0f, 0f, 0f),
            )
            insertChunk(
                db = db,
                chunkId = "chunk-campfire",
                documentId = "doc-fire",
                title = "Campfire Safety",
                category = "fire",
                urgency = "routine",
                hazardTags = """["campfire"]""",
                audienceTags = """["camping"]""",
                text = "Keep campfires small and fully extinguish them before leaving.",
                embedding = floatArrayOf(0f, 1f, 0f, 0f),
            )
            insertChunk(
                db = db,
                chunkId = "chunk-food",
                documentId = "doc-food",
                title = "Food Storage",
                category = "food",
                urgency = "routine",
                hazardTags = """["wildlife"]""",
                audienceTags = """["camping"]""",
                text = "Store food securely away from wildlife.",
                embedding = floatArrayOf(0f, 0f, 1f, 0f),
            )
        }
    }

    private fun insertChunk(
        db: SQLiteDatabase,
        chunkId: String,
        documentId: String,
        title: String,
        category: String,
        urgency: String,
        hazardTags: String,
        audienceTags: String,
        text: String,
        embedding: FloatArray,
    ) {
        db.execSQL(
            "INSERT OR IGNORE INTO retrieval_documents(id, title, source_url, source_citation) VALUES (?, ?, ?, ?)",
            arrayOf(documentId, title, "https://example.test/$documentId", "$title citation"),
        )
        db.execSQL(
            """
            INSERT INTO retrieval_chunks(
                chunk_id, document_id, title, source_url, source_citation, page_start, page_end,
                section_path, category, urgency, hazard_tags, audience_tags, text, token_count, content_hash
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                chunkId,
                documentId,
                title,
                "https://example.test/$documentId",
                "$title citation",
                1,
                1,
                title,
                category,
                urgency,
                hazardTags,
                audienceTags,
                text,
                text.split(" ").size,
                "$chunkId-hash",
            ),
        )
        db.execSQL(
            """
            INSERT INTO retrieval_chunks_fts(
                chunk_id, document_id, title, section_title, heading_path, content,
                category, hazard_tags, audience_tags, urgency
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                chunkId,
                documentId,
                title,
                title,
                title,
                text,
                category,
                hazardTags,
                audienceTags,
                urgency,
            ),
        )
        db.execSQL(
            """
            INSERT INTO retrieval_chunk_embeddings(
                chunk_id, embedding_model_id, embedding_dimension, vector_format, content_hash, embedding
            ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                chunkId,
                "BAAI/bge-small-en-v1.5",
                embedding.size,
                "float32",
                "$chunkId-hash",
                embedding.toBlob(),
            ),
        )
    }

    private fun FloatArray.toBlob(): ByteArray {
        val buffer = ByteBuffer.allocate(size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        forEach { buffer.putFloat(it) }
        return buffer.array()
    }
}
