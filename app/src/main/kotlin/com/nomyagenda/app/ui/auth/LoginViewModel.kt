package com.nomyagenda.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

    fun signIn(email: String, password: String) {
        _authState.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                    ?: run { _authState.value = AuthResult.Error("Authentication failed"); return@launch }
                _authState.value = AuthResult.Success(user)
            } catch (e: Exception) {
                _authState.value = AuthResult.Error(e.localizedMessage ?: e.message ?: "Error")
            }
        }
    }

    fun register(email: String, password: String) {
        _authState.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                    ?: run { _authState.value = AuthResult.Error("Account creation failed"); return@launch }
                _authState.value = AuthResult.Success(user)
            } catch (e: Exception) {
                _authState.value = AuthResult.Error(e.localizedMessage ?: e.message ?: "Error")
            }
        }
    }
}
