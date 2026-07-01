package ai.unplugged.posa.data.local

import ai.unplugged.posa.data.local.repository.RoomLocalRepositories
import ai.unplugged.posa.data.local.repository.repositories
import ai.unplugged.posa.data.model.FieldNote
import ai.unplugged.posa.data.model.GearItem
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalDataRepositoryTest {
    private lateinit var database: PosaDatabase
    private lateinit var repositories: RoomLocalRepositories

    @Before
    fun setUp() {
        database = PosaDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        repositories = database.repositories()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun waypointRepositoryCreatesReadsUpdatesAndDeletes() = runTest {
        val waypoint = PosaDevelopmentSeed.sampleWaypoint(NOW)

        repositories.waypoints.save(waypoint)
        assertEquals(waypoint, repositories.waypoints.get(PosaDevelopmentSeed.WAYPOINT_ID))

        val updated = waypoint.copy(name = "Updated Trailhead", updatedAtEpochMillis = NOW + 1)
        repositories.waypoints.save(updated)
        assertEquals(updated, repositories.waypoints.get(PosaDevelopmentSeed.WAYPOINT_ID))
        assertEquals(listOf(updated), repositories.waypoints.list())

        repositories.waypoints.delete(PosaDevelopmentSeed.WAYPOINT_ID)
        assertNull(repositories.waypoints.get(PosaDevelopmentSeed.WAYPOINT_ID))
    }

    @Test
    fun breadcrumbRepositoryStoresTrailsAndPointsWithCascadeDelete() = runTest {
        val trail = PosaDevelopmentSeed.sampleBreadcrumbTrail(NOW)
        val point = PosaDevelopmentSeed.sampleBreadcrumbPoint(NOW)

        repositories.breadcrumbs.saveTrail(trail)
        repositories.breadcrumbs.savePoint(point)

        assertEquals(trail, repositories.breadcrumbs.getTrail(PosaDevelopmentSeed.BREADCRUMB_TRAIL_ID))
        assertEquals(listOf(point), repositories.breadcrumbs.listPointsForTrail(PosaDevelopmentSeed.BREADCRUMB_TRAIL_ID))

        repositories.breadcrumbs.deleteTrail(PosaDevelopmentSeed.BREADCRUMB_TRAIL_ID)
        assertNull(repositories.breadcrumbs.getPoint(PosaDevelopmentSeed.BREADCRUMB_POINT_ID))
    }

    @Test
    fun checklistAndGearRepositoriesCreateReadUpdateAndDelete() = runTest {
        val checklist = PosaDevelopmentSeed.sampleChecklist(NOW)
        val item = PosaDevelopmentSeed.sampleChecklistItem(NOW)
        val gear = PosaDevelopmentSeed.sampleGearItem(NOW)

        repositories.checklists.saveChecklist(checklist)
        repositories.checklists.saveItem(item)
        assertEquals(listOf(item), repositories.checklists.listItemsForChecklist(PosaDevelopmentSeed.CHECKLIST_ID))

        val checkedItem = item.copy(isChecked = true, updatedAtEpochMillis = NOW + 1)
        repositories.checklists.saveItem(checkedItem)
        assertEquals(checkedItem, repositories.checklists.getItem(PosaDevelopmentSeed.CHECKLIST_ITEM_ID))

        repositories.gear.save(gear)
        val missingGear = gear.copy(isAvailable = false, notes = "Battery missing.", updatedAtEpochMillis = NOW + 1)
        repositories.gear.save(missingGear)
        assertEquals(missingGear, repositories.gear.get(PosaDevelopmentSeed.GEAR_ITEM_ID))

        repositories.checklists.deleteChecklist(PosaDevelopmentSeed.CHECKLIST_ID)
        repositories.gear.delete(PosaDevelopmentSeed.GEAR_ITEM_ID)
        assertNull(repositories.checklists.getItem(PosaDevelopmentSeed.CHECKLIST_ITEM_ID))
        assertNull(repositories.gear.get(PosaDevelopmentSeed.GEAR_ITEM_ID))
    }

    @Test
    fun packGuideCardProvenanceAndFieldNoteRepositoriesPersistLocalLinks() = runTest {
        val provenance = PosaDevelopmentSeed.sampleProvenance()
        val pack = PosaDevelopmentSeed.samplePack(NOW)
        val card = PosaDevelopmentSeed.sampleGuideCard(NOW)
        val waypoint = PosaDevelopmentSeed.sampleWaypoint(NOW)
        val checklist = PosaDevelopmentSeed.sampleChecklist(NOW)
        val gear = PosaDevelopmentSeed.sampleGearItem(NOW)
        val note = PosaDevelopmentSeed.sampleFieldNote(NOW)

        repositories.provenance.save(provenance)
        repositories.packs.save(pack)
        repositories.guideCards.save(card)
        repositories.waypoints.save(waypoint)
        repositories.checklists.saveChecklist(checklist)
        repositories.gear.save(gear)
        repositories.fieldNotes.save(note)

        assertEquals(provenance, repositories.provenance.get(PosaDevelopmentSeed.PROVENANCE_ID))
        assertEquals(pack, repositories.packs.get(PosaDevelopmentSeed.PACK_ID))
        assertEquals(listOf(card), repositories.guideCards.listForPack(PosaDevelopmentSeed.PACK_ID))
        assertEquals(listOf(card), repositories.guideCards.search("navigation"))
        assertEquals(note, repositories.fieldNotes.get(PosaDevelopmentSeed.NOTE_ID))

        repositories.guideCards.delete(PosaDevelopmentSeed.GUIDE_CARD_ID)
        val noteAfterGuideDelete = repositories.fieldNotes.get(PosaDevelopmentSeed.NOTE_ID)
        assertNull(noteAfterGuideDelete?.guideCardId)

        val updatedNote = noteAfterGuideDelete!!.copy(
            body = "Updated local note body.",
            updatedAtEpochMillis = NOW + 2,
        )
        repositories.fieldNotes.save(updatedNote)
        assertEquals(updatedNote, repositories.fieldNotes.get(PosaDevelopmentSeed.NOTE_ID))

        repositories.fieldNotes.delete(PosaDevelopmentSeed.NOTE_ID)
        assertNull(repositories.fieldNotes.get(PosaDevelopmentSeed.NOTE_ID))
    }

    @Test
    fun developmentSeedInstallsOneLocalFixtureSetIdempotently() = runTest {
        PosaDevelopmentSeed.install(database, NOW)
        PosaDevelopmentSeed.install(database, NOW + 1)

        assertEquals(1, repositories.packs.list().size)
        assertEquals(1, repositories.guideCards.list().size)
        assertEquals(1, repositories.waypoints.list().size)
        assertEquals(1, repositories.checklists.listChecklists().size)
        assertEquals(1, repositories.gear.list().size)
        assertEquals(1, repositories.fieldNotes.list().size)
        assertEquals(1, repositories.breadcrumbs.listTrails().size)
        assertEquals(
            NOW + 1,
            repositories.packs.get(PosaDevelopmentSeed.PACK_ID)?.updatedAtEpochMillis,
        )
    }

    @Test
    fun fieldNotesCanStoreWithoutOptionalLinks() = runTest {
        val note = FieldNote(
            id = "unlinked-note",
            title = "Loose note",
            body = "User-owned local note.",
            createdAtEpochMillis = NOW,
            updatedAtEpochMillis = NOW,
            latitude = null,
            longitude = null,
            waypointId = null,
            checklistId = null,
            guideCardId = null,
            gearItemId = null,
        )
        val gear = GearItem(
            id = "loose-gear",
            name = "Notebook",
            category = "notes",
            quantity = 1,
            condition = null,
            notes = null,
            isAvailable = true,
            createdAtEpochMillis = NOW,
            updatedAtEpochMillis = NOW,
        )

        repositories.fieldNotes.save(note)
        repositories.gear.save(gear)

        assertEquals(note, repositories.fieldNotes.get("unlinked-note"))
        assertEquals(listOf(gear), repositories.gear.list())
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
