package ai.unplugged.posa.data.local

import ai.unplugged.posa.data.model.Checklist
import ai.unplugged.posa.data.model.ChecklistItem
import android.content.Context

object StarterChecklistInstaller {
    private const val PREFERENCES_NAME = "posa_local_installers"
    private const val STARTER_CHECKLISTS_INSTALLED_KEY = "starter_checklists_v1_installed"

    suspend fun installIfNeeded(
        context: Context,
        database: PosaDatabase,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): StarterChecklistInstallResult {
        val preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

        if (preferences.getBoolean(STARTER_CHECKLISTS_INSTALLED_KEY, false)) {
            return StarterChecklistInstallResult(
                checklistsCreated = 0,
                itemsCreated = 0,
                skippedByPreference = true,
            )
        }

        val result = install(database, nowEpochMillis)
        preferences.edit()
            .putBoolean(STARTER_CHECKLISTS_INSTALLED_KEY, true)
            .apply()
        return result
    }

    suspend fun install(
        database: PosaDatabase,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): StarterChecklistInstallResult {
        val dao = database.checklistDao()
        var checklistsCreated = 0
        var itemsCreated = 0

        starterChecklists.forEach { definition ->
            if (dao.getChecklist(definition.id) == null) {
                dao.upsertChecklist(
                    Checklist(
                        id = definition.id,
                        title = definition.title,
                        description = definition.description,
                        isArchived = false,
                        createdAtEpochMillis = nowEpochMillis,
                        updatedAtEpochMillis = nowEpochMillis,
                    ).toEntity(),
                )
                checklistsCreated += 1
            }

            definition.items.forEachIndexed { index, item ->
                if (dao.getItem(item.id) == null) {
                    dao.upsertItem(
                        ChecklistItem(
                            id = item.id,
                            checklistId = definition.id,
                            label = item.label,
                            details = item.details,
                            position = index,
                            isChecked = false,
                            updatedAtEpochMillis = nowEpochMillis,
                        ).toEntity(),
                    )
                    itemsCreated += 1
                }
            }
        }

        return StarterChecklistInstallResult(
            checklistsCreated = checklistsCreated,
            itemsCreated = itemsCreated,
            skippedByPreference = false,
        )
    }

    private val starterChecklists = listOf(
        StarterChecklistDefinition(
            id = "starter-day-hike-essentials",
            title = "Day Hike Essentials",
            description = "Preparation items for a short outing, editable and stored locally.",
            items = listOf(
                StarterChecklistItem("starter-day-hike-water", "Water carried", "Include a reserve for delays."),
                StarterChecklistItem("starter-day-hike-navigation", "Navigation tools", "Offline map, compass, or route reference."),
                StarterChecklistItem("starter-day-hike-layer", "Weather layer", "Layer for expected cold, wind, or rain."),
                StarterChecklistItem("starter-day-hike-light", "Headlamp or flashlight", "Check batteries before leaving."),
                StarterChecklistItem("starter-day-hike-signal", "Signal item", "Whistle, mirror, or other non-network signal option."),
                StarterChecklistItem("starter-day-hike-food", "Food for delay", "Compact calories beyond the planned outing."),
            ),
        ),
        StarterChecklistDefinition(
            id = "starter-emergency-overnight",
            title = "Emergency Overnight Basics",
            description = "Preparation items for an unexpected night out; not medical or rescue advice.",
            items = listOf(
                StarterChecklistItem("starter-overnight-shelter", "Shelter layer", "Tarp, bivy, or emergency shelter appropriate to conditions."),
                StarterChecklistItem("starter-overnight-insulation", "Insulation", "Warm layer, hat, gloves, or sleeping insulation as needed."),
                StarterChecklistItem("starter-overnight-water-treatment", "Water treatment method", "Use a method you already know how to operate."),
                StarterChecklistItem("starter-overnight-fire", "Fire starter where lawful", "Keep dry and follow local restrictions."),
                StarterChecklistItem("starter-overnight-power", "Power reserve", "Charged battery bank or spare batteries."),
                StarterChecklistItem("starter-overnight-dry-storage", "Dry storage", "Bag or case for critical items."),
            ),
        ),
        StarterChecklistDefinition(
            id = "starter-first-aid-kit-inventory",
            title = "First-Aid Kit Inventory",
            description = "Kit inventory only. POSA v0 does not provide treatment instructions.",
            items = listOf(
                StarterChecklistItem("starter-first-aid-bandages", "Adhesive bandages", null),
                StarterChecklistItem("starter-first-aid-gauze", "Gauze and wrap", null),
                StarterChecklistItem("starter-first-aid-wipes", "Antiseptic wipes", null),
                StarterChecklistItem("starter-first-aid-blister", "Blister care supplies", null),
                StarterChecklistItem("starter-first-aid-gloves", "Disposable gloves", null),
                StarterChecklistItem("starter-first-aid-personal", "Personal medications", "Pack only medications already prescribed or normally used."),
                StarterChecklistItem("starter-first-aid-blanket", "Emergency blanket", null),
            ),
        ),
        StarterChecklistDefinition(
            id = "starter-vehicle-field-kit",
            title = "Vehicle Field Kit",
            description = "Local inventory checklist for a trailhead, overland, or roadside kit.",
            items = listOf(
                StarterChecklistItem("starter-vehicle-water", "Stored water", "Rotate according to your own storage plan."),
                StarterChecklistItem("starter-vehicle-light", "Work light or headlamp", null),
                StarterChecklistItem("starter-vehicle-power", "Jumper pack or power bank", "Confirm it is charged."),
                StarterChecklistItem("starter-vehicle-tools", "Basic tools", "Match tools to your vehicle and trip."),
                StarterChecklistItem("starter-vehicle-map", "Paper map or offline map file", null),
                StarterChecklistItem("starter-vehicle-visibility", "High-visibility marker", "Vest, triangle, or marker appropriate to local rules."),
            ),
        ),
    )

    val starterChecklistIds: Set<String> = starterChecklists.map { it.id }.toSet()
}

data class StarterChecklistInstallResult(
    val checklistsCreated: Int,
    val itemsCreated: Int,
    val skippedByPreference: Boolean,
)

private data class StarterChecklistDefinition(
    val id: String,
    val title: String,
    val description: String,
    val items: List<StarterChecklistItem>,
)

private data class StarterChecklistItem(
    val id: String,
    val label: String,
    val details: String?,
)
