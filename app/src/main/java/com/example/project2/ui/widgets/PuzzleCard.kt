package com.example.project2.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleProgress
import com.example.project2.ui.util.formatAsDisplay
import java.time.Instant

@Composable
fun PuzzleCard(
    puzzle: PuzzleDescriptor,
    modifier: Modifier = Modifier,
    progress: PuzzleProgress? = null,
    lastPlayed: Instant? = null,
    actionIcon: ImageVector? = null,
    actionText: String,
    onActionClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = puzzle.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = puzzle.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = puzzle.type.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Difficulty: ${puzzle.difficulty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (progress != null) {
                Text(
                    text = "Best score: ${progress.bestScore} · Level ${progress.currentLevel}/${progress.levelsUnlocked}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            lastPlayed?.let { playedAt ->
                Text(
                    text = "Last played ${playedAt.formatAsDisplay()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onActionClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (actionIcon != null) {
                    Icon(actionIcon, contentDescription = actionText)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(actionText)
            }
        }
    }
}
