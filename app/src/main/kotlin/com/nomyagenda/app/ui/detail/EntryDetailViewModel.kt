package com.nomyagenda.app.ui.detail

import androidx.lifecycle.*
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.repository.AgendaRepository
import kotlinx.coroutines.launch

class EntryDetailViewModel(
    private val repository: AgendaRepository
) : ViewModel() {

    private val _entry = MutableLiveData<AgendaEntry?>()
    val entry: LiveData<AgendaEntry?> = _entry

    fun load(id: Int) {
        viewModelScope.launch {
            _entry.value = repository.getById(id)
        }
    }
}

class EntryDetailViewModelFactory(
    private val repository: AgendaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EntryDetailViewModel(repository) as T
    }
}
