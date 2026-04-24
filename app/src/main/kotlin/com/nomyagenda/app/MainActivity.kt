package com.nomyagenda.app

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the decorative (thematic) style before any view inflation.
        val settings = SettingsRepository(this)
        val themeResId = when (settings.decorativeTheme) {
            SettingsRepository.DECORATIVE_THEME_OCEAN -> R.style.Theme_NomyAgenda_Ocean
            SettingsRepository.DECORATIVE_THEME_FOREST -> R.style.Theme_NomyAgenda_Forest
            SettingsRepository.DECORATIVE_THEME_SUNSET -> R.style.Theme_NomyAgenda_Sunset
            else -> 0
        }
        if (themeResId != 0) setTheme(themeResId)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Dynamically choose start destination based on auth state so there is no login-screen
        // flash when the user is already signed in.
        val currentUser = FirebaseAuth.getInstance().currentUser
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_graph)
        if (currentUser != null) {
            graph.setStartDestination(R.id.agendaFragment)
        }
        navController.graph = graph

        binding.bottomNavigation.setupWithNavController(navController)

        val topLevelDestinations = setOf(R.id.agendaFragment, R.id.settingsFragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.visibility =
                if (destination.id in topLevelDestinations) View.VISIBLE else View.GONE
        }

        // If already signed in, kick off a background sync so entries are up-to-date.
        if (currentUser != null) {
            lifecycleScope.launch {
                (application as NomyAgendaApp).agendaRepository
                    .syncFromFirestore(currentUser.uid)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // On Android 12 (API 31–32), SCHEDULE_EXACT_ALARM must be granted by the user via
        // system settings. Without it the OS delays alarms by several minutes.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.exact_alarm_rationale_title)
                    .setMessage(R.string.exact_alarm_rationale_message)
                    .setPositiveButton(R.string.exact_alarm_rationale_confirm) { _, _ ->
                        try {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        } catch (_: Exception) { /* settings screen not available */ }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }
}
