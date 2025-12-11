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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.project2.data.DailyChallengeGeneratorRepository
import kotlinx.coroutines.launch

/**
 * Entry point for authentication where users can sign in or reveal a hidden admin flow.
 *
 * @param modifier layout modifier passed from parent
 * @param onLogin callback invoked after a successful login
 * @param onCreateAccount navigates to the sign-up screen
 * @param onSecretAccess Easter egg action triggered by tapping the invisible hotspot
 */
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

/**
 * Sign-up form that creates a new Firebase auth user and profile.
 *
 * @param modifier layout modifier passed from parent
 * @param onCreateAccount callback fired after successful account creation
 * @param onBackToLogin navigation back to the login screen
 */
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

                    authRepo.signUp(email, password, displayName,context) { success, error ->
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

/**
 * helper screen to generate and store a daily Mastermind challenge via Gemini.
 *
 * @param modifier layout modifier passed from parent
 * @param onGenerateDailyChallenge emits the raw JSON saved to Firestore
 * @param onBackToLogin navigation callback to return to authentication
 */
@Composable
fun DailyChallengeGeneratorScreen(
    modifier: Modifier = Modifier,
    onGenerateDailyChallenge: (String) -> Unit = {},
    onBackToLogin: () -> Unit = {}
) {
    var isGenerating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    val prompt by remember { mutableStateOf(buildMastermindPrompt()) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val generatorRepository = remember { DailyChallengeGeneratorRepository() }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AI Mastermind Puzzle Generator",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Gemini will fill in the Mastermind puzzle builder as if it were a user: choosing colors, slots, guesses, levels, and the secret code under the constraints below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Prompt preview",
            style = MaterialTheme.typography.titleMedium
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = {},
            label = { Text("Prompt to send to Gemini") },
            supportingText = { Text("Gemini fills out the Mastermind builder using these constraints.") },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            readOnly = true
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    isGenerating = true
                    statusMessage = null
                    statusIsError = false
                    try {
                        val result = generatorRepository.generateAndStore(prompt)
                        statusMessage = "Challenge saved (id: ${result.documentId})"
                        Toast.makeText(context, "Daily challenge saved!", Toast.LENGTH_SHORT).show()
                        onGenerateDailyChallenge(result.rawJson)
                        onBackToLogin()
                    } catch (e: Exception) {
                        val message = e.localizedMessage ?: "Unable to generate challenge"
                        statusMessage = message
                        statusIsError = true
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    } finally {
                        isGenerating = false
                    }
                }
            },
            enabled = prompt.isNotBlank() && !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sending to Gemini…")
            } else {
                Text("Generate daily challenge with Gemini")
            }
        }

        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to log in")
        }
    }
}

/**
 * Compose the Gemini prompt used to generate a Mastermind daily challenge.
 *
 * @return formatted prompt describing palette, constraints, and payload shape
 */
private fun buildMastermindPrompt(): String {
    return buildString {
        appendLine("You are an elite puzzle designer for the MindMatch mobile app. Act like a user filling out the Mastermind puzzle builder and generate all required fields yourself.")
        appendLine("Return JSON with keys: title, description, mastermindConfig { colors, slots, guesses, levels, code }.")
        appendLine()
        appendLine("Rules:")
        appendLine("1) Allowed palette only: Red, Blue, Green, Yellow, Orange, Purple, Pink, White. Use these exact words (case-insensitive).")
        appendLine("2) Choose 1-8 colors. Slots: 1-8 and must be >= number of chosen colors. Guesses: 1-20. Levels: at least 1.")
        appendLine("3) code length must equal slots and use only the chosen colors; duplicates allowed.")
        appendLine("4) Keep description under 50 words and encouraging.")
        appendLine("5) Ensure mastermindConfig has all fields: colors[], slots (int), guesses (int), levels (int), code[].")
        appendLine("Return a single JSON object only. No commentary.")
    }
}
