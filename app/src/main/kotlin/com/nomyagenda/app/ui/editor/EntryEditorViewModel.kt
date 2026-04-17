package com.nomyagenda.app.ui.editor

import android.app.Application
import androidx.lifecycle.*
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.EntryType
import com.nomyagenda.app.data.repository.AgendaRepository
import com.nomyagenda.app.notifications.NotificationHelper
import kotlinx.coroutines.launch

class EntryEditorViewModel(
    private val repository: AgendaRepository,
    private val app: Application
) : AndroidViewModel(app) {

    private val _entry = MutableLiveData<AgendaEntry?>()
    val entry: LiveData<AgendaEntry?> = _entry

    fun load(id: Int) {
        viewModelScope.launch {
            _entry.value = repository.getById(id)
        }
    }

    fun save(entry: AgendaEntry, onSaved: () -> Unit) {
        viewModelScope.launch {
            val rowId = repository.upsert(entry)
            // For new entries (id == 0) Room returns the auto-generated row-id which equals
            // the new primary key. For updates it returns the existing id as well.
            val savedId = if (entry.id == 0) rowId.toInt() else entry.id
            val savedEntry = if (entry.id == 0) entry.copy(id = savedId) else entry
            if (savedEntry.type == EntryType.REMINDER && savedEntry.dueAt != null) {
                NotificationHelper.scheduleReminder(app, savedEntry)
            } else {
                NotificationHelper.cancelReminder(app, savedId)
            }
            onSaved()
        }
    }
}

class EntryEditorViewModelFactory(
    private val repository: AgendaRepository,
    private val app: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EntryEditorViewModel(repository, app) as T
    }
}
