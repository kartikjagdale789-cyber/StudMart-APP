package com.kartik.studentmart.viewmodel

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.studentmart.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val isLoggedIn: Boolean
        get() = repository.currentUser != null

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            errorMessage = "Email and password cannot be empty."
            return
        }
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            val result = repository.login(email.trim(), pass)
            isLoading = false
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { err -> errorMessage = err.localizedMessage ?: "Login failed" }
            )
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        pass: String,
        confirmPass: String,
        onSuccess: () -> Unit
    ) {
        if (fullName.isBlank()) {
            errorMessage = "Full name cannot be empty."
            return
        }
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Please enter a valid email address."
            return
        }
        if (phone.isBlank() || phone.length < 10) {
            errorMessage = "Please enter a valid phone number."
            return
        }
        if (pass.isBlank() || pass.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }
        if (pass != confirmPass) {
            errorMessage = "Passwords do not match."
            return
        }

        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            val result = repository.register(fullName.trim(), email.trim(), phone.trim(), pass)
            isLoading = false
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { err -> errorMessage = err.localizedMessage ?: "Registration failed" }
            )
        }
    }

    fun logout(onSuccess: () -> Unit) {
        repository.logout()
        onSuccess()
    }
}
