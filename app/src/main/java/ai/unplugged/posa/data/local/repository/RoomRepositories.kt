package ai.unplugged.posa.data.local.repository

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.dao.BreadcrumbDao
import ai.unplugged.posa.data.local.dao.ChecklistDao
import ai.unplugged.posa.data.local.dao.FieldNoteDao
import ai.unplugged.posa.data.local.dao.GearDao
import ai.unplugged.posa.data.local.dao.GuideCardDao
import ai.unplugged.posa.data.local.dao.InstalledMapDao
import ai.unplugged.posa.data.local.dao.PackDao
import ai.unplugged.posa.data.local.dao.ProvenanceDao
import ai.unplugged.posa.data.local.dao.WaypointDao
import ai.unplugged.posa.data.local.toEntity
import ai.unplugged.posa.data.local.toModel
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
import ai.unplugged.posa.data.repository.BreadcrumbRepository
import ai.unplugged.posa.data.repository.ChecklistRepository
import ai.unplugged.posa.data.repository.FieldNoteRepository
import ai.unplugged.posa.data.repository.GearRepository
import ai.unplugged.posa.data.repository.GuideCardRepository
import ai.unplugged.posa.data.repository.InstalledMapRepository
import ai.unplugged.posa.data.repository.PackRepository
import ai.unplugged.posa.data.repository.ProvenanceRepository
import ai.unplugged.posa.data.repository.WaypointRepository

data class RoomLocalRepositories(
    val waypoints: WaypointRepository,
    val breadcrumbs: BreadcrumbRepository,
    val installedMaps: InstalledMapRepository,
    val fieldNotes: FieldNoteRepository,
    val checklists: ChecklistRepository,
    val gear: GearRepository,
    val packs: PackRepository,
    val guideCards: GuideCardRepository,
    val provenance: ProvenanceRepository,
)

fun PosaDatabase.repositories(): RoomLocalRepositories = RoomLocalRepositories(
    waypoints = RoomWaypointRepository(waypointDao()),
    breadcrumbs = RoomBreadcrumbRepository(breadcrumbDao()),
    installedMaps = RoomInstalledMapRepository(installedMapDao()),
    fieldNotes = RoomFieldNoteRepository(fieldNoteDao()),
    checklists = RoomChecklistRepository(checklistDao()),
    gear = RoomGearRepository(gearDao()),
    packs = RoomPackRepository(packDao()),
    guideCards = RoomGuideCardRepository(guideCardDao()),
    provenance = RoomProvenanceRepository(provenanceDao()),
)

class RoomWaypointRepository(
    private val dao: WaypointDao,
) : WaypointRepository {
    override suspend fun save(waypoint: Waypoint) {
        dao.upsert(waypoint.toEntity())
    }

    override suspend fun get(id: String): Waypoint? = dao.get(id)?.toModel()

    override suspend fun list(): List<Waypoint> = dao.list().map { it.toModel() }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}

class RoomBreadcrumbRepository(
    private val dao: BreadcrumbDao,
) : BreadcrumbRepository {
    override suspend fun saveTrail(trail: BreadcrumbTrail) {
        dao.upsertTrail(trail.toEntity())
    }

    override suspend fun getTrail(id: String): BreadcrumbTrail? = dao.getTrail(id)?.toModel()

    override suspend fun listTrails(): List<BreadcrumbTrail> = dao.listTrails().map { it.toModel() }

    override suspend fun deleteTrail(id: String) {
        dao.deleteTrail(id)
    }

    override suspend fun savePoint(point: BreadcrumbPoint) {
        dao.upsertPoint(point.toEntity())
    }

    override suspend fun getPoint(id: String): BreadcrumbPoint? = dao.getPoint(id)?.toModel()

    override suspend fun listPointsForTrail(trailId: String): List<BreadcrumbPoint> =
        dao.listPointsForTrail(trailId).map { it.toModel() }

    override suspend fun deletePoint(id: String) {
        dao.deletePoint(id)
    }
}

