package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.RagAnswerService
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AskViewModel(
    application: Application,
    private val answerServiceFactory: (Application) -> RagAnswerService? = { app ->
        RagAnswerService.openBundled(app.applicationContext)
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AskContentState())
    val state: StateFlow<AskContentState> = _state.asStateFlow()

    private var answerService: RagAnswerService? = null
    private var searchJob: Job? = null

    fun setQuestion(question: String) {
        _state.value = _state.value.copy(question = question)
    }

    fun search() {
        val question = _state.value.question.trim()
        if (question.isBlank()) {
            _state.value = AskContentState()
            return
        }

        searchJob?.cancel()
        _state.value = _state.value.copy(
            question = question,
            isSearching = true,
            errorMessage = null,
        )
        searchJob = viewModelScope.launch {
            try {
                val result = withContext(ioDispatcher) {
                    val service = getOrOpenAnswerService()
                        ?: return@withContext null
                    service.answer(question)
                }
                _state.value = if (result == null) {
                    _state.value.copy(
                        isSearching = false,
                        result = null,
                        errorMessage = "Local source corpus is not bundled.",
                    )
                } else {
                    _state.value.copy(
                        isSearching = false,
                        result = result.toAskSourceResult(),
                        errorMessage = null,
                    )
                }
            } catch (exception: Exception) {
                _state.value = _state.value.copy(
                    isSearching = false,
                    errorMessage = "Local source search failed: ${exception.message.orEmpty()}",
                )
            }
        }
    }

    private fun getOrOpenAnswerService(): RagAnswerService? {
        answerService?.let { return it }
        return answerServiceFactory(getApplication<Application>())
            ?.also { answerService = it }
    }

    override fun onCleared() {
        searchJob?.cancel()
        answerService?.close()
        answerService = null
        super.onCleared()
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AskViewModel(application = this[APPLICATION_KEY]!!)
            }
        }
    }
}
