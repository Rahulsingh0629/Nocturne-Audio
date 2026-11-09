package com.nocturne.audio.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.nocturne.audio.data.repository.AuthRepository
import com.nocturne.audio.data.repository.AuthRepositoryImpl
import com.nocturne.audio.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val profile = UserProfile(
                        uid = uid,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: (auth.currentUser?.email ?: ""),
                        preferredLanguages = (doc.get("preferredLanguages") as? List<String>) ?: emptyList(),
                        preferredDecades = (doc.get("preferredDecades") as? List<String>) ?: emptyList(),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            editedName = profile.name,
                            selectedLanguages = profile.preferredLanguages.toSet(),
                            selectedDecades = profile.preferredDecades.toSet()
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(editedName = name) }
    }

    fun toggleLanguage(lang: String) {
        _uiState.update { state ->
            val set = state.selectedLanguages.toMutableSet()
            if (set.contains(lang)) set.remove(lang) else set.add(lang)
            state.copy(selectedLanguages = set)
        }
    }

    fun toggleDecade(decade: String) {
        _uiState.update { state ->
            val set = state.selectedDecades.toMutableSet()
            if (set.contains(decade)) set.remove(decade) else set.add(decade)
            state.copy(selectedDecades = set)
        }
    }

    fun saveProfileChanges() {
        val uid = auth.currentUser?.uid ?: return
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, errorMessage = null) }
            val updatedProfile = state.profile.copy(
                name = state.editedName.trim(),
                preferredLanguages = state.selectedLanguages.toList(),
                preferredDecades = state.selectedDecades.toList()
            )

            val result = authRepository.saveUserProfile(updatedProfile)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = updatedProfile,
                            message = "Profile updated successfully!"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    fun toggleChangePasswordDialog(show: Boolean) {
        _uiState.update { it.copy(showChangePasswordDialog = show, errorMessage = null, message = null) }
    }

    fun toggleDeleteAccountDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteAccountDialog = show, errorMessage = null, message = null) }
    }

    fun togglePrivacyPolicyDialog(show: Boolean) {
        _uiState.update { it.copy(showPrivacyPolicyDialog = show) }
    }

    fun changePassword(currentPass: String, newPass: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        if (newPass.length < 8) {
            _uiState.update { it.copy(errorMessage = "New password must be at least 8 characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val credential = EmailAuthProvider.getCredential(email, currentPass)
                user.reauthenticate(credential).await()
                user.updatePassword(newPass).await()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showChangePasswordDialog = false,
                        message = "Password changed successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Re-authentication failed. Please check your current password."
                    )
                }
            }
        }
    }

    fun deleteAccount(confirmPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val uid = user.uid

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // 1. Re-authenticate user before account deletion (Mandatory Section 8 rule)
                val credential = EmailAuthProvider.getCredential(email, confirmPassword)
                user.reauthenticate(credential).await()

                // 2. Delete Firestore profile and subcollections
                val userDocRef = firestore.collection("users").document(uid)
                val favs = userDocRef.collection("favorites").get().await()
                for (doc in favs.documents) doc.reference.delete().await()

                val playlists = userDocRef.collection("playlists").get().await()
                for (doc in playlists.documents) doc.reference.delete().await()

                val recents = userDocRef.collection("recentSearches").get().await()
                for (doc in recents.documents) doc.reference.delete().await()

                val uploads = userDocRef.collection("uploads").get().await()
                for (doc in uploads.documents) {
                    val path = doc.getString("storagePath")
                    if (!path.isNullOrBlank()) {
                        try { storage.reference.child(path).delete().await() } catch (_: Exception) {}
                    }
                    doc.reference.delete().await()
                }

                userDocRef.delete().await()

                // 3. Delete Auth User Account
                user.delete().await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showDeleteAccountDialog = false,
                        isAccountDeleted = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete account. Please verify your password."
                    )
                }
            }
        }
    }
}