class RoomInstalledMapRepository(
    private val dao: InstalledMapDao,
) : InstalledMapRepository {
    override suspend fun save(map: InstalledMap) {
        dao.upsert(map.toEntity())
    }

    override suspend fun get(id: String): InstalledMap? = dao.get(id)?.toModel()

    override suspend fun list(): List<InstalledMap> = dao.list().map { it.toModel() }

    override suspend fun listEnabled(): List<InstalledMap> = dao.listEnabled().map { it.toModel() }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}

class RoomFieldNoteRepository(
    private val dao: FieldNoteDao,
) : FieldNoteRepository {
    override suspend fun save(note: FieldNote) {
        dao.upsert(note.toEntity())
    }

    override suspend fun get(id: String): FieldNote? = dao.get(id)?.toModel()

    override suspend fun list(): List<FieldNote> = dao.list().map { it.toModel() }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}

class RoomChecklistRepository(
    private val dao: ChecklistDao,
) : ChecklistRepository {
    override suspend fun saveChecklist(checklist: Checklist) {
        dao.upsertChecklist(checklist.toEntity())
    }

    override suspend fun getChecklist(id: String): Checklist? = dao.getChecklist(id)?.toModel()

    override suspend fun listChecklists(): List<Checklist> = dao.listChecklists().map { it.toModel() }

    override suspend fun deleteChecklist(id: String) {
        dao.deleteChecklist(id)
    }

    override suspend fun saveItem(item: ChecklistItem) {
        dao.upsertItem(item.toEntity())
    }

    override suspend fun getItem(id: String): ChecklistItem? = dao.getItem(id)?.toModel()

    override suspend fun listItemsForChecklist(checklistId: String): List<ChecklistItem> =
        dao.listItemsForChecklist(checklistId).map { it.toModel() }

    override suspend fun deleteItem(id: String) {
        dao.deleteItem(id)
    }
}

class RoomGearRepository(
    private val dao: GearDao,
) : GearRepository {
    override suspend fun save(item: GearItem) {
        dao.upsert(item.toEntity())
    }

    override suspend fun get(id: String): GearItem? = dao.get(id)?.toModel()

    override suspend fun list(): List<GearItem> = dao.list().map { it.toModel() }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}

class RoomPackRepository(
    private val dao: PackDao,
) : PackRepository {
    override suspend fun save(pack: Pack) {
        dao.upsert(pack.toEntity())
    }

    override suspend fun get(id: String): Pack? = dao.get(id)?.toModel()

    override suspend fun list(): List<Pack> = dao.list().map { it.toModel() }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}

class RoomGuideCardRepository(
    private val dao: GuideCardDao,
) : GuideCardRepository {
    override suspend fun save(card: GuideCard) {
        dao.upsert(card.toEntity())
    }

    override suspend fun get(id: String): GuideCard? = dao.get(id)?.toModel()

    override suspend fun list(): List<GuideCard> = dao.list().map { it.toModel() }

    override suspend fun search(query: String): List<GuideCard> {
        val terms = query.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (terms.isEmpty()) {
            return list()
        }

        return terms
            .flatMap { term -> dao.search(term.toSqlLikePattern()) }
            .distinctBy { it.id }
            .map { it.toModel() }
    }

    override suspend fun listForPack(packId: String): List<GuideCard> =
        dao.listForPack(packId).map { it.toModel() }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}

class RoomProvenanceRepository(
    private val dao: ProvenanceDao,
) : ProvenanceRepository {
    override suspend fun save(provenance: Provenance) {
        dao.upsert(provenance.toEntity())
    }

    override suspend fun get(id: String): Provenance? = dao.get(id)?.toModel()

    override suspend fun list(): List<Provenance> = dao.list().map { it.toModel() }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}

private fun String.toSqlLikePattern(): String = buildString {
    append('%')
    this@toSqlLikePattern.forEach { character ->
        if (character == '%' || character == '_' || character == '\\') {
            append('\\')
        }
        append(character)
    }
    append('%')
}
