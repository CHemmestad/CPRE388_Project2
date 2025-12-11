package com.example.project2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project2.data.DailyChallenge
import com.example.project2.data.DailyPuzzleContent
import com.example.project2.data.PuzzleCell
import com.example.project2.data.PuzzleCellState
import com.example.project2.data.PuzzleControl
import com.example.project2.data.PuzzleStats
import com.example.project2.ui.util.formatAsDisplay
import kotlinx.coroutines.delay

/**
 * Play surface for the daily challenge puzzle, including pattern preview, timer, and controls.
 *
 * @param challenge current daily challenge payload; renders loading while null
 * @param modifier layout modifier passed from parent
 */
@Composable
fun DailyChallengePlayScreen(
    challenge: DailyChallenge?,      // <-- ACCEPT NULL
    modifier: Modifier = Modifier
) {
    // Handle null/initial loading state to avoid crashing while data loads.
    if (challenge == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading daily challenge...")
        }
        return
    }

    val content = challenge.content
    var activeSequence by remember(content) { mutableStateOf(generateSequence(content.grid, content.stats.target)) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var cellStates by remember { mutableStateOf<Map<BoardCoordinate, PuzzleCellState>>(emptyMap()) }
    var streak by remember { mutableIntStateOf(content.stats.streak) }
    var timeRemaining by remember { mutableIntStateOf(content.stats.timeRemainingSeconds) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var sequenceVersion by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Tap \"Scan New Pattern\" to preview today's beam path.") }
    var wildcardEnabled by remember { mutableStateOf(false) }
    var roundResult by remember { mutableStateOf<RoundResult?>(null) }

    LaunchedEffect(content) {
        // Reset state when a new challenge arrives.
        activeSequence = generateSequence(content.grid, content.stats.target)
        currentIndex = 0
        cellStates = emptyMap()
        streak = content.stats.streak
        timeRemaining = content.stats.timeRemainingSeconds
        isTimerRunning = false
        sequenceVersion = 0
        message = "Tap \"Scan New Pattern\" to preview today's beam path."
        wildcardEnabled = false
        roundResult = null
    }

    LaunchedEffect(sequenceVersion) {
        if (sequenceVersion == 0) return@LaunchedEffect
        // Replay the pattern animation each time we bump the version.
        isTimerRunning = false
        currentIndex = 0
        roundResult = null
        cellStates = emptyMap()
        message = "Memorize the pulse…"
        for (coord in activeSequence) {
            cellStates = cellStates + (coord to PuzzleCellState.Active)
            delay(420)
            cellStates = cellStates - coord
            delay(140)
        }
        message = "Your turn! Recreate the sequence."
        isTimerRunning = true
    }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
        if (isTimerRunning && timeRemaining <= 0) {
            isTimerRunning = false
            roundResult = RoundResult.Fail("Time's up! Tap Scan to replay the pattern.")
            message = roundResult?.statusMessage ?: message
            cellStates = markSequenceAsIncorrect(activeSequence)
            streak = 0
        }
    }

    val dynamicGrid = remember(content.grid, cellStates) {
        content.grid.mapIndexed { rowIndex, row ->
            row.mapIndexed { columnIndex, cell ->
                val override = cellStates[BoardCoordinate(rowIndex, columnIndex)]
                val effectiveState = override ?: cell.state
                cell.copy(state = effectiveState)
            }
        }
    }
    val dynamicStats = content.stats.copy(
        target = activeSequence.size,
        streak = streak,
        timeRemainingSeconds = timeRemaining.coerceAtLeast(0)
    )

    // Local helpers for gameplay events.
    fun startNewPattern() {
        activeSequence = generateSequence(content.grid, content.stats.target)
        currentIndex = 0
        cellStates = emptyMap()
        timeRemaining = content.stats.timeRemainingSeconds
        roundResult = null
        sequenceVersion++
    }

    fun completeRoundSuccess() {
        streak++
        isTimerRunning = false
        roundResult = RoundResult.Success("Relay calibrated! Tap Scan for a tougher remix.")
        message = roundResult?.statusMessage ?: message
    }

    fun failRound(reason: String) {
        streak = 0
        isTimerRunning = false
        roundResult = RoundResult.Fail(reason)
        message = reason
    }

    fun handleCellTapped(row: Int, column: Int) {
        if (!isTimerRunning || roundResult != null) return
        val coord = BoardCoordinate(row, column)
        val expected = activeSequence.getOrNull(currentIndex) ?: return
        val matches = coord == expected || (wildcardEnabled && cellLooksWildcard(content.grid, coord))
        if (matches) {
            cellStates = cellStates + (coord to PuzzleCellState.Correct)
            currentIndex++
            if (currentIndex == activeSequence.size) {
                completeRoundSuccess()
            }
        } else {
            cellStates = cellStates + (coord to PuzzleCellState.Incorrect)
            failRound("Misfire! Tap Scan to learn the pattern again.")
        }
    }

    fun handleControl(control: PuzzleControl) {
        when (control.id.lowercase()) {
            "scan" -> {
                message = "Loading a fresh pattern…"
                startNewPattern()
            }
            "lock" -> {
                if (roundResult != null) return
                if (currentIndex == activeSequence.size) {
                    completeRoundSuccess()
                } else {
                    failRound("Sequence incomplete. Tap Scan to retry.")
                }
            }
            "wild" -> {
                wildcardEnabled = !wildcardEnabled
                message = if (wildcardEnabled) {
                    "Wildcard armed. Any wildcard tile counts as correct."
                } else {
                    "Wildcard disabled. Exact taps only."
                }
            }
            else -> {
                message = "${control.label}: feature coming soon."
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Daily Challenge",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Memorize the generated pattern, then recreate it before the relay powers down.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        PuzzleStatusRow(challenge = challenge)
        PuzzleScoreStrip(stats = dynamicStats)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            DailyPuzzleBoard(
                content = content.copy(grid = dynamicGrid, stats = dynamicStats),
                statusMessage = message,
                wildcardEnabled = wildcardEnabled,
                onCellTapped = ::handleCellTapped,
                onControlPressed = ::handleControl,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Card wrapper that shows instructions, status, grid, and controls for the daily puzzle.
 *
 * @param content challenge content with grid, controls, and stats
 * @param statusMessage message displayed above the grid
 * @param wildcardEnabled whether wildcard mode is active
 * @param onCellTapped callback when a grid cell is tapped
 * @param onControlPressed callback when a control button is pressed
 * @param modifier layout modifier passed from parent
 * @return composable card body
 */
@Composable
private fun DailyPuzzleBoard(
    content: DailyPuzzleContent,
    statusMessage: String,
    wildcardEnabled: Boolean,
    onCellTapped: (Int, Int) -> Unit,
    onControlPressed: (PuzzleControl) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = content.instructions,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (statusMessage.isNotBlank()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                PuzzleGrid(
                    grid = content.grid,
                    onCellTapped = onCellTapped
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            PuzzleControlRow(
                controls = content.controls,
                wildcardEnabled = wildcardEnabled,
                onControlPressed = onControlPressed
            )
        }
    }
}

/**
 * Render the puzzle grid with tap handlers.
 *
 * @param grid 2D list of cells to display
 * @param onCellTapped invoked with row/column on tap
 * @param modifier layout modifier passed from parent
 */
@Composable
private fun PuzzleGrid(
    grid: List<List<PuzzleCell>>,
    onCellTapped: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        grid.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEachIndexed { columnIndex, cell ->
                    PuzzleCellView(
                        cell = cell,
                        onTap = { onCellTapped(rowIndex, columnIndex) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Single puzzle cell with visual state and tap handling.
 *
 * @param cell model containing value and state
 * @param onTap invoked when the cell is tapped (if enabled)
 * @param modifier layout modifier passed from parent
 */
@Composable
private fun PuzzleCellView(
    cell: PuzzleCell,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (cell.state) {
        PuzzleCellState.Active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        PuzzleCellState.Correct -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
        PuzzleCellState.Incorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        PuzzleCellState.Disabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        PuzzleCellState.Neutral -> MaterialTheme.colorScheme.surfaceBright
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = cell.state != PuzzleCellState.Disabled, onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cell.value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Strip of high-level stats for the current run.
 *
 * @param stats live puzzle stats to display
 */
@Composable
private fun PuzzleScoreStrip(stats: PuzzleStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MiniStat(label = "Target", value = stats.target.toString())
        MiniStat(label = "Streak", value = stats.streak.toString())
        MiniStat(label = "Time", value = stats.timeRemainingSeconds.asTimeDisplay())
    }
}

/**
 * Horizontal row of puzzle controls (e.g., Scan, Lock, Wildcard).
 *
 * @param controls list of available controls
 * @param wildcardEnabled whether wildcard is toggled on
 * @param onControlPressed callback for control selection
 */
@Composable
private fun PuzzleControlRow(
    controls: List<PuzzleControl>,
    wildcardEnabled: Boolean,
    onControlPressed: (PuzzleControl) -> Unit
) {
    if (controls.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        controls.forEach { control ->
            val activeColor = when {
                control.id.equals("wild", ignoreCase = true) && wildcardEnabled ->
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                control.isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(activeColor)
                    .background(activeColor)
                    .clickable { onControlPressed(control) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = control.label,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * Small stat badge showing a label and value.
 *
 * @param label name of the stat
 * @param value value to display
 */
@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Status chips summarizing the current challenge metadata.
 *
 * @param challenge daily challenge being played; exits early when null
 */
@Composable
private fun PuzzleStatusRow(challenge: DailyChallenge?) {
    if (challenge == null) return  // <-- SAFETY FIX

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusChip(
            title = challenge.puzzle.type.displayName,
            subtitle = "Puzzle type"
        )
        StatusChip(
            title = challenge.puzzle.difficulty.name,
            subtitle = "Difficulty"
        )
        StatusChip(
            title = challenge.expiresAt.formatAsDisplay(),
            subtitle = "Expires"
        )
    }
}

/**
 * Generic chip showing a title and subtitle.
 *
 * @param title primary text
 * @param subtitle supporting text
 */
@Composable
private fun StatusChip(
    title: String,
    subtitle: String
) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 100.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Format seconds to mm:ss for display.
 *
 * @receiver total seconds remaining
 * @return formatted string for timers
 */
private fun Int.asTimeDisplay(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Build a random sequence of playable coordinates for the challenge.
 *
 * @param grid puzzle grid to pull coordinates from
 * @param desiredLength requested sequence length
 * @return list of board coordinates honoring disabled cells and clamped to grid size
 */
private fun generateSequence(
    grid: List<List<PuzzleCell>>,
    desiredLength: Int
): List<BoardCoordinate> {
    val coords = buildList {
        grid.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, cell ->
                if (cell.state != PuzzleCellState.Disabled) {
                    add(BoardCoordinate(rowIndex, columnIndex))
                }
            }
        }
    }
    if (coords.isEmpty()) return emptyList()
    val length = desiredLength.coerceIn(1, coords.size)
    return coords.shuffled().take(length)
}

/**
 * Mark every coordinate in the sequence as incorrect.
 *
 * @param sequence coordinates to flag
 * @return map of coordinates to incorrect state
 */
private fun markSequenceAsIncorrect(sequence: List<BoardCoordinate>): Map<BoardCoordinate, PuzzleCellState> {
    return sequence.associateWith { PuzzleCellState.Incorrect }
}

/**
 * Determine if the tapped cell should count as a wildcard.
 *
 * @param grid puzzle grid
 * @param coord target cell coordinate
 * @return true when the value contains "wild" (case-insensitive)
 */
private fun cellLooksWildcard(
    grid: List<List<PuzzleCell>>,
    coord: BoardCoordinate
): Boolean {
    val value = grid.getOrNull(coord.row)?.getOrNull(coord.column)?.value.orEmpty()
    return value.contains("wild", ignoreCase = true)
}

private data class BoardCoordinate(val row: Int, val column: Int)

private sealed class RoundResult(val statusMessage: String) {
    class Success(message: String) : RoundResult(message)
    class Fail(message: String) : RoundResult(message)
}
