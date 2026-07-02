package ai.unplugged.posa.ui

import ai.unplugged.posa.data.model.GearItem
import ai.unplugged.posa.data.model.GuideCard
import ai.unplugged.posa.data.model.InstalledMap
import ai.unplugged.posa.data.model.Provenance
import ai.unplugged.posa.data.model.Waypoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedQuestionRetrievalTest {
    @Test
    fun questionReturnsCitedInstalledSourceExcerpt() {
        val result = answerGuidedQuestion(
            guideState = GuideContentState(
                allCards = listOf(cardItem(waterCard()), cardItem(signalCard())),
            ),
            toolsState = ToolsContentState(),
            mapState = MapContentState(),
            question = "How should I plan water?",
        )

        assertEquals("Showing installed-source excerpts only.", result.statusText)
        assertEquals(RetrievalConfidence.High, result.confidence)
        assertEquals(listOf("Water Planning"), result.sourceMatches.map { it.item.card.title })
        assertTrue(result.sourceMatches.first().excerpt.contains("planned route"))
        assertEquals("National Park Service - Ten Essentials", result.sourceMatches.first().item.provenance?.sourceTitle)
    }

    @Test
    fun unknownQuestionDoesNotGenerateUnsupportedAnswer() {
        val result = answerGuidedQuestion(
            guideState = GuideContentState(
                allCards = listOf(cardItem(waterCard())),
            ),
            toolsState = ToolsContentState(),
            mapState = MapContentState(),
            question = "How do I set a broken bone?",
        )

        assertEquals(UNKNOWN_FROM_INSTALLED_SOURCES, result.statusText)
        assertEquals(RetrievalConfidence.None, result.confidence)
        assertTrue(result.sourceMatches.isEmpty())
        assertFalse(result.statusText.contains("splint", ignoreCase = true))
    }

    @Test
    fun retrievalIncludesMatchingGearAndMapFactsWhereRelevant() {
        val result = answerGuidedQuestion(
            guideState = GuideContentState(
                allCards = listOf(cardItem(signalCard())),
            ),
            toolsState = ToolsContentState(
                gear = listOf(
                    gearItem("whistle", "Signal whistle", isAvailable = true),
                    gearItem("mirror", "Signal mirror", isAvailable = false),
                    gearItem("filter", "Water filter", isAvailable = true),
                ),
            ),
            mapState = MapContentState(
                installedMaps = listOf(installedMap()),
                waypoints = listOf(waypoint()),
            ),
            question = "How can I signal from my map location?",
        )

        assertEquals(listOf("Have: Signal whistle x1", "Missing: Signal mirror x1"), result.gearFacts)
        assertTrue(result.mapFacts.any { it.contains("Enabled map area: Local map") })
        assertTrue(result.mapFacts.any { it.contains("Saved waypoints: Trailhead") })
    }

    @Test
    fun synonymExpansionFindsInstalledSourceWithoutGeneratingAnswer() {
        val result = answerGuidedQuestion(
            guideState = GuideContentState(
                allCards = listOf(cardItem(waterCard()), cardItem(signalCard())),
            ),
            toolsState = ToolsContentState(),
            mapState = MapContentState(),
            question = "What purifier should I carry?",
        )

        assertEquals("Showing installed-source excerpts only.", result.statusText)
        assertEquals(listOf("Water Planning"), result.sourceMatches.map { it.item.card.title })
        assertTrue(result.sourceMatches.first().matchedTerms.contains("treatment"))
        assertTrue(result.sourceMatches.first().excerpt.contains("Carry a treatment method"))
    }

    @Test
    fun synonymExpansionImprovesSignalQueries() {
        val result = answerGuidedQuestion(
            guideState = GuideContentState(
                allCards = listOf(cardItem(signalCard())),
            ),
            toolsState = ToolsContentState(),
            mapState = MapContentState(),
            question = "How do I stay visible?",
        )

        assertEquals(listOf("Signaling Basics"), result.sourceMatches.map { it.item.card.title })
        assertTrue(result.sourceMatches.first().matchedTerms.contains("signal"))
    }

    private fun cardItem(card: GuideCard): GuideCardItem = GuideCardItem(
        card = card,
        pack = null,
        provenance = Provenance(
            id = "source-${card.id}",
            sourceTitle = "National Park Service - Ten Essentials",
            sourceUrl = "https://www.nps.gov/articles/10essentials.htm",
            citation = "National Park Service. Ten Essentials.",
            license = "Public domain unless otherwise noted.",
            reviewStatus = "draft",
            reviewedBy = "Unplugged AI content draft",
            reviewedAtEpochMillis = NOW,
            notes = "Not medical advice.",
        ),
    )

    private fun waterCard(): GuideCard = guideCard(
        id = "water",
        title = "Water Planning",
        category = "water",
        summary = "Carry a treatment method before walking.",
        body = """
            ## Field Use

            - Carry a treatment method before travel.
            - Confirm likely sources before the planned route plus a delay.
        """.trimIndent(),
    )

    private fun signalCard(): GuideCard = guideCard(
        id = "signaling",
        title = "Signaling Basics",
        category = "signaling",
        summary = "Use carried signaling tools deliberately.",
        body = """
            ## Field Use

            - Use a whistle, mirror, light, or bright marker when visibility matters.
            - Stay near a known location if moving would make you harder to find.
        """.trimIndent(),
    )

    private fun guideCard(
        id: String,
        title: String,
        category: String,
        summary: String,
        body: String,
    ): GuideCard = GuideCard(
        id = id,
        packId = "pack",
        title = title,
        category = category,
        summary = summary,
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

    private fun installedMap(): InstalledMap = InstalledMap(
        id = "map",
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
    )

    private fun waypoint(): Waypoint = Waypoint(
        id = "waypoint",
        name = "Trailhead",
        latitude = 35.12345,
        longitude = -82.12345,
        elevationMeters = null,
        notes = null,
        createdAtEpochMillis = NOW,
        updatedAtEpochMillis = NOW,
    )

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
