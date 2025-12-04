package com.example.project2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.project2.data.PlayerProfile

@Composable
fun ProfileScreen(
    profile: PlayerProfile?,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {}
) {
    var displayName by remember(profile!!.id) { mutableStateOf(profile!!.displayName) }
    var bio by remember(profile.id) { mutableStateOf(profile!!.bio) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Player Profile",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Profiles sync across devices when cloud backup is enabled.",
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = displayName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Preferred difficulty: ${profile.preferredDifficulty}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    trailingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { /* TODO: Save to persistence */ }
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save changes")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { /* TODO: Delete profile */ }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete profile")
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onLogout
                    ) {
                        Text("Log out")
                    }
                }
            }
        }
    }
}
