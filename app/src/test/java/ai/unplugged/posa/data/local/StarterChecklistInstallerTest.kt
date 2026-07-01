package ai.unplugged.posa.data.local

import ai.unplugged.posa.data.local.repository.RoomLocalRepositories
import ai.unplugged.posa.data.local.repository.repositories
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StarterChecklistInstallerTest {
    private lateinit var context: Context
    private lateinit var database: PosaDatabase
    private lateinit var repositories: RoomLocalRepositories

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("posa_local_installers", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        database = PosaDatabase.createInMemory(context)
        repositories = database.repositories()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun starterChecklistsInstallLocallyWithIncompleteItems() = runTest {
        val result = StarterChecklistInstaller.install(database, NOW)

        assertEquals(4, result.checklistsCreated)
        assertEquals(25, result.itemsCreated)
        assertFalse(result.skippedByPreference)

        val checklists = repositories.checklists.listChecklists()
        assertEquals(StarterChecklistInstaller.starterChecklistIds, checklists.map { it.id }.toSet())
        assertTrue(checklists.any { it.title == "Day Hike Essentials" })
        assertTrue(checklists.any { it.title == "First-Aid Kit Inventory" })

        checklists.forEach { checklist ->
            val items = repositories.checklists.listItemsForChecklist(checklist.id)
            assertTrue(items.isNotEmpty())
            assertTrue(items.all { !it.isChecked })
        }
    }

    @Test
    fun starterChecklistInstallDoesNotOverwriteUserEdits() = runTest {
        StarterChecklistInstaller.install(database, NOW)
        val checklist = repositories.checklists.getChecklist("starter-day-hike-essentials")!!
        val item = repositories.checklists.getItem("starter-day-hike-water")!!

        repositories.checklists.saveChecklist(
            checklist.copy(
                title = "My Day Kit",
                updatedAtEpochMillis = NOW + 1,
            ),
        )
        repositories.checklists.saveItem(
            item.copy(
                label = "Two water bottles",
                isChecked = true,
                updatedAtEpochMillis = NOW + 1,
            ),
        )

        val reinstall = StarterChecklistInstaller.install(database, NOW + 2)

        assertEquals(0, reinstall.checklistsCreated)
        assertEquals(0, reinstall.itemsCreated)
        assertEquals("My Day Kit", repositories.checklists.getChecklist("starter-day-hike-essentials")?.title)
        assertEquals("Two water bottles", repositories.checklists.getItem("starter-day-hike-water")?.label)
        assertEquals(true, repositories.checklists.getItem("starter-day-hike-water")?.isChecked)
    }

    @Test
    fun installIfNeededUsesLocalPreferenceMarker() = runTest {
        val first = StarterChecklistInstaller.installIfNeeded(context, database, NOW)
        val second = StarterChecklistInstaller.installIfNeeded(context, database, NOW + 1)

        assertFalse(first.skippedByPreference)
        assertTrue(second.skippedByPreference)
        assertEquals(0, second.checklistsCreated)
        assertEquals(0, second.itemsCreated)
        assertEquals(4, repositories.checklists.listChecklists().size)
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
