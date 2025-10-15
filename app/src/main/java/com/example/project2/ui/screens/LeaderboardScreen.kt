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
                item(key = puzzleId) {
                    val puzzle = puzzles[puzzleId]
                    LeaderboardCard(
                        puzzleTitle = puzzle?.title ?: "Unknown puzzle",
                        entries = entries
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardCard(
    puzzleTitle: String,
    entries: List<LeaderboardEntry>
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = puzzleTitle,
                style = MaterialTheme.typography.titleMedium
            )
            Divider()
            entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "#${index + 1} ${entry.playerName}",
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        text = entry.score.toString(),
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
