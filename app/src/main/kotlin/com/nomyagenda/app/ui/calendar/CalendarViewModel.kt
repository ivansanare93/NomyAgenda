package com.nomyagenda.app.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.nomyagenda.app.core.datetime.toDateKey
import com.nomyagenda.app.data.repository.AgendaRepository
import kotlinx.coroutines.flow.map

class CalendarViewModel(
    repository: AgendaRepository
) : ViewModel() {

    val entryDates: LiveData<Set<String>> = repository.getAll()
        .map { list ->
            list.map { entry ->
                val ts = entry.dueAt ?: entry.createdAt
                ts.toDateKey()
            }.toSet()
        }
        .asLiveData()
}

class CalendarViewModelFactory(
    private val repository: AgendaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CalendarViewModel(repository) as T
    }
}
