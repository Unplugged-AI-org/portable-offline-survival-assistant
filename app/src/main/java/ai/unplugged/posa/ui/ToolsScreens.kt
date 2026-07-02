package ai.unplugged.posa.ui

import ai.unplugged.posa.data.model.Checklist
import ai.unplugged.posa.data.model.ChecklistItem
import ai.unplugged.posa.data.model.FieldNote
import ai.unplugged.posa.data.model.GearItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class GearItemDraft(
    val name: String,
    val category: String?,
    val quantity: Int,
    val condition: String?,
    val notes: String?,
    val isAvailable: Boolean,
)

internal data class FieldNoteDraft(
    val title: String,
    val body: String,
    val latitude: Double?,
    val longitude: Double?,
    val waypointId: String?,
    val checklistId: String?,
    val guideCardId: String?,
    val gearItemId: String?,
)

internal data class ToolsActions(
    val onCreateChecklist: (title: String, description: String?) -> Unit,
    val onUpdateChecklist: (checklist: Checklist, title: String, description: String?, isArchived: Boolean) -> Unit,
    val onDeleteChecklist: (checklist: Checklist) -> Unit,
    val onCreateChecklistItem: (checklistId: String, label: String, details: String?) -> Unit,
    val onUpdateChecklistItem: (item: ChecklistItem, label: String, details: String?, isChecked: Boolean) -> Unit,
    val onDeleteChecklistItem: (item: ChecklistItem) -> Unit,
    val onCreateGearItem: (draft: GearItemDraft) -> Unit,
    val onUpdateGearItem: (item: GearItem, draft: GearItemDraft) -> Unit,
    val onDeleteGearItem: (item: GearItem) -> Unit,
    val onCreateFieldNote: (draft: FieldNoteDraft) -> Unit,
    val onUpdateFieldNote: (note: FieldNote, draft: FieldNoteDraft) -> Unit,
    val onDeleteFieldNote: (note: FieldNote) -> Unit,
)

@Composable
internal fun ToolsSection(
    state: ToolsContentState,
    actions: ToolsActions,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Checklists", "Gear", "Notes")

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ToolsStateBanners(state)
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(label) },
                )
            }
        }

        when (selectedTabIndex) {
            0 -> ChecklistsTab(
                state = state,
                onCreateChecklist = actions.onCreateChecklist,
                onUpdateChecklist = actions.onUpdateChecklist,
                onDeleteChecklist = actions.onDeleteChecklist,
                onCreateChecklistItem = actions.onCreateChecklistItem,
                onUpdateChecklistItem = actions.onUpdateChecklistItem,
                onDeleteChecklistItem = actions.onDeleteChecklistItem,
            )
            1 -> GearTab(
                state = state,
                onCreateGearItem = actions.onCreateGearItem,
                onUpdateGearItem = actions.onUpdateGearItem,
                onDeleteGearItem = actions.onDeleteGearItem,
            )
            else -> FieldNotesTab(
                state = state,
                onCreateFieldNote = actions.onCreateFieldNote,
                onUpdateFieldNote = actions.onUpdateFieldNote,
                onDeleteFieldNote = actions.onDeleteFieldNote,
            )
        }
    }
}

@Composable
private fun ChecklistsTab(
    state: ToolsContentState,
    onCreateChecklist: (title: String, description: String?) -> Unit,
    onUpdateChecklist: (checklist: Checklist, title: String, description: String?, isArchived: Boolean) -> Unit,
    onDeleteChecklist: (checklist: Checklist) -> Unit,
    onCreateChecklistItem: (checklistId: String, label: String, details: String?) -> Unit,
    onUpdateChecklistItem: (item: ChecklistItem, label: String, details: String?, isChecked: Boolean) -> Unit,
    onDeleteChecklistItem: (item: ChecklistItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NewChecklistForm(onCreateChecklist)
        if (!state.isLoading && state.checklists.isEmpty()) {
            EmptyToolsText("No checklists are stored locally.")
        }
        state.checklists.forEach { checklistWithItems ->
            ChecklistPanel(
                checklistWithItems = checklistWithItems,
                onUpdateChecklist = onUpdateChecklist,
                onDeleteChecklist = onDeleteChecklist,
                onCreateChecklistItem = onCreateChecklistItem,
                onUpdateChecklistItem = onUpdateChecklistItem,
                onDeleteChecklistItem = onDeleteChecklistItem,
            )
        }
    }
}

