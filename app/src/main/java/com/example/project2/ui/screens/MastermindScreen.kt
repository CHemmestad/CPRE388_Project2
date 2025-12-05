package com.example.project2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project2.data.MastermindConfig
import com.example.project2.data.PuzzleDescriptor
import androidx.compose.foundation.layout.ExperimentalLayoutApi

data class MastermindFeedback(val exact: Int, val colorOnly: Int)
data class MastermindGuess(val values: List<String>, val feedback: MastermindFeedback)

@Composable
fun MastermindScreen(
    puzzle: PuzzleDescriptor,
    config: MastermindConfig,
    onBack: () -> Unit
) {
    val colors = remember(config.colors) { config.colors }
    val secret = remember(config.code) { config.code }
    val slots = config.slots.coerceIn(1, 8)
    val maxGuesses = config.guesses.coerceAtLeast(1)

    var currentGuess by remember { mutableStateOf(List(slots) { "" }) }
    val history = remember { mutableStateListOf<MastermindGuess>() }
    var statusMessage by remember { mutableStateOf("Tap colors to fill slots, then Submit.") }
    var gameOver by remember { mutableStateOf(false) }

    LaunchedEffect(config) {
        currentGuess = List(slots) { "" }
        history.clear()
        statusMessage = "Tap colors to fill slots, then Submit."
        gameOver = false
    }

    val remaining = maxGuesses - history.size
    val won = history.any { it.feedback.exact == slots }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = puzzle.title,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = puzzle.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Guesses left: $remaining / $maxGuesses",
            style = MaterialTheme.typography.labelLarge
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                history.forEachIndexed { index, guess ->
                    GuessRow(
                        label = "Guess ${index + 1}",
                        values = guess.values,
                        feedback = guess.feedback,
                        palette = colors
                    )
                }

                if (!gameOver) {
                    GuessRow(
                        label = "Current",
                        values = currentGuess,
                        feedback = null,
                        palette = colors,
                        onSlotTapped = { slotIndex ->
                            if (gameOver) return@GuessRow
                            // remove value on tap to allow changing quickly
                            currentGuess = currentGuess.toMutableList().also {
                                it[slotIndex] = ""
                            }
                        }
                    )
                }
            }
        }

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ColorPickerRow(
            palette = colors,
            onColorSelected = { colorName ->
                if (gameOver) return@ColorPickerRow
                val firstEmpty = currentGuess.indexOfFirst { it.isBlank() }
                if (firstEmpty != -1) {
                    currentGuess = currentGuess.toMutableList().also { it[firstEmpty] = colorName }
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = !gameOver,
                onClick = {
                    currentGuess = List(slots) { "" }
                    statusMessage = "Cleared current guess."
                }
            ) {
                Text("Clear")
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = !gameOver && currentGuess.none { it.isBlank() },
                onClick = {
                    val feedback = evaluateGuess(secret, currentGuess)
                    history.add(MastermindGuess(currentGuess, feedback))
                    if (feedback.exact == slots) {
                        statusMessage = "You cracked the code!"
                        gameOver = true
                    } else if (history.size >= maxGuesses) {
                        statusMessage = "Out of guesses. Code was ${secret.joinToString()}."
                        gameOver = true
                    } else {
                        statusMessage = "Exact: ${feedback.exact}, Color only: ${feedback.colorOnly}"
                        currentGuess = List(slots) { "" }
                    }
                }
            ) {
                Text("Submit")
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun GuessRow(
    label: String,
    values: List<String>,
    feedback: MastermindFeedback?,
    palette: List<String>,
    onSlotTapped: ((Int) -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            values.forEachIndexed { index, value ->
                val color = colorForName(value, palette)
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Transparent, shape = CircleShape)
                        .clickable(enabled = onSlotTapped != null) { onSlotTapped?.invoke(index) },
                    shape = CircleShape,
                    color = color,
                    tonalElevation = 2.dp,
                ) {}
            }
        }
        if (feedback != null) {
            FeedbackDots(feedback = feedback)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedbackDots(feedback: MastermindFeedback) {
    val dots = List(feedback.exact) { true } + List(feedback.colorOnly) { false }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        dots.forEach { isExact ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (isExact) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape
                    )
                    .let {
                        if (!isExact) it.border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else it
                    }
            )
        }
    }
}

@Composable
private fun ColorPickerRow(
    palette: List<String>,
    onColorSelected: (String) -> Unit
) {
    Text("Pick a color", style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        palette.forEach { name ->
            val color = colorForName(name, palette)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onColorSelected(name) }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color, shape = CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                )
                Text(name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
    }
}

private fun evaluateGuess(secret: List<String>, guess: List<String>): MastermindFeedback {
    var exact = 0
    val secretCounts = mutableMapOf<String, Int>()
    val guessCounts = mutableMapOf<String, Int>()

    secret.forEachIndexed { index, value ->
        if (guess.getOrNull(index) == value) {
            exact++
        } else {
            secretCounts[value] = secretCounts.getOrDefault(value, 0) + 1
            val g = guess.getOrNull(index) ?: ""
            guessCounts[g] = guessCounts.getOrDefault(g, 0) + 1
        }
    }

    var colorOnly = 0
    guessCounts.forEach { (color, count) ->
        if (color.isNotBlank()) {
            colorOnly += minOf(count, secretCounts.getOrDefault(color, 0))
        }
    }

    return MastermindFeedback(exact, colorOnly)
}

private fun colorForName(name: String, palette: List<String>): Color {
    return when (name.trim().lowercase()) {
        "red" -> Color(0xFFE53935)
        "blue" -> Color(0xFF1E88E5)
        "green" -> Color(0xFF43A047)
        "yellow" -> Color(0xFFFDD835)
        "orange" -> Color(0xFFFB8C00)
        "purple" -> Color(0xFF8E24AA)
        "pink" -> Color(0xFFE91E63)
        "white" -> Color(0xFFF5F5F5)
        "black" -> Color(0xFF212121)
        else -> {
            // deterministic fallback based on position in palette or hash
            val idx = palette.indexOfFirst { it.equals(name, ignoreCase = true) }.takeIf { it >= 0 }
                ?: (name.hashCode().absoluteValue % palette.size)
            val colors = listOf(
                Color(0xFF26C6DA), Color(0xFF8D6E63), Color(0xFF5E35B1),
                Color(0xFF7CB342), Color(0xFFFFB300), Color(0xFF00897B)
            )
            colors[idx % colors.size]
        }
    }
}

private val Int.absoluteValue: Int get() = if (this < 0) -this else this
