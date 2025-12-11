package com.example.project2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// A simple data class to define a difficulty level for clarity
private data class Difficulty(val name: String, val gridSize: Int)

/**
 * Lets the player pick a jigsaw difficulty, exposing the grid size via callback.
 *
 * @param onDifficultySelected emits the chosen grid size (e.g., 3 for 3x3)
 */
@Composable
fun DifficultySelectionScreen(
    onDifficultySelected: (Int) -> Unit // A callback to notify which grid size was chosen
) {
    val difficulties = listOf(
        Difficulty("Easy", 3),
        Difficulty("Medium", 4),
        Difficulty("Hard", 5),
        Difficulty("Expert", 6)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Select Difficulty",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Create a button for each difficulty level
        difficulties.forEach { difficulty ->
            Button(
                onClick = { onDifficultySelected(difficulty.gridSize) },
                modifier = Modifier.size(width = 250.dp, height = 50.dp)
            ) {
                Text("${difficulty.name} (${difficulty.gridSize}x${difficulty.gridSize})")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
