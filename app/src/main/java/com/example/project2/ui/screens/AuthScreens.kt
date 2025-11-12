package com.example.project2.ui.screens

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project2.R
import com.example.project2.data.AuthRepository

// ====================== LOGIN SCREEN ======================
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLogin: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
    onSecretAccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val authRepo = AuthRepository()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val secretInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo1),
                contentDescription = "MindMatch logo",
                modifier = Modifier.size(200.dp)
            )

            Text(
                text = "Sign in to keep your progress, saved puzzles, and community streaks in sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    authRepo.login(email, password) { success, error ->
                        if (success) {
                            val uid = authRepo.getCurrentUserId() ?: return@login
                            authRepo.loadUserProfile(uid) { profile, err ->
                                if (profile != null) {
                                    Toast.makeText(context, "Welcome back, ${profile.displayName}!", Toast.LENGTH_SHORT).show()
                                    Log.d("Auth", "Welcome back, ${profile.displayName}")
                                    onLogin() // Navigate to dashboard
                                } else {
                                    Toast.makeText(context, "Error loading profile: $err", Toast.LENGTH_SHORT).show()
                                    Log.e("Auth", "Failed to load profile: $err")
                                }
                            }
                        } else {
                            Toast.makeText(context, error ?: "Login failed", Toast.LENGTH_SHORT).show()
                            Log.e("Auth", "Login failed: $error")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log in")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onCreateAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create an account")
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(48.dp)
                    .clickable(
                        interactionSource = secretInteractionSource,
                        indication = null,
                        onClick = onSecretAccess
                    )
            )
        }
    }
}

// ====================== CREATE ACCOUNT SCREEN ======================
@Composable
fun CreateAccountScreen(
    modifier: Modifier = Modifier,
    onCreateAccount: () -> Unit = {},
    onBackToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val authRepo = AuthRepository()
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo1),
                contentDescription = "MindMatch logo",
                modifier = Modifier.size(200.dp)
            )

            Text(
                text = "Create your account",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Pick a name, secure your account, and start crafting puzzles to share.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (password != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (password.length < 6) {
                        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    authRepo.signUp(email, password, displayName) { success, error ->
                        Log.d("AuthDebug", "signUp callback triggered: success=$success, error=$error")
                        if (success) {
                            val uid = authRepo.getCurrentUserId() ?: return@signUp
                            authRepo.loadUserProfile(uid) { profile, err ->
                                if (profile != null) {
                                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    Log.d("Auth", "Account created for ${profile.displayName}")

                                    // 🔹 Delay navigation slightly so toast can show first
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        onCreateAccount()   // call the navigation callback properly
                                    }, 1200)
                                } else {
                                    Toast.makeText(context, "Error loading profile: $err", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, error ?: "Signup failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create your account")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to log in")
            }
        }
    }
}

@Composable
fun DailyChallengeGeneratorScreen(
    modifier: Modifier = Modifier,
    onGenerateDailyChallenge: () -> Unit = {},
    onBackToLogin: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                onGenerateDailyChallenge()
                onBackToLogin()
            }
        ) {
            Text("Generate Daily Challenge")
        }
    }
}
