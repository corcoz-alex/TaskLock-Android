package com.corcozalex.tasklock.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corcozalex.tasklock.network.NetworkClient
import com.corcozalex.tasklock.network.RegisterRequest
import com.corcozalex.tasklock.network.TokenManager
import com.corcozalex.tasklock.network.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle: AuthState()
    object Loading: AuthState()
    data class Success(val message: String): AuthState()
    data class Error(val error: String): AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    // the memory vaults
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    private val tokenManager = TokenManager(application.dataStore)

    // the action
    fun login(email: String, password: String) {
        // instantly tell the ui to show a loading state
        _authState.value = AuthState.Loading

        // launch a background thread so the app doesn't freeze
        viewModelScope.launch {
            try {
                // fire the request to the server
                val response = NetworkClient.api.login(email, password)
                // Succes! Save the token in the vault
                tokenManager.saveToken(response.access_token)
                // Tell the UI we did it
                _authState.value = AuthState.Success("Logged in successfully!")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Invalid credentials or network error")
            }
        }
    }

    fun logout() {
        // launch background thread
        viewModelScope.launch {
            // Clear the vault
            tokenManager.clearToken()
            // Tell the UI we did it
            _authState.value = AuthState.Idle
        }
    }

    fun register(email: String, password: String){
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                // pack the data into the RegisterRequest format
                val request = RegisterRequest(email, password)
                // fire the request to the server
                NetworkClient.api.register(request)
                // Tell the UI we did it
                _authState.value = AuthState.Success("Account created successfully!! Please log in.")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed. Email might already be in use or network error.")
            }
        }
    }
}