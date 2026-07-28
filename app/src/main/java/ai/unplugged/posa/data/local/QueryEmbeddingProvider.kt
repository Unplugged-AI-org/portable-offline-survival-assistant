package ai.unplugged.posa.data.local

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.Closeable
import java.io.File
import java.nio.LongBuffer
import java.text.Normalizer
import java.util.Locale
import kotlin.math.sqrt

interface QueryEmbeddingProvider : Closeable {
    val modelId: String
    val dimension: Int
    fun embedQuery(query: String): FloatArray
}

class BgeOnnxQueryEmbeddingProvider private constructor(
    private val tokenizer: BgeWordPieceTokenizer,
    private val runner: BgeModelRunner,
    private val maxSequenceLength: Int,
    private val queryInstruction: String,
    override val modelId: String,
    override val dimension: Int,
) : QueryEmbeddingProvider {
    override fun embedQuery(query: String): FloatArray {
        val encoded = tokenizer.encode(
            text = queryInstruction + query,
            maxSequenceLength = maxSequenceLength,
        )
        val tokenEmbeddings = runner.run(
            inputIds = encoded.inputIds,
            attentionMask = encoded.attentionMask,
            tokenTypeIds = encoded.tokenTypeIds,
            shape = longArrayOf(1L, maxSequenceLength.toLong()),
        )
        require(tokenEmbeddings.size == maxSequenceLength) {
            "Model returned ${tokenEmbeddings.size} token embeddings, expected $maxSequenceLength."
        }
        val pooled = meanPoolAndNormalize(tokenEmbeddings, encoded.attentionMask, dimension)
        return pooled
    }

    override fun close() {
        runner.close()
    }

    companion object {
        const val DEFAULT_MODEL_ID = "BAAI/bge-small-en-v1.5"
        const val DEFAULT_DIMENSION = 384
        const val DEFAULT_MAX_SEQUENCE_LENGTH = 512
        const val DEFAULT_QUERY_INSTRUCTION = "Represent this sentence for searching relevant passages: "
        const val DEFAULT_MODEL_ASSET_PATH = "embedding/bge-small-en-v1.5/model.onnx"
        const val DEFAULT_VOCAB_ASSET_PATH = "embedding/bge-small-en-v1.5/vocab.txt"

        fun create(
            context: Context,
            modelAssetPath: String = DEFAULT_MODEL_ASSET_PATH,
            vocabAssetPath: String = DEFAULT_VOCAB_ASSET_PATH,
            maxSequenceLength: Int = DEFAULT_MAX_SEQUENCE_LENGTH,
        ): BgeOnnxQueryEmbeddingProvider {
            val tokenizer = context.assets.open(vocabAssetPath).use { input ->
                BgeWordPieceTokenizer.fromVocab(input.bufferedReader().readLines())
            }
            val modelFile = copyAssetToNoBackupFile(
                context = context,
                assetPath = modelAssetPath,
                fileName = "bge-small-en-v1.5.onnx",
            )
            val runner = OnnxBgeModelRunner(modelFile)
            return BgeOnnxQueryEmbeddingProvider(
                tokenizer = tokenizer,
                runner = runner,
                maxSequenceLength = maxSequenceLength,
                queryInstruction = DEFAULT_QUERY_INSTRUCTION,
                modelId = DEFAULT_MODEL_ID,
                dimension = DEFAULT_DIMENSION,
            )
        }

        internal fun createForTest(
            tokenizer: BgeWordPieceTokenizer,
            runner: BgeModelRunner,
            maxSequenceLength: Int,
            queryInstruction: String = "",
            modelId: String = DEFAULT_MODEL_ID,
            dimension: Int = runner.hiddenSize,
        ): BgeOnnxQueryEmbeddingProvider =
            BgeOnnxQueryEmbeddingProvider(
                tokenizer = tokenizer,
                runner = runner,
                maxSequenceLength = maxSequenceLength,
                queryInstruction = queryInstruction,
                modelId = modelId,
                dimension = dimension,
            )

        private fun copyAssetToNoBackupFile(
            context: Context,
            assetPath: String,
            fileName: String,
        ): File {
            val destination = File(File(context.noBackupFilesDir, "embedding"), fileName)
            if (destination.exists()) {
                return destination
            }
            destination.parentFile?.mkdirs()
            val temporary = File(destination.parentFile, "$fileName.tmp")
            context.assets.open(assetPath).use { input ->
                temporary.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (destination.exists()) {
                destination.delete()
            }
            require(temporary.renameTo(destination)) {
                "Unable to install BGE ONNX model asset."
            }
            return destination
        }
    }
}

internal data class BgeEncodedInput(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokenTypeIds: LongArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BgeEncodedInput) return false
        return inputIds.contentEquals(other.inputIds) &&
            attentionMask.contentEquals(other.attentionMask) &&
            tokenTypeIds.contentEquals(other.tokenTypeIds)
    }

    override fun hashCode(): Int {
        var result = inputIds.contentHashCode()
        result = 31 * result + attentionMask.contentHashCode()
        result = 31 * result + tokenTypeIds.contentHashCode()
        return result
    }
}

