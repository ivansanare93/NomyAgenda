package com.nomyagenda.app.ui.diary

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.nomyagenda.app.data.local.entity.DiaryEntry
import com.nomyagenda.app.data.repository.DiaryRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

class DiaryEntryEditorViewModel(
    private val repository: DiaryRepository
) : ViewModel() {

    private var currentEntry: DiaryEntry? = null

    val dateKey = MutableLiveData<String>()
    val title = MutableLiveData("")
    val content = MutableLiveData("")
    val mood = MutableLiveData("")
    val color = MutableLiveData("")
    val contentColor = MutableLiveData("")
    val background = MutableLiveData("")
    val photoPaths = MutableLiveData<List<String>>(emptyList())
    private val _saveSuccessEvent = MutableLiveData(false)
    val saveSuccessEvent: LiveData<Boolean> = _saveSuccessEvent
    private val _saveErrorEvent = MutableLiveData<String?>(null)
    val saveErrorEvent: LiveData<String?> = _saveErrorEvent

    fun loadEntry(entryId: Int, defaultDateKey: String) {
        viewModelScope.launch {
            if (entryId > 0) {
                val entry = repository.getById(entryId)
                if (entry != null) {
                    currentEntry = entry
                    dateKey.value = entry.dateKey
                    title.value = entry.title
                    content.value = entry.content
                    mood.value = entry.mood
                    color.value = entry.color
                    contentColor.value = entry.contentColor
                    background.value = entry.background
                    photoPaths.value = parsePhotoPaths(entry.photoPaths)
                }
            } else {
                dateKey.value = defaultDateKey
            }
        }
    }

    fun addPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            val path = copyImageToPrivateStorage(context, uri) ?: return@launch
            val current = photoPaths.value ?: emptyList()
            photoPaths.value = current + path
        }
    }

    fun removePhoto(path: String) {
        val current = photoPaths.value ?: emptyList()
        photoPaths.value = current.filter { it != path }
        File(path).delete()
    }

    fun save() {
        val dk = dateKey.value ?: return
        val now = System.currentTimeMillis()
        val pathsJson = buildPhotoPaths(photoPaths.value ?: emptyList())
        val entry = currentEntry?.copy(
            dateKey = dk,
            title = title.value ?: "",
            content = content.value ?: "",
            mood = mood.value ?: "",
            color = color.value ?: "",
            contentColor = contentColor.value ?: "",
            background = background.value ?: "",
            photoPaths = pathsJson,
            updatedAt = now
        ) ?: DiaryEntry(
            dateKey = dk,
            title = title.value ?: "",
            content = content.value ?: "",
            mood = mood.value ?: "",
            color = color.value ?: "",
            contentColor = contentColor.value ?: "",
            background = background.value ?: "",
            photoPaths = pathsJson,
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch {
            try {
                repository.upsert(entry)
                _saveSuccessEvent.value = true
            } catch (_: Exception) {
                _saveErrorEvent.value = "No se pudo guardar el diario"
            }
        }
    }

    fun consumeSaveSuccessEvent() {
        _saveSuccessEvent.value = false
    }

    fun consumeSaveErrorEvent() {
        _saveErrorEvent.value = null
    }

    private fun parsePhotoPaths(json: String): List<String> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildPhotoPaths(paths: List<String>): String {
        val arr = JSONArray()
        paths.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun copyImageToPrivateStorage(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "diary_photos").also { it.mkdirs() }
            val dest = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            dest.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}

class DiaryEntryEditorViewModelFactory(
    private val repository: DiaryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DiaryEntryEditorViewModel(repository) as T
    }
}
