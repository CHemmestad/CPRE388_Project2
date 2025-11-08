package com.example.project2.data

import java.time.Duration
import java.time.Instant

/**
 * Repository abstraction for MindMatch data sources.
 * Replace the fake implementation with Firebase/local persistence later.
 */
interface MindMatchRepository {
    val activeProfile: PlayerProfile
    val dailyChallenge: DailyChallenge
    val puzzles: List<PuzzleDescriptor>
    val progressByPuzzle: Map<String, PuzzleProgress>
    val leaderboard: Map<String, List<LeaderboardEntry>>
}

class FakeMindMatchRepository : MindMatchRepository {
    override val activeProfile: PlayerProfile = PlayerProfile(displayName = "Avery")

    private val colorPulse = PuzzleDescriptor(
        title = "Color Pulse",
        description = "Memorize flashing colors and repeat the sequence.",
        type = PuzzleType.PATTERN_MEMORY,
        creatorId = activeProfile.id,
        difficulty = Difficulty.MEDIUM,
        isUserCreated = false
    )

    override val puzzles: List<PuzzleDescriptor> = listOf(
        colorPulse,
        PuzzleDescriptor(
            title = "Logic Links",
            description = "Deduce correct pairings using logical clues.",
            type = PuzzleType.LOGIC_GRID,
            creatorId = "community",
            difficulty = Difficulty.HARD,
            isUserCreated = true
        ),
        PuzzleDescriptor(
            title = "Sequence Sprint",
            description = "Remember number sequences while they speed up.",
            type = PuzzleType.SEQUENCE_RECALL,
            creatorId = "community",
            difficulty = Difficulty.EASY,
            isUserCreated = true
        )
    )

    override val progressByPuzzle: Map<String, PuzzleProgress> = puzzles.associate { puzzle ->
        puzzle.id to PuzzleProgress(
            puzzleId = puzzle.id,
            currentLevel = 3,
            levelsUnlocked = 10,
            bestScore = 12450,
            bestTime = Duration.ofSeconds(82),
            inProgressState = null
        )
    }

    override val leaderboard: Map<String, List<LeaderboardEntry>> = puzzles.associate { puzzle ->
        puzzle.id to listOf(
            LeaderboardEntry(
                puzzleId = puzzle.id,
                playerName = "Jordan",
                score = 15000,
                recordedAt = Instant.now().minusSeconds(86400)
            ),
            LeaderboardEntry(
                puzzleId = puzzle.id,
                playerName = "Morgan",
                score = 14100,
                recordedAt = Instant.now().minusSeconds(7200)
            ),
            LeaderboardEntry(
                puzzleId = puzzle.id,
                playerName = activeProfile.displayName,
                score = 12450,
                recordedAt = Instant.now().minusSeconds(3600)
            )
        )
    }

    override val dailyChallenge: DailyChallenge = DailyChallenge(
        puzzle = colorPulse.copy(
            id = "daily_mind_jogger",
            title = "Mind Jogger",
            description = "Daily challenge: conquer a curated memory scramble.",
            isUserCreated = false,
            creatorId = "daily-team"
        ),
        expiresAt = Instant.now().plusSeconds(60 * 60 * 18),
        content = DailyPuzzleContent(
            instructions = "Memorize the flashing pattern, then reproduce it within the time limit.",
            grid = listOf(
                listOf(
                    PuzzleCell("A"),
                    PuzzleCell("B", state = PuzzleCellState.Active),
                    PuzzleCell("C")
                ),
                listOf(
                    PuzzleCell("D"),
                    PuzzleCell("E"),
                    PuzzleCell("F")
                ),
                listOf(
                    PuzzleCell("G"),
                    PuzzleCell("H"),
                    PuzzleCell("I", state = PuzzleCellState.Disabled)
                )
            ),
            controls = listOf(
                PuzzleControl(id = "submit", label = "Submit", isPrimary = true),
                PuzzleControl(id = "hint", label = "Hint"),
                PuzzleControl(id = "shuffle", label = "Shuffle")
            ),
            stats = PuzzleStats(
                target = 16,
                streak = 3,
                timeRemainingSeconds = 72
            )
        )
    )
}
