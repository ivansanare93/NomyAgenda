package com.nomyagenda.app.ui.editor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.repository.AgendaRepository
import com.nomyagenda.app.notifications.ReminderService
import kotlinx.coroutines.launch

class EntryEditorViewModel(
    private val repository: AgendaRepository,
    private val reminderService: ReminderService
) : ViewModel() {

    private val _entry = MutableLiveData<AgendaEntry?>()
    val entry: LiveData<AgendaEntry?> = _entry
    private val _saveSuccessEvent = MutableLiveData(false)
    val saveSuccessEvent: LiveData<Boolean> = _saveSuccessEvent
    private val _saveErrorEvent = MutableLiveData<String?>(null)
    val saveErrorEvent: LiveData<String?> = _saveErrorEvent

    fun load(id: Int) {
        viewModelScope.launch {
            _entry.value = repository.getById(id)
        }
    }

    fun save(entry: AgendaEntry) {
        viewModelScope.launch {
            try {
                val rowId = repository.upsert(entry)
                // For new entries (id == 0) Room returns the auto-generated row-id which equals
                // the new primary key. For updates it returns the existing id as well.
                val savedId = if (entry.id == 0) rowId.toInt() else entry.id
                val savedEntry = if (entry.id == 0) entry.copy(id = savedId) else entry
                reminderService.syncForSavedEntry(savedEntry)
                _saveSuccessEvent.value = true
            } catch (_: Exception) {
                _saveErrorEvent.value = "No se pudo guardar la entrada"
            }
        }
    }

    fun consumeSaveSuccessEvent() {
        _saveSuccessEvent.value = false
    }

    fun consumeSaveErrorEvent() {
        _saveErrorEvent.value = null
    }
}

class EntryEditorViewModelFactory(
    private val repository: AgendaRepository,
    private val reminderService: ReminderService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EntryEditorViewModel(repository, reminderService) as T
    }
}
