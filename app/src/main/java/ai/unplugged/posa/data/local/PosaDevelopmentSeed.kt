package ai.unplugged.posa.data.local

import ai.unplugged.posa.data.model.BreadcrumbPoint
import ai.unplugged.posa.data.model.BreadcrumbTrail
import ai.unplugged.posa.data.model.Checklist
import ai.unplugged.posa.data.model.ChecklistItem
import ai.unplugged.posa.data.model.FieldNote
import ai.unplugged.posa.data.model.GearItem
import ai.unplugged.posa.data.model.GuideCard
import ai.unplugged.posa.data.model.Pack
import ai.unplugged.posa.data.model.Provenance
import ai.unplugged.posa.data.model.Waypoint

object PosaDevelopmentSeed {
    const val PACK_ID = "dev-wilderness-basics"
    const val PROVENANCE_ID = "dev-provenance-placeholder"
    const val GUIDE_CARD_ID = "dev-guide-card-navigation"
    const val WAYPOINT_ID = "dev-waypoint-trailhead"
    const val CHECKLIST_ID = "dev-checklist-day-kit"
    const val CHECKLIST_ITEM_ID = "dev-checklist-item-water"
    const val GEAR_ITEM_ID = "dev-gear-headlamp"
    const val NOTE_ID = "dev-note-trailhead"
    const val BREADCRUMB_TRAIL_ID = "dev-breadcrumb-trail"
    const val BREADCRUMB_POINT_ID = "dev-breadcrumb-point-001"

    suspend fun install(database: PosaDatabase, nowEpochMillis: Long = System.currentTimeMillis()) {
        database.provenanceDao().upsert(sampleProvenance().toEntity())
        database.packDao().upsert(samplePack(nowEpochMillis).toEntity())
        database.guideCardDao().upsert(sampleGuideCard(nowEpochMillis).toEntity())
        database.waypointDao().upsert(sampleWaypoint(nowEpochMillis).toEntity())
        database.checklistDao().upsertChecklist(sampleChecklist(nowEpochMillis).toEntity())
        database.checklistDao().upsertItem(sampleChecklistItem(nowEpochMillis).toEntity())
        database.gearDao().upsert(sampleGearItem(nowEpochMillis).toEntity())
        database.fieldNoteDao().upsert(sampleFieldNote(nowEpochMillis).toEntity())
        database.breadcrumbDao().upsertTrail(sampleBreadcrumbTrail(nowEpochMillis).toEntity())
        database.breadcrumbDao().upsertPoint(sampleBreadcrumbPoint(nowEpochMillis).toEntity())
    }

    fun sampleProvenance(): Provenance = Provenance(
        id = PROVENANCE_ID,
        sourceTitle = "Development placeholder",
        sourceUrl = null,
        citation = "Internal POSA sample data for exercising local storage.",
        license = "NOASSERTION",
        reviewStatus = "draft",
        reviewedBy = null,
        reviewedAtEpochMillis = null,
        notes = "Not user-facing guidance. Replace with sourced pack content in Phase 3.",
    )

    fun samplePack(nowEpochMillis: Long): Pack = Pack(
        id = PACK_ID,
        title = "Development Wilderness Basics",
        version = "0.1.0-dev",
        category = "wilderness",
        description = "Small local fixture for Room and repository development.",
        packType = "official",
        license = "NOASSERTION",
        sourceName = "Development placeholder",
        sourceUrl = null,
        author = "Unplugged AI",
        lastReviewedEpochMillis = null,
        reviewStatus = "draft",
        installedAtEpochMillis = nowEpochMillis,
        updatedAtEpochMillis = nowEpochMillis,
        isBundled = true,
    )

    fun sampleGuideCard(nowEpochMillis: Long): GuideCard = GuideCard(
        id = GUIDE_CARD_ID,
        packId = PACK_ID,
        title = "Navigation Placeholder",
        category = "navigation",
        summary = "Local-only placeholder card used to verify pack and guide storage.",
        bodyMarkdown = "This is development fixture content, not field guidance.",
        warnings = "Draft placeholder. Do not present as survival instruction.",
        sortOrder = 10,
        provenanceId = PROVENANCE_ID,
        createdAtEpochMillis = nowEpochMillis,
        updatedAtEpochMillis = nowEpochMillis,
    )

    fun sampleWaypoint(nowEpochMillis: Long): Waypoint = Waypoint(
        id = WAYPOINT_ID,
        name = "Sample Trailhead",
        latitude = 35.60095,
        longitude = -82.55402,
        elevationMeters = 646.0,
        notes = "Development waypoint for local CRUD checks.",
        createdAtEpochMillis = nowEpochMillis,
        updatedAtEpochMillis = nowEpochMillis,
    )

    fun sampleChecklist(nowEpochMillis: Long): Checklist = Checklist(
        id = CHECKLIST_ID,
        title = "Development Day Kit",
        description = "Fixture checklist for local storage only.",
        isArchived = false,
        createdAtEpochMillis = nowEpochMillis,
        updatedAtEpochMillis = nowEpochMillis,
    )

    fun sampleChecklistItem(nowEpochMillis: Long): ChecklistItem = ChecklistItem(
        id = CHECKLIST_ITEM_ID,
        checklistId = CHECKLIST_ID,
        label = "Water bottle",
        details = "Fixture item.",
        position = 0,
        isChecked = false,
        updatedAtEpochMillis = nowEpochMillis,
    )

    fun sampleGearItem(nowEpochMillis: Long): GearItem = GearItem(
        id = GEAR_ITEM_ID,
        name = "Headlamp",
        category = "lighting",
        quantity = 1,
        condition = "working",
        notes = "Development gear fixture.",
        isAvailable = true,
        createdAtEpochMillis = nowEpochMillis,
        updatedAtEpochMillis = nowEpochMillis,
    )

    fun sampleFieldNote(nowEpochMillis: Long): FieldNote = FieldNote(
        id = NOTE_ID,
        title = "Trailhead note",
        body = "Development note linked to sample local objects.",
        createdAtEpochMillis = nowEpochMillis,
        updatedAtEpochMillis = nowEpochMillis,
        latitude = 35.60095,
        longitude = -82.55402,
        waypointId = WAYPOINT_ID,
        checklistId = CHECKLIST_ID,
        guideCardId = GUIDE_CARD_ID,
        gearItemId = GEAR_ITEM_ID,
    )

    fun sampleBreadcrumbTrail(nowEpochMillis: Long): BreadcrumbTrail = BreadcrumbTrail(
        id = BREADCRUMB_TRAIL_ID,
        name = "Development Breadcrumb Trail",
        startedAtEpochMillis = nowEpochMillis,
        endedAtEpochMillis = null,
        createdAtEpochMillis = nowEpochMillis,
        updatedAtEpochMillis = nowEpochMillis,
    )

    fun sampleBreadcrumbPoint(nowEpochMillis: Long): BreadcrumbPoint = BreadcrumbPoint(
        id = BREADCRUMB_POINT_ID,
        trailId = BREADCRUMB_TRAIL_ID,
        latitude = 35.60095,
        longitude = -82.55402,
        accuracyMeters = 6.0,
        recordedAtEpochMillis = nowEpochMillis,
        sequenceNumber = 0,
    )
}
