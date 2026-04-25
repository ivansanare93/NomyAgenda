package com.nomyagenda.app.ui.diary

import androidx.lifecycle.*
import com.nomyagenda.app.data.local.entity.DiaryEntry
import com.nomyagenda.app.data.repository.DiaryRepository
import kotlinx.coroutines.launch

class DiaryViewModel(private val repository: DiaryRepository) : ViewModel() {

    val entries: LiveData<List<DiaryEntry>> = repository.getAll().asLiveData()

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}

class DiaryViewModelFactory(
    private val repository: DiaryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DiaryViewModel(repository) as T
    }
}
