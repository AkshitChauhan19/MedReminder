package com.example.medreminder.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medreminder.domain.model.UserAuthState
import com.example.medreminder.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<UserAuthState> = authRepository.getAuthStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserAuthState.Loading
        )

    private val _uiError = MutableStateFlow<String?>(null)
    val uiError: StateFlow<String?> = _uiError.asStateFlow()

    fun signInWithEmail(email: String, pass: String) {
        _uiError.value = null
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, pass)
            result.onFailure {
                _uiError.value = it.message
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String) {
        _uiError.value = null
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(email, pass, name)
            result.onFailure {
                _uiError.value = it.message
            }
        }
    }

    fun signInWithGoogle(idToken: String? = null, email: String? = null, name: String? = null, photoUrl: String? = null) {
        _uiError.value = null
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken, email, name, photoUrl)
            result.onFailure {
                _uiError.value = it.message
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            result.onSuccess {
                onResult(true, "Password reset email sent to $email! Please check your inbox.")
            }.onFailure {
                onResult(false, it.message ?: "Failed to send password reset email.")
            }
        }
    }

    fun clearError() {
        _uiError.value = null
    }

    fun signOut() {
        _uiError.value = null
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
