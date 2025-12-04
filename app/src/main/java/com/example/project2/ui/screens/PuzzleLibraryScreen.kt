package com.example.project2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleProgress
import com.example.project2.ui.widgets.PuzzleCard

@Composable
fun PuzzleLibraryScreen(
    puzzles: List<PuzzleDescriptor>,
    progress: Map<String, PuzzleProgress>,
    modifier: Modifier = Modifier,
    onPlayPuzzle: (PuzzleDescriptor) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Puzzle Library",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Select a puzzle type to dive in or filter to match your mood.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(puzzles) { puzzle ->
                PuzzleCard(
                    puzzle = puzzle,
                    progress = progress[puzzle.id],
                    actionText = "Play",
                    modifier = Modifier.fillMaxWidth(),
                    onActionClick = { onPlayPuzzle(puzzle) }
                )
            }
        }
    }
}
