package com.example.project2.data

import java.time.Duration
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

/**
 * Contract for loading and persisting MindMatch data such as puzzles, progress, leaderboards,
 * and the daily challenge. Implementations may be backed by Firebase, local storage, or test fakes.
 */
interface MindMatchRepository {
    /** Currently active player profile. */
    val activeProfile: PlayerProfile
    /** Daily challenge content. */
    val dailyChallenge: DailyChallenge
    /** All available puzzles. */
    val puzzles: List<PuzzleDescriptor>
    /** Progress map keyed by puzzle id. */
    val progressByPuzzle: Map<String, PuzzleProgress>
    /** Leaderboard entries grouped by puzzle id. */
    val leaderboard: Map<String, List<LeaderboardEntry>>
}

/**
 * Fake implementation of [MindMatchRepository] used for previews or offline development.
 * Provides deterministic, hardcoded data without any remote dependencies.
 */
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
            title = "Jigsaw Puzzle",
            description = "Assemble the scrambled pieces to form a picture.",
            type = PuzzleType.JIGSAW,
            creatorId = "community",
            difficulty = Difficulty.EASY,
            isUserCreated = true
        ),
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
            id = "daily_prism_pulse_relay",
            title = "Prism Pulse Relay",
            description = "An ever-shifting beam-tracking memory run.",
            isUserCreated = false,
            creatorId = "daily-team"
        ),
        expiresAt = Instant.now().plusSeconds(60 * 60 * 18),
        content = parseDailyPuzzleContent(PRISM_PULSE_JSON)
    )
}

/**
 * Parse a JSON blob into [DailyPuzzleContent] for the Prism Pulse relay demo.
 *
 * @param json serialized daily puzzle payload
 * @return fully hydrated [DailyPuzzleContent] used by [FakeMindMatchRepository]
 */
private fun parseDailyPuzzleContent(json: String): DailyPuzzleContent {
    val root = JSONObject(json)
    val instructions = root.optString("instructions")

    val gridArray = root.optJSONArray("grid") ?: JSONArray()
    val grid = buildList {
        for (rowIndex in 0 until gridArray.length()) {
            val rowArray = gridArray.optJSONArray(rowIndex) ?: continue
            add(buildList {
                for (colIndex in 0 until rowArray.length()) {
                    val cellObject = rowArray.optJSONObject(colIndex) ?: continue
                    val value = cellObject.optString("value")
                    val stateName = cellObject.optString("state")
                    add(PuzzleCell(value = value, state = stateName.toPuzzleCellState()))
                }
            })
        }
    }

    val controlsArray = root.optJSONArray("controls") ?: JSONArray()
    val controls = buildList {
        for (index in 0 until controlsArray.length()) {
            val controlObject = controlsArray.optJSONObject(index) ?: continue
            add(
                PuzzleControl(
                    id = controlObject.optString("id"),
                    label = controlObject.optString("label"),
                    isPrimary = controlObject.optBoolean("isPrimary", false)
                )
            )
        }
    }

    val statsObject = root.optJSONObject("stats") ?: JSONObject()
    val stats = PuzzleStats(
        target = statsObject.optInt("target"),
        streak = statsObject.optInt("streak"),
        timeRemainingSeconds = statsObject.optInt("timeRemainingSeconds")
    )

    return DailyPuzzleContent(
        instructions = instructions,
        grid = grid,
        controls = controls,
        stats = stats
    )
}

/** Map a nullable string to a [PuzzleCellState], defaulting to Neutral when unknown. */
private fun String?.toPuzzleCellState(): PuzzleCellState =
    PuzzleCellState.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: PuzzleCellState.Neutral

/** Sample JSON used to seed the fake daily challenge content. */
private val PRISM_PULSE_JSON = """
{
  "instructions": "You're calibrating the Prism Pulse Relay. Tap the tiles in the sequence that matches today's beam path. After every perfect chain the grid morphs--adapt fast, keep your accuracy streak alive, and rack up the most luminous combo before the relay cools.",
  "grid": [
    [{"value":"P1","state":"Neutral"},{"value":"P2","state":"Neutral"},{"value":"P3","state":"Neutral"},{"value":"P4","state":"Neutral"}],
    [{"value":"A","state":"Neutral"},{"value":"B","state":"Neutral"},{"value":"C","state":"Neutral"},{"value":"W","state":"Neutral"}],
    [{"value":"D1","state":"Neutral"},{"value":"D2","state":"Neutral"},{"value":"D3","state":"Neutral"},{"value":"D4","state":"Neutral"}],
    [{"value":"R1","state":"Neutral"},{"value":"T2","state":"Neutral"},{"value":"P3","state":"Neutral"},{"value":"Wildcard","state":"Neutral"}]
  ],
  "controls": [
    {"id":"scan","label":"Scan New Pattern","isPrimary":true},
    {"id":"lock","label":"Lock Sequence","isPrimary":false},
    {"id":"wild","label":"Toggle Wildcard","isPrimary":false}
  ],
  "stats": {
    "target": 5,
    "streak": 0,
    "timeRemainingSeconds": 120
  }
}
""".trimIndent()
