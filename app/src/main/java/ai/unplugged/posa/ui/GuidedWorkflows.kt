package ai.unplugged.posa.ui

import ai.unplugged.posa.data.model.ChecklistItem
import ai.unplugged.posa.data.model.GearItem
import ai.unplugged.posa.data.model.GuideCard
import ai.unplugged.posa.data.model.InstalledMap
import ai.unplugged.posa.data.model.Waypoint
import java.util.Locale

internal enum class GuidedWorkflowId(
    val title: String,
    val actionLabel: String,
    val workflowTag: String,
) {
    Water("I need water", "Water", "water"),
    Lost("I am lost", "Lost", "lost"),
    Shelter("I need shelter", "Shelter", "shelter"),
    Fire("I need fire", "Fire", "fire"),
    Signal("I need to signal", "Signal", "signal"),
    Battery("Save battery", "Battery", "battery"),
}

internal data class GuidedWorkflowResult(
    val id: GuidedWorkflowId,
    val guideCards: List<GuideCardItem>,
    val checklistSteps: List<GuidedChecklistStep>,
    val availableGear: List<GearItem>,
    val missingGear: List<GearItem>,
    val locationFacts: List<String>,
    val missingDataWarnings: List<String>,
) {
    val guideBullets: List<GuidedGuideBullet> = guideCards
        .flatMap { cardItem ->
            cardItem.card.fieldUseBullets().map { bullet ->
                GuidedGuideBullet(
                    text = bullet,
                    sourceCard = cardItem.card,
                )
            }
        }
        .take(MAX_GUIDE_BULLETS)
}

internal data class GuidedChecklistStep(
    val checklistTitle: String,
    val item: ChecklistItem,
)

internal data class GuidedGuideBullet(
    val text: String,
    val sourceCard: GuideCard,
)

internal fun buildGuidedWorkflows(
    guideState: GuideContentState,
    toolsState: ToolsContentState,
    mapState: MapContentState,
): List<GuidedWorkflowResult> {
    val definitions = GuidedWorkflowDefinition.entries
    val sourceCards = guideState.allCards.ifEmpty { guideState.cards }
    return definitions.map { definition ->
        val guideCards = sourceCards
            .filter { it.card.matchesWorkflow(definition) }
            .sortedWith(
                compareBy<GuideCardItem>(
                    { it.card.sortOrder },
                    { it.card.title.lowercase() },
                ),
            )
            .take(MAX_GUIDE_CARDS)

        val checklistSteps = toolsState.checklists
            .filterNot { it.checklist.isArchived }
            .flatMap { checklist ->
                checklist.items
                    .filter { item -> item.matchesAny(definition.checklistTerms) }
                    .sortedBy { it.position }
                    .map { item ->
                        GuidedChecklistStep(
                            checklistTitle = checklist.checklist.title,
                            item = item,
                        )
                    }
            }
            .take(MAX_CHECKLIST_STEPS)

        val matchingGear = toolsState.gear
            .filter { it.matchesAny(definition.gearTerms) }
            .sortedWith(compareBy<GearItem>({ !it.isAvailable }, { it.name.lowercase() }))

        GuidedWorkflowResult(
            id = definition.id,
            guideCards = guideCards,
            checklistSteps = checklistSteps,
            availableGear = matchingGear.filter { it.isAvailable }.take(MAX_GEAR_ITEMS),
            missingGear = matchingGear.filterNot { it.isAvailable }.take(MAX_GEAR_ITEMS),
            locationFacts = mapState.locationFactsFor(definition),
            missingDataWarnings = missingWarningsFor(
                definition = definition,
                guideCards = guideCards,
                checklistSteps = checklistSteps,
                matchingGear = matchingGear,
                mapState = mapState,
            ),
        )
    }
}

private data class GuidedWorkflowDefinition(
    val id: GuidedWorkflowId,
    val guideTerms: List<String>,
    val checklistTerms: List<String>,
    val gearTerms: List<String>,
    val needsLocation: Boolean,
) {
    companion object {
        val entries = listOf(
            GuidedWorkflowDefinition(
                id = GuidedWorkflowId.Water,
                guideTerms = listOf("water"),
                checklistTerms = listOf("water", "treatment", "container"),
                gearTerms = listOf("water", "filter", "purifier", "treatment", "bottle", "container"),
                needsLocation = true,
            ),
            GuidedWorkflowDefinition(
                id = GuidedWorkflowId.Lost,
                guideTerms = listOf("navigation", "signaling", "battery", "power"),
                checklistTerms = listOf("navigation", "map", "compass", "signal", "light", "power"),
                gearTerms = listOf("map", "compass", "gps", "phone", "whistle", "mirror", "light", "headlamp", "power"),
                needsLocation = true,
            ),
            GuidedWorkflowDefinition(
                id = GuidedWorkflowId.Shelter,
                guideTerms = listOf("shelter"),
                checklistTerms = listOf("shelter", "insulation", "layer", "blanket", "dry"),
                gearTerms = listOf("shelter", "tarp", "bivy", "tent", "blanket", "layer", "jacket", "insulation"),
                needsLocation = true,
            ),
            GuidedWorkflowDefinition(
                id = GuidedWorkflowId.Fire,
                guideTerms = listOf("fire"),
                checklistTerms = listOf("fire", "starter", "ignition"),
                gearTerms = listOf("fire", "lighter", "match", "starter", "stove", "fuel", "ignition"),
                needsLocation = true,
            ),
            GuidedWorkflowDefinition(
                id = GuidedWorkflowId.Signal,
                guideTerms = listOf("signaling", "navigation", "battery", "power"),
                checklistTerms = listOf("signal", "whistle", "mirror", "light", "visibility", "power"),
                gearTerms = listOf("signal", "whistle", "mirror", "light", "headlamp", "bright", "marker", "power"),
                needsLocation = true,
            ),
            GuidedWorkflowDefinition(
                id = GuidedWorkflowId.Battery,
                guideTerms = listOf("battery", "power", "navigation"),
                checklistTerms = listOf("power", "battery", "light", "navigation", "map"),
                gearTerms = listOf("power", "battery", "charger", "phone", "gps", "headlamp", "light"),
                needsLocation = false,
            ),
        )
    }
}

