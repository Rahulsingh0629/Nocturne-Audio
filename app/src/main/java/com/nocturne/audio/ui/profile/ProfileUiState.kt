package com.nocturne.audio.ui.profile

import com.nocturne.audio.model.UserProfile

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isEditingName: Boolean = false,
    val editedName: String = "",
    val selectedLanguages: Set<String> = emptySet(),
    val selectedDecades: Set<String> = emptySet(),
    val showChangePasswordDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val showPrivacyPolicyDialog: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
    val isAccountDeleted: Boolean = false
)
