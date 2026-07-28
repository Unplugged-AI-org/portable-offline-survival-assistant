package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.RagCorpusDatabase
import ai.unplugged.posa.data.local.LocalLlmGeneration
import ai.unplugged.posa.data.local.LocalLlmRequest
import ai.unplugged.posa.data.local.LocalLlmRuntime
import ai.unplugged.posa.data.local.RagAnswerLlmStatus
import ai.unplugged.posa.data.local.RagAnswerService
import ai.unplugged.posa.data.local.RagQueryEmbeddingStatus
import ai.unplugged.posa.data.local.RagQueryService
import ai.unplugged.posa.data.local.RagRetrievalService
import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AskViewModelTest {
    private lateinit var application: Application
    private lateinit var tempDb: File

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        application = ApplicationProvider.getApplicationContext()
        tempDb = File.createTempFile("posa-ask-test", ".db")
        createAskCorpus(tempDb)
    }

    @After
    fun tearDown() {
        tempDb.delete()
        Dispatchers.resetMain()
    }

    @Test
    fun searchReturnsLocalSourceMatches() = runBlocking {
        val viewModel = AskViewModel(
            application = application,
            answerServiceFactory = { answerService(FakeLocalLlmRuntime()) },
            ioDispatcher = UnconfinedTestDispatcher(),
        )

        viewModel.setQuestion("How do I make water safe?")
        viewModel.search()

        val state = viewModel.awaitState { !it.isSearching && it.result != null }
        assertNull(state.errorMessage)
        assertEquals(RagQueryEmbeddingStatus.Unavailable, state.result?.embeddingStatus)
        assertEquals("Keyword", state.result?.retrievalModeLabel)
        assertEquals(listOf("Water Safety"), state.result?.sourceMatches?.map { it.title })
        assertTrue(state.result!!.sourceMatches.first().excerpt.contains("Boil water"))
        assertEquals(RagAnswerLlmStatus.Generated, state.result?.answer?.llmStatus)
        assertEquals(true, state.result?.answer?.verifierPassed)
        assertTrue(state.result?.answer?.ttftMillis != null)
        assertTrue(state.result!!.answer!!.ttftMillis!! < 10_000L)
    }

    @Test
    fun missingCorpusSurfacesError() = runBlocking {
        val viewModel = AskViewModel(
            application = application,
            answerServiceFactory = { null },
            ioDispatcher = UnconfinedTestDispatcher(),
        )

        viewModel.setQuestion("water")
        viewModel.search()

        val state = viewModel.awaitState { !it.isSearching && it.errorMessage != null }
        assertEquals("Local source corpus is not bundled.", state.errorMessage)
    }

    @Test
    fun missingLlmStillReturnsEvidence() = runBlocking {
        val viewModel = AskViewModel(
            application = application,
            answerServiceFactory = { answerService(localLlmRuntime = null) },
            ioDispatcher = UnconfinedTestDispatcher(),
        )

        viewModel.setQuestion("How do I make water safe?")
        viewModel.search()

        val state = viewModel.awaitState { !it.isSearching && it.result != null }
        assertEquals(listOf("Water Safety"), state.result?.sourceMatches?.map { it.title })
        assertEquals(RagAnswerLlmStatus.Unavailable, state.result?.answer?.llmStatus)
        assertEquals("Local LLM model is not bundled.", state.result?.answer?.llmFailure)
        assertEquals(null, state.result?.answer?.ttftMillis)
    }

    private fun answerService(localLlmRuntime: LocalLlmRuntime?): RagAnswerService {
        val corpus = RagCorpusDatabase.openReadOnly(tempDb)
        val queryService = RagQueryService(
            retrievalService = RagRetrievalService(corpus),
            queryEmbeddingProvider = null,
            ownedResources = listOf(corpus),
        )
        return RagAnswerService(
            queryService = queryService,
            localLlmRuntime = localLlmRuntime,
        )
    }

    private class FakeLocalLlmRuntime : LocalLlmRuntime {
        override val modelId: String = "test-local-llm"

        override fun generate(
            request: LocalLlmRequest,
            onFirstToken: () -> Unit,
        ): LocalLlmGeneration {
            onFirstToken()
            return LocalLlmGeneration(
                text = "Boil water before drinking when safe tap water is unavailable. [S1]",
                tokenCount = 11,
            )
        }

        override fun close() = Unit
    }

    private suspend fun AskViewModel.awaitState(
        predicate: (AskContentState) -> Boolean,
    ): AskContentState = withTimeout(5_000) { state.first(predicate) }

    private fun createAskCorpus(file: File) {
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
                "document_count" to "2",
                "chunk_count" to "2",
                "embedding_count" to "0",
                "embedding_dimension" to "384",
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
            insertFtsChunk(
                db = db,
                chunkId = "chunk-water",
                documentId = "doc-water",
                title = "Water Safety",
                sectionTitle = "Emergency disinfection",
                content = "Boil water before drinking when safe tap water is unavailable.",
                category = "water",
                hazardTags = """["water"]""",
                urgency = "immediate",
            )
            insertFtsChunk(
                db = db,
                chunkId = "chunk-fire",
                documentId = "doc-fire",
                title = "Campfire Safety",
                sectionTitle = "Extinguish",
                content = "Drown and stir campfires until they are cold.",
                category = "campfire",
                hazardTags = """["campfire"]""",
                urgency = "routine",
            )
        }
    }

    private fun insertFtsChunk(
        db: SQLiteDatabase,
        chunkId: String,
        documentId: String,
        title: String,
        sectionTitle: String,
        content: String,
        category: String,
        hazardTags: String,
        urgency: String,
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
                sectionTitle,
                content,
                category,
                hazardTags,
                """["general"]""",
                urgency,
            ),
        )
    }
}
