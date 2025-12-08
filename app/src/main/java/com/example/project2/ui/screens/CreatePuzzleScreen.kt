package com.example.project2.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.project2.data.AuthRepository
import com.example.project2.data.Difficulty
import com.example.project2.data.FirebaseMindMatchRepository
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleType
import com.example.project2.data.MastermindConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.UUID
import android.net.Uri
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePuzzleScreen(
    modifier: Modifier = Modifier,
    onPuzzleCreated: () -> Unit = {}
) {
    val authRepository = remember { AuthRepository() }
    val repository = remember { FirebaseMindMatchRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fallbackOptions = listOf("Mastermind", "Color Match", "Jigsaw")
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var creatorId by remember { mutableStateOf(authRepository.getCurrentUserId() ?: "unknown") }
    var estimatedDurationSeconds by remember { mutableIntStateOf(120) }
    var puzzleType by remember { mutableStateOf("") }
    var puzzleTypeOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isTypeMenuExpanded by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var mastermindSelectedColors by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mastermindSlots by remember { mutableIntStateOf(4) }
    var mastermindGuesses by remember { mutableIntStateOf(8) }
    var mastermindLevels by remember { mutableIntStateOf(1) }
    var mastermindCode by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> // The result is a nullable Uri
            imageUri = uri
        }
    )
    val mastermindPalette = remember {
        listOf(
            "Red" to Color(0xFFE53935),
            "Blue" to Color(0xFF1E88E5),
            "Green" to Color(0xFF43A047),
            "Yellow" to Color(0xFFFDD835),
            "Orange" to Color(0xFFFB8C00),
            "Purple" to Color(0xFF8E24AA),
            "Pink" to Color(0xFFE91E63),
            "White" to Color(0xFFF5F5F5)
        )
    }

    LaunchedEffect(Unit) {
        val loaded = try {
            withContext(Dispatchers.IO) { repository.loadPuzzleTypes() }
        } catch (e: Exception) {
            fallbackOptions
        }
        puzzleTypeOptions = loaded.ifEmpty { fallbackOptions }
        if (puzzleType.isBlank() && puzzleTypeOptions.isNotEmpty()) {
            puzzleType = puzzleTypeOptions.first()
        }
    }

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
                    value = creatorId,
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    label = { Text("Creator ID (from auth)") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                    value = estimatedDurationSeconds.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { estimatedDurationSeconds = it.coerceAtLeast(30) }
                    },
                    label = { Text("Estimated duration (seconds)") },
                    supportingText = { Text("Defaults to 120s if not provided. Minimum 30s.") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = isTypeMenuExpanded,
                    onExpandedChange = { isTypeMenuExpanded = !isTypeMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = puzzleType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Puzzle type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    DropdownMenu(
                        expanded = isTypeMenuExpanded,
                        onDismissRequest = { isTypeMenuExpanded = false }
                    ) {
                        puzzleTypeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    puzzleType = option
                                    isTypeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { repository.seedPuzzleTypesIfEmpty(fallbackOptions) }
                                val loaded = withContext(Dispatchers.IO) { repository.loadPuzzleTypes() }
                                puzzleTypeOptions = loaded.ifEmpty { fallbackOptions }
                                if (puzzleTypeOptions.isNotEmpty()) {
                                    puzzleType = puzzleTypeOptions.first()
                                }
                            } catch (_: Exception) {
                                puzzleTypeOptions = fallbackOptions
                                puzzleType = fallbackOptions.first()
                            }
                        }
                    }
                ) {
                    Text("Seed puzzle types (dev)")
                }

                if (puzzleType.equals("Mastermind", ignoreCase = true)) {
                    MastermindConfigSection(
                        palette = mastermindPalette,
                        selectedColors = mastermindSelectedColors,
                        slots = mastermindSlots,
                        guesses = mastermindGuesses,
                        levels = mastermindLevels,
                        code = mastermindCode,
                        onColorsChanged = { colors ->
                            mastermindSelectedColors = colors
                            val minSlots = colors.size.coerceAtLeast(1)
                            mastermindSlots = mastermindSlots.coerceIn(minSlots, 8)
                            mastermindCode = adjustMastermindCode(colors, mastermindSlots, mastermindCode)
                        },
                        onSlotsChanged = { desired ->
                            val minSlots = mastermindSelectedColors.size.coerceAtLeast(1)
                            mastermindSlots = desired.coerceIn(minSlots, 8)
                            mastermindCode = adjustMastermindCode(mastermindSelectedColors, mastermindSlots, mastermindCode)
                        },
                        onGuessesChanged = { mastermindGuesses = it.coerceAtLeast(1) },
                        onLevelsChanged = { mastermindLevels = it.coerceAtLeast(1) },
                        onCodeChanged = { mastermindCode = it }
                    )
                }

                if (puzzleType.equals("Jigsaw", ignoreCase = true)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Jigsaw Settings",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Select a default puzzle",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        // A simple data class to represent our selectable local images
                        data class LocalImageOption(val name: String, val resId: Int, val difficulty: Difficulty)

                        val localImages = listOf(
                            LocalImageOption("Jigsaw #1", com.example.project2.R.drawable.jigsaw_image1, Difficulty.EASY),
                            LocalImageOption("Jigsaw #2", com.example.project2.R.drawable.jigsaw_image2, Difficulty.EASY),
                            LocalImageOption("Jigsaw #1", com.example.project2.R.drawable.jigsaw_image3, Difficulty.EASY),
                            LocalImageOption("Jigsaw #1", com.example.project2.R.drawable.jigsaw_image4, Difficulty.EASY)
                        )

                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
                        ) {
                            localImages.forEach { imageOption ->
                                Card(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(140.dp)
                                        .padding(end = 8.dp)
                                        .clickable {
                                            // Instantly create this default puzzle
                                            scope.launch {
                                                try {
                                                    val puzzleId = UUID.randomUUID().toString()
                                                    val resourceUri =
                                                        Uri.parse("android.resource://${context.packageName}/${imageOption.resId}")
                                                    val imageUrl = withContext(Dispatchers.IO) {
                                                        repository.uploadPuzzleImage(
                                                            resourceUri,
                                                            puzzleId
                                                        )
                                                    }
                                                    val descriptor = PuzzleDescriptor(
                                                        id = puzzleId,
                                                        title = imageOption.name,
                                                        description = "A default jigsaw puzzle.",
                                                        type = PuzzleType.JIGSAW,
                                                        creatorId = creatorId.ifBlank { "unknown" },
                                                        difficulty = imageOption.difficulty,
                                                        isUserCreated = true,
                                                        imageUrl = imageUrl
                                                    )
                                                    withContext(Dispatchers.IO) {
                                                        repository.savePuzzleToFirebase(
                                                            descriptor
                                                        )
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            context,
                                                            "Puzzle '${imageOption.name}' created!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        onPuzzleCreated()
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to create puzzle: ${e.message}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.BottomCenter) {
                                        Image(
                                            painter = painterResource(id = imageOption.resId),
                                            contentDescription = imageOption.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Text(
                                            text = imageOption.name,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Or create from your device",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                            }
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload Image")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Jigsaw Image")
                        }

                        imageUri?.let {
                            Image(
                                painter = rememberAsyncImagePainter(model = imageUri),
                                contentDescription = "Selected Jigsaw Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                    }
                }

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
                onClick = {
                    scope.launch {
                        try {
                            val typeEnum = resolvePuzzleType(puzzleType)
                            val duration = Duration.ofSeconds(estimatedDurationSeconds.toLong())

                            if (typeEnum == PuzzleType.JIGSAW) {
                                if (imageUri == null) {
                                    Toast.makeText(context, "Please select an image for the Jigsaw puzzle.", Toast.LENGTH_LONG).show()
                                    return@launch
                                }

                                val puzzleId = UUID.randomUUID().toString()
                                val imageUrl = withContext(Dispatchers.IO) {
                                    repository.uploadPuzzleImage(imageUri!!, puzzleId)
                                }

                                val descriptor = PuzzleDescriptor(
                                    id = puzzleId,
                                    title = title.ifBlank { "Untitled Jigsaw" },
                                    description = description.ifBlank { "A user-created jigsaw puzzle." },
                                    type = typeEnum,
                                    creatorId = creatorId.ifBlank { "unknown" },
                                    difficulty = selectedDifficulty,
                                    estimatedDuration = duration,
                                    isUserCreated = true,
                                    imageUrl = imageUrl
                                )
                                withContext(Dispatchers.IO) { repository.savePuzzleToFirebase(descriptor) }

                            } else {
                                val mastermindConfig = if (typeEnum == PuzzleType.MASTERMIND && mastermindSelectedColors.isNotEmpty()) {
                                    val adjustedCode = adjustMastermindCode(mastermindSelectedColors, mastermindSlots, mastermindCode).map {
                                        if (it.isBlank()) mastermindSelectedColors.first() else it
                                    }
                                    MastermindConfig(
                                        colors = mastermindSelectedColors.toList(),
                                        slots = mastermindSlots,
                                        guesses = mastermindGuesses,
                                        levels = mastermindLevels,
                                        code = adjustedCode
                                    )
                                } else null

                                val descriptor = PuzzleDescriptor(
                                    id = UUID.randomUUID().toString(),
                                    title = title.ifBlank { "Untitled Puzzle" },
                                    description = description.ifBlank { "Created in-app" },
                                    type = typeEnum,
                                    creatorId = creatorId.ifBlank { "unknown" },
                                    difficulty = selectedDifficulty,
                                    estimatedDuration = duration,
                                    isUserCreated = true,
                                    mastermindConfig = mastermindConfig
                                )
                                withContext(Dispatchers.IO) { repository.savePuzzleToFirebase(descriptor) }
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Puzzle created!", Toast.LENGTH_SHORT).show()
                            }
                            onPuzzleCreated()
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to create puzzle: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MastermindConfigSection(
    palette: List<Pair<String, Color>>,
    selectedColors: Set<String>,
    slots: Int,
    guesses: Int,
    levels: Int,
    code: List<String>,
    onColorsChanged: (Set<String>) -> Unit,
    onSlotsChanged: (Int) -> Unit,
    onGuessesChanged: (Int) -> Unit,
    onLevelsChanged: (Int) -> Unit,
    onCodeChanged: (List<String>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Mastermind settings",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Choose the colors used in the code and tune difficulty (slots/guesses/levels).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(text = "Available colors", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            palette.forEach { (name, color) ->
                val selected = selectedColors.contains(name)
                AssistChip(
                    onClick = {
                        val updated = if (selected) selectedColors - name else selectedColors + name
                        onColorsChanged(updated)
                    },
                    label = { Text(name) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) color.copy(alpha = 0.6f) else color.copy(alpha = 0.2f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        val minSlots = selectedColors.size.coerceAtLeast(1)
        Text(text = "Slots (1-8)", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..8).forEach { option ->
                val enabled = option >= minSlots
                FilterChip(
                    selected = slots == option,
                    onClick = { if (enabled) onSlotsChanged(option) },
                    enabled = enabled,
                    label = { Text(option.toString()) }
                )
            }
        }
        Text(
            text = "At least $minSlots slots; colors can repeat when slots exceed unique colors.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(text = "Guesses (1-20)", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..20).forEach { option ->
                FilterChip(
                    selected = guesses == option,
                    onClick = { onGuessesChanged(option) },
                    label = { Text(option.toString()) }
                )
            }
        }

        Text(text = "Secret code placement", style = MaterialTheme.typography.labelLarge)
        if (selectedColors.isEmpty()) {
            Text(
                text = "Select at least one color above to configure the code.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            var expandedIndex by remember { mutableStateOf<Int?>(null) }
            val availableColors = selectedColors.toList()
            val slotsList = adjustMastermindCode(selectedColors, slots, code)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                slotsList.forEachIndexed { index, selection ->
                    ExposedDropdownMenuBox(
                        expanded = expandedIndex == index,
                        onExpandedChange = { expandedIndex = if (expandedIndex == index) null else index }
                    ) {
                        OutlinedTextField(
                            value = selection.ifBlank { "Select color" },
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Slot ${index + 1}") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIndex == index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        DropdownMenu(
                            expanded = expandedIndex == index,
                            onDismissRequest = { expandedIndex = null }
                        ) {
                            availableColors.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        val updated = slotsList.toMutableList()
                                        updated[index] = option
                                        onCodeChanged(updated)
                                        expandedIndex = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = levels.toString(),
            onValueChange = { value -> value.toIntOrNull()?.let { onLevelsChanged(it) } },
            label = { Text("Levels") },
            supportingText = { Text("Number of stages in this Mastermind puzzle.") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun adjustMastermindCode(colors: Set<String>, slots: Int, current: List<String>): List<String> {
    if (slots <= 0) return emptyList()
    if (colors.isEmpty()) return List(slots) { "" }
    val available = colors.toList()
    return MutableList(slots) { index ->
        current.getOrNull(index)?.takeIf { it in colors } ?: available.first()
    }
}

private fun resolvePuzzleType(selected: String): PuzzleType {
    val normalized = selected.trim().replace("\\s+".toRegex(), "_").uppercase()
    return PuzzleType.entries.firstOrNull { entry ->
        entry.name == normalized || entry.displayName.equals(selected, ignoreCase = true)
    } ?: PuzzleType.PATTERN_MEMORY
}
