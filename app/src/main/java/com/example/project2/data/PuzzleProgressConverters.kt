package com.example.project2.data

import java.time.Duration
import com.google.firebase.Timestamp

/** Convert app progress to Firestore payload. */
fun PuzzleProgress.toFirebase(): FirebasePuzzleProgress =
    FirebasePuzzleProgress(
        puzzleId = puzzleId,
        currentLevel = currentLevel,
        levelsUnlocked = levelsUnlocked,
        bestScore = bestScore,
        bestTimeSeconds = bestTime?.seconds,
        inProgressState = inProgressState
    )

/** Convert Firestore progress document to app progress model. */
fun FirebasePuzzleProgress.toProgress(): PuzzleProgress =
    PuzzleProgress(
        puzzleId = puzzleId,
        currentLevel = currentLevel,
        levelsUnlocked = levelsUnlocked,
        bestScore = bestScore,
        bestTime = bestTimeSeconds?.let { Duration.ofSeconds(it) },
        inProgressState = inProgressState
    )
