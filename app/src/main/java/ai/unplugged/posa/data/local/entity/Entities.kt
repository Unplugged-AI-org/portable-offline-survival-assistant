package ai.unplugged.posa.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waypoints",
    indices = [
        Index(value = ["name"]),
        Index(value = ["created_at_epoch_millis"]),
    ],
)
data class WaypointEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "elevation_meters") val elevationMeters: Double?,
    val notes: String?,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "breadcrumb_trails",
    indices = [
        Index(value = ["started_at_epoch_millis"]),
    ],
)
data class BreadcrumbTrailEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "started_at_epoch_millis") val startedAtEpochMillis: Long,
    @ColumnInfo(name = "ended_at_epoch_millis") val endedAtEpochMillis: Long?,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "breadcrumb_points",
    foreignKeys = [
        ForeignKey(
            entity = BreadcrumbTrailEntity::class,
            parentColumns = ["id"],
            childColumns = ["trail_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["trail_id"]),
        Index(value = ["trail_id", "sequence_number"], unique = true),
        Index(value = ["recorded_at_epoch_millis"]),
    ],
)
data class BreadcrumbPointEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "trail_id") val trailId: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "accuracy_meters") val accuracyMeters: Double?,
    @ColumnInfo(name = "recorded_at_epoch_millis") val recordedAtEpochMillis: Long,
    @ColumnInfo(name = "sequence_number") val sequenceNumber: Int,
)

@Entity(
    tableName = "installed_maps",
    indices = [
        Index(value = ["display_name"]),
        Index(value = ["is_enabled"]),
        Index(value = ["imported_at_epoch_millis"]),
    ],
)
data class InstalledMapEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "byte_size") val byteSize: Long,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
    @ColumnInfo(name = "center_latitude") val centerLatitude: Double?,
    @ColumnInfo(name = "center_longitude") val centerLongitude: Double?,
    @ColumnInfo(name = "start_zoom_level") val startZoomLevel: Int?,
    @ColumnInfo(name = "bounding_box_min_latitude") val boundingBoxMinLatitude: Double?,
    @ColumnInfo(name = "bounding_box_min_longitude") val boundingBoxMinLongitude: Double?,
    @ColumnInfo(name = "bounding_box_max_latitude") val boundingBoxMaxLatitude: Double?,
    @ColumnInfo(name = "bounding_box_max_longitude") val boundingBoxMaxLongitude: Double?,
    @ColumnInfo(name = "imported_at_epoch_millis") val importedAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "checklists",
    indices = [
        Index(value = ["title"]),
        Index(value = ["is_archived"]),
    ],
)
data class ChecklistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["checklist_id"]),
        Index(value = ["checklist_id", "position"]),
    ],
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "checklist_id") val checklistId: String,
    val label: String,
    val details: String?,
    val position: Int,
    @ColumnInfo(name = "is_checked") val isChecked: Boolean,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "gear_items",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"]),
        Index(value = ["is_available"]),
    ],
)
data class GearItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,
    val quantity: Int,
    val condition: String?,
    val notes: String?,
    @ColumnInfo(name = "is_available") val isAvailable: Boolean,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "packs",
    indices = [
        Index(value = ["title"]),
        Index(value = ["pack_type"]),
        Index(value = ["review_status"]),
    ],
)
data class PackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val version: String,
    val category: String?,
    val description: String?,
    @ColumnInfo(name = "pack_type") val packType: String,
    val license: String,
    @ColumnInfo(name = "source_name") val sourceName: String?,
    @ColumnInfo(name = "source_url") val sourceUrl: String?,
    val author: String?,
    @ColumnInfo(name = "last_reviewed_epoch_millis") val lastReviewedEpochMillis: Long?,
    @ColumnInfo(name = "review_status") val reviewStatus: String,
    @ColumnInfo(name = "installed_at_epoch_millis") val installedAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "is_bundled") val isBundled: Boolean,
)

@Entity(tableName = "provenance")
data class ProvenanceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "source_title") val sourceTitle: String,
    @ColumnInfo(name = "source_url") val sourceUrl: String?,
    val citation: String?,
    val license: String?,
    @ColumnInfo(name = "review_status") val reviewStatus: String,
    @ColumnInfo(name = "reviewed_by") val reviewedBy: String?,
    @ColumnInfo(name = "reviewed_at_epoch_millis") val reviewedAtEpochMillis: Long?,
    val notes: String?,
)

@Entity(
    tableName = "guide_cards",
    foreignKeys = [
        ForeignKey(
            entity = PackEntity::class,
            parentColumns = ["id"],
            childColumns = ["pack_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProvenanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["provenance_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["pack_id"]),
        Index(value = ["category"]),
        Index(value = ["provenance_id"]),
        Index(value = ["pack_id", "sort_order"]),
    ],
)
data class GuideCardEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "pack_id") val packId: String,
    val title: String,
    val category: String,
    val summary: String,
    @ColumnInfo(name = "body_markdown") val bodyMarkdown: String,
    val warnings: String?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "provenance_id") val provenanceId: String?,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "field_notes",
    foreignKeys = [
        ForeignKey(
            entity = WaypointEntity::class,
            parentColumns = ["id"],
            childColumns = ["waypoint_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklist_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = GuideCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["guide_card_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = GearItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["gear_item_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["created_at_epoch_millis"]),
        Index(value = ["waypoint_id"]),
        Index(value = ["checklist_id"]),
        Index(value = ["guide_card_id"]),
        Index(value = ["gear_item_id"]),
    ],
)
data class FieldNoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "waypoint_id") val waypointId: String?,
    @ColumnInfo(name = "checklist_id") val checklistId: String?,
    @ColumnInfo(name = "guide_card_id") val guideCardId: String?,
    @ColumnInfo(name = "gear_item_id") val gearItemId: String?,
)
