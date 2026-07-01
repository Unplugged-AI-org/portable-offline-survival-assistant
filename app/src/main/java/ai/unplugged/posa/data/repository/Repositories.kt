package ai.unplugged.posa.data.repository

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

interface WaypointRepository {
    suspend fun save(waypoint: Waypoint)
    suspend fun get(id: String): Waypoint?
    suspend fun list(): List<Waypoint>
    suspend fun delete(id: String)
}

interface BreadcrumbRepository {
    suspend fun saveTrail(trail: BreadcrumbTrail)
    suspend fun getTrail(id: String): BreadcrumbTrail?
    suspend fun listTrails(): List<BreadcrumbTrail>
    suspend fun deleteTrail(id: String)
    suspend fun savePoint(point: BreadcrumbPoint)
    suspend fun getPoint(id: String): BreadcrumbPoint?
    suspend fun listPointsForTrail(trailId: String): List<BreadcrumbPoint>
    suspend fun deletePoint(id: String)
}

interface InstalledMapRepository {
    suspend fun save(map: InstalledMap)
    suspend fun get(id: String): InstalledMap?
    suspend fun list(): List<InstalledMap>
    suspend fun listEnabled(): List<InstalledMap>
    suspend fun delete(id: String)
}

interface FieldNoteRepository {
    suspend fun save(note: FieldNote)
    suspend fun get(id: String): FieldNote?
    suspend fun list(): List<FieldNote>
    suspend fun delete(id: String)
}

interface ChecklistRepository {
    suspend fun saveChecklist(checklist: Checklist)
    suspend fun getChecklist(id: String): Checklist?
    suspend fun listChecklists(): List<Checklist>
    suspend fun deleteChecklist(id: String)
    suspend fun saveItem(item: ChecklistItem)
    suspend fun getItem(id: String): ChecklistItem?
    suspend fun listItemsForChecklist(checklistId: String): List<ChecklistItem>
    suspend fun deleteItem(id: String)
}

interface GearRepository {
    suspend fun save(item: GearItem)
    suspend fun get(id: String): GearItem?
    suspend fun list(): List<GearItem>
    suspend fun delete(id: String)
}

interface PackRepository {
    suspend fun save(pack: Pack)
    suspend fun get(id: String): Pack?
    suspend fun list(): List<Pack>
    suspend fun delete(id: String)
}

interface GuideCardRepository {
    suspend fun save(card: GuideCard)
    suspend fun get(id: String): GuideCard?
    suspend fun list(): List<GuideCard>
    suspend fun search(query: String): List<GuideCard>
    suspend fun listForPack(packId: String): List<GuideCard>
    suspend fun delete(id: String)
}

interface ProvenanceRepository {
    suspend fun save(provenance: Provenance)
    suspend fun get(id: String): Provenance?
    suspend fun list(): List<Provenance>
    suspend fun delete(id: String)
}
