package ai.unplugged.posa.ui

import ai.unplugged.posa.data.model.Checklist
import ai.unplugged.posa.data.model.ChecklistItem
import ai.unplugged.posa.data.model.GearItem
import ai.unplugged.posa.data.model.GuideCard
import ai.unplugged.posa.data.model.InstalledMap
import ai.unplugged.posa.data.model.Provenance
import ai.unplugged.posa.data.model.Waypoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedWorkflowsTest {
    @Test
    fun waterWorkflowComposesGuideChecklistGearAndLocationContext() {
        val workflows = buildGuidedWorkflows(
            guideState = guideState(
                waterCard(),
                navigationCard(),
            ),
            toolsState = toolsState(
                checklistItems = listOf(
                    ChecklistItem(
                        id = "water-step",
                        checklistId = "checklist",
                        label = "Water treatment method",
                        details = "Use a method you already know how to operate.",
                        position = 0,
                        isChecked = false,
                        updatedAtEpochMillis = NOW,
                    ),
                ),
                gear = listOf(
                    gearItem("filter", "Water filter", isAvailable = true),
                    gearItem("missing-bottle", "Water bottle", isAvailable = false),
                ),
            ),
            mapState = mapState(),
        )

        val water = workflows.single { it.id == GuidedWorkflowId.Water }

        assertEquals(listOf("Water Planning"), water.guideCards.map { it.card.title })
        assertEquals(listOf("Identify likely water sources before travel and confirm whether they are seasonal."), water.guideBullets.take(1).map { it.text })
        assertEquals(listOf("Water treatment method"), water.checklistSteps.map { it.item.label })
        assertEquals(listOf("Water filter"), water.availableGear.map { it.name })
        assertEquals(listOf("Water bottle"), water.missingGear.map { it.name })
        assertTrue(water.locationFacts.any { it.contains("Enabled map area") })
        assertTrue(water.locationFacts.any { it.contains("Saved waypoints") })
        assertFalse(water.missingDataWarnings.any { it.contains("No installed guide card") })
    }

    @Test
    fun workflowsUseAllSourceCardsWhenVisibleCardListIsSearchFiltered() {
        val guideState = GuideContentState(
            allCards = listOf(cardItem(waterCard()), cardItem(navigationCard())),
            cards = listOf(cardItem(navigationCard())),
        )

        val water = buildGuidedWorkflows(
            guideState = guideState,
            toolsState = toolsState(),
            mapState = mapState(),
        ).single { it.id == GuidedWorkflowId.Water }

        assertEquals(listOf("Water Planning"), water.guideCards.map { it.card.title })
    }

    @Test
    fun missingWarningsCallOutAbsentLocalContext() {
        val fire = buildGuidedWorkflows(
            guideState = GuideContentState(),
            toolsState = ToolsContentState(),
            mapState = MapContentState(),
        ).single { it.id == GuidedWorkflowId.Fire }

        assertTrue(fire.missingDataWarnings.contains("No installed guide card matched this workflow."))
        assertTrue(fire.missingDataWarnings.contains("No local checklist step matched this workflow."))
        assertTrue(fire.missingDataWarnings.contains("No gear inventory item matched this workflow."))
        assertTrue(
            fire.missingDataWarnings.contains(
                "No saved waypoint, active breadcrumb, or enabled map area is available as location context.",
            ),
        )
    }

    private fun guideState(vararg cards: GuideCard): GuideContentState {
        val items = cards.map(::cardItem)
        return GuideContentState(
            allCards = items,
            cards = items,
        )
    }

    private fun cardItem(card: GuideCard): GuideCardItem = GuideCardItem(
        card = card,
        pack = null,
        provenance = Provenance(
            id = "source-${card.id}",
            sourceTitle = "Source for ${card.title}",
            sourceUrl = "https://example.test/${card.id}",
            citation = "Example citation.",
            license = "NOASSERTION",
            reviewStatus = "draft",
            reviewedBy = null,
            reviewedAtEpochMillis = null,
            notes = null,
        ),
    )

    private fun toolsState(
        checklistItems: List<ChecklistItem> = emptyList(),
        gear: List<GearItem> = emptyList(),
    ): ToolsContentState = ToolsContentState(
        checklists = listOf(
            ChecklistWithItems(
                checklist = Checklist(
                    id = "checklist",
                    title = "Field checklist",
                    description = null,
                    isArchived = false,
                    createdAtEpochMillis = NOW,
                    updatedAtEpochMillis = NOW,
                ),
                items = checklistItems,
            ),
        ),
        gear = gear,
    )

    private fun mapState(): MapContentState = MapContentState(
        installedMaps = listOf(
            InstalledMap(
                id = "map-area",
                displayName = "Local map",
                fileName = "local.map",
                filePath = "/maps/local.map",
                byteSize = 1_024L,
                isEnabled = true,
                centerLatitude = 35.0,
                centerLongitude = -82.0,
                startZoomLevel = 10,
                boundingBoxMinLatitude = 34.0,
                boundingBoxMinLongitude = -83.0,
                boundingBoxMaxLatitude = 36.0,
                boundingBoxMaxLongitude = -81.0,
                importedAtEpochMillis = NOW,
                updatedAtEpochMillis = NOW,
            ),
        ),
        waypoints = listOf(
            Waypoint(
                id = "waypoint",
                name = "Trailhead",
                latitude = 35.12345,
                longitude = -82.12345,
                elevationMeters = null,
                notes = null,
                createdAtEpochMillis = NOW,
                updatedAtEpochMillis = NOW,
            ),
        ),
    )

    private fun waterCard(): GuideCard = guideCard(
        id = "water",
        title = "Water Planning",
        category = "water",
        body = """
            ## Field Use

            - Identify likely water sources before travel and confirm whether they are seasonal.
            - Carry enough water for the planned route plus a delay.

            ## Watchpoints

            - Treat collected water when possible.
        """.trimIndent(),
    )

    private fun navigationCard(): GuideCard = guideCard(
        id = "navigation",
        title = "Navigation Baseline",
        category = "navigation",
        body = """
            ## Field Use

            - Stop moving long enough to think, observe, and make a deliberate plan.
        """.trimIndent(),
    )

    private fun guideCard(
        id: String,
        title: String,
        category: String,
        body: String,
    ): GuideCard = GuideCard(
        id = id,
        packId = "pack",
        title = title,
        category = category,
        summary = "$title summary",
        bodyMarkdown = body,
        warnings = null,
        sortOrder = 1,
        provenanceId = "source-$id",
        createdAtEpochMillis = NOW,
        updatedAtEpochMillis = NOW,
    )

    private fun gearItem(
        id: String,
        name: String,
        isAvailable: Boolean,
    ): GearItem = GearItem(
        id = id,
        name = name,
        category = null,
        quantity = 1,
        condition = null,
        notes = null,
        isAvailable = isAvailable,
        createdAtEpochMillis = NOW,
        updatedAtEpochMillis = NOW,
    )

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
