package com.example.medreminder.domain.model

sealed class UserAuthState {
    object Idle : UserAuthState()
    object Loading : UserAuthState()
    data class Authenticated(
        val uid: String,
        val email: String?,
        val displayName: String?,
        val photoUrl: String? = null
    ) : UserAuthState()
    data class Unauthenticated(val message: String? = null) : UserAuthState()
    data class Error(val message: String) : UserAuthState()
}
