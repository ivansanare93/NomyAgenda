package com.nomyagenda.app.ui.agenda

import androidx.lifecycle.*
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.repository.AgendaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AgendaViewModel(private val repository: AgendaRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val entries: LiveData<List<AgendaEntry>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) repository.getAll() else repository.search(q)
        }
        .asLiveData()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteEntry(entry: AgendaEntry) {
        viewModelScope.launch { repository.delete(entry) }
    }
}

class AgendaViewModelFactory(private val repository: AgendaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AgendaViewModel(repository) as T
    }
}
