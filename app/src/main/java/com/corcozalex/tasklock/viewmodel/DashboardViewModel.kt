package com.corcozalex.tasklock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corcozalex.tasklock.network.NetworkClient
import com.corcozalex.tasklock.network.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(val userProfile: UserProfile) : DashboardState()
    data class Error(val error: String) : DashboardState()
}

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            try {
                val profile = NetworkClient.api.getMyProfile()

                _uiState.value = DashboardState.Success(profile)
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error(e.message ?: "Failed to load profile. Are you sure the token is valid?")
            }
        }
    }
}