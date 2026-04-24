package com.nomyagenda.app.ui.auth

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken)
                } else {
                    binding.textError.text = getString(R.string.login_error_google_failed)
                    binding.textError.visibility = View.VISIBLE
                }
            } catch (e: ApiException) {
                binding.textError.text = getString(R.string.login_error_google_failed)
                binding.textError.visibility = View.VISIBLE
            }
        }
    }

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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthResult.Loading -> {
                    binding.progressLogin.visibility = View.VISIBLE
                    binding.btnGoogleSignIn.isEnabled = false
                    binding.textError.visibility = View.GONE
                }
                is AuthResult.Error -> {
                    binding.progressLogin.visibility = View.GONE
                    binding.btnGoogleSignIn.isEnabled = true
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

        binding.btnGoogleSignIn.setOnClickListener {
            binding.textError.visibility = View.GONE
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
