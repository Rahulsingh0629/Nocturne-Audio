package com.nocturne.audio.ui.auth

enum class PasswordStrength {
    NONE,
    WEAK,
    MEDIUM,
    STRONG
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val failedAttempts: Int = 0,
    val cooldownSecondsRemaining: Int = 0,
    val showForgotPasswordDialog: Boolean = false,
    val resetEmailMessage: String? = null,
    val isSuccess: Boolean = false,
    val needsEmailVerification: Boolean = false
)

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.NONE,
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val currentStep: Int = 1,
    val selectedLanguages: Set<String> = setOf("English", "Hindi"),
    val selectedDecades: Set<String> = setOf("2020s", "2010s"),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

data class VerifyEmailUiState(
    val email: String = "",
    val isChecking: Boolean = false,
    val resendCooldown: Int = 0,
    val message: String? = null,
    val isVerified: Boolean = false
)
