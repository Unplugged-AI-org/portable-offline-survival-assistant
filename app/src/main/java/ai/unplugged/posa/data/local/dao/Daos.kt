package ai.unplugged.posa.data.local.dao

import ai.unplugged.posa.data.local.entity.BreadcrumbPointEntity
import ai.unplugged.posa.data.local.entity.BreadcrumbTrailEntity
import ai.unplugged.posa.data.local.entity.ChecklistEntity
import ai.unplugged.posa.data.local.entity.ChecklistItemEntity
import ai.unplugged.posa.data.local.entity.FieldNoteEntity
import ai.unplugged.posa.data.local.entity.GearItemEntity
import ai.unplugged.posa.data.local.entity.GuideCardEntity
import ai.unplugged.posa.data.local.entity.PackEntity
import ai.unplugged.posa.data.local.entity.ProvenanceEntity
import ai.unplugged.posa.data.local.entity.WaypointEntity
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface WaypointDao {
    @Upsert
    suspend fun upsert(waypoint: WaypointEntity)

    @Query("SELECT * FROM waypoints WHERE id = :id")
    suspend fun get(id: String): WaypointEntity?

    @Query("SELECT * FROM waypoints ORDER BY name COLLATE NOCASE")
    suspend fun list(): List<WaypointEntity>

    @Query("DELETE FROM waypoints WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface BreadcrumbDao {
    @Upsert
    suspend fun upsertTrail(trail: BreadcrumbTrailEntity)

    @Query("SELECT * FROM breadcrumb_trails WHERE id = :id")
    suspend fun getTrail(id: String): BreadcrumbTrailEntity?

    @Query("SELECT * FROM breadcrumb_trails ORDER BY started_at_epoch_millis DESC")
    suspend fun listTrails(): List<BreadcrumbTrailEntity>

    @Query("DELETE FROM breadcrumb_trails WHERE id = :id")
    suspend fun deleteTrail(id: String)

    @Upsert
    suspend fun upsertPoint(point: BreadcrumbPointEntity)

    @Query("SELECT * FROM breadcrumb_points WHERE id = :id")
    suspend fun getPoint(id: String): BreadcrumbPointEntity?

    @Query(
        """
        SELECT * FROM breadcrumb_points
        WHERE trail_id = :trailId
        ORDER BY sequence_number ASC
        """,
    )
    suspend fun listPointsForTrail(trailId: String): List<BreadcrumbPointEntity>

    @Query("DELETE FROM breadcrumb_points WHERE id = :id")
    suspend fun deletePoint(id: String)
}

@Dao
interface FieldNoteDao {
    @Upsert
    suspend fun upsert(note: FieldNoteEntity)

    @Query("SELECT * FROM field_notes WHERE id = :id")
    suspend fun get(id: String): FieldNoteEntity?

    @Query("SELECT * FROM field_notes ORDER BY updated_at_epoch_millis DESC")
    suspend fun list(): List<FieldNoteEntity>

    @Query("DELETE FROM field_notes WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ChecklistDao {
    @Upsert
    suspend fun upsertChecklist(checklist: ChecklistEntity)

    @Query("SELECT * FROM checklists WHERE id = :id")
    suspend fun getChecklist(id: String): ChecklistEntity?

    @Query("SELECT * FROM checklists ORDER BY is_archived ASC, title COLLATE NOCASE")
    suspend fun listChecklists(): List<ChecklistEntity>

    @Query("DELETE FROM checklists WHERE id = :id")
    suspend fun deleteChecklist(id: String)

    @Upsert
    suspend fun upsertItem(item: ChecklistItemEntity)

    @Query("SELECT * FROM checklist_items WHERE id = :id")
    suspend fun getItem(id: String): ChecklistItemEntity?

    @Query(
        """
        SELECT * FROM checklist_items
        WHERE checklist_id = :checklistId
        ORDER BY position ASC, label COLLATE NOCASE
        """,
    )
    suspend fun listItemsForChecklist(checklistId: String): List<ChecklistItemEntity>

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteItem(id: String)
}

@Dao
interface GearDao {
    @Upsert
    suspend fun upsert(item: GearItemEntity)

    @Query("SELECT * FROM gear_items WHERE id = :id")
    suspend fun get(id: String): GearItemEntity?

    @Query("SELECT * FROM gear_items ORDER BY category COLLATE NOCASE, name COLLATE NOCASE")
    suspend fun list(): List<GearItemEntity>

    @Query("DELETE FROM gear_items WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PackDao {
    @Upsert
    suspend fun upsert(pack: PackEntity)

    @Query("SELECT * FROM packs WHERE id = :id")
    suspend fun get(id: String): PackEntity?

    @Query("SELECT * FROM packs ORDER BY title COLLATE NOCASE, version COLLATE NOCASE")
    suspend fun list(): List<PackEntity>

    @Query("DELETE FROM packs WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface GuideCardDao {
    @Upsert
    suspend fun upsert(card: GuideCardEntity)

    @Query("SELECT * FROM guide_cards WHERE id = :id")
    suspend fun get(id: String): GuideCardEntity?

    @Query("SELECT * FROM guide_cards ORDER BY category COLLATE NOCASE, sort_order ASC, title COLLATE NOCASE")
    suspend fun list(): List<GuideCardEntity>

    @Query(
        """
        SELECT * FROM guide_cards
        WHERE title LIKE :likeQuery ESCAPE '\'
            OR category LIKE :likeQuery ESCAPE '\'
            OR summary LIKE :likeQuery ESCAPE '\'
            OR body_markdown LIKE :likeQuery ESCAPE '\'
        ORDER BY category COLLATE NOCASE, sort_order ASC, title COLLATE NOCASE
        """,
    )
    suspend fun search(likeQuery: String): List<GuideCardEntity>

    @Query(
        """
        SELECT * FROM guide_cards
        WHERE pack_id = :packId
        ORDER BY sort_order ASC, title COLLATE NOCASE
        """,
    )
    suspend fun listForPack(packId: String): List<GuideCardEntity>

    @Query("DELETE FROM guide_cards WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ProvenanceDao {
    @Upsert
    suspend fun upsert(provenance: ProvenanceEntity)

    @Query("SELECT * FROM provenance WHERE id = :id")
    suspend fun get(id: String): ProvenanceEntity?

    @Query("SELECT * FROM provenance ORDER BY source_title COLLATE NOCASE")
    suspend fun list(): List<ProvenanceEntity>

    @Query("DELETE FROM provenance WHERE id = :id")
    suspend fun delete(id: String)
}
