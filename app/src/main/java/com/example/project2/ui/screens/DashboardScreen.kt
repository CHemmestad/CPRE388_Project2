package com.example.project2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project2.data.DailyChallenge
import com.example.project2.data.PlayerProfile
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleProgress
import com.example.project2.ui.util.formatAsDisplay
import com.example.project2.ui.widgets.PuzzleCard
import com.example.project2.ui.widgets.StatChip

@Composable
fun DashboardScreen(
    profile: PlayerProfile,
    puzzles: List<PuzzleDescriptor>,
    progress: Map<String, PuzzleProgress>,
    dailyChallenge: DailyChallenge,
    modifier: Modifier = Modifier,
    onPlayPuzzle: (PuzzleDescriptor) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GreetingHeader(profile = profile)
        DailyChallengeCard(
            dailyChallenge = dailyChallenge,
            onPlayPuzzle = { onPlayPuzzle(dailyChallenge.puzzle) }
        )

        Text(
            text = "Continue Playing",
            style = MaterialTheme.typography.titleMedium
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(puzzles) { puzzle ->
                val puzzleProgress = progress[puzzle.id]
                PuzzleCard(
                    puzzle = puzzle,
                    progress = puzzleProgress,
                    actionIcon = Icons.Default.PlayArrow,
                    actionText = "Resume",
                    onActionClick = { onPlayPuzzle(puzzle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Highlights",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChip(title = "Levels Cleared", value = "32")
            StatChip(title = "Longest Streak", value = "5 days")
            StatChip(title = "Adaptive Level", value = profile.preferredDifficulty.name)
        }
    }
}

@Composable
private fun GreetingHeader(profile: PlayerProfile) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Welcome back, ${profile.displayName}",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Sharpen your memory with a new puzzle.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DailyChallengeCard(
    dailyChallenge: DailyChallenge,
    onPlayPuzzle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Daily Challenge",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = dailyChallenge.puzzle.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = dailyChallenge.puzzle.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Expires ${dailyChallenge.expiresAt.formatAsDisplay()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Button(
                onClick = onPlayPuzzle,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Play now")
            }
        }
    }
}
