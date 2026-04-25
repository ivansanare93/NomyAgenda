package com.nomyagenda.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.data.repository.AgendaRepository
import com.nomyagenda.app.security.AppLockManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel(
    app: Application,
    private val agendaRepository: AgendaRepository
) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)
    private val lockManager = AppLockManager(app)

    val themeMode = MutableLiveData(settingsRepo.themeMode)
    val decorativeTheme = MutableLiveData(settingsRepo.decorativeTheme)
    val language = MutableLiveData(settingsRepo.language)
    val notificationsEnabled = MutableLiveData(settingsRepo.notificationsEnabled)
    val appBackground = MutableLiveData(settingsRepo.appBackground)

    val lockType = MutableLiveData(settingsRepo.lockType)

    /** true when the activity must recreate itself to apply a new decorative theme. */
    val recreateEvent = MutableLiveData(false)

    /** true when sign-out has completed and the UI should navigate to the login screen. */
    val signOutEvent = MutableLiveData(false)

    /** true when the user should be navigated to LockSetupFragment. */
    val navigateToLockSetup = MutableLiveData(false)

    /** Error string when biometric is unavailable; null otherwise. */
    val biometricError = MutableLiveData<String?>(null)

    fun consumeRecreateEvent() {
        recreateEvent.value = false
    }

    fun consumeSignOutEvent() {
        signOutEvent.value = false
    }

    fun consumeNavigateToLockSetup() {
        navigateToLockSetup.value = false
    }

    fun consumeBiometricError() {
        biometricError.value = null
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

    /**
     * Called when the user toggles the lock switch.
     * - Enabling with current type NONE defaults to PATTERN setup flow.
     * - Disabling clears all lock data.
     */
    fun setLockEnabled(enabled: Boolean) {
        if (!enabled) {
            lockManager.disableLock()
            lockType.value = SettingsRepository.LOCK_TYPE_NONE
        } else {
            // Default to pattern setup when first enabling
            navigateToLockSetup.value = true
        }
    }

    fun selectPatternLock() {
        navigateToLockSetup.value = true
    }

    fun selectBiometricLock() {
        val bm = BiometricManager.from(getApplication())
        if (bm.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            lockManager.enableBiometric()
            lockType.value = SettingsRepository.LOCK_TYPE_BIOMETRIC
        } else {
            biometricError.value = (getApplication() as android.app.Application)
                .getString(com.nomyagenda.app.R.string.lock_biometric_unavailable)
        }
    }

    /** Re-reads lock type from SharedPreferences (e.g. after returning from LockSetupFragment). */
    fun refreshLockType() {
        lockType.value = settingsRepo.lockType
    }

    fun signOut() {
        viewModelScope.launch {
            agendaRepository.deleteAll()
            FirebaseAuth.getInstance().signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            try {
                GoogleSignIn.getClient(getApplication(), gso).signOut().await()
            } catch (e: Exception) {
                android.util.Log.w("SettingsViewModel", "Google sign-out failed", e)
            }
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
