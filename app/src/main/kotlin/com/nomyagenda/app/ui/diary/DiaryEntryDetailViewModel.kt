package com.nomyagenda.app.ui.diary

import androidx.lifecycle.*
import com.nomyagenda.app.data.local.entity.DiaryEntry
import com.nomyagenda.app.data.repository.DiaryRepository
import kotlinx.coroutines.launch

class DiaryEntryDetailViewModel(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _entry = MutableLiveData<DiaryEntry?>()
    val entry: LiveData<DiaryEntry?> = _entry

    fun load(entryId: Int) {
        viewModelScope.launch {
            _entry.value = repository.getById(entryId)
        }
    }
}

class DiaryEntryDetailViewModelFactory(
    private val repository: DiaryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DiaryEntryDetailViewModel(repository) as T
    }
}
