package ai.unplugged.posa.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RagCorpusAssetInstallerTest {
    private lateinit var context: Context
    private lateinit var tempDb: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tempDb = File.createTempFile("posa-rag-test", ".db")
        createTinyCorpus(tempDb)
    }

    @After
    fun tearDown() {
        tempDb.delete()
    }

    @Test
    fun installIfBundledReturnsNullWhenPrivateAssetIsAbsent() {
        assertNull(RagCorpusAssetInstaller.installIfBundled(context, assetPath = "rag/missing.db"))
    }

    @Test
    fun openReadOnlyReadsMetadataFtsMatchesAndEmbeddings() {
        RagCorpusDatabase.openReadOnly(tempDb).use { corpus ->
            val metadata = corpus.metadata()
            assertEquals("test-corpus", metadata.corpusId)
            assertEquals("private-test", metadata.corpusVersion)
            assertEquals(1, metadata.documentCount)
            assertEquals(1, metadata.chunkCount)
            assertEquals(1, metadata.embeddingCount)
            assertEquals(4, metadata.embeddingDimension)
            assertTrue(metadata.normalized)

            val matches = corpus.searchFts("water", limit = 5)
            assertEquals(1, matches.size)
            assertEquals("chunk-water", matches.single().chunkId)
            assertEquals("Water Safety", matches.single().title)
            assertEquals("Use clean water for drinking.", matches.single().content)

            val embedding = corpus.getEmbedding("chunk-water")
            assertEquals("chunk-water", embedding?.chunkId)
            assertEquals("BAAI/bge-small-en-v1.5", embedding?.embeddingModelId)
            assertArrayEquals(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f), embedding!!.vector, 0.00001f)
            assertEquals(listOf(embedding), corpus.listEmbeddings(limit = 1))
        }
    }

    private fun createTinyCorpus(file: File) {
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
                "document_count" to "1",
                "chunk_count" to "1",
                "embedding_count" to "1",
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
            db.execSQL(
                "INSERT INTO retrieval_documents(id, title, source_url, source_citation) VALUES (?, ?, ?, ?)",
                arrayOf("doc-water", "Water Safety", "https://example.test/water", "Water Safety citation."),
            )
            db.execSQL(
                """
                INSERT INTO retrieval_chunks_fts(
                    chunk_id, document_id, title, section_title, heading_path, content,
                    category, hazard_tags, audience_tags, urgency
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    "chunk-water",
                    "doc-water",
                    "Water Safety",
                    "Clean Water",
                    "Water > Clean Water",
                    "Use clean water for drinking.",
                    "water",
                    """["water"]""",
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
                    "chunk-water",
                    "BAAI/bge-small-en-v1.5",
                    4,
                    "float32",
                    "content-hash",
                    floatBytes(0.1f, 0.2f, 0.3f, 0.4f),
                ),
            )
        }
    }

    private fun floatBytes(vararg values: Float): ByteArray {
        val buffer = ByteBuffer.allocate(values.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach(buffer::putFloat)
        return buffer.array()
    }
}
