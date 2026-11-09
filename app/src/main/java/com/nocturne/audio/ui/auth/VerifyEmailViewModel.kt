package com.nocturne.audio.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.audio.data.repository.AuthRepository
import com.nocturne.audio.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerifyEmailViewModel(
    private val repository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VerifyEmailUiState(
            email = repository.getCurrentUser()?.email ?: ""
        )
    )
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        checkVerificationStatus()
    }

    fun checkVerificationStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, message = null) }
            val result = repository.reloadUser()

            result.fold(
                onSuccess = { user ->
                    val isVerified = user?.isEmailVerified == true
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            isVerified = isVerified,
                            message = if (!isVerified) "Email is not verified yet. Please check your inbox and click the verification link." else null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            message = "Error checking verification status: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun resendVerificationEmail() {
        if (_uiState.value.resendCooldown > 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, message = null) }
            val result = repository.sendEmailVerification()

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            message = "Verification email resent successfully! Please check your inbox."
                        )
                    }
                    startCooldownTimer(60)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            message = e.message ?: "Failed to resend verification email."
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
                _uiState.update { it.copy(resendCooldown = i) }
                delay(1000)
            }
            _uiState.update { it.copy(resendCooldown = 0) }
        }
    }
}
