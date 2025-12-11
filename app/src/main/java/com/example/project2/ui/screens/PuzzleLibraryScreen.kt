package com.example.project2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleProgress
import com.example.project2.data.PuzzleType
import com.example.project2.data.Difficulty
import com.google.firebase.Timestamp
import com.example.project2.ui.widgets.PuzzleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleLibraryScreen(
    puzzles: List<PuzzleDescriptor>,
    progress: Map<String, PuzzleProgress>,
    lastPlayed: Map<String, Timestamp> = emptyMap(),
    modifier: Modifier = Modifier,
    onPlayPuzzle: (PuzzleDescriptor) -> Unit = {}
) {
    val availableTypes = listOf("All") + puzzles.map { it.type.displayName }.distinct().sorted()
    val difficultyOrder = listOf("EASY", "MEDIUM", "HARD", "EXPERT")
    val availableDifficulties = listOf("All") + puzzles
        .map { it.difficulty.name }
        .distinct()
        .sortedWith(compareBy { difficultyOrder.indexOf(it).let { idx -> if (idx >= 0) idx else Int.MAX_VALUE } })

    var typeFilter by rememberSaveable { mutableStateOf(availableTypes.first()) }
    var typeExpanded by remember { mutableStateOf(false) }
    var difficultyFilter by rememberSaveable { mutableStateOf(availableDifficulties.first()) }
    var difficultyExpanded by remember { mutableStateOf(false) }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = typeFilter,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    availableTypes.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                typeFilter = option
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = difficultyExpanded,
                onExpandedChange = { difficultyExpanded = !difficultyExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = difficultyFilter,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Difficulty") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = difficultyExpanded,
                    onDismissRequest = { difficultyExpanded = false }
                ) {
                    availableDifficulties.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                difficultyFilter = option
                                difficultyExpanded = false
                            }
                        )
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            val puzzlesToShow = puzzles
                .filter { puzzle ->
                    puzzle.type != PuzzleType.JIGSAW || puzzle.id == "play_jigsaw_template"
                }
                .filter { puzzle ->
                    typeFilter == "All" || puzzle.type.displayName == typeFilter
                }
                .filter { puzzle ->
                    difficultyFilter == "All" || puzzle.difficulty.name == difficultyFilter
                }
                .sortedByDescending { lastPlayed[it.id]?.seconds ?: 0 }
            items(puzzlesToShow) { puzzle ->
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
