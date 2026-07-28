package ai.unplugged.posa.data.local

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RagRetrievalServiceTest {
    private lateinit var tempDb: File
    private lateinit var corpus: RagCorpusDatabase
    private lateinit var service: RagRetrievalService

    @Before
    fun setUp() {
        tempDb = File.createTempFile("posa-rag-retrieval-test", ".db")
        createRetrievalCorpus(tempDb)
        corpus = RagCorpusDatabase.openReadOnly(tempDb)
        service = RagRetrievalService(corpus)
    }

    @After
    fun tearDown() {
        if (::corpus.isInitialized) {
            corpus.close()
        }
        tempDb.delete()
    }

    @Test
    fun ftsSearchReturnsKeywordMatchesWhenNoQueryEmbeddingIsProvided() {
        val hits = service.search(
            RagRetrievalRequest(
                query = "How do I make water safe?",
                topK = 2,
            ),
        )

        assertEquals("chunk-water", hits.first().chunk.chunkId)
        assertEquals(RagRetrievalSource.Fts, hits.first().source)
        assertEquals(1, hits.first().ftsRank)
    }

    @Test
    fun vectorSearchWorksWhenTextHasNoFtsMatch() {
        val hits = service.search(
            RagRetrievalRequest(
                query = "unmatched terms",
                queryEmbedding = floatArrayOf(0f, 1f, 0f, 0f),
                topK = 1,
            ),
        )

        assertEquals("chunk-campfire", hits.single().chunk.chunkId)
        assertEquals(RagRetrievalSource.Vector, hits.single().source)
        assertEquals(1, hits.single().vectorRank)
        assertEquals(1f, hits.single().vectorScore!!, 0.00001f)
    }

    @Test
    fun hybridMergeCombinesFtsAndVectorRanks() {
        val hits = service.search(
            RagRetrievalRequest(
                query = "water campfire",
                queryEmbedding = floatArrayOf(0f, 1f, 0f, 0f),
                topK = 2,
            ),
        )

        assertEquals("chunk-campfire", hits.first().chunk.chunkId)
        assertEquals(RagRetrievalSource.Hybrid, hits.first().source)
        assertTrue(hits.first().hybridScore > hits[1].hybridScore)
        assertEquals(setOf("chunk-campfire", "chunk-water"), hits.map { it.chunk.chunkId }.toSet())
    }

    @Test
    fun filtersApplyToFtsAndVectorCandidates() {
        val hits = service.search(
            RagRetrievalRequest(
                query = "water campfire",
                queryEmbedding = floatArrayOf(0f, 1f, 0f, 0f),
                topK = 3,
                categories = setOf("water"),
            ),
        )

        assertEquals(listOf("chunk-water"), hits.map { it.chunk.chunkId })
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
                documentId = "doc-water",
                title = "Water Safety",
                chunkId = "chunk-water",
                sectionTitle = "Disinfection",
                content = "Boil or disinfect water before drinking.",
                category = "water",
                hazardTags = """["water","disinfection"]""",
                vector = floatArrayOf(1f, 0f, 0f, 0f),
            )
            insertChunk(
                db = db,
                documentId = "doc-campfire",
                title = "Campfire Safety",
                chunkId = "chunk-campfire",
                sectionTitle = "Put Out Fire",
                content = "Drown and stir a campfire until it is cold.",
                category = "campfire-wildfire",
                hazardTags = """["campfire"]""",
                vector = floatArrayOf(0f, 1f, 0f, 0f),
            )
            insertChunk(
                db = db,
                documentId = "doc-kit",
                title = "Emergency Kit",
                chunkId = "chunk-kit",
                sectionTitle = "Radio",
                content = "Carry a battery-powered or hand-crank radio.",
                category = "all-hazards",
                hazardTags = """["alerts"]""",
                vector = floatArrayOf(0f, 0f, 1f, 0f),
            )
        }
    }

    private fun insertChunk(
        db: SQLiteDatabase,
        documentId: String,
        title: String,
        chunkId: String,
        sectionTitle: String,
        content: String,
        category: String,
        hazardTags: String,
        vector: FloatArray,
    ) {
        db.execSQL(
            "INSERT OR IGNORE INTO retrieval_documents(id, title, source_url, source_citation) VALUES (?, ?, ?, ?)",
            arrayOf(documentId, title, "https://example.test/$documentId", "$title citation."),
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
                sectionTitle,
                title,
                content,
                category,
                hazardTags,
                """["general"]""",
                "immediate",
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
                vector.size,
                "float32",
                "$chunkId-hash",
                floatBytes(vector),
            ),
        )
    }

    private fun floatBytes(values: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(values.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach(buffer::putFloat)
        return buffer.array()
    }
}
