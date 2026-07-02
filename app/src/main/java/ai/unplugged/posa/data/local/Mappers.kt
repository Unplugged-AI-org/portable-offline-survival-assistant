package ai.unplugged.posa.data.local

import ai.unplugged.posa.data.local.entity.BreadcrumbPointEntity
import ai.unplugged.posa.data.local.entity.BreadcrumbTrailEntity
import ai.unplugged.posa.data.local.entity.ChecklistEntity
import ai.unplugged.posa.data.local.entity.ChecklistItemEntity
import ai.unplugged.posa.data.local.entity.FieldNoteEntity
import ai.unplugged.posa.data.local.entity.GearItemEntity
import ai.unplugged.posa.data.local.entity.GuideCardEntity
import ai.unplugged.posa.data.local.entity.InstalledMapEntity
import ai.unplugged.posa.data.local.entity.PackEntity
import ai.unplugged.posa.data.local.entity.ProvenanceEntity
import ai.unplugged.posa.data.local.entity.WaypointEntity
import ai.unplugged.posa.data.model.BreadcrumbPoint
import ai.unplugged.posa.data.model.BreadcrumbTrail
import ai.unplugged.posa.data.model.Checklist
import ai.unplugged.posa.data.model.ChecklistItem
import ai.unplugged.posa.data.model.FieldNote
import ai.unplugged.posa.data.model.GearItem
import ai.unplugged.posa.data.model.GuideCard
import ai.unplugged.posa.data.model.InstalledMap
import ai.unplugged.posa.data.model.Pack
import ai.unplugged.posa.data.model.Provenance
import ai.unplugged.posa.data.model.Waypoint

