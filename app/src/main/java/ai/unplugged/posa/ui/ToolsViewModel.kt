package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.StarterChecklistInstaller
import ai.unplugged.posa.data.local.repository.repositories
import ai.unplugged.posa.data.model.Checklist
import ai.unplugged.posa.data.model.ChecklistItem
import ai.unplugged.posa.data.model.FieldNote
import ai.unplugged.posa.data.model.GearItem
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the Tools tab's content state, load, and mutations. Second step of the
 * ViewModel migration (workstreams.md, UI Architecture #2), mirroring [MapViewModel].
 *
 * [reload] is public because tools content depends on data owned elsewhere: it must
 * refresh once the bundled guide pack finishes installing (field notes reference
 * guide cards) and after map mutations (field notes reference waypoints). Callers
 * wire those triggers; decoupling them is a later task in the UI Architecture plan.
 */
internal class ToolsViewModel(
    application: Application,
    private val database: PosaDatabase?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ToolsContentState(isLoading = database != null))
    val state: StateFlow<ToolsContentState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        val localDatabase = database
        if (localDatabase == null) {
            _state.value = ToolsContentState(
                errorMessage = "Local tools database is not connected.",
            )
            return
        }

        val appContext = getApplication<Application>().applicationContext
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val loadedState = withContext(ioDispatcher) {
                    StarterChecklistInstaller.installIfNeeded(appContext, localDatabase)
                    loadToolsContent(localDatabase)
                }
                _state.value = loadedState
            } catch (exception: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Tools data could not be loaded: ${exception.message.orEmpty()}",
                )
            }
        }
    }

    private fun mutate(block: suspend (PosaDatabase) -> Unit) {
        val localDatabase = database
        if (localDatabase == null) {
            _state.value = _state.value.copy(
                errorMessage = "Local tools database is not connected.",
            )
            return
        }

        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    block(localDatabase)
                }
                reload()
            } catch (exception: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Tools data could not be saved: ${exception.message.orEmpty()}",
                )
            }
        }
    }

    fun createChecklist(title: String, description: String?) {
        mutate { localDatabase ->
            val now = System.currentTimeMillis()
            localDatabase.repositories().checklists.saveChecklist(
                Checklist(
                    id = newLocalId("checklist"),
                    title = title,
                    description = description,
                    isArchived = false,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
        }
    }

    fun updateChecklist(checklist: Checklist, title: String, description: String?, isArchived: Boolean) {
        mutate { localDatabase ->
            localDatabase.repositories().checklists.saveChecklist(
                checklist.copy(
                    title = title,
                    description = description,
                    isArchived = isArchived,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteChecklist(checklist: Checklist) {
        mutate { localDatabase ->
            localDatabase.repositories().checklists.deleteChecklist(checklist.id)
        }
    }

    fun createChecklistItem(checklistId: String, label: String, details: String?) {
        mutate { localDatabase ->
            val repositories = localDatabase.repositories()
            val now = System.currentTimeMillis()
            val nextPosition = repositories.checklists
                .listItemsForChecklist(checklistId)
                .maxOfOrNull { it.position + 1 } ?: 0
            repositories.checklists.saveItem(
                ChecklistItem(
                    id = newLocalId("checklist-item"),
                    checklistId = checklistId,
                    label = label,
                    details = details,
                    position = nextPosition,
                    isChecked = false,
                    updatedAtEpochMillis = now,
                ),
            )
        }
    }

    fun updateChecklistItem(item: ChecklistItem, label: String, details: String?, isChecked: Boolean) {
        mutate { localDatabase ->
            localDatabase.repositories().checklists.saveItem(
                item.copy(
                    label = label,
                    details = details,
                    isChecked = isChecked,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteChecklistItem(item: ChecklistItem) {
        mutate { localDatabase ->
            localDatabase.repositories().checklists.deleteItem(item.id)
        }
    }

    fun createGearItem(draft: GearItemDraft) {
        mutate { localDatabase ->
            val now = System.currentTimeMillis()
            localDatabase.repositories().gear.save(
                GearItem(
                    id = newLocalId("gear"),
                    name = draft.name,
                    category = draft.category,
                    quantity = draft.quantity,
                    condition = draft.condition,
                    notes = draft.notes,
                    isAvailable = draft.isAvailable,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
        }
    }

    fun updateGearItem(item: GearItem, draft: GearItemDraft) {
        mutate { localDatabase ->
            localDatabase.repositories().gear.save(
                item.copy(
                    name = draft.name,
                    category = draft.category,
                    quantity = draft.quantity,
                    condition = draft.condition,
                    notes = draft.notes,
                    isAvailable = draft.isAvailable,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteGearItem(item: GearItem) {
        mutate { localDatabase ->
            localDatabase.repositories().gear.delete(item.id)
        }
    }

    fun createFieldNote(draft: FieldNoteDraft) {
        mutate { localDatabase ->
            val now = System.currentTimeMillis()
            localDatabase.repositories().fieldNotes.save(
                FieldNote(
                    id = newLocalId("field-note"),
                    title = draft.title,
                    body = draft.body,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    latitude = draft.latitude,
                    longitude = draft.longitude,
                    waypointId = draft.waypointId,
                    checklistId = draft.checklistId,
                    guideCardId = draft.guideCardId,
                    gearItemId = draft.gearItemId,
                ),
            )
        }
    }

    fun updateFieldNote(note: FieldNote, draft: FieldNoteDraft) {
        mutate { localDatabase ->
            localDatabase.repositories().fieldNotes.save(
                note.copy(
                    title = draft.title,
                    body = draft.body,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                    latitude = draft.latitude,
                    longitude = draft.longitude,
                    waypointId = draft.waypointId,
                    checklistId = draft.checklistId,
                    guideCardId = draft.guideCardId,
                    gearItemId = draft.gearItemId,
                ),
            )
        }
    }

    fun deleteFieldNote(note: FieldNote) {
        mutate { localDatabase ->
            localDatabase.repositories().fieldNotes.delete(note.id)
        }
    }

    companion object {
        fun factory(database: PosaDatabase?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ToolsViewModel(
                    application = this[APPLICATION_KEY]!!,
                    database = database,
                )
            }
        }
    }
}
