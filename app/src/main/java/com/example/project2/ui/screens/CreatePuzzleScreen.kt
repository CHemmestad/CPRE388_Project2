package com.example.project2.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project2.data.Difficulty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePuzzleScreen(
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var puzzleType by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var adaptiveRules by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create a Puzzle",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Design challenges for other MindMatch players. Save locally or publish to the community.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Puzzle title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )

                OutlinedTextField(
                    value = puzzleType,
                    onValueChange = { puzzleType = it },
                    label = { Text("Puzzle type") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("e.g., Pattern Memory, Logic Grid, Sequence Recall")
                    }
                )

                Text(
                    text = "Base difficulty",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Difficulty.entries.forEach { difficulty ->
                        FilterChip(
                            selected = selectedDifficulty == difficulty,
                            onClick = { selectedDifficulty = difficulty },
                            label = { Text(difficulty.name) }
                        )
                    }
                }

                Text(
                    text = "Adaptive difficulty rules",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Outline how the puzzle should scale when a player succeeds or fails. This can later convert into code-driven tuning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = adaptiveRules,
                    onValueChange = { adaptiveRules = it },
                    label = { Text("Pseudo rules (e.g., \"Increase sequence length after two perfect runs\")") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { /* TODO: Save locally */ }
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save draft")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { /* TODO: Create puzzle and attach to profile */ }
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create")
            }
        }
    }
}