internal fun WaypointEntity.toModel(): Waypoint = Waypoint(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    elevationMeters = elevationMeters,
    notes = notes,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun Waypoint.toEntity(): WaypointEntity = WaypointEntity(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    elevationMeters = elevationMeters,
    notes = notes,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun BreadcrumbTrailEntity.toModel(): BreadcrumbTrail = BreadcrumbTrail(
    id = id,
    name = name,
    startedAtEpochMillis = startedAtEpochMillis,
    endedAtEpochMillis = endedAtEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun BreadcrumbTrail.toEntity(): BreadcrumbTrailEntity = BreadcrumbTrailEntity(
    id = id,
    name = name,
    startedAtEpochMillis = startedAtEpochMillis,
    endedAtEpochMillis = endedAtEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun BreadcrumbPointEntity.toModel(): BreadcrumbPoint = BreadcrumbPoint(
    id = id,
    trailId = trailId,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    recordedAtEpochMillis = recordedAtEpochMillis,
    sequenceNumber = sequenceNumber,
)

internal fun BreadcrumbPoint.toEntity(): BreadcrumbPointEntity = BreadcrumbPointEntity(
    id = id,
    trailId = trailId,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    recordedAtEpochMillis = recordedAtEpochMillis,
    sequenceNumber = sequenceNumber,
)

internal fun InstalledMapEntity.toModel(): InstalledMap = InstalledMap(
    id = id,
    displayName = displayName,
    fileName = fileName,
    filePath = filePath,
    byteSize = byteSize,
    isEnabled = isEnabled,
    centerLatitude = centerLatitude,
    centerLongitude = centerLongitude,
    startZoomLevel = startZoomLevel,
    boundingBoxMinLatitude = boundingBoxMinLatitude,
    boundingBoxMinLongitude = boundingBoxMinLongitude,
    boundingBoxMaxLatitude = boundingBoxMaxLatitude,
    boundingBoxMaxLongitude = boundingBoxMaxLongitude,
    importedAtEpochMillis = importedAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun InstalledMap.toEntity(): InstalledMapEntity = InstalledMapEntity(
    id = id,
    displayName = displayName,
    fileName = fileName,
    filePath = filePath,
    byteSize = byteSize,
    isEnabled = isEnabled,
    centerLatitude = centerLatitude,
    centerLongitude = centerLongitude,
    startZoomLevel = startZoomLevel,
    boundingBoxMinLatitude = boundingBoxMinLatitude,
    boundingBoxMinLongitude = boundingBoxMinLongitude,
    boundingBoxMaxLatitude = boundingBoxMaxLatitude,
    boundingBoxMaxLongitude = boundingBoxMaxLongitude,
    importedAtEpochMillis = importedAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun FieldNoteEntity.toModel(): FieldNote = FieldNote(
    id = id,
    title = title,
    body = body,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    latitude = latitude,
    longitude = longitude,
    waypointId = waypointId,
    checklistId = checklistId,
    guideCardId = guideCardId,
    gearItemId = gearItemId,
)

internal fun FieldNote.toEntity(): FieldNoteEntity = FieldNoteEntity(
    id = id,
    title = title,
    body = body,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    latitude = latitude,
    longitude = longitude,
    waypointId = waypointId,
    checklistId = checklistId,
    guideCardId = guideCardId,
    gearItemId = gearItemId,
)

internal fun ChecklistEntity.toModel(): Checklist = Checklist(
    id = id,
    title = title,
    description = description,
    isArchived = isArchived,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun Checklist.toEntity(): ChecklistEntity = ChecklistEntity(
    id = id,
    title = title,
    description = description,
    isArchived = isArchived,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun ChecklistItemEntity.toModel(): ChecklistItem = ChecklistItem(
    id = id,
    checklistId = checklistId,
    label = label,
    details = details,
    position = position,
    isChecked = isChecked,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun ChecklistItem.toEntity(): ChecklistItemEntity = ChecklistItemEntity(
    id = id,
    checklistId = checklistId,
    label = label,
    details = details,
    position = position,
    isChecked = isChecked,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun GearItemEntity.toModel(): GearItem = GearItem(
    id = id,
    name = name,
    category = category,
    quantity = quantity,
    condition = condition,
    notes = notes,
    isAvailable = isAvailable,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun GearItem.toEntity(): GearItemEntity = GearItemEntity(
    id = id,
    name = name,
    category = category,
    quantity = quantity,
    condition = condition,
    notes = notes,
    isAvailable = isAvailable,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun PackEntity.toModel(): Pack = Pack(
    id = id,
    title = title,
    version = version,
    category = category,
    description = description,
    packType = packType,
    license = license,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    author = author,
    lastReviewedEpochMillis = lastReviewedEpochMillis,
    reviewStatus = reviewStatus,
    installedAtEpochMillis = installedAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    isBundled = isBundled,
)

internal fun Pack.toEntity(): PackEntity = PackEntity(
    id = id,
    title = title,
    version = version,
    category = category,
    description = description,
    packType = packType,
    license = license,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    author = author,
    lastReviewedEpochMillis = lastReviewedEpochMillis,
    reviewStatus = reviewStatus,
    installedAtEpochMillis = installedAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    isBundled = isBundled,
)

internal fun GuideCardEntity.toModel(): GuideCard = GuideCard(
    id = id,
    packId = packId,
    title = title,
    category = category,
    summary = summary,
    bodyMarkdown = bodyMarkdown,
    warnings = warnings,
    sortOrder = sortOrder,
    provenanceId = provenanceId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    workflowTags = workflowTags.toWorkflowTagList(),
)

internal fun GuideCard.toEntity(): GuideCardEntity = GuideCardEntity(
    id = id,
    packId = packId,
    title = title,
    category = category,
    summary = summary,
    bodyMarkdown = bodyMarkdown,
    warnings = warnings,
    sortOrder = sortOrder,
    provenanceId = provenanceId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    workflowTags = workflowTags.toWorkflowTagStorage(),
)

internal fun ProvenanceEntity.toModel(): Provenance = Provenance(
    id = id,
    sourceTitle = sourceTitle,
    sourceUrl = sourceUrl,
    citation = citation,
    license = license,
    reviewStatus = reviewStatus,
    reviewedBy = reviewedBy,
    reviewedAtEpochMillis = reviewedAtEpochMillis,
    notes = notes,
)

internal fun Provenance.toEntity(): ProvenanceEntity = ProvenanceEntity(
    id = id,
    sourceTitle = sourceTitle,
    sourceUrl = sourceUrl,
    citation = citation,
    license = license,
    reviewStatus = reviewStatus,
    reviewedBy = reviewedBy,
    reviewedAtEpochMillis = reviewedAtEpochMillis,
    notes = notes,
)

private fun String?.toWorkflowTagList(): List<String> =
    orEmpty()
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

private fun List<String>.toWorkflowTagStorage(): String? =
    map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .takeIf { it.isNotEmpty() }
        ?.joinToString(",")
