package com.example.condomanagement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.condomanagement.ui.DashboardScreen
import com.example.condomanagement.ui.ManageRoomsScreen
import com.example.condomanagement.ui.auth.LoginScreen
import com.example.condomanagement.ui.auth.RegisterScreen
import com.example.condomanagement.ui.theme.CondoManagementTheme
import com.example.condomanagement.viewmodel.AuthViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        setContent {
            CondoManagementTheme {

                val authViewModel: AuthViewModel = viewModel()
                val uiState by authViewModel.uiState.collectAsState()

                var currentScreen by remember {
                    mutableStateOf("login")
                }

                LaunchedEffect(Unit) {
                    authViewModel.restoreSession()
                }

                val userRole = uiState.userRole
                val isOwner = userRole.equals("owner", ignoreCase = true) ||
                        userRole.equals("admin", ignoreCase = true)

                // เลือกหน้าตามสถานะการเข้าสู่ระบบและบทบาท
                val screen = when {
                    uiState.loading -> "loading"

                    uiState.isLoggedIn && isOwner -> "manage_rooms"

                    uiState.isLoggedIn && userRole != null -> "dashboard"

                    else -> currentScreen
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F6F8)
                ) {

                    when (screen) {

                        "loading" -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF192F50)
                                )
                            }
                        }

                        "register" -> {
                            RegisterScreen(
                                authViewModel = authViewModel,
                                onRegisterSuccess = {
                                    currentScreen = "dashboard"
                                },
                                onBackToLogin = {
                                    authViewModel.clearError()
                                    currentScreen = "login"
                                }
                            )
                        }

                        // หน้าเจ้าของห้องและผู้ดูแลระบบ
                        "manage_rooms" -> {
                            ManageRoomsScreen(
                                onBack = {
                                    currentScreen = "manage_rooms"
                                },
                                onLogout = {
                                    currentScreen = "login"
                                    authViewModel.logout()
                                }
                            )
                        }

                        // Dashboard สำหรับผู้เช่า
                        "dashboard" -> {
                            DashboardScreen(
                                name = uiState.userName ?: "ผู้ใช้งาน",
                                role = userRole ?: "tenant",
                                onLogout = {
                                    currentScreen = "login"
                                    authViewModel.logout()
                                }
                            )
                        }

                        // หน้าเข้าสู่ระบบ
                        else -> {
                            LoginScreen(
                                authViewModel = authViewModel,
                                onLoginSuccess = {
                                    currentScreen = "dashboard"
                                },
                                onRegisterClick = {
                                    authViewModel.clearError()
                                    currentScreen = "register"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
