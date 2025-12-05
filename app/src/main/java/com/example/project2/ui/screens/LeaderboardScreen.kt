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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project2.data.LeaderboardEntry
import com.example.project2.data.PuzzleDescriptor

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

@Composable
private fun LeaderboardCard(
    puzzle: PuzzleDescriptor,
    entries: List<LeaderboardEntry>
) {
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

            // Column headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rank",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Player",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "Puzzle",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "Score",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Divider()

            // Rows: rank, player, puzzle type+name, score
            entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Rank
                    Text(
                        text = "#${index + 1}",
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal
                    )

                    // Player
                    Text(
                        text = entry.playerName,
                        modifier = Modifier.weight(1.5f),
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal
                    )

                    // Puzzle type + name
                    Text(
                        text = "${puzzle.type.displayName} – ${puzzle.title}",
                        modifier = Modifier.weight(2f),
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal
                    )

                    // Score
                    Text(
                        text = entry.score.toString(),
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
