package com.nocturne.audio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.nocturne.audio.ui.screens.LoginScreen
import com.nocturne.audio.ui.screens.RegisterScreen
import com.nocturne.audio.ui.screens.VerifyEmailScreen

@Composable
fun AppNavigation() {
    val rootNavController = rememberNavController()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestination = if (currentUser != null && currentUser.isEmailVerified) "main" else "login"

    NavHost(
        navController = rootNavController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null && !user.isEmailVerified) {
                        rootNavController.navigate("verify_email") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        rootNavController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onGoRegister = {
                    rootNavController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    rootNavController.navigate("verify_email") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onGoLogin = {
                    rootNavController.popBackStack()
                }
            )
        }

        composable("verify_email") {
            VerifyEmailScreen(
                onVerified = {
                    rootNavController.navigate("main") {
                        popUpTo("verify_email") { inclusive = true }
                    }
                },
                onSignOut = {
                    FirebaseAuth.getInstance().signOut()
                    rootNavController.navigate("login") {
                        popUpTo("verify_email") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainContainer(
                rootNavController = rootNavController,
                onSignOut = {
                    FirebaseAuth.getInstance().signOut()
                    rootNavController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
