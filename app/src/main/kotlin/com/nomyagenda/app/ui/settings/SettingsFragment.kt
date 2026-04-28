package com.nomyagenda.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.databinding.FragmentSettingsBinding
import com.nomyagenda.app.ui.lock.LockSetupFragment

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var isUpdatingLockUi = false

    private val viewModel: SettingsViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        SettingsViewModelFactory(app, app.agendaRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.themeMode.observe(viewLifecycleOwner) { mode ->
            val btnId = when (mode) {
                SettingsRepository.THEME_LIGHT -> R.id.btn_theme_light
                SettingsRepository.THEME_DARK -> R.id.btn_theme_dark
                else -> R.id.btn_theme_system
            }
            if (binding.toggleTheme.checkedButtonId != btnId) {
                binding.toggleTheme.check(btnId)
            }
        }

        viewModel.decorativeTheme.observe(viewLifecycleOwner) { theme ->
            val chipId = when (theme) {
                SettingsRepository.DECORATIVE_THEME_OCEAN -> R.id.chip_theme_ocean
                SettingsRepository.DECORATIVE_THEME_FOREST -> R.id.chip_theme_forest
                SettingsRepository.DECORATIVE_THEME_SUNSET -> R.id.chip_theme_sunset
                else -> R.id.chip_theme_lavanda
            }
            val chip = binding.chipGroupDecorativeTheme.findViewById<View>(chipId)
            if (chip != null && !binding.chipGroupDecorativeTheme.checkedChipIds.contains(chipId)) {
                binding.chipGroupDecorativeTheme.check(chipId)
            }
        }

        viewModel.language.observe(viewLifecycleOwner) { lang ->
            val btnId = when (lang) {
                SettingsRepository.LANGUAGE_ES -> R.id.btn_lang_es
                SettingsRepository.LANGUAGE_EN -> R.id.btn_lang_en
                else -> R.id.btn_lang_system
            }
            if (binding.toggleLanguage.checkedButtonId != btnId) {
                binding.toggleLanguage.check(btnId)
            }
        }

        viewModel.notificationsEnabled.observe(viewLifecycleOwner) { enabled ->
            if (binding.switchNotifications.isChecked != enabled) {
                binding.switchNotifications.isChecked = enabled
            }
        }

        viewModel.appBackground.observe(viewLifecycleOwner) { bg ->
            val chipId = when (bg) {
                SettingsRepository.APP_BACKGROUND_FLORAL -> R.id.chip_bg_floral
                SettingsRepository.APP_BACKGROUND_STARS -> R.id.chip_bg_stars
                SettingsRepository.APP_BACKGROUND_GEOMETRIC -> R.id.chip_bg_geometric
                SettingsRepository.APP_BACKGROUND_DOTS -> R.id.chip_bg_dots
                SettingsRepository.APP_BACKGROUND_LEAVES -> R.id.chip_bg_leaves
                SettingsRepository.APP_BACKGROUND_BUTTERFLY -> R.id.chip_bg_butterfly
                SettingsRepository.APP_BACKGROUND_MANDALA -> R.id.chip_bg_mandala
                SettingsRepository.APP_BACKGROUND_MOUNTAINS -> R.id.chip_bg_mountains
                SettingsRepository.APP_BACKGROUND_WAVES -> R.id.chip_bg_waves
                else -> R.id.chip_bg_none
            }
            val chip = binding.chipGroupBackground.findViewById<View>(chipId)
            if (chip != null && !binding.chipGroupBackground.checkedChipIds.contains(chipId)) {
                binding.chipGroupBackground.check(chipId)
            }
        }

        viewModel.recreateEvent.observe(viewLifecycleOwner) { shouldRecreate ->
            if (shouldRecreate) {
                viewModel.consumeRecreateEvent()
                requireActivity().recreate()
            }
        }

        viewModel.signOutEvent.observe(viewLifecycleOwner) { shouldSignOut ->
            if (shouldSignOut) {
                viewModel.consumeSignOutEvent()
                findNavController().navigate(
                    R.id.loginFragment,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(findNavController().graph.id, true)
                        .build()
                )
            }
        }

        viewModel.signOutError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                binding.textBiometricError.text = error
                binding.textBiometricError.visibility = View.VISIBLE
                viewModel.consumeSignOutError()
            }
        }

        // ── Security / Lock section ──────────────────────────────────────────

        viewModel.lockType.observe(viewLifecycleOwner) { type ->
            isUpdatingLockUi = true
            val lockEnabled = type != SettingsRepository.LOCK_TYPE_NONE
            if (binding.switchLock.isChecked != lockEnabled) {
                binding.switchLock.isChecked = lockEnabled
            }
            binding.layoutLockOptions.visibility = if (lockEnabled) View.VISIBLE else View.GONE

            val btnId = when (type) {
                SettingsRepository.LOCK_TYPE_BIOMETRIC -> R.id.btn_lock_biometric
                else -> R.id.btn_lock_pattern
            }
            if (binding.toggleLockType.checkedButtonId != btnId) {
                binding.toggleLockType.check(btnId)
            }
            binding.btnSetupPattern.visibility =
                if (type == SettingsRepository.LOCK_TYPE_PATTERN) View.VISIBLE else View.GONE
            isUpdatingLockUi = false
        }

        viewModel.navigateToLockSetup.observe(viewLifecycleOwner) { go ->
            if (go) {
                viewModel.consumeNavigateToLockSetup()
                findNavController().navigate(R.id.action_settingsFragment_to_lockSetupFragment)
            }
        }

        // Refresh the lock type when returning from LockSetupFragment after a successful setup.
        parentFragmentManager.setFragmentResultListener(
            LockSetupFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            viewModel.refreshLockType()
        }

        viewModel.biometricError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.textBiometricError.text = error
                binding.textBiometricError.visibility = View.VISIBLE
                viewModel.consumeBiometricError()
            } else {
                binding.textBiometricError.visibility = View.GONE
            }
        }

        binding.switchLock.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingLockUi) viewModel.setLockEnabled(isChecked)
        }

        binding.toggleLockType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !isUpdatingLockUi) {
                when (checkedId) {
                    R.id.btn_lock_pattern -> {
                        // Only navigate to setup if not already in pattern mode.
                        // Re-setup is handled by the dedicated "Configurar patrón" button.
                        if (viewModel.lockType.value != SettingsRepository.LOCK_TYPE_PATTERN) {
                            viewModel.selectPatternLock()
                        }
                    }
                    R.id.btn_lock_biometric -> viewModel.selectBiometricLock()
                }
            }
        }

        binding.btnSetupPattern.setOnClickListener {
            viewModel.selectPatternLock()
        }

        // ── Account ──────────────────────────────────────────────────────────
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            binding.textAccountEmail.text = currentUser.email
            binding.cardAccount.visibility = View.VISIBLE
        } else {
            binding.cardAccount.visibility = View.GONE
        }

        binding.btnSignOut.setOnClickListener {
            viewModel.signOut()
        }

        binding.toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.btn_theme_light -> SettingsRepository.THEME_LIGHT
                    R.id.btn_theme_dark -> SettingsRepository.THEME_DARK
                    else -> SettingsRepository.THEME_SYSTEM
                }
                viewModel.setTheme(mode)
            }
        }

        binding.chipGroupDecorativeTheme.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val theme = when (checkedIds.first()) {
                    R.id.chip_theme_ocean -> SettingsRepository.DECORATIVE_THEME_OCEAN
                    R.id.chip_theme_forest -> SettingsRepository.DECORATIVE_THEME_FOREST
                    R.id.chip_theme_sunset -> SettingsRepository.DECORATIVE_THEME_SUNSET
                    else -> SettingsRepository.DECORATIVE_THEME_DEFAULT
                }
                viewModel.setDecorativeTheme(theme)
            }
        }

        binding.toggleLanguage.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val lang = when (checkedId) {
                    R.id.btn_lang_es -> SettingsRepository.LANGUAGE_ES
                    R.id.btn_lang_en -> SettingsRepository.LANGUAGE_EN
                    else -> SettingsRepository.LANGUAGE_SYSTEM
                }
                viewModel.setLanguage(lang)
            }
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setNotificationsEnabled(isChecked)
        }

        binding.chipGroupBackground.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val bg = when (checkedIds.first()) {
                    R.id.chip_bg_floral -> SettingsRepository.APP_BACKGROUND_FLORAL
                    R.id.chip_bg_stars -> SettingsRepository.APP_BACKGROUND_STARS
                    R.id.chip_bg_geometric -> SettingsRepository.APP_BACKGROUND_GEOMETRIC
                    R.id.chip_bg_dots -> SettingsRepository.APP_BACKGROUND_DOTS
                    R.id.chip_bg_leaves -> SettingsRepository.APP_BACKGROUND_LEAVES
                    R.id.chip_bg_butterfly -> SettingsRepository.APP_BACKGROUND_BUTTERFLY
                    R.id.chip_bg_mandala -> SettingsRepository.APP_BACKGROUND_MANDALA
                    R.id.chip_bg_mountains -> SettingsRepository.APP_BACKGROUND_MOUNTAINS
                    R.id.chip_bg_waves -> SettingsRepository.APP_BACKGROUND_WAVES
                    else -> SettingsRepository.APP_BACKGROUND_NONE
                }
                viewModel.setAppBackground(bg)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
