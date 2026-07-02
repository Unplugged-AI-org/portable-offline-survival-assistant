package ai.unplugged.posa.data.model

data class Waypoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double?,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class BreadcrumbTrail(
    val id: String,
    val name: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class BreadcrumbPoint(
    val id: String,
    val trailId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double?,
    val recordedAtEpochMillis: Long,
    val sequenceNumber: Int,
)

data class InstalledMap(
    val id: String,
    val displayName: String,
    val fileName: String,
    val filePath: String,
    val byteSize: Long,
    val isEnabled: Boolean,
    val centerLatitude: Double?,
    val centerLongitude: Double?,
    val startZoomLevel: Int?,
    val boundingBoxMinLatitude: Double?,
    val boundingBoxMinLongitude: Double?,
    val boundingBoxMaxLatitude: Double?,
    val boundingBoxMaxLongitude: Double?,
    val importedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class FieldNote(
    val id: String,
    val title: String,
    val body: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val waypointId: String?,
    val checklistId: String?,
    val guideCardId: String?,
    val gearItemId: String?,
)

data class Checklist(
    val id: String,
    val title: String,
    val description: String?,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class ChecklistItem(
    val id: String,
    val checklistId: String,
    val label: String,
    val details: String?,
    val position: Int,
    val isChecked: Boolean,
    val updatedAtEpochMillis: Long,
)

data class GearItem(
    val id: String,
    val name: String,
    val category: String?,
    val quantity: Int,
    val condition: String?,
    val notes: String?,
    val isAvailable: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class Pack(
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
    val installedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val isBundled: Boolean,
)

data class GuideCard(
    val id: String,
    val packId: String,
    val title: String,
    val category: String,
    val summary: String,
    val bodyMarkdown: String,
    val warnings: String?,
    val sortOrder: Int,
    val provenanceId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val workflowTags: List<String> = emptyList(),
)

data class Provenance(
    val id: String,
    val sourceTitle: String,
    val sourceUrl: String?,
    val citation: String?,
    val license: String?,
    val reviewStatus: String,
    val reviewedBy: String?,
    val reviewedAtEpochMillis: Long?,
    val notes: String?,
)