private fun missingWarningsFor(
    definition: GuidedWorkflowDefinition,
    guideCards: List<GuideCardItem>,
    checklistSteps: List<GuidedChecklistStep>,
    matchingGear: List<GearItem>,
    mapState: MapContentState,
): List<String> = buildList {
    if (guideCards.isEmpty()) {
        add("No installed guide card matched this workflow.")
    }
    if (checklistSteps.isEmpty()) {
        add("No local checklist step matched this workflow.")
    }
    if (matchingGear.isEmpty()) {
        add("No gear inventory item matched this workflow.")
    }
    if (definition.needsLocation && !mapState.hasLocationContext()) {
        add("No saved waypoint, active breadcrumb, or enabled map area is available as location context.")
    }
}

private fun MapContentState.locationFactsFor(definition: GuidedWorkflowDefinition): List<String> = buildList {
    activeInstalledMap?.let { map ->
        add("Enabled map area: ${map.displayName}")
        map.boundingBoxLabel()?.let { add("Map bounds: $it") }
    }
    activeTrail?.let { trail ->
        add("Active breadcrumb: ${trail.trail.name} with ${trail.points.size} recorded points")
    }
    if (waypoints.isNotEmpty()) {
        add("Saved waypoints: ${waypoints.take(3).joinToString { it.locationLabel() }}")
    }
    if (definition.needsLocation && isEmpty()) {
        add("Open the Map tab to save a waypoint, enable an offline map area, or start breadcrumbs.")
    }
}

private fun MapContentState.hasLocationContext(): Boolean =
    activeInstalledMap != null || activeTrail != null || waypoints.isNotEmpty()

private fun GuideCard.matchesWorkflow(definition: GuidedWorkflowDefinition): Boolean =
    if (workflowTags.isNotEmpty()) {
        definition.id.workflowTag in workflowTags.map { it.lowercase(Locale.US) }
    } else {
        matchesAny(definition.guideTerms)
    }

private fun GuideCard.matchesAny(terms: List<String>): Boolean =
    searchableText(
        id,
        title,
        category,
        summary,
        bodyMarkdown,
    ).containsAny(terms)

private fun ChecklistItem.matchesAny(terms: List<String>): Boolean =
    searchableText(label, details.orEmpty()).containsAny(terms)

private fun GearItem.matchesAny(terms: List<String>): Boolean =
    searchableText(name, category.orEmpty(), condition.orEmpty(), notes.orEmpty()).containsAny(terms)

private fun searchableText(vararg values: String): String =
    values.joinToString(" ").lowercase(Locale.US)

private fun String.containsAny(terms: List<String>): Boolean =
    terms.any { term -> contains(term.lowercase(Locale.US)) }

private fun GuideCard.fieldUseBullets(): List<String> {
    var inFieldUse = false
    val bullets = mutableListOf<String>()
    bodyMarkdown.lines().forEach { line ->
        when {
            line == "## Field Use" -> inFieldUse = true
            line.startsWith("## ") -> inFieldUse = false
            inFieldUse && line.startsWith("- ") -> bullets += line.removePrefix("- ").trim()
        }
    }
    return bullets
}

private fun Waypoint.locationLabel(): String =
    "$name (${latitude.toCoordinateLabel()}, ${longitude.toCoordinateLabel()})"

private fun InstalledMap.boundingBoxLabel(): String? {
    val minLatitude = boundingBoxMinLatitude ?: return null
    val minLongitude = boundingBoxMinLongitude ?: return null
    val maxLatitude = boundingBoxMaxLatitude ?: return null
    val maxLongitude = boundingBoxMaxLongitude ?: return null
    return "${minLatitude.toCoordinateLabel()}, ${minLongitude.toCoordinateLabel()} to " +
        "${maxLatitude.toCoordinateLabel()}, ${maxLongitude.toCoordinateLabel()}"
}

private fun Double.toCoordinateLabel(): String = String.format(Locale.US, "%.5f", this)

private const val MAX_GUIDE_CARDS = 3
private const val MAX_GUIDE_BULLETS = 8
private const val MAX_CHECKLIST_STEPS = 6
private const val MAX_GEAR_ITEMS = 6
