package com.nomyagenda.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthResult.Loading -> {
                    binding.progressLogin.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                    binding.btnRegister.isEnabled = false
                    binding.textError.visibility = View.GONE
                }
                is AuthResult.Error -> {
                    binding.progressLogin.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    binding.btnRegister.isEnabled = true
                    binding.textError.text = state.message
                    binding.textError.visibility = View.VISIBLE
                }
                is AuthResult.Success -> {
                    binding.textError.visibility = View.GONE
                    val app = requireActivity().application as NomyAgendaApp
                    lifecycleScope.launch {
                        app.agendaRepository.syncFromFirestore(state.user.uid)
                        binding.progressLogin.visibility = View.GONE
                        findNavController().navigate(R.id.action_loginFragment_to_agendaFragment)
                    }
                }
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.editEmail.text?.toString()?.trim() ?: ""
            val password = binding.editPassword.text?.toString() ?: ""
            if (validateInput(email, password)) {
                viewModel.signIn(email, password)
            }
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.editEmail.text?.toString()?.trim() ?: ""
            val password = binding.editPassword.text?.toString() ?: ""
            if (validateInput(email, password)) {
                viewModel.register(email, password)
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var valid = true
        if (email.isEmpty()) {
            binding.inputLayoutEmail.error = getString(R.string.login_error_email_required)
            valid = false
        } else {
            binding.inputLayoutEmail.error = null
        }
        if (password.isEmpty()) {
            binding.inputLayoutPassword.error = getString(R.string.login_error_password_required)
            valid = false
        } else {
            binding.inputLayoutPassword.error = null
        }
        return valid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
