package ai.unplugged.posa.data.local

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object RagCorpusAssetInstaller {
    const val DEFAULT_ASSET_PATH = "rag/posa_rag.db"
    const val DEFAULT_FILE_NAME = "posa_rag.db"

    fun installIfBundled(
        context: Context,
        assetPath: String = DEFAULT_ASSET_PATH,
        fileName: String = DEFAULT_FILE_NAME,
        forceReplace: Boolean = false,
    ): File? {
        if (!context.hasAsset(assetPath)) {
            return null
        }
        val destination = installedFile(context, fileName)
        if (destination.exists() && !forceReplace) {
            return destination
        }

        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, "$fileName.tmp")
        if (temporary.exists()) {
            temporary.delete()
        }
        context.assets.open(assetPath).use { input ->
            temporary.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        if (destination.exists()) {
            destination.delete()
        }
        require(temporary.renameTo(destination)) {
            "Unable to install bundled RAG corpus."
        }
        return destination
    }

    fun installedFile(
        context: Context,
        fileName: String = DEFAULT_FILE_NAME,
    ): File = File(File(context.noBackupFilesDir, "rag"), fileName)

    private fun Context.hasAsset(assetPath: String): Boolean =
        try {
            assets.open(assetPath).use { true }
        } catch (_: Exception) {
            false
        }
}

class RagCorpusDatabase private constructor(
    private val database: SQLiteDatabase,
) : Closeable {
    fun metadata(): RagCorpusMetadata {
        val rows = database.rawQuery("SELECT key, value FROM rag_metadata", emptyArray()).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    put(cursor.string("key"), cursor.string("value"))
                }
            }
        }
        return RagCorpusMetadata(
            corpusId = rows.getValue("corpus_id"),
            corpusVersion = rows.getValue("corpus_version"),
            createdAt = rows.getValue("created_at"),
            documentCount = rows.getValue("document_count").toInt(),
            chunkCount = rows.getValue("chunk_count").toInt(),
            embeddingCount = rows.getValue("embedding_count").toInt(),
            embeddingDimension = rows.getValue("embedding_dimension").toInt(),
            embeddingModelId = rows.getValue("embedding_model_id"),
            distanceMetric = rows.getValue("distance_metric"),
            vectorFormat = rows.getValue("vector_format"),
            normalized = rows.getValue("normalized").toBoolean(),
            schemaVersion = rows.getValue("schema_version").toInt(),
            sourceHash = rows.getValue("source_hash"),
        )
    }

    fun searchFts(
        matchQuery: String,
        limit: Int,
    ): List<RagCorpusFtsMatch> {
        require(limit > 0) { "limit must be positive." }
        return database.rawQuery(
            """
            SELECT
                f.chunk_id,
                f.document_id,
                f.title,
                f.section_title,
                f.content,
                f.category,
                f.hazard_tags,
                f.audience_tags,
                f.urgency,
                d.source_url,
                d.source_citation
            FROM retrieval_chunks_fts f
            JOIN retrieval_documents d ON d.id = f.document_id
            WHERE retrieval_chunks_fts MATCH ?
            LIMIT ?
            """.trimIndent(),
            arrayOf(matchQuery, limit.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toFtsMatch())
                }
            }
        }
    }

    fun getEmbedding(chunkId: String): RagCorpusEmbedding? =
        database.rawQuery(
            """
            SELECT chunk_id, embedding_model_id, embedding_dimension, vector_format, content_hash, embedding
            FROM retrieval_chunk_embeddings
            WHERE chunk_id = ?
            """.trimIndent(),
            arrayOf(chunkId),
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.toEmbedding()
            } else {
                null
            }
        }

    fun listEmbeddings(limit: Int? = null): List<RagCorpusEmbedding> {
        val sql = buildString {
            append(
                """
                SELECT chunk_id, embedding_model_id, embedding_dimension, vector_format, content_hash, embedding
                FROM retrieval_chunk_embeddings
                ORDER BY chunk_id
                """.trimIndent(),
            )
            if (limit != null) {
                append(" LIMIT ?")
            }
        }
        val args = if (limit != null) arrayOf(limit.toString()) else emptyArray()
        return database.rawQuery(sql, args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toEmbedding())
                }
            }
        }
    }

    fun listVectorChunks(): List<RagCorpusVectorChunk> =
        database.rawQuery(
            """
            SELECT
                f.chunk_id,
                f.document_id,
                f.title,
                f.section_title,
                f.content,
                f.category,
                f.hazard_tags,
                f.audience_tags,
                f.urgency,
                d.source_url,
                d.source_citation,
                e.embedding_model_id,
                e.embedding_dimension,
                e.vector_format,
                e.content_hash,
                e.embedding
            FROM retrieval_chunk_embeddings e
            JOIN retrieval_chunks_fts f ON f.chunk_id = e.chunk_id
            JOIN retrieval_documents d ON d.id = f.document_id
            ORDER BY f.document_id, f.chunk_id
            """.trimIndent(),
            emptyArray(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toVectorChunk())
                }
            }
        }

    fun listNeighboringChunks(
        documentId: String,
        chunkId: String,
        radius: Int,
    ): List<RagCorpusFtsMatch> {
        require(radius >= 0) { "radius must not be negative." }
        if (radius == 0) return emptyList()

        return runCatching {
            database.rawQuery(
                """
                SELECT
                    c.id AS chunk_id,
                    c.document_id,
                    d.title,
                    c.section_title,
                    c.content,
                    c.category,
                    c.hazard_tags,
                    c.audience_tags,
                    c.urgency,
                    d.source_url,
                    d.source_citation
                FROM retrieval_chunks c
                JOIN retrieval_documents d ON d.id = c.document_id
                WHERE c.document_id = ?
                    AND c.id != ?
                    AND c.chunk_ordinal BETWEEN
                        (SELECT chunk_ordinal FROM retrieval_chunks WHERE id = ? AND document_id = ?) - ?
                        AND
                        (SELECT chunk_ordinal FROM retrieval_chunks WHERE id = ? AND document_id = ?) + ?
                ORDER BY c.chunk_ordinal
                """.trimIndent(),
                arrayOf(
                    documentId,
                    chunkId,
                    chunkId,
                    documentId,
                    radius.toString(),
                    chunkId,
                    documentId,
                    radius.toString(),
                ),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.toFtsMatch())
                    }
                }
            }
        }.getOrElse {
            emptyList()
        }
    }

    override fun close() {
        database.close()
    }

    companion object {
        fun openBundled(context: Context): RagCorpusDatabase? {
            val file = RagCorpusAssetInstaller.installIfBundled(context) ?: return null
            return openReadOnly(file)
        }

        fun openReadOnly(file: File): RagCorpusDatabase {
            require(file.exists()) { "RAG corpus DB does not exist: ${file.absolutePath}" }
            return RagCorpusDatabase(
                SQLiteDatabase.openDatabase(
                    file.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY,
                ),
            )
        }
    }
}

