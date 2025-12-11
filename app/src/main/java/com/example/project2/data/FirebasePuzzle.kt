package com.example.project2.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


/** Firestore-friendly version of PuzzleDescriptor. */
data class FirebasePuzzle(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",                // enum to string
    val creatorId: String = "",
    val difficulty: String = "",          // enum to string
    val estimatedDurationSeconds: Long = 120, // Duration to seconds
    val isUserCreated: Boolean = false,
    val lastPlayed: Timestamp? = null,     // Instant to Timestamp
    val mastermindColors: List<String>? = null,
    val mastermindSlots: Int? = null,
    val mastermindGuesses: Int? = null,
    val mastermindLevels: Int? = null,
    val mastermindCode: List<String>? = null
)

/** Firestore-friendly version of PuzzleProgress. */
data class FirebasePuzzleProgress(
    val puzzleId: String = "",
    val currentLevel: Int = 0,
    val levelsUnlocked: Int = 0,
    val bestScore: Int = 0,
    val bestTimeSeconds: Long? = null,
    val inProgressState: String? = null
)
