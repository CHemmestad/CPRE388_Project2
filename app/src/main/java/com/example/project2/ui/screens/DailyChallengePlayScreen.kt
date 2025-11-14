package com.example.project2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project2.data.*
import com.example.project2.ui.util.formatAsDisplay

@Composable
fun DailyChallengePlayScreen(
    challenge: DailyChallenge?,      // <-- ACCEPT NULL
    modifier: Modifier = Modifier
) {
    // 1️⃣ Loading or null state
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

    // 2️⃣ Extract content safely
    val content = challenge.content

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
            text = "This surface loads the stored daily puzzle so players drop straight into gameplay.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        PuzzleStatusRow(challenge = challenge)

        PuzzleScoreStrip(stats = content.stats)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            DailyPuzzleBoard(
                content = content,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DailyPuzzleBoard(
    content: DailyPuzzleContent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = content.instructions,
                    style = MaterialTheme.typography.bodyLarge
                )
                PuzzleGrid(grid = content.grid)
            }
            PuzzleControlRow(controls = content.controls)
        }
    }
}

@Composable
private fun PuzzleGrid(
    grid: List<List<PuzzleCell>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        grid.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { cell ->
                    PuzzleCellView(
                        cell = cell,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PuzzleCellView(
    cell: PuzzleCell,
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
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cell.value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

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

@Composable
private fun PuzzleControlRow(controls: List<PuzzleControl>) {
    if (controls.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        controls.forEach { control ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (control.isPrimary)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = control.label,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

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

private fun Int.asTimeDisplay(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%02d:%02d".format(minutes, seconds)
}
