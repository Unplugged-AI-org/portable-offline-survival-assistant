package ai.unplugged.posa.data.local

import ai.liquid.leap.GenerationOptions
import ai.liquid.leap.ModelLoadingOptions
import ai.liquid.leap.ModelRunner
import ai.liquid.leap.manifest.LeapDownloader
import ai.liquid.leap.manifest.LeapDownloaderConfig
import ai.liquid.leap.manifest.ModelSource
import ai.liquid.leap.message.MessageResponse
import android.content.Context
import android.util.Log
import java.io.Closeable
import java.io.File
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

interface LocalLlmRuntime : Closeable {
    val modelId: String
    fun generate(
        request: LocalLlmRequest,
        onFirstToken: () -> Unit = {},
    ): LocalLlmGeneration
}

data class LocalLlmRequest(
    val prompt: String,
    val maxTokens: Int = 384,
    val temperature: Float = 0.1f,
)

data class LocalLlmGeneration(
    val text: String,
    val tokenCount: Int?,
)

class LocalLlmUnavailableException(message: String) : IllegalStateException(message)

object LocalLlmAssetInstaller {
    const val DEFAULT_MODEL_ID = "LiquidAI/LFM2.5-1.2B-Instruct-GGUF:Q4_K_M"
    const val DEFAULT_ASSET_PATH = "llm/lfm2.5-1.2b-instruct/LFM2.5-1.2B-Instruct-Q4_K_M.gguf"
    const val DEFAULT_FILE_NAME = "LFM2.5-1.2B-Instruct-Q4_K_M.gguf"
    const val DEFAULT_LEAP_MODEL_NAME = "LFM2.5-1.2B-Instruct"
    const val DEFAULT_QUANTIZATION_ID = "Q4_K_M"

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
            "Unable to install bundled local LLM model."
        }
        return destination
    }

    fun installedFile(
        context: Context,
        fileName: String = DEFAULT_FILE_NAME,
    ): File = File(File(context.noBackupFilesDir, "llm"), fileName)

    private fun Context.hasAsset(assetPath: String): Boolean =
        try {
            assets.open(assetPath).use { true }
        } catch (_: Exception) {
            false
        }
}

data class LocalLlmRuntimeTuning(
    val cpuThreads: Int? = DEFAULT_CPU_THREADS,
    val contextSize: Int? = DEFAULT_CONTEXT_SIZE,
) {
    fun toModelLoadingOptions(): ModelLoadingOptions =
        ModelLoadingOptions().apply {
            this@LocalLlmRuntimeTuning.cpuThreads?.let { this.cpuThreads = it }
            this@LocalLlmRuntimeTuning.contextSize?.let { this.contextSize = it }
        }

    companion object {
        private const val DEFAULT_CPU_THREADS = 4
        private const val DEFAULT_CONTEXT_SIZE = 8192
        private const val RUNTIME_PROPERTIES_FILE = "runtime.properties"

        fun read(context: Context): LocalLlmRuntimeTuning {
            val file = File(File(context.noBackupFilesDir, "llm"), RUNTIME_PROPERTIES_FILE)
            if (!file.exists()) {
                Log.i(LOG_TAG, "No local LLM runtime override at ${file.absolutePath}")
                return LocalLlmRuntimeTuning()
            }

            return runCatching {
                val values = file.readLines()
                    .mapNotNull { line ->
                        val trimmed = line.trim()
                        if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                            null
                        } else {
                            val key = trimmed.substringBefore("=").trim()
                            val value = trimmed.substringAfter("=").trim()
                            key to value
                        }
                    }
                    .toMap()
                LocalLlmRuntimeTuning(
                    cpuThreads = values["cpuThreads"]?.toIntOrNull()?.takeIf { it > 0 },
                    contextSize = values["contextSize"]?.toIntOrNull()?.takeIf { it > 0 }
                        ?: DEFAULT_CONTEXT_SIZE,
                ).also {
                    Log.i(
                        LOG_TAG,
                        "Read local LLM runtime override from ${file.absolutePath}: cpuThreads=${it.cpuThreads}, contextSize=${it.contextSize}",
                    )
                }
            }.getOrElse { exception ->
                Log.w(LOG_TAG, "Failed to read local LLM runtime override at ${file.absolutePath}", exception)
                LocalLlmRuntimeTuning()
            }
        }

        private const val LOG_TAG = "LocalLlmRuntime"
    }
}

class BundledLfmLocalLlmRuntime private constructor(
    context: Context,
    private val modelFile: File,
    override val modelId: String,
) : LocalLlmRuntime {
    private val appContext = context.applicationContext
    private val downloader by lazy {
        LeapDownloader(
            config = LeapDownloaderConfig(
                saveDir = File(appContext.noBackupFilesDir, "leap").absolutePath,
            ),
        )
    }
    private var modelRunner: ModelRunner? = null

    private fun getOrLoadModelRunner(): ModelRunner =
        modelRunner ?: runBlocking {
            val tuning = LocalLlmRuntimeTuning.read(appContext)
            val loadingOptions = tuning.toModelLoadingOptions()
            Log.i(
                LOG_TAG,
                "Loading local LLM with cpuThreads=${loadingOptions.cpuThreads}, contextSize=${loadingOptions.contextSize}",
            )
            downloader.loadSimpleModel(
                ModelSource(
                    modelPath = modelFile.absolutePath,
                    modelName = LocalLlmAssetInstaller.DEFAULT_LEAP_MODEL_NAME,
                    quantizationId = LocalLlmAssetInstaller.DEFAULT_QUANTIZATION_ID,
                ),
                loadingOptions,
            )
        }.also { modelRunner = it }

    override fun generate(
        request: LocalLlmRequest,
        onFirstToken: () -> Unit,
    ): LocalLlmGeneration = runBlocking {
        var emittedFirstToken = false
        var tokenCount: Int? = null
        val text = StringBuilder()
        val conversation = getOrLoadModelRunner().createConversation(
            systemPrompt = "You are POSA, an offline source-grounded field assistant.",
        )
        val options = GenerationOptions().apply {
            maxTokens = request.maxTokens
            temperature = request.temperature
            minP = 0.15f
            repetitionPenalty = 1.05f
            enableThinking = false
        }
        conversation.generateResponse(request.prompt, options).collect { response: MessageResponse ->
            when (response) {
                is MessageResponse.Chunk -> {
                    if (!emittedFirstToken && response.text.isNotEmpty()) {
                        emittedFirstToken = true
                        onFirstToken()
                    }
                    text.append(response.text)
                }
                is MessageResponse.Complete -> {
                    tokenCount = response.stats?.completionTokens?.toInt()
                }
                else -> Unit
            }
        }
        LocalLlmGeneration(
            text = text.toString(),
            tokenCount = tokenCount,
        )
    }

    override fun close() {
        modelRunner?.let { runner ->
            runBlocking { runner.unload() }
        }
        modelRunner = null
    }

    companion object {
        private const val LOG_TAG = "LocalLlmRuntime"

        fun openBundled(context: Context): LocalLlmRuntime? {
            val modelFile = LocalLlmAssetInstaller.installIfBundled(context) ?: return null
            return BundledLfmLocalLlmRuntime(
                context = context,
                modelFile = modelFile,
                modelId = LocalLlmAssetInstaller.DEFAULT_MODEL_ID,
            )
        }
    }
}
