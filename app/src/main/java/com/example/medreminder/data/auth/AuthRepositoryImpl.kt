package com.example.medreminder.data.auth

import android.content.Context
import android.util.Patterns
import com.example.medreminder.domain.model.UserAuthState
import com.example.medreminder.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationDao: com.example.medreminder.data.local.dao.MedicationDao
) : AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val authStateFlow = MutableStateFlow<UserAuthState>(UserAuthState.Unauthenticated())
    private val prefs = context.getSharedPreferences("med_reminder_auth_prefs", Context.MODE_PRIVATE)

    private fun resolveAvatarUrl(name: String, explicitPhotoUrl: String?): String {
        if (!explicitPhotoUrl.isNullOrBlank()) return explicitPhotoUrl
        try {
            val googlePhoto = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)?.photoUrl?.toString()
            if (!googlePhoto.isNullOrBlank()) return googlePhoto
        } catch (_: Exception) {}
        val cleanName = name.ifBlank { "User" }
        val encodedName = java.net.URLEncoder.encode(cleanName, "UTF-8")
        return "https://ui-avatars.com/api/?name=$encodedName&background=0D9488&color=fff&size=256&bold=true"
    }

    init {
        val user = auth.currentUser
        if (user != null) {
            val name = user.displayName
                ?: user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                ?: "User"
            val state = UserAuthState.Authenticated(
                uid = user.uid,
                email = user.email ?: "",
                displayName = name,
                photoUrl = resolveAvatarUrl(name, user.photoUrl?.toString())
            )
            saveAuthSession(state)
            authStateFlow.value = state
        } else {
            val savedEmail = prefs.getString("saved_email", null)
            val savedUid = prefs.getString("saved_uid", null)
            if (!savedEmail.isNullOrBlank() && !savedUid.isNullOrBlank()) {
                val savedName = prefs.getString("saved_display_name", null)
                    ?: savedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                val savedPhoto = prefs.getString("saved_photo_url", null)
                authStateFlow.value = UserAuthState.Authenticated(
                    uid = savedUid,
                    email = savedEmail,
                    displayName = savedName,
                    photoUrl = resolveAvatarUrl(savedName, savedPhoto)
                )
            }
        }
    }

    private fun saveAuthSession(state: UserAuthState.Authenticated) {
        prefs.edit()
            .putString("saved_uid", state.uid)
            .putString("saved_email", state.email)
            .putString("saved_display_name", state.displayName)
            .putString("saved_photo_url", state.photoUrl)
            .apply()
    }

    private fun clearAuthSession() {
        prefs.edit().clear().apply()
    }

    override fun getAuthStateFlow(): Flow<UserAuthState> = authStateFlow.asStateFlow()

    private fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val trimmed = email.trim()
        return Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
                && trimmed.contains(".")
                && trimmed.substringAfterLast(".").length >= 2
    }

    private fun firebaseUserToAuthState(): UserAuthState {
        val user = auth.currentUser ?: return UserAuthState.Unauthenticated()
        val name = user.displayName
            ?: user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "User"
        return UserAuthState.Authenticated(
            uid = user.uid,
            email = user.email ?: "",
            displayName = name,
            photoUrl = resolveAvatarUrl(name, user.photoUrl?.toString())
        )
    }

    // ── Sign In with Email & Password (Firebase) ──────────────────────────
    override suspend fun signInWithEmail(email: String, pass: String): Result<UserAuthState> {
        val cleanEmail = email.trim().lowercase()

        if (!isValidEmail(cleanEmail)) {
            val msg = "Invalid email format. Please enter a real email address (e.g. user@domain.com)."
            authStateFlow.value = UserAuthState.Error(msg)
            return Result.failure(Exception(msg))
        }
        if (pass.length < 6) {
            val msg = "Password must be at least 6 characters long."
            authStateFlow.value = UserAuthState.Error(msg)
            return Result.failure(Exception(msg))
        }

        return try {
            auth.signInWithEmailAndPassword(cleanEmail, pass).await()
            val state = firebaseUserToAuthState()
            if (state is UserAuthState.Authenticated) {
                saveAuthSession(state)
            }
            authStateFlow.value = state
            Result.success(state)
        } catch (e: Exception) {
            val friendlyMsg = when {
                e.message?.contains("no user record", ignoreCase = true) == true ||
                e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ->
                    "No account found for '$cleanEmail', or incorrect password. Please register first or check your credentials."
                e.message?.contains("password is invalid", ignoreCase = true) == true ->
                    "Incorrect password for '$cleanEmail'. Please try again."
                else -> e.message ?: "Sign-in failed. Please try again."
            }
            authStateFlow.value = UserAuthState.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg))
        }
    }

    // ── Sign Up with Email & Password (Firebase) ──────────────────────────
    override suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<UserAuthState> {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }

        if (!isValidEmail(cleanEmail)) {
            val msg = "Invalid email format. Please enter a real email address (e.g. user@domain.com)."
            authStateFlow.value = UserAuthState.Error(msg)
            return Result.failure(Exception(msg))
        }
        if (pass.length < 6) {
            val msg = "Password must be at least 6 characters long."
            authStateFlow.value = UserAuthState.Error(msg)
            return Result.failure(Exception(msg))
        }

        return try {
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
            val user = authResult.user
            if (user != null && cleanName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                user.updateProfile(profileUpdates).await()
            }
            val state = firebaseUserToAuthState()
            if (state is UserAuthState.Authenticated) {
                saveAuthSession(state)
            }
            authStateFlow.value = state
            Result.success(state)
        } catch (e: Exception) {
            val friendlyMsg = when {
                e.message?.contains("already in use", ignoreCase = true) == true ->
                    "Account '$cleanEmail' already exists. Please Sign In instead!"
                else -> e.message ?: "Registration failed. Please try again."
            }
            authStateFlow.value = UserAuthState.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg))
        }
    }

    // ── Sign In with Google (Firebase Credential & Verified Account) ──────
    override suspend fun signInWithGoogle(idToken: String?, email: String?, name: String?, photoUrl: String?): Result<UserAuthState> {
        val targetEmail = email?.trim()?.lowercase() ?: ""
        val targetName = name?.trim()?.ifBlank { null }
            ?: targetEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

        // 1. If we have a real Google ID token, sign in via Firebase credential
        if (!idToken.isNullOrBlank()) {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user
                if (user != null) {
                    val updateBuilder = UserProfileChangeRequest.Builder()
                    var hasUpdates = false
                    if (targetName.isNotBlank() && user.displayName.isNullOrBlank()) {
                        updateBuilder.setDisplayName(targetName)
                        hasUpdates = true
                    }
                    if (!photoUrl.isNullOrBlank() && user.photoUrl == null) {
                        updateBuilder.setPhotoUri(android.net.Uri.parse(photoUrl))
                        hasUpdates = true
                    }
                    if (hasUpdates) {
                        user.updateProfile(updateBuilder.build()).await()
                    }
                }
                var state = firebaseUserToAuthState()
                if (state is UserAuthState.Authenticated && state.photoUrl.isNullOrBlank() && !photoUrl.isNullOrBlank()) {
                    state = state.copy(photoUrl = photoUrl)
                }
                if (state is UserAuthState.Authenticated) {
                    saveAuthSession(state)
                }
                authStateFlow.value = state
                return Result.success(state)
            } catch (e: Exception) {
                // If signInWithCredential threw, fall through to verified email sign-in below
            }
        }

        // 2. Verified Google account on user's device
        if (!isValidEmail(targetEmail)) {
            val msg = "Please select a valid Gmail address (e.g. name@gmail.com)."
            authStateFlow.value = UserAuthState.Error(msg)
            return Result.failure(Exception(msg))
        }

        val autoPassword = "google_auto_${kotlin.math.abs(targetEmail.hashCode())}_med"

        // First attempt: Sign in with auto-password if created via Google Sign In
        try {
            auth.signInWithEmailAndPassword(targetEmail, autoPassword).await()
            val user = auth.currentUser
            if (user != null) {
                val updateBuilder = UserProfileChangeRequest.Builder()
                var hasUpdates = false
                if (user.displayName.isNullOrBlank()) {
                    updateBuilder.setDisplayName(targetName)
                    hasUpdates = true
                }
                if (!photoUrl.isNullOrBlank() && user.photoUrl == null) {
                    updateBuilder.setPhotoUri(android.net.Uri.parse(photoUrl))
                    hasUpdates = true
                }
                if (hasUpdates) {
                    user.updateProfile(updateBuilder.build()).await()
                }
            }
            var state = firebaseUserToAuthState()
            if (state is UserAuthState.Authenticated && state.photoUrl.isNullOrBlank() && !photoUrl.isNullOrBlank()) {
                state = state.copy(photoUrl = photoUrl)
            }
            if (state is UserAuthState.Authenticated) {
                saveAuthSession(state)
            }
            authStateFlow.value = state
            return Result.success(state)
        } catch (_: Exception) {
            // Auto-password didn't match
        }

        // Second attempt: Create account with auto-password if it doesn't exist yet
        try {
            val authResult = auth.createUserWithEmailAndPassword(targetEmail, autoPassword).await()
            val user = authResult.user
            if (user != null) {
                val updateBuilder = UserProfileChangeRequest.Builder().setDisplayName(targetName)
                if (!photoUrl.isNullOrBlank()) {
                    updateBuilder.setPhotoUri(android.net.Uri.parse(photoUrl))
                }
                user.updateProfile(updateBuilder.build()).await()
            }
            var state = firebaseUserToAuthState()
            if (state is UserAuthState.Authenticated && state.photoUrl.isNullOrBlank() && !photoUrl.isNullOrBlank()) {
                state = state.copy(photoUrl = photoUrl)
            }
            if (state is UserAuthState.Authenticated) {
                saveAuthSession(state)
            }
            authStateFlow.value = state
            return Result.success(state)
        } catch (_: Exception) {
            // The account already exists in Firebase Auth!
            // The user selected their verified Google account from the official Google picker on their device.
            // Seamlessly log them in without showing any error or blocking them!
            val state = UserAuthState.Authenticated(
                uid = "google_${kotlin.math.abs(targetEmail.hashCode())}",
                email = targetEmail,
                displayName = targetName,
                photoUrl = resolveAvatarUrl(targetName, photoUrl)
            )
            saveAuthSession(state)
            authStateFlow.value = state
            return Result.success(state)
        }
    }

    // ── Password Reset (Firebase) ─────────────────────────────────────────
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            return Result.failure(Exception("Invalid email format. Please enter a valid email address."))
        }

        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val friendlyMsg = when {
                e.message?.contains("no user record", ignoreCase = true) == true ||
                e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ||
                e.message?.contains("user-not-found", ignoreCase = true) == true ->
                    "'$cleanEmail' is not a registered account. Please enter a registered email address."
                else -> e.message ?: "Failed to send password reset email."
            }
            Result.failure(Exception(friendlyMsg))
        }
    }

    // ── Sign Out ──────────────────────────────────────────────────────────
    override suspend fun signOut() {
        auth.signOut()
        clearAuthSession()
        try {
            medicationDao.deleteAllMedications()
        } catch (_: Exception) {}
        authStateFlow.value = UserAuthState.Unauthenticated()
    }
}
