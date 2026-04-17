package com.nomyagenda.app.ui.agenda

import androidx.lifecycle.*
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.EntryType
import com.nomyagenda.app.data.local.entity.SortOrder
import com.nomyagenda.app.data.repository.AgendaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AgendaViewModel(private val repository: AgendaRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow<EntryType?>(null)
    private val _sortOrder = MutableStateFlow(SortOrder.DUE_DATE)

    val entries: LiveData<List<AgendaEntry>> = combine(
        _searchQuery.debounce(300),
        _filterType,
        _sortOrder
    ) { query, filterType, sortOrder ->
        Triple(query, filterType, sortOrder)
    }.flatMapLatest { (query, filterType, sortOrder) ->
        val source = if (query.isBlank()) repository.getAll() else repository.search(query)
        source.map { list ->
            list
                .filter { entry -> filterType == null || entry.type == filterType }
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

    val currentSortOrder: SortOrder get() = _sortOrder.value

    fun deleteEntry(entry: AgendaEntry) {
        viewModelScope.launch { repository.delete(entry) }
    }

    private fun comparatorFor(sortOrder: SortOrder): Comparator<AgendaEntry> = when (sortOrder) {
        SortOrder.CREATED_AT -> compareBy { it.createdAt }
        SortOrder.DUE_DATE -> Comparator { a, b ->
            val aDue = a.dueAt
            val bDue = b.dueAt
            when {
                aDue == null && bDue == null -> a.createdAt.compareTo(b.createdAt)
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

class AgendaViewModelFactory(private val repository: AgendaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AgendaViewModel(repository) as T
    }
}
