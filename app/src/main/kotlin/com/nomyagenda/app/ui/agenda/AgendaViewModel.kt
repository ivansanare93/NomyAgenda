package com.nomyagenda.app.ui.agenda

import androidx.lifecycle.*
import com.nomyagenda.app.core.datetime.toDateKey
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.EntryType
import com.nomyagenda.app.data.local.entity.SortOrder
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
    private val _sortOrder = MutableStateFlow(SortOrder.DUE_DATE)
    private val _selectedDate = MutableStateFlow<String?>(null)

    private data class Filters(
        val query: String,
        val filterType: EntryType?,
        val sortOrder: SortOrder,
        val selectedDate: String?
    )

    val entries: LiveData<List<AgendaEntry>> = combine(
        _searchQuery.debounce(300),
        _filterType,
        _sortOrder,
        _selectedDate
    ) { query, filterType, sortOrder, selectedDate ->
        Filters(query, filterType, sortOrder, selectedDate)
    }.flatMapLatest { (query, filterType, sortOrder, selectedDate) ->
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
                .sortedWith(comparatorFor(sortOrder))
        }
    }.asLiveData()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: EntryType?) {
        _filterType.value = type
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun setSelectedDate(dateKey: String?) {
        _selectedDate.value = dateKey
    }

    val currentSortOrder: SortOrder get() = _sortOrder.value

    fun deleteEntry(entry: AgendaEntry) {
        viewModelScope.launch {
            reminderService.cancelForDeletedEntry(entry)
            repository.delete(entry)
        }
    }

    private fun comparatorFor(sortOrder: SortOrder): Comparator<AgendaEntry> = when (sortOrder) {
        SortOrder.CREATED_AT -> compareByDescending { it.createdAt }
        SortOrder.DUE_DATE -> Comparator { a, b ->
            val aDue = a.dueAt
            val bDue = b.dueAt
            when {
                aDue == null && bDue == null -> b.createdAt.compareTo(a.createdAt)
                aDue == null -> 1
                bDue == null -> -1
                else -> aDue.compareTo(bDue)
            }
        }
        SortOrder.CATEGORY -> Comparator { a, b ->
            val aCat = a.category
            val bCat = b.category
            when {
                aCat.isEmpty() && bCat.isEmpty() -> a.createdAt.compareTo(b.createdAt)
                aCat.isEmpty() -> 1
                bCat.isEmpty() -> -1
                else -> {
                    val cmp = String.CASE_INSENSITIVE_ORDER.compare(aCat, bCat)
                    if (cmp != 0) cmp else a.createdAt.compareTo(b.createdAt)
                }
            }
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
