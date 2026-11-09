package com.nocturne.audio.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.audio.data.repository.AuthRepository
import com.nocturne.audio.data.repository.AuthRepositoryImpl
import com.nocturne.audio.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val repository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChanged(name: String) {
        _uiState.update { it.copy(fullName = name, fullNameError = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        val strength = calculatePasswordStrength(password)
        _uiState.update {
            it.copy(
                password = password,
                passwordStrength = strength,
                passwordError = null
            )
        }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPassword = password, confirmPasswordError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun toggleLanguageSelection(language: String) {
        _uiState.update { state ->
            val current = state.selectedLanguages.toMutableSet()
            if (current.contains(language)) {
                if (current.size > 1) current.remove(language) // Keep at least one
            } else {
                current.add(language)
            }
            state.copy(selectedLanguages = current)
        }
    }

    fun toggleDecadeSelection(decade: String) {
        _uiState.update { state ->
            val current = state.selectedDecades.toMutableSet()
            if (current.contains(decade)) {
                if (current.size > 1) current.remove(decade) // Keep at least one
            } else {
                current.add(decade)
            }
            state.copy(selectedDecades = current)
        }
    }

    fun proceedToStep2(): Boolean {
        val state = _uiState.value
        var hasError = false
        var nameErr: String? = null
        var emailErr: String? = null
        var passErr: String? = null
        var confirmErr: String? = null

        if (state.fullName.trim().isBlank()) {
            nameErr = "Please enter your full name"
            hasError = true
        }

        val trimmedEmail = state.email.trim()
        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailErr = "Please enter a valid email address"
            hasError = true
        }

        if (state.password.length < 8) {
            passErr = "Password must be at least 8 characters"
            hasError = true
        }

        if (state.password != state.confirmPassword) {
            confirmErr = "Passwords do not match"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    fullNameError = nameErr,
                    emailError = emailErr,
                    passwordError = passErr,
                    confirmPasswordError = confirmErr
                )
            }
            return false
        }

        // Advance to Step 2 without creating Firebase Auth account yet
        _uiState.update { it.copy(currentStep = 2, errorMessage = null) }
        return true
    }

    fun backToStep1() {
        _uiState.update { it.copy(currentStep = 1, errorMessage = null) }
    }

    fun createAccount() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Create Auth user
            val regResult = repository.registerWithEmail(state.email.trim(), state.password)

            regResult.fold(
                onSuccess = { user ->
                    // 2. Send email verification
                    repository.sendEmailVerification()

                    // 3. Save profile to Firestore
                    val profile = UserProfile(
                        uid = user.uid,
                        name = state.fullName.trim(),
                        email = state.email.trim(),
                        preferredLanguages = state.selectedLanguages.toList(),
                        preferredDecades = state.selectedDecades.toList(),
                        createdAt = System.currentTimeMillis()
                    )
                    repository.saveUserProfile(profile)

                    // 4. Update UI state
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            password = "",
                            confirmPassword = ""
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to create account. Please try again."
                        )
                    }
                }
            )
        }
    }

    private fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.length < 8) return if (password.isEmpty()) PasswordStrength.NONE else PasswordStrength.WEAK

        var score = 0
        if (password.contains(Regex("[0-9]"))) score++
        if (password.contains(Regex("[A-Z]"))) score++
        if (password.contains(Regex("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]"))) score++

        return when {
            score >= 2 -> PasswordStrength.STRONG
            score == 1 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}
