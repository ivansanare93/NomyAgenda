package com.nomyagenda.app.security

import android.content.Context
import com.nomyagenda.app.data.preferences.SettingsRepository
import java.security.MessageDigest
import java.util.UUID

/**
 * Manages the app-level lock state (pattern / biometric).
 *
 * Background logic:
 *  - [recordBackground] is called from MainActivity.onStop to note the exact instant
 *    the app left the foreground.
 *  - [shouldLock] returns true when the chosen lock type is active **and** the app has
 *    been in the background for longer than [SettingsRepository.lockBackgroundTimeoutMs].
 *  - After a successful unlock, [onUnlocked] resets the background timestamp so the
 *    user is not asked again until the next background→foreground cycle.
 */
class AppLockManager(context: Context) {

    private val settings = SettingsRepository(context.applicationContext)
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_LOCK_NAME, Context.MODE_PRIVATE)

    /** Epoch-ms when the app last moved to background; 0L means never or already unlocked. */
    private var backgroundTimestamp: Long = 0L

    /** Whether the screen is currently locked (pending authentication). */
    var isLocked: Boolean = false
        private set

    val lockType: String
        get() = settings.lockType

    fun recordBackground() {
        if (settings.lockType != SettingsRepository.LOCK_TYPE_NONE) {
            backgroundTimestamp = System.currentTimeMillis()
        }
    }

    /**
     * Returns true when the lock screen should be shown to the user.
     * Also transitions [isLocked] to true when criteria are met.
     */
    fun shouldLock(): Boolean {
        if (settings.lockType == SettingsRepository.LOCK_TYPE_NONE) return false
        if (backgroundTimestamp == 0L) return false
        val elapsed = System.currentTimeMillis() - backgroundTimestamp
        val timeout = settings.lockBackgroundTimeoutMs
        return if (elapsed >= timeout) {
            isLocked = true
            true
        } else {
            false
        }
    }

    /** Call after the user successfully authenticates. */
    fun onUnlocked() {
        isLocked = false
        backgroundTimestamp = 0L
    }

    /**
     * Verifies whether [pattern] (a list of dot indices) matches the stored hash.
     * Returns true on a match.
     */
    fun verifyPattern(pattern: List<Int>): Boolean {
        val stored = settings.lockPatternHash
        if (stored.isEmpty()) return false
        return hashPattern(pattern) == stored
    }

    /** Persists a new pattern hash and activates pattern lock. */
    fun savePattern(pattern: List<Int>) {
        settings.lockPatternHash = hashPattern(pattern)
        settings.lockType = SettingsRepository.LOCK_TYPE_PATTERN
    }

    /** Clears any saved pattern and disables pattern lock (unless biometric is set). */
    fun clearPattern() {
        settings.lockPatternHash = ""
        if (settings.lockType == SettingsRepository.LOCK_TYPE_PATTERN) {
            settings.lockType = SettingsRepository.LOCK_TYPE_NONE
        }
    }

    /** Activates biometric lock without a pattern. Clears any stored pattern hash. */
    fun enableBiometric() {
        settings.lockPatternHash = ""
        settings.lockType = SettingsRepository.LOCK_TYPE_BIOMETRIC
    }

    /** Disables all lock types. */
    fun disableLock() {
        settings.lockPatternHash = ""
        settings.lockType = SettingsRepository.LOCK_TYPE_NONE
        backgroundTimestamp = 0L
        isLocked = false
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a per-device salt stored in [PREFS_LOCK_NAME], creating one on first use.
     * This prevents rainbow-table attacks against the stored pattern hash.
     */
    private fun getOrCreateSalt(): String {
        val existing = prefs.getString(KEY_PATTERN_SALT, null)
        if (existing != null) return existing
        val newSalt = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_PATTERN_SALT, newSalt).apply()
        return newSalt
    }

    private fun hashPattern(pattern: List<Int>): String {
        val salt = getOrCreateSalt()
        val raw = salt + ":" + pattern.joinToString(",")
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_LOCK_NAME = "nomy_lock"
        private const val KEY_PATTERN_SALT = "pattern_salt"

        /** Number of failed unlock attempts before a temporary cooldown is imposed. */
        const val MAX_PATTERN_FAILURES = 5

        /** Duration of the cooldown period after [MAX_PATTERN_FAILURES] failed attempts. */
        const val FAILURE_COOLDOWN_MS = 30_000L
    }
}