@Composable
private fun NewChecklistForm(
    onCreateChecklist: (title: String, description: String?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    PanelSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "New checklist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Title") },
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Description") },
            )
            Button(
                onClick = {
                    onCreateChecklist(title.trim(), description.trim().blankToNull())
                    title = ""
                    description = ""
                },
                enabled = title.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text("Add checklist")
            }
        }
    }
}

@Composable
private fun ChecklistPanel(
    checklistWithItems: ChecklistWithItems,
    onUpdateChecklist: (checklist: Checklist, title: String, description: String?, isArchived: Boolean) -> Unit,
    onDeleteChecklist: (checklist: Checklist) -> Unit,
    onCreateChecklistItem: (checklistId: String, label: String, details: String?) -> Unit,
    onUpdateChecklistItem: (item: ChecklistItem, label: String, details: String?, isChecked: Boolean) -> Unit,
    onDeleteChecklistItem: (item: ChecklistItem) -> Unit,
) {
    val checklist = checklistWithItems.checklist
    val items = checklistWithItems.items
    val completed = items.count { it.isChecked }
    var title by rememberSaveable(checklist.id, checklist.updatedAtEpochMillis) {
        mutableStateOf(checklist.title)
    }
    var description by rememberSaveable(checklist.id, checklist.updatedAtEpochMillis) {
        mutableStateOf(checklist.description.orEmpty())
    }
    var isArchived by rememberSaveable(checklist.id, checklist.updatedAtEpochMillis) {
        mutableStateOf(checklist.isArchived)
    }
    var newItemLabel by rememberSaveable(checklist.id) { mutableStateOf("") }
    var newItemDetails by rememberSaveable(checklist.id) { mutableStateOf("") }

    PanelSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${completed}/${items.size} complete",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isArchived) "Archived" else "Active",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Switch(
                        checked = isArchived,
                        onCheckedChange = { isArchived = it },
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Checklist title") },
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Description") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onUpdateChecklist(
                            checklist,
                            title.trim(),
                            description.trim().blankToNull(),
                            isArchived,
                        )
                    },
                    enabled = title.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Save")
                }
                OutlinedButton(onClick = { onDeleteChecklist(checklist) }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Delete")
                }
            }

            HorizontalDivider()
            items.forEach { item ->
                ChecklistItemRow(
                    item = item,
                    onUpdateChecklistItem = onUpdateChecklistItem,
                    onDeleteChecklistItem = onDeleteChecklistItem,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newItemLabel,
                        onValueChange = { newItemLabel = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("New item") },
                    )
                    OutlinedTextField(
                        value = newItemDetails,
                        onValueChange = { newItemDetails = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                        label = { Text("Details") },
                    )
                }
                IconButton(
                    onClick = {
                        onCreateChecklistItem(
                            checklist.id,
                            newItemLabel.trim(),
                            newItemDetails.trim().blankToNull(),
                        )
                        newItemLabel = ""
                        newItemDetails = ""
                    },
                    enabled = newItemLabel.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add checklist item",
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onUpdateChecklistItem: (item: ChecklistItem, label: String, details: String?, isChecked: Boolean) -> Unit,
    onDeleteChecklistItem: (item: ChecklistItem) -> Unit,
) {
    var label by rememberSaveable(item.id, item.updatedAtEpochMillis) { mutableStateOf(item.label) }
    var details by rememberSaveable(item.id, item.updatedAtEpochMillis) { mutableStateOf(item.details.orEmpty()) }
    var isChecked by rememberSaveable(item.id, item.updatedAtEpochMillis) { mutableStateOf(item.isChecked) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { checked ->
                isChecked = checked
                onUpdateChecklistItem(item, label.trim(), details.trim().blankToNull(), checked)
            },
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Item") },
            )
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                label = { Text("Details") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        onUpdateChecklistItem(item, label.trim(), details.trim().blankToNull(), isChecked)
                    },
                    enabled = label.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Save")
                }
                IconButton(onClick = { onDeleteChecklistItem(item) }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete checklist item",
                    )
                }
            }
        }
    }
}

