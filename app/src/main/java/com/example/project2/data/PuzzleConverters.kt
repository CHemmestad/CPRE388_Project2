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
        },
        mastermindColors = mastermindConfig?.colors,
        mastermindSlots = mastermindConfig?.slots,
        mastermindGuesses = mastermindConfig?.guesses,
        mastermindLevels = mastermindConfig?.levels,
        mastermindCode = mastermindConfig?.code
    )
}

fun FirebasePuzzle.toPuzzle(): PuzzleDescriptor {
    fun normalizeEnum(value: String): String =
        value.trim().replace("\\s+".toRegex(), "_").uppercase()

    val config = if (mastermindColors != null && mastermindSlots != null && mastermindGuesses != null && mastermindLevels != null && mastermindCode != null) {
        MastermindConfig(
            colors = mastermindColors,
            slots = mastermindSlots,
            guesses = mastermindGuesses,
            levels = mastermindLevels,
            code = mastermindCode
        )
    } else null

    return PuzzleDescriptor(
        id = id,
        title = title,
        description = description,
        type = PuzzleType.entries.firstOrNull { it.name == normalizeEnum(type) }
            ?: PuzzleType.PATTERN_MEMORY,       // fallback to a safe default
        creatorId = creatorId,
        difficulty = Difficulty.entries.firstOrNull { it.name == normalizeEnum(difficulty) }
            ?: Difficulty.MEDIUM, // default if malformed
        estimatedDuration = Duration.ofSeconds(estimatedDurationSeconds),
        isUserCreated = isUserCreated,
        lastPlayed = lastPlayed?.toDate()?.toInstant(), // Timestamp → Instant
        mastermindConfig = config
    )
}
