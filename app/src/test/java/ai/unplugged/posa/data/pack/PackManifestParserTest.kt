package ai.unplugged.posa.data.pack

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PackManifestParserTest {
    @Test
    fun parseManifestReadsRequiredAndOptionalFields() {
        val manifest = PackManifestParser.parseManifest(
            """
            {
              "id": "wilderness-basics",
              "title": "Wilderness Basics",
              "version": "0.1.0",
              "category": "wilderness",
              "description": "Starter cards.",
              "pack_type": "official",
              "license": "NOASSERTION",
              "source_name": "Example Source",
              "source_url": "https://example.test/source",
              "author": "Unplugged AI",
              "last_reviewed": "2026-07-01",
              "review_status": "draft",
              "files": ["cards/water.md", "cards/fire.md"]
            }
            """.trimIndent(),
        )

        assertEquals("wilderness-basics", manifest.id)
        assertEquals("Wilderness Basics", manifest.title)
        assertEquals(listOf("cards/water.md", "cards/fire.md"), manifest.files)
        assertEquals(1_782_864_000_000L, manifest.lastReviewedEpochMillis)
    }

    @Test
    fun parseGuideCardReadsWorkflowTagsFromFrontMatter() {
        val manifest = PackManifest(
            id = "pack",
            title = "Pack",
            version = "1",
            category = null,
            description = null,
            packType = "official",
            license = "NOASSERTION",
            sourceName = null,
            sourceUrl = null,
            author = null,
            lastReviewedEpochMillis = null,
            reviewStatus = "draft",
            files = listOf("cards/navigation.md"),
        )

        val parsed = PackManifestParser.parseGuideCard(
            manifest = manifest,
            relativePath = "cards/navigation.md",
            markdown = """
                ---
                id: navigation
                title: Navigation Baseline
                category: navigation
                summary: Stop early when position is uncertain.
                workflow_tags: lost, signal, battery, lost
                source_title: Example Source
                sort_order: 10
                ---
                ## Field Use

                - Stop, think, observe, and plan.
            """.trimIndent(),
            nowEpochMillis = NOW,
        )

        assertEquals("pack:navigation", parsed.card.id)
        assertEquals(listOf("lost", "signal", "battery"), parsed.card.workflowTags)
        assertEquals("pack:navigation:provenance", parsed.provenance.id)
    }

    @Test
    fun parseGuideCardAllowsMissingWorkflowTagsForLegacyContent() {
        val manifest = PackManifest(
            id = "pack",
            title = "Pack",
            version = "1",
            category = null,
            description = null,
            packType = "official",
            license = "NOASSERTION",
            sourceName = null,
            sourceUrl = null,
            author = null,
            lastReviewedEpochMillis = null,
            reviewStatus = "draft",
            files = listOf("cards/water.md"),
        )

        val parsed = PackManifestParser.parseGuideCard(
            manifest = manifest,
            relativePath = "cards/water.md",
            markdown = """
                ---
                id: water
                title: Water Planning
                category: water
                summary: Carry a treatment method.
                source_title: Example Source
                ---
                ## Field Use

                - Carry water.
            """.trimIndent(),
            nowEpochMillis = NOW,
        )

        assertEquals(emptyList<String>(), parsed.card.workflowTags)
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
