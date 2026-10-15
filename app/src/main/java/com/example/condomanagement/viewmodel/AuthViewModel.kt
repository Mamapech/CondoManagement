
package com.example.condomanagement.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condomanagement.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val userRole: String? = null,
    val userName: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = repository.currentUser != null
        )
    )

    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "กรุณากรอก Username และรหัสผ่าน"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loading = true,
                error = null
            )

            try {
                repository.login(username, password).getOrThrow()
                val user = repository.getUserProfile()
                Log.d("CondoManagement", "User role = '${user.role}'")
                _uiState.value = AuthUiState(
                    isLoggedIn = true,
                    userRole = user.role,
                    userName = user.name
                )
            } catch (e: Exception) {
                Log.e("CondoManagement", "Login failed", e)
                repository.logout()

                _uiState.value = AuthUiState(
                    error = e.message
                        ?: "เข้าสู่ระบบไม่สำเร็จ กรุณาตรวจสอบ Username และรหัสผ่าน"
                )
            }
        }
    }

    fun register(
        username: String,
        email: String,
        password: String
    ) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "กรุณากรอกข้อมูลให้ครบทุกช่อง"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loading = true,
                error = null
            )

            try {
                repository.register(
                    username = username,
                    email = email,
                    password = password
                ).getOrThrow()

                val user = repository.getUserProfile()

                _uiState.value = AuthUiState(
                    isLoggedIn = true,
                    userRole = user.role,
                    userName = user.name
                )
            } catch (e: Exception) {
                Log.e("CondoManagement", "Registration failed", e)

                _uiState.value = AuthUiState(
                    error = e.message
                        ?: "สมัครสมาชิกไม่สำเร็จ กรุณาลองอีกครั้ง"
                )
            }
        }
    }

    fun restoreSession() {
        if (repository.currentUser == null) {
            _uiState.value = AuthUiState()
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)

            try {
                val user = repository.getUserProfile()

                _uiState.value = AuthUiState(
                    isLoggedIn = true,
                    userRole = user.role,
                    userName = user.name
                )
            } catch (e: Exception) {
                Log.e("CondoManagement", "Restore session failed", e)
                repository.logout()

                _uiState.value = AuthUiState(
                    error = "ไม่สามารถโหลดข้อมูลผู้ใช้ได้ กรุณาเข้าสู่ระบบใหม่"
                )
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
