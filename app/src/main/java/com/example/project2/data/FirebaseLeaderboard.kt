package com.example.project2.data

import com.google.firebase.Timestamp
import java.time.Instant

/**
 * Firestore representation of a leaderboard entry.
 */
data class FirebaseLeaderboardEntry(
    val puzzleId: String = "",
    val playerName: String = "",
    val score: Int = 0,
    val recordedAt: Timestamp = Timestamp.now()
)

/** Convert Firestore entry to app model. */
fun FirebaseLeaderboardEntry.toEntry(): LeaderboardEntry =
    LeaderboardEntry(
        puzzleId = puzzleId,
        playerName = playerName,
        score = score,
        recordedAt = Instant.ofEpochMilli(recordedAt.toDate().time)
    )

/** Convert app model to Firestore entry. */
fun LeaderboardEntry.toFirebase(): FirebaseLeaderboardEntry =
    FirebaseLeaderboardEntry(
        puzzleId = puzzleId,
        playerName = playerName,
        score = score,
        recordedAt = Timestamp.now()
    )
