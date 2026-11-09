package com.nocturne.audio.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.nocturne.audio.data.repository.AuthRepository
import com.nocturne.audio.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = null,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                errorMessage = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onForgotPasswordClicked(show: Boolean) {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = show,
                resetEmailMessage = null
            )
        }
    }

    fun sendForgotPassword(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.update { it.copy(resetEmailMessage = "Please enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.sendPasswordReset(trimmedEmail)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    resetEmailMessage = "If an account exists for $trimmedEmail, a reset email has been sent."
                )
            }
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.cooldownSecondsRemaining > 0) return

        var hasError = false
        var emailErr: String? = null
        var passwordErr: String? = null

        val trimmedEmail = state.email.trim()
        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailErr = "Please enter a valid email address"
            hasError = true
        }

        if (state.password.length < 8) {
            passwordErr = "Password must be at least 8 characters"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    emailError = emailErr,
                    passwordError = passwordErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.loginWithEmail(trimmedEmail, state.password)

            result.fold(
                onSuccess = { user ->
                    if (!user.isEmailVerified) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                needsEmailVerification = true,
                                password = "" // Clear password from state
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                failedAttempts = 0,
                                password = "" // Clear password from state
                            )
                        }
                    }
                },
                onFailure = {
                    val newFailedAttempts = state.failedAttempts + 1
                    val shouldCooldown = newFailedAttempts >= 5

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = if (shouldCooldown) {
                                "Too many failed attempts. Please wait 30 seconds."
                            } else {
                                "Incorrect email or password." // Generic error message
                            },
                            failedAttempts = newFailedAttempts,
                            password = "" // Do not store password in state after failed attempt
                        )
                    }

                    if (shouldCooldown) {
                        startCooldownTimer(30)
                    }
                }
            )
        }
    }

    fun handleGoogleSignInResult(credential: AuthCredential) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.signInWithGoogleCredential(credential)

            result.fold(
                onSuccess = { user ->
                    // For Google Sign In, profile is saved or verified
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            failedAttempts = 0,
                            password = ""
                        )
                    }
                },
                onFailure = {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = "Google Sign-In failed. Please try again."
                        )
                    }
                }
            )
        }
    }

    private fun startCooldownTimer(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _uiState.update { it.copy(cooldownSecondsRemaining = i) }
                delay(1000)
            }
            _uiState.update {
                it.copy(
                    cooldownSecondsRemaining = 0,
                    failedAttempts = 0,
                    errorMessage = null
                )
            }
        }
    }

    fun clearSensitiveData() {
        _uiState.update {
            it.copy(
                password = "",
                errorMessage = null,
                emailError = null,
                passwordError = null
            )
        }
    }
}
