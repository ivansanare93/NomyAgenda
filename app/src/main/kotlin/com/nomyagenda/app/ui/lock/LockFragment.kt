package com.nomyagenda.app.ui.lock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.databinding.FragmentLockBinding
import com.nomyagenda.app.security.AppLockManager

class LockFragment : Fragment() {

    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!

    private val lockManager get() =
        (requireActivity().application as NomyAgendaApp).lockManager

    private var failCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (lockManager.lockType) {
            SettingsRepository.LOCK_TYPE_BIOMETRIC -> setupBiometric()
            SettingsRepository.LOCK_TYPE_PATTERN -> setupPattern()
            else -> navigateToAgenda()
        }
    }

    // ── Biometric ─────────────────────────────────────────────────────────────

    private fun setupBiometric() {
        binding.textLockMessage.text = getString(R.string.lock_biometric_prompt_subtitle)
        binding.patternLockView.visibility = View.GONE
        binding.btnUseBiometric.visibility = View.GONE
        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(requireContext())
        val canAuth = biometricManager.canAuthenticate(BIOMETRIC_STRONG)

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // Biometric not available – fallback to pattern if configured
            fallbackToPattern()
            return
        }

        val executor = ContextCompat.getMainExecutor(requireContext())
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Offer pattern fallback if the user has one saved
                    fallbackToPattern()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    showError(getString(R.string.lock_error_biometric_failed))
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_biometric_prompt_title))
            .setSubtitle(getString(R.string.lock_biometric_prompt_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun fallbackToPattern() {
        if ((requireActivity().application as NomyAgendaApp)
                .lockManager.lockType == SettingsRepository.LOCK_TYPE_BIOMETRIC
        ) {
            // Biometric-only mode – just show a retry button
            binding.textLockMessage.text = getString(R.string.lock_biometric_unavailable)
            binding.btnUseBiometric.visibility = View.VISIBLE
            binding.btnUseBiometric.setOnClickListener { showBiometricPrompt() }
        } else {
            setupPattern()
        }
    }

    // ── Pattern ───────────────────────────────────────────────────────────────

    private fun setupPattern() {
        binding.textLockMessage.text = getString(R.string.lock_draw_pattern)
        binding.patternLockView.visibility = View.VISIBLE
        binding.btnUseBiometric.visibility = View.GONE

        binding.patternLockView.onPatternComplete = { pattern ->
            if (lockManager.verifyPattern(pattern)) {
                binding.patternLockView.setState(PatternLockView.State.SUCCESS)
                binding.textLockError.visibility = View.GONE
                binding.patternLockView.postDelayed({ onUnlocked() }, 400)
            } else {
                failCount++
                binding.patternLockView.setState(PatternLockView.State.ERROR)
                if (failCount >= AppLockManager.MAX_PATTERN_FAILURES) {
                    showError(getString(R.string.lock_error_too_many))
                    binding.patternLockView.isEnabled = false
                    binding.patternLockView.postDelayed({
                        failCount = 0
                        binding.patternLockView.isEnabled = true
                        binding.patternLockView.reset()
                        binding.textLockError.visibility = View.GONE
                    }, AppLockManager.FAILURE_COOLDOWN_MS)
                } else {
                    showError(getString(R.string.lock_error_wrong_pattern))
                    binding.patternLockView.postDelayed({
                        binding.patternLockView.reset()
                        binding.textLockError.visibility = View.GONE
                    }, 1_200)
                }
            }
        }
    }

    // ── Common ────────────────────────────────────────────────────────────────

    private fun showError(message: String) {
        binding.textLockError.text = message
        binding.textLockError.visibility = View.VISIBLE
    }

    private fun onUnlocked() {
        lockManager.onUnlocked()
        navigateToAgenda()
    }

    private fun navigateToAgenda() {
        findNavController().navigate(
            R.id.agendaFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(findNavController().graph.id, true)
                .build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
