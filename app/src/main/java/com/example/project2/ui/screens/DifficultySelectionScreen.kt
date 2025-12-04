package com.example.project2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// A simple data class to define a difficulty level for clarity
private data class Difficulty(val name: String, val gridSize: Int)

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
