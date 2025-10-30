package com.example.project2.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleProgress
import com.example.project2.data.PuzzleType
import com.example.project2.ui.widgets.StatChip
import java.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun PatternMemoryScreen(
    puzzle: PuzzleDescriptor,
    progress: PuzzleProgress?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val tiles = remember { defaultPatternTiles() }
    val scope = rememberCoroutineScope()

    var round by rememberSaveable { mutableStateOf(1) }
    var phase by remember { mutableStateOf(PatternPhase.Preview) }
    var activeTileIndex by remember { mutableStateOf<Int?>(null) }
    var previewTrigger by remember { mutableStateOf(0) }
    var userStep by remember { mutableStateOf(0) }
    var mistakes by remember { mutableStateOf(0) }

    val patternLength = max(3, round + 2)
    val pattern = remember(puzzle.id, round) {
        generatePatternSequence(
            seed = puzzle.id,
            length = patternLength,
            tileCount = tiles.size
        )
    }

    LaunchedEffect(round, previewTrigger, phase) {
        if (phase == PatternPhase.Preview) {
            activeTileIndex = null
            userStep = 0
            delay(400)
            pattern.forEach { tileIndex ->
                activeTileIndex = tileIndex
                delay(420)
                activeTileIndex = null
                delay(160)
            }
            phase = PatternPhase.Recall
        }
    }

    fun startPreview() {
        phase = PatternPhase.Preview
        activeTileIndex = null
        mistakes = 0
        previewTrigger += 1
    }

    fun handleTileTap(tileIndex: Int) {
        if (phase != PatternPhase.Recall) return

        scope.launch {
            activeTileIndex = tileIndex
            delay(150)
            activeTileIndex = null
        }

        val expectedTile = pattern[userStep]
        if (tileIndex == expectedTile) {
            userStep += 1
            if (userStep == pattern.size) {
                phase = PatternPhase.Completed
            }
        } else {
            mistakes += 1
            phase = PatternPhase.Failed
        }
    }

    fun advanceRound() {
        round += 1
        startPreview()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = puzzle.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = puzzle.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${puzzle.type.displayName} • Difficulty ${puzzle.difficulty}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val status = phase.statusHeadline()
                val instruction = when (phase) {
                    PatternPhase.Preview -> "Watch the tiles pulse to memorize the order."
                    PatternPhase.Recall -> "Tap the tiles in the exact order you just saw."
                    PatternPhase.Completed -> "Great recall! Increase the length or replay to push your streak."
                    PatternPhase.Failed -> "Sequence interrupted. Replay the pattern or start over."
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Round $round · ${pattern.size} steps · Mistakes $mistakes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        PatternBoard(
            tiles = tiles,
            columns = 3,
            activeTileIndex = activeTileIndex,
            enabled = phase == PatternPhase.Recall,
            onTileSelected = ::handleTileTap,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { startPreview() },
                enabled = phase != PatternPhase.Preview
            ) {
                Text("Replay pattern")
            }
            when (phase) {
                PatternPhase.Completed -> {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { advanceRound() }
                    ) {
                        Text("Next round")
                    }
                }
                PatternPhase.Failed -> {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { startPreview() }
                    ) {
                        Text("Try again")
                    }
                }
                else -> {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { /* Waiting for recall */ },
                        enabled = false
                    ) {
                        Text("Ready")
                    }
                }
            }
        }

        progress?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(
                    title = "Best score",
                    value = it.bestScore.toString(),
                    modifier = Modifier.weight(1f)
                )
                it.bestTime?.let { duration ->
                    StatChip(
                        title = "Best time",
                        value = duration.toReadableString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PuzzleNotReadyScreen(
    puzzle: PuzzleDescriptor,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = puzzle.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "The ${puzzle.type.displayName} experience is coming soon.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Check back after the core gameplay loop lands in the roadmap.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PuzzleNotFoundScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Puzzle not found",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "We couldn't load that puzzle. Please try again from the library.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PatternBoard(
    tiles: List<PatternTile>,
    columns: Int,
    activeTileIndex: Int?,
    enabled: Boolean,
    onTileSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { tile ->
                    PatternTileButton(
                        tile = tile,
                        isActive = activeTileIndex == tile.index,
                        enabled = enabled,
                        onClick = { onTileSelected(tile.index) },
                        modifier = Modifier.weight(1f)
                    )
                }
                val placeholders = columns - row.size
                repeat(placeholders.coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PatternTileButton(
    tile: PatternTile,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = tile.color
    val background = if (isActive) {
        baseColor.copy(alpha = 0.95f)
    } else {
        baseColor.copy(alpha = 0.7f)
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
    }

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        color = background,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = if (isActive) 10.dp else 2.dp,
        shadowElevation = if (isActive) 6.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tile.label,
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor
            )
        }
    }
}

private data class PatternTile(
    val index: Int,
    val label: String,
    val color: Color
)

private enum class PatternPhase {
    Preview,
    Recall,
    Completed,
    Failed
}

private fun PatternPhase.statusHeadline(): String = when (this) {
    PatternPhase.Preview -> "Memorize the pulse"
    PatternPhase.Recall -> "Recreate the pattern"
    PatternPhase.Completed -> "Perfect memory!"
    PatternPhase.Failed -> "Let's try again"
}

private fun defaultPatternTiles(): List<PatternTile> = listOf(
    PatternTile(index = 0, label = "A", color = Color(0xFFE53935)),
    PatternTile(index = 1, label = "B", color = Color(0xFF8E24AA)),
    PatternTile(index = 2, label = "C", color = Color(0xFF3949AB)),
    PatternTile(index = 3, label = "D", color = Color(0xFF00897B)),
    PatternTile(index = 4, label = "E", color = Color(0xFFFDD835)),
    PatternTile(index = 5, label = "F", color = Color(0xFFFF7043))
)

private fun generatePatternSequence(
    seed: String,
    length: Int,
    tileCount: Int
): List<Int> {
    val random = Random(seed.hashCode() * 31 + length)
    return List(length) { random.nextInt(tileCount) }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Duration.toReadableString(): String {
    val totalSeconds = seconds
    val minutes = (totalSeconds / 60).toInt()
    val secondsRemainder = (totalSeconds % 60).toInt()
    return "%d:%02d".format(minutes, secondsRemainder)
}
