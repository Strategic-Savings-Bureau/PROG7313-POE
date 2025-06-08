package com.ssba.strategic_savings_budget_app.models

import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UserViewModel : ViewModel() {

    // region Declarations
    // Fields for user Input
    val username = MutableLiveData<String>()
    val fullName = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val currentPassword = MutableLiveData<String>() // ADDED: Current Password Field
    val password = MutableLiveData<String>()
    val confirmPassword = MutableLiveData<String>()

    // Error States
    val usernameError = MutableLiveData<String?>()
    val fullNameError = MutableLiveData<String?>()
    val emailError = MutableLiveData<String?>()
    val currentPasswordError = MutableLiveData<String?>() // ADDED: Current Password Error
    val passwordError = MutableLiveData<String?>()
    val confirmPasswordError = MutableLiveData<String?>()
    // endregion

    // Method to validate all fields (consider if you need to validate all for every save/update)
    fun validateAll(): Boolean {
        validateUsername()
        validateFullName()
        validateEmail()
        // Not calling password validation here as it's separate
        // validatePassword()
        // validateConfirmPassword()
        return usernameError.value == null &&
                fullNameError.value == null &&
                emailError.value == null
        // You might need to adjust this if validateAll is used for password updates
        // && passwordError.value == null &&
        // confirmPasswordError.value == null
    }

    // Method to validate each field
    fun validateUsername() {
        when {
            username.value.isNullOrEmpty() -> {
                usernameError.value = "Username cannot be empty"
            }
            // Ensure this regex is appropriate for your desired username format
            !username.value!!.matches(Regex("^(?=.*[A-Z])(?=.*\\d).+$")) -> {
                usernameError.value = "Username must contain an uppercase letter and a digit"
            }
            else -> {
                usernameError.value = null
            }
        }
    }

    fun validateFullName() {
        when {
            fullName.value.isNullOrEmpty() -> {
                fullNameError.value = "Full name cannot be empty"
            }
            else -> {
                fullNameError.value = null
            }
        }
    }

    fun validateEmail() {
        val emailPattern = Patterns.EMAIL_ADDRESS
        when {
            email.value.isNullOrEmpty() -> {
                emailError.value = "Email cannot be empty"
            }
            !emailPattern.matcher(email.value ?: "").matches() -> {
                emailError.value = "Invalid email format"
            }
            else -> {
                emailError.value = null
            }
        }
    }

    fun validateCurrentPassword() { // ADDED: Validation for Current Password
        currentPasswordError.value = if (currentPassword.value.isNullOrEmpty()) {
            "Current password is required."
        } else {
            null
        }
    }

    fun validatePassword() {
        when {
            password.value.isNullOrEmpty() -> {
                passwordError.value = "New password cannot be empty"
            }
            // Firebase recommends at least 6 characters. Your regex also adds complexity.
            !password.value!!.matches(Regex("^(?=.*[A-Z])(?=.*\\d).+$")) || password.value!!.length < 6 -> {
                passwordError.value = "Password must be at least 6 characters long and contain an uppercase letter and a digit"
            }
            else -> {
                passwordError.value = null
            }
        }
        // Always re-validate confirm password if the new password changes
        validateConfirmPassword()
    }

    fun validateConfirmPassword() {
        when {
            confirmPassword.value.isNullOrEmpty() -> {
                confirmPasswordError.value = "Confirm password cannot be empty"
            }
            confirmPassword.value != password.value -> {
                confirmPasswordError.value = "Passwords do not match"
            }
            else -> {
                confirmPasswordError.value = null
            }
        }
    }
}