package ai.unplugged.posa.data.local

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryEmbeddingProviderTest {
    @Test
    fun wordPieceTokenizerLowercasesSplitsAndPadsInput() {
        val tokenizer = testTokenizer()

        assertEquals(listOf("water", "safe", "##ly"), tokenizer.tokenize("Water safely!"))

        val encoded = tokenizer.encode("Water safely", maxSequenceLength = 6)
        assertArrayEquals(longArrayOf(2, 5, 6, 7, 3, 0), encoded.inputIds)
        assertArrayEquals(longArrayOf(1, 1, 1, 1, 1, 0), encoded.attentionMask)
        assertArrayEquals(longArrayOf(0, 0, 0, 0, 0, 0), encoded.tokenTypeIds)
    }

    @Test
    fun embedQueryMeanPoolsAttentionTokensAndNormalizes() {
        val tokenizer = testTokenizer()
        val provider = BgeOnnxQueryEmbeddingProvider.createForTest(
            tokenizer = tokenizer,
            runner = FakeBgeModelRunner(hiddenSize = 2),
            maxSequenceLength = 4,
            queryInstruction = "",
        )

        val embedding = provider.embedQuery("water")

        val expected = floatArrayOf(1f, 1f).normalizeForTest()
        assertArrayEquals(expected, embedding, 0.00001f)
        assertEquals(2, embedding.size)
        assertTrue(embedding.sumOf { (it * it).toDouble() } in 0.9999..1.0001)
    }

    private fun testTokenizer(): BgeWordPieceTokenizer =
        BgeWordPieceTokenizer.fromVocab(
            listOf(
                "[PAD]",
                "[UNK]",
                "[CLS]",
                "[SEP]",
                "[MASK]",
                "water",
                "safe",
                "##ly",
                "camp",
                "##fire",
            ),
        )

    private class FakeBgeModelRunner(
        override val hiddenSize: Int,
    ) : BgeModelRunner {
        override fun run(
            inputIds: LongArray,
            attentionMask: LongArray,
            tokenTypeIds: LongArray,
            shape: LongArray,
        ): Array<FloatArray> =
            inputIds.map { id ->
                when (id) {
                    2L -> floatArrayOf(1f, 0f) // CLS
                    3L -> floatArrayOf(0f, 1f) // SEP
                    5L -> floatArrayOf(2f, 2f) // water
                    else -> floatArrayOf(100f, 100f) // would dominate if padding was included
                }
            }.toTypedArray()

        override fun close() = Unit
    }

    private fun FloatArray.normalizeForTest(): FloatArray {
        val norm = kotlin.math.sqrt(sumOf { (it * it).toDouble() }).toFloat()
        return map { it / norm }.toFloatArray()
    }
}
