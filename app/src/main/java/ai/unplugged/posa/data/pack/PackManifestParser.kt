package ai.unplugged.posa.data.pack

import ai.unplugged.posa.data.model.GuideCard
import ai.unplugged.posa.data.model.Pack
import ai.unplugged.posa.data.model.Provenance
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset

data class PackManifest(
    val id: String,
    val title: String,
    val version: String,
    val category: String?,
    val description: String?,
    val packType: String,
    val license: String,
    val sourceName: String?,
    val sourceUrl: String?,
    val author: String?,
    val lastReviewedEpochMillis: Long?,
    val reviewStatus: String,
    val files: List<String>,
)

data class ParsedGuideCard(
    val card: GuideCard,
    val provenance: Provenance,
)

object PackManifestParser {
    fun parseManifest(json: String): PackManifest {
        val source = JSONObject(json)
        val files = source.getJSONArray("files")

        return PackManifest(
            id = source.requiredString("id"),
            title = source.requiredString("title"),
            version = source.requiredString("version"),
            category = source.optionalString("category"),
            description = source.optionalString("description"),
            packType = source.requiredString("pack_type"),
            license = source.requiredString("license"),
            sourceName = source.optionalString("source_name"),
            sourceUrl = source.optionalString("source_url"),
            author = source.optionalString("author"),
            lastReviewedEpochMillis = parseIsoDateToEpochMillis(source.optionalString("last_reviewed")),
            reviewStatus = source.requiredString("review_status"),
            files = List(files.length()) { index -> files.getString(index) },
        )
    }

    fun toPack(
        manifest: PackManifest,
        installedAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ): Pack = Pack(
        id = manifest.id,
        title = manifest.title,
        version = manifest.version,
        category = manifest.category,
        description = manifest.description,
        packType = manifest.packType,
        license = manifest.license,
        sourceName = manifest.sourceName,
        sourceUrl = manifest.sourceUrl,
        author = manifest.author,
        lastReviewedEpochMillis = manifest.lastReviewedEpochMillis,
        reviewStatus = manifest.reviewStatus,
        installedAtEpochMillis = installedAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        isBundled = true,
    )

    fun parseGuideCard(
        manifest: PackManifest,
        relativePath: String,
        markdown: String,
        nowEpochMillis: Long,
    ): ParsedGuideCard {
        val (metadata, bodyMarkdown) = parseFrontMatter(markdown, relativePath)
        val localCardId = metadata.requiredValue("id", relativePath)
        val cardId = "${manifest.id}:$localCardId"
        val provenanceId = "$cardId:provenance"
        val reviewStatus = metadata.optionalValue("review_status") ?: manifest.reviewStatus

        return ParsedGuideCard(
            card = GuideCard(
                id = cardId,
                packId = manifest.id,
                title = metadata.requiredValue("title", relativePath),
                category = metadata.requiredValue("category", relativePath),
                summary = metadata.requiredValue("summary", relativePath),
                bodyMarkdown = bodyMarkdown,
                warnings = metadata.optionalValue("warnings"),
                sortOrder = metadata.optionalValue("sort_order")?.toIntOrNull() ?: 0,
                provenanceId = provenanceId,
                createdAtEpochMillis = nowEpochMillis,
                updatedAtEpochMillis = nowEpochMillis,
                workflowTags = metadata.optionalValue("workflow_tags").toWorkflowTags(),
            ),
            provenance = Provenance(
                id = provenanceId,
                sourceTitle = metadata.requiredValue("source_title", relativePath),
                sourceUrl = metadata.optionalValue("source_url"),
                citation = metadata.optionalValue("citation"),
                license = metadata.optionalValue("license") ?: manifest.license,
                reviewStatus = reviewStatus,
                reviewedBy = metadata.optionalValue("reviewed_by"),
                reviewedAtEpochMillis = parseIsoDateToEpochMillis(metadata.optionalValue("reviewed_at")),
                notes = metadata.optionalValue("notes"),
            ),
        )
    }

    private fun parseFrontMatter(markdown: String, relativePath: String): Pair<Map<String, String>, String> {
        val lines = markdown.replace("\r\n", "\n").lines()
        require(lines.firstOrNull()?.trim() == "---") {
            "Guide card $relativePath must start with front matter."
        }

        val endIndex = lines.drop(1).indexOfFirst { it.trim() == "---" }
        require(endIndex >= 0) {
            "Guide card $relativePath is missing the closing front matter delimiter."
        }

        val metadataEnd = endIndex + 1
        val metadata = lines.subList(1, metadataEnd)
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            .associate { line ->
                val separatorIndex = line.indexOf(':')
                require(separatorIndex > 0) {
                    "Guide card $relativePath has invalid metadata line: $line"
                }
                val key = line.substring(0, separatorIndex).trim()
                val value = line.substring(separatorIndex + 1).trim()
                key to value
            }

        val body = lines.drop(metadataEnd + 1).joinToString("\n").trim()
        require(body.isNotBlank()) {
            "Guide card $relativePath must include body content."
        }

        return metadata to body
    }

    private fun JSONObject.requiredString(name: String): String =
        getString(name).takeIf { it.isNotBlank() }
            ?: error("Manifest field $name must not be blank.")

    private fun JSONObject.optionalString(name: String): String? =
        optString(name).takeIf { it.isNotBlank() }

    private fun Map<String, String>.requiredValue(key: String, relativePath: String): String =
        optionalValue(key) ?: error("Guide card $relativePath is missing required metadata: $key")

    private fun Map<String, String>.optionalValue(key: String): String? =
        this[key]?.takeIf { it.isNotBlank() }

    private fun parseIsoDateToEpochMillis(value: String?): Long? =
        value?.let {
            LocalDate.parse(it)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }

    private fun String?.toWorkflowTags(): List<String> =
        orEmpty()
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
}
