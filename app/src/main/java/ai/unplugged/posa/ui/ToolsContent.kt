package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.repository.repositories
import ai.unplugged.posa.data.model.Checklist
import ai.unplugged.posa.data.model.ChecklistItem
import ai.unplugged.posa.data.model.FieldNote
import ai.unplugged.posa.data.model.GearItem
import ai.unplugged.posa.data.model.GuideCard
import ai.unplugged.posa.data.model.Waypoint

internal data class ToolsContentState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val checklists: List<ChecklistWithItems> = emptyList(),
    val gear: List<GearItem> = emptyList(),
    val fieldNotes: List<FieldNote> = emptyList(),
    val waypoints: List<Waypoint> = emptyList(),
    val guideCards: List<GuideCard> = emptyList(),
)

internal data class ChecklistWithItems(
    val checklist: Checklist,
    val items: List<ChecklistItem>,
)

internal suspend fun loadToolsContent(database: PosaDatabase): ToolsContentState {
    val repositories = database.repositories()
    val checklists = repositories.checklists.listChecklists()

    return ToolsContentState(
        checklists = checklists.map { checklist ->
            ChecklistWithItems(
                checklist = checklist,
                items = repositories.checklists.listItemsForChecklist(checklist.id),
            )
        },
        gear = repositories.gear.list(),
        fieldNotes = repositories.fieldNotes.list(),
        waypoints = repositories.waypoints.list(),
        guideCards = repositories.guideCards.list(),
    )
}
