package ai.unplugged.posa.data.local

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Debug
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import android.util.Log
import java.io.File
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class LocalLfmAnswerEvalInstrumentedTest {
    @Test
    fun runTwentyFiveQuestionAnswerEval() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val limit = InstrumentationRegistry.getArguments()
            .getString("limit")
            ?.toIntOrNull()
            ?.coerceIn(1, QUESTIONS.size)
            ?: QUESTIONS.size
        val questions = QUESTIONS.take(limit)
        writeRuntimeTuning(context)
        val output = File(context.filesDir, "local_lfm_answer_eval_${limit}.jsonl")
        val rows = mutableListOf<JSONObject>()

        RagAnswerService.openBundled(context).use { service ->
            requireNotNull(service) { "Bundled RAG answer service is unavailable." }
            questions.forEachIndexed { index, item ->
                val diagnosticsBefore = DeviceDiagnostics.capture(context)
                val started = System.currentTimeMillis()
                val result = service.answer(item.question)
                val elapsed = System.currentTimeMillis() - started
                val diagnosticsAfter = DeviceDiagnostics.capture(context)
                val answer = result.answer.orEmpty()
                val row = score(item, answer)
                    .put("index", index + 1)
                    .put("answer", answer)
                    .put("elapsedMillis", elapsed)
                    .put("timings", result.timings.toJson())
                    .put("diagnosticsBefore", diagnosticsBefore.toJson())
                    .put("diagnosticsAfter", diagnosticsAfter.toJson())
                    .put(
                        "promptEvidence",
                        JSONArray(
                            result.promptEvidenceDebug.map { evidence ->
                                JSONObject()
                                    .put("id", evidence.id)
                                    .put("title", evidence.title)
                                    .put("sectionTitle", evidence.sectionTitle)
                                    .put("snippet", evidence.snippet)
                            },
                        ),
                    )
                    .put(
                        "sources",
                        JSONArray(
                            result.evidence
                                .take(5)
                                .map { evidence ->
                                    JSONObject()
                                        .put("id", evidence.id)
                                        .put("title", evidence.title)
                                        .put("sectionTitle", evidence.sectionTitle)
                                        .put("category", evidence.category)
                                },
                        ),
                    )
                rows += row
                output.appendText(row.toString() + "\n")
                Log.i(
                    LOG_TAG,
                    "row=${index + 1}/${questions.size} id=${item.id} passed=${row.getBoolean("passed")} " +
                        "missing=${row.getJSONArray("missingTerms")} " +
                        "promptChars=${result.timings.promptChars} evidenceChars=${result.timings.evidenceChars} " +
                        "thermal=${diagnosticsBefore.thermalStatusLabel}->${diagnosticsAfter.thermalStatusLabel} " +
                        "batteryC=${diagnosticsBefore.batteryTempCelsius}->${diagnosticsAfter.batteryTempCelsius} " +
                        "pssKb=${diagnosticsBefore.totalPssKb}->${diagnosticsAfter.totalPssKb} " +
                        "ttft=${result.timings.ttftMillis} total=${result.timings.totalMillis} " +
                        "promptEvidence=${result.promptEvidenceDebug.joinToString { "${it.id}:${it.sectionTitle ?: it.title}" }} " +
                        "answer=${answer.toSingleLine().take(260)}",
                )
            }
        }

        val passed = rows.count { it.getBoolean("passed") }
        val report = File(context.filesDir, "local_lfm_answer_eval_${limit}_report.txt")
        report.writeText(
            buildString {
                appendLine("Local LFM answer eval")
                appendLine("Questions: ${rows.size}")
                appendLine("Passed: $passed/${rows.size}")
                appendLine("Output: ${output.absolutePath}")
                appendLine()
                rows.filterNot { it.getBoolean("passed") }.forEach { row ->
                    appendLine("${row.getString("id")}:")
                    appendLine("  missing: ${row.getJSONArray("missingTerms")}")
                    appendLine("  forbidden: ${row.getJSONArray("forbiddenFound")}")
                    appendLine("  citationOk: ${row.getBoolean("citationOk")}")
                    appendLine("  answer: ${row.getString("answer").take(500)}")
                }
            },
        )
        Log.i(LOG_TAG, "summary passed=$passed/${rows.size} output=${output.absolutePath} report=${report.absolutePath}")
    }

    private fun writeRuntimeTuning(context: android.content.Context) {
        val llmDir = File(context.noBackupFilesDir, "llm")
        llmDir.mkdirs()
        File(llmDir, "runtime.properties").writeText("cpuThreads=4\ncontextSize=8192\n")
    }

    private fun score(
        item: EvalQuestion,
        answer: String,
    ): JSONObject {
        val lowerAnswer = answer.normalizeForMatch()
        val missingTerms = item.expectedAnswerTerms.filterNot { term -> lowerAnswer.contains(term.normalizeForMatch()) }
        val forbiddenFound = item.forbiddenAnswerTerms.filter { term -> lowerAnswer.contains(term.normalizeForMatch()) }
        val citationOk = Regex("\\[S\\d+]").containsMatchIn(answer)
        val passed = missingTerms.isEmpty() && forbiddenFound.isEmpty() && citationOk
        return JSONObject()
            .put("id", item.id)
            .put("category", item.category)
            .put("type", item.type)
            .put("question", item.question)
            .put("passed", passed)
            .put("citationOk", citationOk)
            .put("missingTerms", JSONArray(missingTerms))
            .put("forbiddenFound", JSONArray(forbiddenFound))
            .put("expectedTerms", JSONArray(item.expectedAnswerTerms))
    }

    private fun RagAnswerTimings.toJson(): JSONObject =
        JSONObject()
            .put("retrievalMillis", retrievalMillis)
            .put("queryEmbeddingMillis", queryEmbeddingMillis)
            .put("ftsMillis", ftsMillis)
            .put("vectorMillis", vectorMillis)
            .put("hybridMergeMillis", hybridMergeMillis)
            .put("evidenceMillis", evidenceMillis)
            .put("promptPackingMillis", promptPackingMillis)
            .put("promptChars", promptChars)
            .put("evidenceChars", evidenceChars)
            .put("evidenceSources", evidenceSources)
            .put("ttftMillis", ttftMillis)
            .put("firstGenerationMillis", firstGenerationMillis)
            .put("verifierMillis", verifierMillis)
            .put("repairMillis", repairMillis)
            .put("generationMillis", generationMillis)
            .put("totalMillis", totalMillis)

    private data class DeviceDiagnostics(
        val thermalStatus: Int?,
        val thermalStatusLabel: String,
        val batteryTempCelsius: Float?,
        val batteryLevelPercent: Int?,
        val totalPssKb: Int,
    ) {
        fun toJson(): JSONObject =
            JSONObject()
                .put("thermalStatus", thermalStatus)
                .put("thermalStatusLabel", thermalStatusLabel)
                .put("batteryTempCelsius", batteryTempCelsius)
                .put("batteryLevelPercent", batteryLevelPercent)
                .put("totalPssKb", totalPssKb)

        companion object {
            fun capture(context: Context): DeviceDiagnostics {
                val powerManager = context.getSystemService(PowerManager::class.java)
                val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val memoryInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memoryInfo)
                return DeviceDiagnostics(
                    thermalStatus = powerManager?.currentThermalStatus,
                    thermalStatusLabel = powerManager?.currentThermalStatus.toThermalStatusLabel(),
                    batteryTempCelsius = batteryStatus?.batteryTempCelsius(),
                    batteryLevelPercent = batteryStatus?.batteryLevelPercent(),
                    totalPssKb = memoryInfo.totalPss,
                )
            }

            private fun Intent.batteryTempCelsius(): Float? {
                val raw = getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                return raw.takeIf { it != Int.MIN_VALUE }?.let { it / 10f }
            }

            private fun Intent.batteryLevelPercent(): Int? {
                val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level < 0 || scale <= 0) return null
                return ((level * 100f) / scale).toInt()
            }

            private fun Int?.toThermalStatusLabel(): String =
                when (this) {
                    PowerManager.THERMAL_STATUS_NONE -> "none"
                    PowerManager.THERMAL_STATUS_LIGHT -> "light"
                    PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
                    PowerManager.THERMAL_STATUS_SEVERE -> "severe"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
                    else -> "unknown"
                }
        }
    }

    private fun String.normalizeForMatch(): String =
        lowercase(Locale.US)
            .replace("-", " ")
            .replace("\u2010", " ")
            .replace("\u2011", " ")
            .replace("\u2012", " ")
            .replace("\u2013", " ")
            .replace("\u2014", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun String.toSingleLine(): String =
        replace(Regex("\\s+"), " ").trim()

    private data class EvalQuestion(
        val id: String,
        val category: String,
        val type: String,
        val question: String,
        val expectedAnswerTerms: List<String>,
        val forbiddenAnswerTerms: List<String> = emptyList(),
    )

    private companion object {
        private const val LOG_TAG = "LocalLfmEval"

        private val QUESTIONS = listOf(
            EvalQuestion(
                "water_boil_altitude_bleach",
                "water",
                "multi-hop",
                "If I do not have bottled water, how do I make emergency water safe, and does altitude change the boil time?",
                listOf("1 minute", "6,500 feet", "3 minutes", "bleach", "30 minutes"),
                forbiddenAnswerTerms = listOf(
                    "1 minute at elevations above 6,500 feet",
                    "1 minute above 6,500 feet",
                    "3 minutes otherwise",
                    "boil instead",
                ),
            ),
            EvalQuestion("water_chemical_contamination", "water", "single-hop", "Can I boil or bleach water if I think fuel, pesticides, or other chemicals got into it?", listOf("fuel", "chemicals", "cannot make")),
            EvalQuestion("water_storage_amount", "water", "single-hop", "How much water should I store for each person for an emergency supply?", listOf("1 gallon", "per person", "per day")),
            EvalQuestion("water_storage_replace", "water", "single-hop", "If I fill my own containers for stored water, how often should I replace that water?", listOf("replace", "6 months")),
            EvalQuestion("water_find_inside_home", "water", "single-hop", "Where can I find clean water inside my home if bottled or treated water is not available?", listOf("water heater", "melted ice", "toilet tank")),
            EvalQuestion("well_disinfect_after_flood", "water", "procedure", "After a disaster, what are the basic steps for disinfecting a private well with chlorine?", listOf("chlorine", "open all faucets", "odor of chlorine")),
            EvalQuestion("rainwater_safe_use", "water", "single-hop", "Is rainwater safe to drink during an emergency, and what should I know before using it?", listOf("rainwater", "germs", "chemicals")),
            EvalQuestion("food_fridge_power_outage", "food", "single-hop", "During a power outage, when should refrigerated food be thrown out?", listOf("40", "2 hours")),
            EvalQuestion("food_freezer_power_outage", "food", "single-hop", "How long can food stay safe in a full freezer during a power outage if the door stays closed?", listOf("full freezer", "48 hours")),
            EvalQuestion("food_flood_contact", "food", "single-hop", "What should I do with food that may have touched flood water?", listOf("flood water", "discard")),
            EvalQuestion("food_emergency_supply", "food", "single-hop", "What kinds of food should I keep in an emergency supply kit?", listOf("nonperishable", "no refrigeration")),
            EvalQuestion("generator_distance", "power", "single-hop", "How far away from windows should I run a portable generator during a power outage?", listOf("20 feet", "windows")),
            EvalQuestion("generator_indoor_warning", "power", "single-hop", "Can I use a generator, grill, or camp stove inside if the power is out?", listOf("never", "inside", "carbon monoxide")),
            EvalQuestion("power_heat_home_safely", "power", "multi-hop", "What are safe ways to keep warm during a winter power outage without causing carbon monoxide poisoning?", listOf("wood stove", "fireplace", "space heater", "gas range")),
            EvalQuestion("power_surge_disconnect", "power", "single-hop", "What should I do with appliances and electronics before or during a power outage to protect them from surges?", listOf("disconnect", "surge")),
            EvalQuestion("alerts_weather_radio_features", "alerts", "single-hop", "Why is a NOAA Weather Radio useful and what features should I look for?", listOf("tone alarm", "battery backup", "SAME")),
            EvalQuestion("alerts_watch_warning_difference", "alerts", "single-hop", "What is the difference between a weather watch and a warning?", listOf("watch", "warning")),
            EvalQuestion("kit_basic_supply_items", "all-hazards", "checklist", "What basic items belong in an emergency supply kit?", listOf("water", "radio", "flashlight", "first aid")),
            EvalQuestion("kit_documents_cash_pets", "all-hazards", "checklist", "What extra kit items should I consider for documents, cash, infants, and pets?", listOf("documents", "cash", "pet food")),
            EvalQuestion("plan_family_communications", "all-hazards", "single-hop", "How should my family plan communication and meeting places before an emergency?", listOf("communication plan", "meeting")),
            EvalQuestion("campfire_extinguish", "campfire-wildfire", "procedure", "How do I fully put out a campfire before leaving camp?", listOf("drown", "stir", "cold")),
            EvalQuestion("campfire_before_lighting", "campfire-wildfire", "checklist", "What should I check before starting a campfire at a campsite?", listOf("fire restrictions", "fire ring")),
            EvalQuestion("wildfire_home_defensible_space", "campfire-wildfire", "single-hop", "What should I clear around my home to reduce wildfire risk?", listOf("30 feet", "vegetation")),
            EvalQuestion("wildfire_evacuation_go_bag", "campfire-wildfire", "single-hop", "What should I do if officials tell me to evacuate during a wildfire?", listOf("evacuation", "leave")),
            EvalQuestion("lightning_camping_shelter", "outdoor-weather", "multi-hop", "If I hear thunder while camping or boating, where should I go?", listOf("thunder", "sturdy", "vehicle")),
        )
    }
}
