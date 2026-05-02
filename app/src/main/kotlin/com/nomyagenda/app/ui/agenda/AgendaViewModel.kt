package com.nomyagenda.app.ui.agenda

import androidx.lifecycle.*
import com.nomyagenda.app.core.datetime.toDateKey
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.EntryType
import com.nomyagenda.app.data.repository.AgendaRepository
import com.nomyagenda.app.notifications.ReminderService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AgendaViewModel(
    private val repository: AgendaRepository,
    private val reminderService: ReminderService
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow<EntryType?>(null)
    private val _selectedDate = MutableStateFlow<String?>(null)

    private data class Filters(
        val query: String,
        val filterType: EntryType?,
        val selectedDate: String?
    )

    val entries: LiveData<List<AgendaEntry>> = combine(
        _searchQuery.debounce(300),
        _filterType,
        _selectedDate
    ) { query, filterType, selectedDate ->
        Filters(query, filterType, selectedDate)
    }.flatMapLatest { (query, filterType, selectedDate) ->
        val source = if (query.isBlank()) repository.getAll() else repository.search(query)
        source.map { list ->
            list
                .filter { entry -> filterType == null || entry.type == filterType }
                .filter { entry ->
                    if (selectedDate == null) true
                    else {
                        val ts = entry.dueAt ?: entry.createdAt
                        ts.toDateKey() == selectedDate
                    }
                }
                .sortedByDescending { it.createdAt }
        }
    }.asLiveData()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: EntryType?) {
        _filterType.value = type
    }

    fun setSelectedDate(dateKey: String?) {
        _selectedDate.value = dateKey
    }

    fun deleteEntry(entry: AgendaEntry) {
        viewModelScope.launch {
            reminderService.cancelForDeletedEntry(entry)
            repository.delete(entry)
        }
    }

}

class AgendaViewModelFactory(
    private val repository: AgendaRepository,
    private val reminderService: ReminderService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AgendaViewModel(repository, reminderService) as T
    }
}
