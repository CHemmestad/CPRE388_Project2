package com.example.project2.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant


/**
 * Handles Firebase Authentication and Firestore operations for user accounts.
 */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Create a new user account and store the PlayerProfile document in Firestore.
     *
     * @param email user email
     * @param password raw password
     * @param displayName name to show in the app
     * @param context Android context for toast
     * @param onResult callback with success flag and optional error message
     */
    fun signUp(
        email: String,
        password: String,
        displayName: String,
        context: Context,
        onResult: (Boolean, String?) -> Unit
    ) {
        Log.d("AuthDebug", "signUp() started for $email")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                Log.d("AuthDebug", "addOnCompleteListener triggered: success=${task.isSuccessful}")
                Toast.makeText(context, "Sign Up Successful, go back to Login!", Toast.LENGTH_SHORT).show()
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser == null) {
                        Log.w("AuthDebug", "currentUser was null; retrying in 500ms")
                        Handler(Looper.getMainLooper()).postDelayed({
                            val retryUser = auth.currentUser
                            if (retryUser != null) {
                                createFirestoreProfile(retryUser.uid, displayName, email, onResult)
                            } else {
                                onResult(false, "User object was null after signup")
                            }
                        }, 500)
                    } else {
                        createFirestoreProfile(firebaseUser.uid, displayName, email, onResult)
                    }
                } else {
                    val errorMsg = task.exception?.message ?: "Unknown signup error"
                    Log.e("AuthRepository", "Signup failed: $errorMsg")
                    onResult(false, errorMsg)
                }
            }
    }

    /**
     * Helper to create a Firestore profile document for a new user.
     *
     * @param userId Firebase auth uid
     * @param displayName name to store on the profile
     * @param email user email for logging
     * @param onResult callback with success flag and optional error message
     */
    private fun createFirestoreProfile(
        userId: String,
        displayName: String,
        email: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val playerProfile = PlayerProfile(
            id = userId,
            displayName = displayName,
            createdAt = com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(userId)
            .set(playerProfile)
            .addOnSuccessListener {
                Log.d("AuthRepository", "User profile created for $email")
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("AuthRepository", "Failed to create Firestore profile: ${e.message}")
                onResult(false, e.message)
            }
    }


    /**
     * Log in an existing user via Firebase Auth.
     *
     * @param email user email
     * @param password raw password
     * @param onResult callback with success flag and optional error message
     */
    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthRepository", "Login successful for $email")
                    onResult(true, null)
                } else {
                    Log.e("AuthRepository", "Login failed: ${task.exception?.message}")
                    onResult(false, task.exception?.message)
                }
            }
    }

    /**
     * Load a user's PlayerProfile document from Firestore.
     *
     * @param userId UID of the user to load
     * @param onResult callback with the profile (or null) and optional error message
     */
    fun loadUserProfile(
        userId: String,
        onResult: (PlayerProfile?, String?) -> Unit
    ) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val profile = document.toObject(PlayerProfile::class.java)
                    Log.d("AuthRepository", "Profile loaded for userId=$userId, name=${profile?.displayName}")
                    onResult(profile, null)
                } else {
                    Log.e("AuthRepository", "Profile not found for $userId")
                    onResult(null, "Profile not found for user $userId")
                }
            }
            .addOnFailureListener { e ->
                Log.e("AuthRepository", "Error loading profile: ${e.message}")
                onResult(null, e.message)
            }
    }

    /**
     * Log out the current Firebase Auth user.
     */
    fun logout() {
        auth.signOut()
        Log.d("AuthRepository", "User logged out")
    }

    /**
     * Update basic profile fields in Firestore and return the updated profile.
     *
     * @param userId UID to update
     * @param displayName new display name
     * @param bio new bio
     */
    suspend fun updateProfile(userId: String, displayName: String, bio: String): PlayerProfile? {
        val docRef = db.collection("users").document(userId)
        docRef.update(mapOf("displayName" to displayName, "bio" to bio)).await()
        val snapshot = docRef.get().await()
        return snapshot.toObject(PlayerProfile::class.java)
    }

    /**
     * Delete the Firestore user document and Firebase Auth account.
     *
     * @return true on success; false otherwise
     */
    suspend fun deleteAccount(): Boolean {
        val currentUser = auth.currentUser ?: return false
        return try {
            val userId = currentUser.uid
            db.collection("users").document(userId).delete().await()
            currentUser.delete().await()
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to delete account: ${e.message}")
            false
        }
    }

    /**
     * Utility: Get current user info.
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
}
