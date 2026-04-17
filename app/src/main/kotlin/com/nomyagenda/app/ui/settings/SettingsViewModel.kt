package com.nomyagenda.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.data.local.database.NomyAgendaDatabase
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.data.repository.AgendaRepository
import com.nomyagenda.app.notifications.NotificationHelper
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)
    private val agendaRepo = AgendaRepository(
        NomyAgendaDatabase.getDatabase(app).agendaEntryDao()
    )

    val themeMode = MutableLiveData(settingsRepo.themeMode)
    val language = MutableLiveData(settingsRepo.language)
    val notificationsEnabled = MutableLiveData(settingsRepo.notificationsEnabled)
    val advanceNoticeMinutes = MutableLiveData(settingsRepo.advanceNoticeMinutes)

    fun setTheme(mode: String) {
        settingsRepo.themeMode = mode
        themeMode.value = mode
        settingsRepo.applyTheme()
    }

    fun setLanguage(lang: String) {
        settingsRepo.language = lang
        language.value = lang
        settingsRepo.applyLanguage()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        settingsRepo.notificationsEnabled = enabled
        notificationsEnabled.value = enabled
    }

    fun setAdvanceNoticeMinutes(minutes: Int) {
        settingsRepo.advanceNoticeMinutes = minutes
        advanceNoticeMinutes.value = minutes
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            agendaRepo.getFutureReminders(now).forEach { entry ->
                NotificationHelper.scheduleReminder(getApplication(), entry)
            }
        }
    }
}
