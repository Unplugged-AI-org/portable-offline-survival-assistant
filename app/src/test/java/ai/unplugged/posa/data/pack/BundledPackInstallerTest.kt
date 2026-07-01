package ai.unplugged.posa.data.pack

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.repository.RoomLocalRepositories
import ai.unplugged.posa.data.local.repository.repositories
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BundledPackInstallerTest {
    private lateinit var database: PosaDatabase
    private lateinit var repositories: RoomLocalRepositories
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = PosaDatabase.createInMemory(context)
        repositories = database.repositories()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun bundledStarterPackInstallsGuideCardsWithProvenanceAndSearch() = runTest {
        val results = BundledPackInstaller.installAll(context, database, NOW)

        assertEquals(1, results.size)
        assertEquals("wilderness-basics", results.single().packId)
        assertEquals(6, results.single().cardsInstalled)

        val pack = repositories.packs.get("wilderness-basics")
        assertNotNull(pack)
        assertEquals("draft", pack?.reviewStatus)
        assertEquals(NOW, pack?.installedAtEpochMillis)

        val cards = repositories.guideCards.list()
        assertEquals(6, cards.size)
        cards.forEach { card ->
            val provenanceId = card.provenanceId
            assertNotNull(provenanceId)
            val provenance = repositories.provenance.get(provenanceId!!)
            assertNotNull(provenance)
            assertTrue(provenance!!.sourceTitle.isNotBlank())
            assertEquals("draft", provenance.reviewStatus)
        }

        assertTrue(repositories.guideCards.search("water").any { it.title == "Water Planning" })
        assertTrue(repositories.guideCards.search("compass").any { it.title == "Navigation Baseline" })
        assertTrue(repositories.guideCards.search("battery").any { it.title == "Battery Conservation" })
    }

    @Test
    fun bundledStarterPackReinstallIsIdempotentAndPreservesInstallTime() = runTest {
        BundledPackInstaller.installAll(context, database, NOW)
        BundledPackInstaller.installAll(context, database, NOW + 1)

        val pack = repositories.packs.get("wilderness-basics")
        assertEquals(1, repositories.packs.list().size)
        assertEquals(6, repositories.guideCards.list().size)
        assertEquals(NOW, pack?.installedAtEpochMillis)
        assertEquals(NOW + 1, pack?.updatedAtEpochMillis)
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
