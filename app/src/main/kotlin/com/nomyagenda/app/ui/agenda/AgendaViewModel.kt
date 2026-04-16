package com.nomyagenda.app.ui.agenda

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.nomyagenda.app.data.local.entity.AgendaEvent
import com.nomyagenda.app.data.repository.AgendaRepository
import kotlinx.coroutines.launch

class AgendaViewModel(private val repository: AgendaRepository) : ViewModel() {

    val events: LiveData<List<AgendaEvent>> = repository.allEvents.asLiveData()

    fun addEvent(event: AgendaEvent) {
        viewModelScope.launch {
            repository.insert(event)
        }
    }

    fun deleteEvent(event: AgendaEvent) {
        viewModelScope.launch {
            repository.delete(event)
        }
    }
}

class AgendaViewModelFactory(private val repository: AgendaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AgendaViewModel(repository) as T
    }
}