internal class BgeWordPieceTokenizer private constructor(
    private val vocab: Map<String, Long>,
) {
    private val clsId = requireToken("[CLS]")
    private val sepId = requireToken("[SEP]")
    private val padId = requireToken("[PAD]")
    private val unkId = requireToken("[UNK]")

    fun encode(text: String, maxSequenceLength: Int): BgeEncodedInput {
        require(maxSequenceLength >= 2) { "maxSequenceLength must leave room for CLS and SEP." }
        val pieces = basicTokens(text)
            .flatMap { token -> wordPieces(token) }
            .take(maxSequenceLength - 2)

        val ids = LongArray(maxSequenceLength) { padId }
        val mask = LongArray(maxSequenceLength)
        val tokenTypes = LongArray(maxSequenceLength)
        val sequence = listOf(clsId) + pieces.map { vocab[it] ?: unkId } + listOf(sepId)
        sequence.forEachIndexed { index, id ->
            ids[index] = id
            mask[index] = 1L
        }
        return BgeEncodedInput(
            inputIds = ids,
            attentionMask = mask,
            tokenTypeIds = tokenTypes,
        )
    }

    internal fun tokenize(text: String): List<String> =
        basicTokens(text).flatMap { token -> wordPieces(token) }

    private fun basicTokens(text: String): List<String> =
        text.lowercase(Locale.US)
            .stripAccents()
            .split(Regex("[^a-z0-9]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun wordPieces(token: String): List<String> {
        if (token.length > MAX_WORD_CHARS) {
            return listOf("[UNK]")
        }
        val pieces = mutableListOf<String>()
        var start = 0
        while (start < token.length) {
            var end = token.length
            var current: String? = null
            while (start < end) {
                val substring = token.substring(start, end)
                val candidate = if (start == 0) substring else "##$substring"
                if (candidate in vocab) {
                    current = candidate
                    break
                }
                end--
            }
            if (current == null) {
                return listOf("[UNK]")
            }
            pieces += current
            start = end
        }
        return pieces
    }

    private fun requireToken(token: String): Long =
        requireNotNull(vocab[token]) { "BGE vocab is missing required token $token." }

    private fun String.stripAccents(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

    companion object {
        private const val MAX_WORD_CHARS = 100

        fun fromVocab(lines: List<String>): BgeWordPieceTokenizer {
            val vocab = lines
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapIndexed { index, token -> token to index.toLong() }
                .toMap()
            return BgeWordPieceTokenizer(vocab)
        }
    }
}

internal interface BgeModelRunner : Closeable {
    val hiddenSize: Int
    fun run(
        inputIds: LongArray,
        attentionMask: LongArray,
        tokenTypeIds: LongArray,
        shape: LongArray,
    ): Array<FloatArray>
}

private class OnnxBgeModelRunner(
    modelFile: File,
) : BgeModelRunner {
    private val environment = OrtEnvironment.getEnvironment()
    private val session = environment.createSession(modelFile.absolutePath, OrtSession.SessionOptions())

    override val hiddenSize: Int = BgeOnnxQueryEmbeddingProvider.DEFAULT_DIMENSION

    override fun run(
        inputIds: LongArray,
        attentionMask: LongArray,
        tokenTypeIds: LongArray,
        shape: LongArray,
    ): Array<FloatArray> {
        OnnxTensor.createTensor(environment, LongBuffer.wrap(inputIds), shape).use { inputTensor ->
            OnnxTensor.createTensor(environment, LongBuffer.wrap(attentionMask), shape).use { maskTensor ->
                OnnxTensor.createTensor(environment, LongBuffer.wrap(tokenTypeIds), shape).use { tokenTypeTensor ->
                    val inputs = mapOf(
                        "input_ids" to inputTensor,
                        "attention_mask" to maskTensor,
                        "token_type_ids" to tokenTypeTensor,
                    )
                    session.run(inputs).use { results ->
                        val value = results[0].value
                        @Suppress("UNCHECKED_CAST")
                        val batch = value as Array<Array<FloatArray>>
                        return batch[0]
                    }
                }
            }
        }
    }

    override fun close() {
        session.close()
    }
}

private fun meanPoolAndNormalize(
    tokenEmbeddings: Array<FloatArray>,
    attentionMask: LongArray,
    dimension: Int,
): FloatArray {
    val pooled = FloatArray(dimension)
    var tokenCount = 0
    tokenEmbeddings.forEachIndexed { index, embedding ->
        if (attentionMask[index] == 1L) {
            require(embedding.size == dimension) {
                "Token embedding dimension ${embedding.size} does not match expected dimension $dimension."
            }
            for (dimensionIndex in 0 until dimension) {
                pooled[dimensionIndex] += embedding[dimensionIndex]
            }
            tokenCount++
        }
    }
    require(tokenCount > 0) { "Attention mask did not include any tokens." }
    for (dimensionIndex in 0 until dimension) {
        pooled[dimensionIndex] /= tokenCount
    }
    return pooled.normalize()
}

private fun FloatArray.normalize(): FloatArray {
    var sum = 0f
    forEach { value -> sum += value * value }
    val norm = sqrt(sum)
    if (norm == 0f) {
        return this
    }
    for (index in indices) {
        this[index] /= norm
    }
    return this
}
