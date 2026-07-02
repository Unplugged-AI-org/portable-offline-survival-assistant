package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.pack.BundledPackInstaller
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
 * Owns the Guide tab's content state, the bundled-pack install, the search-driven
 * reload, and the tab's UI selection state. Third step of the ViewModel migration
 * (workstreams.md, UI Architecture #2/#3), mirroring [MapViewModel] and [ToolsViewModel].
 *
 * @param onGuidePackInstalled invoked once the bundled pack finishes installing.
 *   Tools content reflects guide cards, so callers wire this to refresh tools. This
 *   replaces the old `bundledGuideInstalled` seam in PosaApp; decoupling it further
 *   is a later task in the UI Architecture plan.
 */
internal class GuideViewModel(
    application: Application,
    private val database: PosaDatabase?,
    private val onGuidePackInstalled: () -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(GuideContentState(isLoading = database != null))
    val state: StateFlow<GuideContentState> = _state.asStateFlow()

    private val _guidedQuestionQuery = MutableStateFlow("")
    val guidedQuestionQuery: StateFlow<String> = _guidedQuestionQuery.asStateFlow()

    private val _guideSearchQuery = MutableStateFlow("")
    val guideSearchQuery: StateFlow<String> = _guideSearchQuery.asStateFlow()

    private val _selectedGuideCardId = MutableStateFlow<String?>(null)
    val selectedGuideCardId: StateFlow<String?> = _selectedGuideCardId.asStateFlow()

    private val _selectedWorkflowId = MutableStateFlow(GuidedWorkflowId.Water)
    val selectedWorkflowId: StateFlow<GuidedWorkflowId> = _selectedWorkflowId.asStateFlow()

    private var bundledGuideInstalled = database == null

    init {
        if (database == null) {
            _state.value = GuideContentState(
                errorMessage = "Local guide database is not connected.",
            )
        } else {
            installBundledPack()
        }
    }

    fun setGuidedQuestion(query: String) {
        _guidedQuestionQuery.value = query
    }

    fun setGuideSearch(query: String) {
        _guideSearchQuery.value = query
        _selectedGuideCardId.value = null
        reload()
    }

    fun selectCard(id: String) {
        _selectedGuideCardId.value = id
    }

    fun clearSelectedCard() {
        _selectedGuideCardId.value = null
    }

    fun selectWorkflow(id: GuidedWorkflowId) {
        _selectedWorkflowId.value = id
    }

    private fun installBundledPack() {
        val localDatabase = database ?: return
        val appContext = getApplication<Application>().applicationContext
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    BundledPackInstaller.installAll(appContext, localDatabase)
                }
                bundledGuideInstalled = true
                onGuidePackInstalled()
                reload()
            } catch (exception: Exception) {
                bundledGuideInstalled = false
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Bundled guide pack could not be loaded: ${exception.message.orEmpty()}",
                )
            }
        }
    }

    private fun reload() {
        val localDatabase = database
        if (localDatabase == null || !bundledGuideInstalled) {
            return
        }

        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val loadedState = withContext(ioDispatcher) {
                    loadGuideContent(localDatabase, _guideSearchQuery.value)
                }
                _state.value = loadedState
                val selected = _selectedGuideCardId.value
                if (selected != null && loadedState.cards.none { it.card.id == selected }) {
                    _selectedGuideCardId.value = null
                }
            } catch (exception: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Guide cards could not be loaded: ${exception.message.orEmpty()}",
                )
            }
        }
    }

    companion object {
        fun factory(
            database: PosaDatabase?,
            onGuidePackInstalled: () -> Unit = {},
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GuideViewModel(
                    application = this[APPLICATION_KEY]!!,
                    database = database,
                    onGuidePackInstalled = onGuidePackInstalled,
                )
            }
        }
    }
}
