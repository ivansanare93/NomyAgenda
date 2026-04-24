package com.nomyagenda.app.ui.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    object Loading : AuthResult()
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableLiveData<AuthResult>()
    val authState: LiveData<AuthResult> = _authState

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthResult.Loading
        viewModelScope.launch {
            Log.d(TAG, "Attempting Firebase sign-in with Google credential")
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                if (user == null) {
                    Log.w(TAG, "Firebase sign-in returned null user")
                    _authState.value = AuthResult.Error("Authentication failed")
                    return@launch
                }
                Log.d(TAG, "Firebase sign-in success uid=${user.uid}")
                _authState.value = AuthResult.Success(user)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sign-in failed: ${e.localizedMessage}", e)
                _authState.value = AuthResult.Error(e.localizedMessage ?: e.message ?: "Error")
            }
        }
    }

    companion object {
        private const val TAG = "LoginViewModel"
    }
}
