package com.example.project2.data

import com.google.firebase.Timestamp

// Firestore-friendly version of PuzzleDescriptor
data class FirebasePuzzle(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",                // enum to string
    val creatorId: String = "",
    val difficulty: String = "",          // enum to string
    val estimatedDurationSeconds: Long = 120, // Duration to seconds
    val isUserCreated: Boolean = false,
    val lastPlayed: Timestamp? = null     // Instant to Timestamp
)