@Composable
private fun GearTab(
    state: ToolsContentState,
    onCreateGearItem: (draft: GearItemDraft) -> Unit,
    onUpdateGearItem: (item: GearItem, draft: GearItemDraft) -> Unit,
    onDeleteGearItem: (item: GearItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GearItemEditor(
            header = "New gear or tool",
            initialItem = null,
            onSave = onCreateGearItem,
            onDelete = null,
        )
        if (!state.isLoading && state.gear.isEmpty()) {
            EmptyToolsText("No gear or tools are stored locally.")
        }
        state.gear.forEach { item ->
            GearItemEditor(
                header = item.name,
                initialItem = item,
                onSave = { draft -> onUpdateGearItem(item, draft) },
                onDelete = { onDeleteGearItem(item) },
            )
        }
    }
}

@Composable
private fun GearItemEditor(
    header: String,
    initialItem: GearItem?,
    onSave: (GearItemDraft) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val resetKey = initialItem?.id ?: "new-gear"
    val resetVersion = initialItem?.updatedAtEpochMillis ?: 0L
    var name by rememberSaveable(resetKey, resetVersion) { mutableStateOf(initialItem?.name.orEmpty()) }
    var category by rememberSaveable(resetKey, resetVersion) { mutableStateOf(initialItem?.category.orEmpty()) }
    var quantity by rememberSaveable(resetKey, resetVersion) {
        mutableStateOf(initialItem?.quantity?.toString() ?: "1")
    }
    var condition by rememberSaveable(resetKey, resetVersion) { mutableStateOf(initialItem?.condition.orEmpty()) }
    var notes by rememberSaveable(resetKey, resetVersion) { mutableStateOf(initialItem?.notes.orEmpty()) }
    var isAvailable by rememberSaveable(resetKey, resetVersion) {
        mutableStateOf(initialItem?.isAvailable ?: true)
    }
    var formError by rememberSaveable(resetKey, resetVersion) { mutableStateOf<String?>(null) }

    PanelSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isAvailable) "Have" else "Missing",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Name") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Category") },
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            OutlinedTextField(
                value = condition,
                onValueChange = { condition = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Condition") },
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Notes") },
            )
            formError?.let { ErrorText(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val parsedQuantity = quantity.trim().toIntOrNull()
                        if (name.isBlank()) {
                            formError = "Name is required."
                        } else if (parsedQuantity == null || parsedQuantity < 0) {
                            formError = "Quantity must be zero or greater."
                        } else {
                            formError = null
                            onSave(
                                GearItemDraft(
                                    name = name.trim(),
                                    category = category.trim().blankToNull(),
                                    quantity = parsedQuantity,
                                    condition = condition.trim().blankToNull(),
                                    notes = notes.trim().blankToNull(),
                                    isAvailable = isAvailable,
                                ),
                            )
                            if (initialItem == null) {
                                name = ""
                                category = ""
                                quantity = "1"
                                condition = ""
                                notes = ""
                                isAvailable = true
                            }
                        }
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Save")
                }
                onDelete?.let { delete ->
                    OutlinedButton(onClick = delete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldNotesTab(
    state: ToolsContentState,
    onCreateFieldNote: (draft: FieldNoteDraft) -> Unit,
    onUpdateFieldNote: (note: FieldNote, draft: FieldNoteDraft) -> Unit,
    onDeleteFieldNote: (note: FieldNote) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FieldNoteEditor(
            header = "New field note",
            initialNote = null,
            state = state,
            onSave = onCreateFieldNote,
            onDelete = null,
        )
        if (!state.isLoading && state.fieldNotes.isEmpty()) {
            EmptyToolsText("No field notes are stored locally.")
        }
        state.fieldNotes.forEach { note ->
            FieldNoteEditor(
                header = note.title,
                initialNote = note,
                state = state,
                onSave = { draft -> onUpdateFieldNote(note, draft) },
                onDelete = { onDeleteFieldNote(note) },
            )
        }
    }
}

@Composable
private fun FieldNoteEditor(
    header: String,
    initialNote: FieldNote?,
    state: ToolsContentState,
    onSave: (FieldNoteDraft) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val resetKey = initialNote?.id ?: "new-note"
    val resetVersion = initialNote?.updatedAtEpochMillis ?: 0L
    var title by rememberSaveable(resetKey, resetVersion) { mutableStateOf(initialNote?.title.orEmpty()) }
    var body by rememberSaveable(resetKey, resetVersion) { mutableStateOf(initialNote?.body.orEmpty()) }
    var latitude by rememberSaveable(resetKey, resetVersion) {
        mutableStateOf(initialNote?.latitude?.toString().orEmpty())
    }
    var longitude by rememberSaveable(resetKey, resetVersion) {
        mutableStateOf(initialNote?.longitude?.toString().orEmpty())
    }
    var waypointId by rememberSaveable(resetKey, resetVersion) { mutableStateOf(initialNote?.waypointId) }
    var checklistId by rememberSaveable(resetKey, resetVersion) { mutableStateOf(initialNote?.checklistId) }
    var guideCardId by rememberSaveable(resetKey, resetVersion) { mutableStateOf(initialNote?.guideCardId) }
    var gearItemId by rememberSaveable(resetKey, resetVersion) { mutableStateOf(initialNote?.gearItemId) }
    var formError by rememberSaveable(resetKey, resetVersion) { mutableStateOf<String?>(null) }

    val waypointOptions = state.waypoints.map { SelectionOption(it.id, it.name) }
    val checklistOptions = state.checklists.map { SelectionOption(it.checklist.id, it.checklist.title) }
    val guideCardOptions = state.guideCards.map { SelectionOption(it.id, it.title) }
    val gearOptions = state.gear.map { SelectionOption(it.id, it.name) }

    PanelSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = header,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            initialNote?.let { note ->
                MetadataLine("Created", note.createdAtEpochMillis.toDateTimeLabel())
                MetadataLine("Updated", note.updatedAtEpochMillis.toDateTimeLabel())
                note.linkLabels(state).forEach { linkLabel ->
                    Text(
                        text = linkLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Title") },
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("Body") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Latitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Longitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            LinkSelector(
                label = "Waypoint link",
                selectedId = waypointId,
                options = waypointOptions,
                onSelect = { waypointId = it },
            )
            LinkSelector(
                label = "Checklist link",
                selectedId = checklistId,
                options = checklistOptions,
                onSelect = { checklistId = it },
            )
            LinkSelector(
                label = "Guide card link",
                selectedId = guideCardId,
                options = guideCardOptions,
                onSelect = { guideCardId = it },
            )
            LinkSelector(
                label = "Gear link",
                selectedId = gearItemId,
                options = gearOptions,
                onSelect = { gearItemId = it },
            )
            formError?.let { ErrorText(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val coordinateResult = parseOptionalCoordinates(latitude, longitude)
                        if (title.isBlank()) {
                            formError = "Title is required."
                        } else if (body.isBlank()) {
                            formError = "Body is required."
                        } else if (coordinateResult is CoordinateResult.Invalid) {
                            formError = coordinateResult.message
                        } else {
                            val coordinates = coordinateResult as CoordinateResult.Valid
                            formError = null
                            onSave(
                                FieldNoteDraft(
                                    title = title.trim(),
                                    body = body.trim(),
                                    latitude = coordinates.latitude,
                                    longitude = coordinates.longitude,
                                    waypointId = waypointId,
                                    checklistId = checklistId,
                                    guideCardId = guideCardId,
                                    gearItemId = gearItemId,
                                ),
                            )
                            if (initialNote == null) {
                                title = ""
                                body = ""
                                latitude = ""
                                longitude = ""
                                waypointId = null
                                checklistId = null
                                guideCardId = null
                                gearItemId = null
                            }
                        }
                    },
                    enabled = title.isNotBlank() && body.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Save")
                }
                onDelete?.let { delete ->
                    OutlinedButton(onClick = delete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkSelector(
    label: String,
    selectedId: String?,
    options: List<SelectionOption>,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label ?: "None"

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Box {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = true },
            ) {
                Text(
                    text = selectedLabel,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    },
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {
                            onSelect(option.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolsStateBanners(state: ToolsContentState) {
    if (state.isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    state.errorMessage?.let { message ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PanelSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = { content() },
        )
    }
}

@Composable
private fun MetadataLine(
    label: String,
    value: String?,
) {
    if (value.isNullOrBlank()) {
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun EmptyToolsText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

private data class SelectionOption(
    val id: String,
    val label: String,
)

private sealed interface CoordinateResult {
    data class Valid(
        val latitude: Double?,
        val longitude: Double?,
    ) : CoordinateResult

    data class Invalid(
        val message: String,
    ) : CoordinateResult
}

private fun parseOptionalCoordinates(
    latitudeText: String,
    longitudeText: String,
): CoordinateResult {
    val trimmedLatitude = latitudeText.trim()
    val trimmedLongitude = longitudeText.trim()
    if (trimmedLatitude.isBlank() && trimmedLongitude.isBlank()) {
        return CoordinateResult.Valid(latitude = null, longitude = null)
    }
    if (trimmedLatitude.isBlank() || trimmedLongitude.isBlank()) {
        return CoordinateResult.Invalid("Enter both latitude and longitude or leave both blank.")
    }

    val latitude = trimmedLatitude.toDoubleOrNull()
    val longitude = trimmedLongitude.toDoubleOrNull()
    if (latitude == null || longitude == null) {
        return CoordinateResult.Invalid("Latitude and longitude must be numbers.")
    }
    if (latitude !in -90.0..90.0) {
        return CoordinateResult.Invalid("Latitude must be between -90 and 90.")
    }
    if (longitude !in -180.0..180.0) {
        return CoordinateResult.Invalid("Longitude must be between -180 and 180.")
    }
    return CoordinateResult.Valid(latitude = latitude, longitude = longitude)
}

private fun FieldNote.linkLabels(state: ToolsContentState): List<String> {
    val labels = mutableListOf<String>()
    if (latitude != null && longitude != null) {
        labels += "Location: ${latitude.toCoordinateLabel()}, ${longitude.toCoordinateLabel()}"
    }
    waypointId?.let { id ->
        labels += "Waypoint: ${state.waypoints.firstOrNull { it.id == id }?.name ?: id}"
    }
    checklistId?.let { id ->
        labels += "Checklist: ${state.checklists.firstOrNull { it.checklist.id == id }?.checklist?.title ?: id}"
    }
    guideCardId?.let { id ->
        labels += "Guide: ${state.guideCards.firstOrNull { it.id == id }?.title ?: id}"
    }
    gearItemId?.let { id ->
        labels += "Gear: ${state.gear.firstOrNull { it.id == id }?.name ?: id}"
    }
    return labels
}

private fun Double.toCoordinateLabel(): String = String.format(Locale.US, "%.5f", this)

private fun Long.toDateTimeLabel(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

private fun String.blankToNull(): String? = takeIf { it.isNotBlank() }
