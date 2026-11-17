package com.example.medreminder.domain.repository

import com.example.medreminder.domain.model.UserAuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getAuthStateFlow(): Flow<UserAuthState>
    suspend fun signInWithGoogle(idToken: String? = null, email: String? = null, name: String? = null, photoUrl: String? = null): Result<UserAuthState>
    suspend fun signInWithEmail(email: String, pass: String): Result<UserAuthState>
    suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<UserAuthState>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signOut()
}
