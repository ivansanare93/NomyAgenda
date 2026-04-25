package com.nomyagenda.app.ui.lock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.databinding.FragmentLockSetupBinding

/**
 * Two-step pattern setup screen.
 *
 * Step 1: user draws the new pattern.
 * Step 2: user confirms by drawing the same pattern again.
 * On success: saves the pattern hash and navigates back.
 */
class LockSetupFragment : Fragment() {

    private var _binding: FragmentLockSetupBinding? = null
    private val binding get() = _binding!!

    private val lockManager get() =
        (requireActivity().application as NomyAgendaApp).lockManager

    private enum class Step { DRAW, CONFIRM }

    private var step = Step.DRAW
    private var firstPattern: List<Int> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLockSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterDrawStep()

        binding.patternSetupView.onPatternComplete = { pattern ->
            when (step) {
                Step.DRAW -> {
                    firstPattern = pattern
                    binding.patternSetupView.setState(PatternLockView.State.SUCCESS)
                    binding.patternSetupView.postDelayed({
                        if (_binding != null) enterConfirmStep()
                    }, 500)
                }
                Step.CONFIRM -> {
                    if (pattern == firstPattern) {
                        binding.patternSetupView.setState(PatternLockView.State.SUCCESS)
                        lockManager.savePattern(pattern)
                        binding.patternSetupView.postDelayed({
                            if (_binding != null) findNavController().popBackStack()
                        }, 500)
                    } else {
                        binding.patternSetupView.setState(PatternLockView.State.ERROR)
                        showError(getString(R.string.lock_setup_mismatch))
                        binding.patternSetupView.postDelayed({
                            if (_binding != null) {
                                binding.patternSetupView.reset()
                                hideError()
                                enterDrawStep()
                            }
                        }, 1_200)
                    }
                }
            }
        }
    }

    private fun enterDrawStep() {
        step = Step.DRAW
        firstPattern = emptyList()
        binding.textSetupStep.text = getString(R.string.lock_setup_draw)
        binding.textSetupHint.text = getString(R.string.lock_setup_hint_min)
        binding.patternSetupView.reset()
        hideError()
    }

    private fun enterConfirmStep() {
        step = Step.CONFIRM
        binding.textSetupStep.text = getString(R.string.lock_setup_confirm)
        binding.textSetupHint.text = getString(R.string.lock_setup_hint_confirm)
        binding.patternSetupView.reset()
        hideError()
    }

    private fun showError(message: String) {
        binding.textSetupError.text = message
        binding.textSetupError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.textSetupError.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
