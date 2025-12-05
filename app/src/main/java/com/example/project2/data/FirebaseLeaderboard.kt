package com.example.project2.data

import com.google.firebase.Timestamp
import java.time.Instant

data class FirebaseLeaderboardEntry(
    val puzzleId: String = "",
    val playerName: String = "",
    val score: Int = 0,
    val recordedAt: Timestamp = Timestamp.now()
)

// Firebase -> app model
fun FirebaseLeaderboardEntry.toEntry(): LeaderboardEntry =
    LeaderboardEntry(
        puzzleId = puzzleId,
        playerName = playerName,
        score = score,
        recordedAt = Instant.ofEpochMilli(recordedAt.toDate().time)
    )

// app model -> Firebase
fun LeaderboardEntry.toFirebase(): FirebaseLeaderboardEntry =
    FirebaseLeaderboardEntry(
        puzzleId = puzzleId,
        playerName = playerName,
        score = score,
        recordedAt = Timestamp.now()
    )