data class RagCorpusMetadata(
    val corpusId: String,
    val corpusVersion: String,
    val createdAt: String,
    val documentCount: Int,
    val chunkCount: Int,
    val embeddingCount: Int,
    val embeddingDimension: Int,
    val embeddingModelId: String,
    val distanceMetric: String,
    val vectorFormat: String,
    val normalized: Boolean,
    val schemaVersion: Int,
    val sourceHash: String,
)

data class RagCorpusFtsMatch(
    val chunkId: String,
    val documentId: String,
    val title: String,
    val sectionTitle: String?,
    val content: String,
    val category: String,
    val hazardTags: String?,
    val audienceTags: String?,
    val urgency: String?,
    val sourceUrl: String?,
    val sourceCitation: String?,
)

data class RagCorpusEmbedding(
    val chunkId: String,
    val embeddingModelId: String,
    val embeddingDimension: Int,
    val vectorFormat: String,
    val contentHash: String,
    val vector: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RagCorpusEmbedding) return false
        return chunkId == other.chunkId &&
            embeddingModelId == other.embeddingModelId &&
            embeddingDimension == other.embeddingDimension &&
            vectorFormat == other.vectorFormat &&
            contentHash == other.contentHash &&
            vector.contentEquals(other.vector)
    }

    override fun hashCode(): Int {
        var result = chunkId.hashCode()
        result = 31 * result + embeddingModelId.hashCode()
        result = 31 * result + embeddingDimension
        result = 31 * result + vectorFormat.hashCode()
        result = 31 * result + contentHash.hashCode()
        result = 31 * result + vector.contentHashCode()
        return result
    }
}

data class RagCorpusVectorChunk(
    val match: RagCorpusFtsMatch,
    val embedding: RagCorpusEmbedding,
)

private fun Cursor.toFtsMatch(): RagCorpusFtsMatch =
    RagCorpusFtsMatch(
        chunkId = string("chunk_id"),
        documentId = string("document_id"),
        title = string("title"),
        sectionTitle = nullableString("section_title"),
        content = string("content"),
        category = string("category"),
        hazardTags = nullableString("hazard_tags"),
        audienceTags = nullableString("audience_tags"),
        urgency = nullableString("urgency"),
        sourceUrl = nullableString("source_url"),
        sourceCitation = nullableString("source_citation"),
    )

private fun Cursor.toEmbedding(): RagCorpusEmbedding {
    val bytes = blob("embedding")
    val dimension = int("embedding_dimension")
    require(bytes.size == dimension * Float.SIZE_BYTES) {
        "Embedding byte length ${bytes.size} does not match dimension $dimension."
    }
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    val vector = FloatArray(dimension)
    buffer.get(vector)
    return RagCorpusEmbedding(
        chunkId = string("chunk_id"),
        embeddingModelId = string("embedding_model_id"),
        embeddingDimension = dimension,
        vectorFormat = string("vector_format"),
        contentHash = string("content_hash"),
        vector = vector,
    )
}

private fun Cursor.toVectorChunk(): RagCorpusVectorChunk =
    RagCorpusVectorChunk(
        match = toFtsMatch(),
        embedding = toEmbedding(),
    )

private fun Cursor.string(column: String): String = getString(getColumnIndexOrThrow(column))

private fun Cursor.nullableString(column: String): String? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getString(index)
}

private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))

private fun Cursor.blob(column: String): ByteArray = getBlob(getColumnIndexOrThrow(column))
