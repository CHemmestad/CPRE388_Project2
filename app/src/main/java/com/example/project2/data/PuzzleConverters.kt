package com.example.project2.data

import com.google.firebase.Timestamp
import java.time.Duration
import java.time.Instant
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


fun PuzzleDescriptor.toFirebase(): FirebasePuzzle {
    return FirebasePuzzle(
        id = id,
        title = title,
        description = description,
        type = type.name,  // ENUM → STRING
        creatorId = creatorId,
        difficulty = difficulty.name, // ENUM → STRING
        estimatedDurationSeconds = estimatedDuration.seconds, // Duration → Long
        isUserCreated = isUserCreated,
        lastPlayed = lastPlayed?.let {
            Timestamp(it.epochSecond, 0) // Instant → Timestamp
        }
    )
}

fun FirebasePuzzle.toPuzzle(): PuzzleDescriptor {
    return PuzzleDescriptor(
        id = id,
        title = title,
        description = description,
        type = PuzzleType.valueOf(type),       // STRING → ENUM
        creatorId = creatorId,
        difficulty = Difficulty.valueOf(difficulty), // STRING → ENUM
        estimatedDuration = Duration.ofSeconds(estimatedDurationSeconds),
        isUserCreated = isUserCreated,
        lastPlayed = lastPlayed?.toDate()?.toInstant() // Timestamp → Instant
    )
}

