package com.nomyagenda.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.data.repository.AgendaRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    app: Application,
    private val agendaRepository: AgendaRepository
) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)

    val themeMode = MutableLiveData(settingsRepo.themeMode)
    val decorativeTheme = MutableLiveData(settingsRepo.decorativeTheme)
    val language = MutableLiveData(settingsRepo.language)
    val notificationsEnabled = MutableLiveData(settingsRepo.notificationsEnabled)
    val appBackground = MutableLiveData(settingsRepo.appBackground)

    /** true when the activity must recreate itself to apply a new decorative theme. */
    val recreateEvent = MutableLiveData(false)

    /** true when sign-out has completed and the UI should navigate to the login screen. */
    val signOutEvent = MutableLiveData(false)

    fun consumeRecreateEvent() {
        recreateEvent.value = false
    }

    fun consumeSignOutEvent() {
        signOutEvent.value = false
    }

    fun setTheme(mode: String) {
        settingsRepo.themeMode = mode
        themeMode.value = mode
        settingsRepo.applyTheme()
    }

    fun setDecorativeTheme(theme: String) {
        if (settingsRepo.decorativeTheme == theme) return
        settingsRepo.decorativeTheme = theme
        decorativeTheme.value = theme
        settingsRepo.applyTheme()
        recreateEvent.value = true
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

    fun setAppBackground(bg: String) {
        if (settingsRepo.appBackground == bg) return
        settingsRepo.appBackground = bg
        appBackground.value = bg
        settingsRepo.applyTheme()
        recreateEvent.value = true
    }

    fun signOut() {
        viewModelScope.launch {
            agendaRepository.deleteAll()
            FirebaseAuth.getInstance().signOut()
            signOutEvent.value = true
        }
    }
}

class SettingsViewModelFactory(
    private val app: Application,
    private val repository: AgendaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(app, repository) as T
    }
}
