package com.example.project2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project2.data.LeaderboardEntry
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleType

/**
 * Displays leaderboards grouped by puzzle, showing the top scores for each.
 *
 * @param leaderboard map of puzzle id to leaderboard entries
 * @param puzzles map of puzzle id to descriptors for display metadata
 * @param modifier layout modifier passed from parent
 */
@Composable
fun LeaderboardScreen(
    leaderboard: Map<String, List<LeaderboardEntry>>,
    puzzles: Map<String, PuzzleDescriptor>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Leaderboards",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Compare scores across puzzle types and see where you rank.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            leaderboard.entries.forEach { (puzzleId, entries) ->
                val puzzle = puzzles[puzzleId]
                if (puzzle != null && entries.isNotEmpty()) {
                    item(key = puzzleId) {
                        LeaderboardCard(
                            puzzle = puzzle,
                            entries = entries
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card showing the top entries for a specific puzzle.
 *
 * @param puzzle puzzle descriptor for metadata
 * @param entries leaderboard entries to render
 */
@Composable
private fun LeaderboardCard(
    puzzle: PuzzleDescriptor,
    entries: List<LeaderboardEntry>
) {
    val sortedEntries = if (puzzle.type == PuzzleType.JIGSAW) {
        entries.sortedBy { it.score } // Faster completion is better for Jigsaw.
    } else {
        entries.sortedByDescending { it.score } // Higher score wins for other puzzles.
    }.take(5)

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card title: puzzle type + name
            Text(
                text = "${puzzle.type.displayName} • ${puzzle.title}",
                style = MaterialTheme.typography.titleMedium
            )

            // Rows: rank, player, puzzle type+name, score
            sortedEntries.forEachIndexed { index, entry ->
                val colors: Pair<Color, Color> = when (index) {
                    0 -> Pair(Color(0xFFD6B536), MaterialTheme.colorScheme.onPrimary)
                    1 -> Pair(Color(0xFFD6D6D6), MaterialTheme.colorScheme.onSurface)
                    2 -> Pair(Color(0xFFB66A2B), MaterialTheme.colorScheme.onPrimary)
                    else -> Pair(Color.Transparent, MaterialTheme.colorScheme.onSurface)
                }
                val rowColor = colors.first
                val textColor = colors.second

                Surface(
                    color = rowColor,
                    contentColor = textColor,
                    tonalElevation = if (index < 3) 2.dp else 0.dp,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Rank
                        Text(
                            text = "#${index + 1}",
                            fontWeight = if (index < 3) FontWeight.Bold else FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        // Player
                        Text(
                            text = entry.playerName,
                            modifier = Modifier.weight(1.5f),
                            fontWeight = if (index < 3) FontWeight.SemiBold else FontWeight.Normal
                        )

                        val scoreText = if (puzzle.type == PuzzleType.JIGSAW) {
                            val minutes = entry.score / 60
                            val seconds = entry.score % 60
                            String.format("%02d:%02d", minutes, seconds)
                        } else {
                            entry.score.toString()
                        }

                        // Score
                        Text(
                            text = scoreText,
                            fontWeight = if (index < 3) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
