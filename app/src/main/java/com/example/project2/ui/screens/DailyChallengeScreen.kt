package com.example.project2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project2.data.DailyChallenge

@Composable
fun DailyChallengeScreen(
    challenge: DailyChallenge,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Daily Challenge",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "A curated puzzle from the community to stretch your skills once a day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = challenge.puzzle.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = challenge.puzzle.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Type: ${challenge.puzzle.type.displayName} · Difficulty: ${challenge.puzzle.difficulty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Available until ${challenge.expiresAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { /* TODO: Launch daily puzzle */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start challenge")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Upcoming rewards and streak tracking will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
