package com.ssba.strategic_savings_budget_app.models

import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UserViewModel : ViewModel() {

    // region Declarations
    // Fields for Sser Input
    val username = MutableLiveData<String>()
    val fullName = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val confirmPassword = MutableLiveData<String>()

    // Error States
    val usernameError = MutableLiveData<String?>()
    val fullNameError = MutableLiveData<String?>()
    val emailError = MutableLiveData<String?>()
    val passwordError = MutableLiveData<String?>()
    val confirmPasswordError = MutableLiveData<String?>()
    // endregion

    // Method to validate all fields
    fun validateAll(): Boolean {
        validateUsername()
        validateFullName()
        validateEmail()
        validatePassword()
        validateConfirmPassword()
        return usernameError.value == null &&
                fullNameError.value == null &&
                emailError.value == null &&
                passwordError.value == null &&
                confirmPasswordError.value == null
    }

    // Method to validate each field
    fun validateUsername() {
        when {
            username.value.isNullOrEmpty() -> {
                usernameError.value = "Username cannot be empty"
            }
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

    fun validatePassword() {
        when {
            password.value.isNullOrEmpty() -> {
                passwordError.value = "Password cannot be empty"
            }
            !password.value!!.matches(Regex("^(?=.*[A-Z])(?=.*\\d).+$")) -> {
                passwordError.value = "Password must contain an uppercase letter and a digit"
            }
            else -> {
                passwordError.value = null
            }
        }
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