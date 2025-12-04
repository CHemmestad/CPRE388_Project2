package com.example.project2.data

import com.google.firebase.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Lightweight data models representing the core entities in MindMatch.
 * These will eventually be backed by a repository (Firebase/local DB),
 * but for now they describe the state the UI needs to render.
 */
data class PlayerProfile(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String = "Player",
    val avatarUrl: String? = null,
    val bio: String = "",
    val preferredDifficulty: Difficulty = Difficulty.MEDIUM,
    val createdAt: Timestamp = Timestamp.now()
)

data class PuzzleDescriptor(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val type: PuzzleType,
    val gridSize: Int? = null,
    val creatorId: String,
    val difficulty: Difficulty,
    val estimatedDuration: Duration = Duration.ofMinutes(2),
    val isUserCreated: Boolean = false,
    val imageName: String? = null,
    val lastPlayed: Instant? = null
)

data class PuzzleProgress(
    val puzzleId: String,
    val currentLevel: Int,
    val levelsUnlocked: Int,
    val bestScore: Int,
    val bestTime: Duration?,
    val inProgressState: String? = null // Serialized game state for resume
)

data class LeaderboardEntry(
    val puzzleId: String,
    val playerName: String,
    val score: Int,
    val recordedAt: Instant
)

data class DailyChallenge(
    val puzzle: PuzzleDescriptor,
    val expiresAt: Instant,
    val content: DailyPuzzleContent
)

enum class PuzzleType(val displayName: String) {
    PATTERN_MEMORY("Pattern Memory"),
    LOGIC_GRID("Logic Grid"),
    SEQUENCE_RECALL("Sequence Recall"),
    FOCUS_TAPPER("Focus Tapper"),
    SPEED_MATCH("Speed Match"),
    JIGSAW("Jigsaw Puzzle")
}

enum class Difficulty {
    EASY, MEDIUM, HARD, EXPERT
}

data class DailyPuzzleContent(
    val instructions: String,
    val grid: List<List<PuzzleCell>>,
    val controls: List<PuzzleControl>,
    val stats: PuzzleStats
)

data class PuzzleCell(
    val value: String,
    val state: PuzzleCellState = PuzzleCellState.Neutral
)

enum class PuzzleCellState {
    Neutral,
    Active,
    Correct,
    Incorrect,
    Disabled
}

data class PuzzleControl(
    val id: String,
    val label: String,
    val isPrimary: Boolean = false
)

data class PuzzleStats(
    val target: Int,
    val streak: Int,
    val timeRemainingSeconds: Int
)
