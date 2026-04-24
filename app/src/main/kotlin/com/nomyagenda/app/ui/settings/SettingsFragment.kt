package com.nomyagenda.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nomyagenda.app.R
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

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

        viewModel.recreateEvent.observe(viewLifecycleOwner) { shouldRecreate ->
            if (shouldRecreate) {
                requireActivity().recreate()
            }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
