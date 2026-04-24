package com.nomyagenda.app.ui.auth

import android.app.Activity
import android.os.Bundle
import android.util.Log
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
            Log.d(TAG, "Google Sign-In result OK")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    Log.d(TAG, "Google idToken obtained; calling signInWithGoogle")
                    viewModel.signInWithGoogle(idToken)
                } else {
                    Log.w(TAG, "Google Sign-In idToken is null (check google-services.json / SHA-1 config)")
                    binding.progressLogin.visibility = View.GONE
                    binding.btnGoogleSignIn.isEnabled = true
                    binding.textError.text = getString(R.string.login_error_token_null)
                    binding.textError.visibility = View.VISIBLE
                }
            } catch (e: ApiException) {
                Log.w(TAG, "Google Sign-In ApiException statusCode=${e.statusCode}: ${e.localizedMessage}")
                binding.progressLogin.visibility = View.GONE
                binding.btnGoogleSignIn.isEnabled = true
                binding.textError.text = getString(
                    R.string.login_error_google_api,
                    e.statusCode,
                    e.localizedMessage ?: ""
                )
                binding.textError.visibility = View.VISIBLE
            }
        } else {
            Log.d(TAG, "Google Sign-In cancelled or failed resultCode=${result.resultCode}")
            binding.progressLogin.visibility = View.GONE
            binding.btnGoogleSignIn.isEnabled = true
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
                    binding.progressLogin.visibility = View.GONE
                    binding.textError.visibility = View.GONE
                    Log.d(TAG, "Firebase sign-in success uid=${state.user.uid}; navigating to agenda")
                    val uid = state.user.uid
                    val activityScope = requireActivity().lifecycleScope
                    val app = requireActivity().application as NomyAgendaApp
                    findNavController().navigate(R.id.action_loginFragment_to_agendaFragment)
                    activityScope.launch {
                        try {
                            app.agendaRepository.syncFromFirestore(uid)
                            Log.d(TAG, "syncFromFirestore success uid=$uid")
                        } catch (e: Exception) {
                            Log.w(TAG, "syncFromFirestore failed uid=$uid", e)
                        }
                    }
                }
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            Log.d(TAG, "Starting Google Sign-In flow")
            binding.textError.visibility = View.GONE
            binding.progressLogin.visibility = View.VISIBLE
            binding.btnGoogleSignIn.isEnabled = false
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}
