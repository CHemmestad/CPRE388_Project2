package com.example.project2.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.project2.data.PuzzleDescriptor
import kotlin.math.roundToInt

@Composable
fun JigsawPuzzleScreen(
    puzzle: PuzzleDescriptor,
    onBack: () -> Unit,
    gridSize: Int, // <-- 1. ADD gridSize AS A PARAMETER
) {
    // --- STATE MANAGEMENT ---
    var puzzlePieces by remember { mutableStateOf<List<PuzzlePiece>>(emptyList()) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    var boardTopLeft by remember { mutableStateOf(Offset.Zero) }
    var draggedPieceId by remember { mutableStateOf<Int?>(null) }
    var targetDropIndex by remember { mutableStateOf<Int?>(null) }
    var isSolved by remember { mutableStateOf(false) }
    var fingerDragOffset by remember { mutableStateOf(Offset.Zero) }

    // --- LOGIC ---
    val context = LocalContext.current

    fun checkForSolution() {
        if (puzzlePieces.isEmpty()) return
        // A puzzle is solved if every piece's ID matches its current position in the list
        val solved = puzzlePieces.all { piece -> piece.id == puzzlePieces.indexOf(piece) }
        if (solved) {
            isSolved = true
        }
    }

    // This effect runs when the board is measured or the difficulty (gridSize) changes
    LaunchedEffect(boardSize, gridSize) { // <-- 2. ADD gridSize TO KEY
        if (boardSize != IntSize.Zero && puzzlePieces.isEmpty()) {
            val availableImages = listOf("jigsaw_image1", "jigsaw_image2", "jigsaw_image3", "jigsaw_image4")
            val randomImageName = availableImages.random()
            val imageResId = context.resources.getIdentifier(randomImageName, "drawable", context.packageName)

            if (imageResId != 0) {
                val originalBitmap = BitmapFactory.decodeResource(context.resources, imageResId)
                val scaled = Bitmap.createScaledBitmap(
                    originalBitmap, boardSize.width, boardSize.height, false
                ).asImageBitmap()

                // <-- 3. USE THE PASSED gridSize TO SLICE THE BITMAP
                val newPieces = sliceBitmap(scaled, gridSize = gridSize)
                puzzlePieces = newPieces.shuffled()
                checkForSolution()
            }
        }
    }

    // --- UI LAYOUT ---
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = puzzle.title, style = MaterialTheme.typography.headlineSmall)
            Text(text = puzzle.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            // Display the difficulty based on the grid size
            Text(text = "${puzzle.type.displayName} • ${gridSize}x$gridSize", style = MaterialTheme.typography.labelLarge)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { layoutCoordinates ->
                        if (boardSize == IntSize.Zero) {
                            boardSize = layoutCoordinates.size
                            boardTopLeft = layoutCoordinates.positionInWindow()
                        }
                    }
            ) {
                if (puzzlePieces.isNotEmpty()) {
                    PuzzleBoard(
                        pieces = puzzlePieces,
                        gridSize = gridSize, // <-- 4. PASS gridSize TO THE PuzzleBoard
                        draggedPieceId = draggedPieceId,
                        targetDropIndex = targetDropIndex,
                        isSolved = isSolved,
                        fingerDragOffset = fingerDragOffset,
                        onDragStart = { id ->
                            if (!isSolved) {
                                draggedPieceId = id
                            }
                        },
                        onDrag = { position ->
                            fingerDragOffset = position
                            val screenPosition = boardTopLeft + position
                            val localFingerPosition = screenPosition - boardTopLeft

                            // <-- 5. MAKE DROP TARGET LOGIC DYNAMIC
                            val pieceSize = boardSize.width / gridSize
                            val col = (localFingerPosition.x / pieceSize).toInt()
                            val row = (localFingerPosition.y / pieceSize).toInt()

                            if (col in 0 until gridSize && row in 0 until gridSize) {
                                targetDropIndex = (row * gridSize) + col
                            } else {
                                targetDropIndex = null
                            }
                        },
                        onDragEnd = {
                            val draggedIndex = puzzlePieces.indexOfFirst { it.id == draggedPieceId }
                            if (targetDropIndex != null && draggedIndex != -1 && targetDropIndex != draggedIndex) {
                                val mutablePieces = puzzlePieces.toMutableList()
                                val temp = mutablePieces[draggedIndex]
                                mutablePieces[draggedIndex] = mutablePieces[targetDropIndex!!]
                                mutablePieces[targetDropIndex!!] = temp
                                puzzlePieces = mutablePieces
                                checkForSolution()
                            }
                            draggedPieceId = null
                            targetDropIndex = null
                            fingerDragOffset = Offset.Zero
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (boardSize == IntSize.Zero) "Loading puzzle..." else "Error loading image.")
                    }
                }
            }

            // ... inside the JigsawPuzzleScreen composable

            if (isSolved) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing to fit the new button
                    ) {
                        Text("You Win!", style = MaterialTheme.typography.displayMedium, color = Color.White)

                        // This button restarts the same puzzle
                        Button(onClick = {
                            puzzlePieces = puzzlePieces.shuffled()
                            isSolved = false
                        }) { Text("Play Again") }

                        // ▼▼▼ NEW BUTTON ADDED HERE ▼▼▼
                        // This button calls onBack, which will navigate to the previous screen (DifficultySelectionScreen)
                        Button(onClick = onBack) { Text("Switch Difficulty") }

                        // This button is for going back to the main puzzle library (now functionally similar to Switch Difficulty, but could be different in other contexts)
                        Button(onClick = {
                            puzzlePieces = emptyList()
                            boardSize = IntSize.Zero
                            isSolved = false
                            // You might want to navigate further back, but for now, onBack is sufficient.
                            // To go all the way home, you'd need a more specific callback.
                            // For this use case, let's also have it just go back.
                            onBack()
                        }) { Text("Switch Puzzle") }

                        // This button remains for navigating all the way back home.
                        // Note: Depending on your nav graph, this might do the same as the others.
                        // We'll leave it for completeness.
                        Button(onClick = onBack) { Text("Back to Home") }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (puzzlePieces.isNotEmpty() && !isSolved) {
                    puzzlePieces = puzzlePieces.shuffled()
                }
            },
            modifier = Modifier.padding(16.dp),
            enabled = !isSolved
        ) {
            Text("Shuffle")
        }
    }
}

@Composable
private fun PuzzleBoard(
    pieces: List<PuzzlePiece>,
    gridSize: Int, // <-- Already accepts this
    draggedPieceId: Int?,
    targetDropIndex: Int?,
    isSolved: Boolean,
    fingerDragOffset: Offset,
    onDragStart: (Int) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val draggedPiece = pieces.find { it.id == draggedPieceId }

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridSize),
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isSolved
        ) {
            itemsIndexed(pieces) { index, piece ->
                val isBeingDragged = (piece.id == draggedPieceId)
                val isDropTarget = (index == targetDropIndex)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .padding(1.dp)
                        .background(
                            if (isDropTarget && !isSolved) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                ) {
                    Image(
                        bitmap = piece.bitmap,
                        contentDescription = "Puzzle Piece ${piece.id}",
                        alpha = if (isBeingDragged) 0.0f else 1.0f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (!isSolved) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pieces) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                // <-- 6. MAKE onDragStart DYNAMIC
                                val pieceSize = size.width / gridSize
                                val col = (startOffset.x / pieceSize).toInt().coerceIn(0, gridSize - 1)
                                val row = (startOffset.y / pieceSize).toInt().coerceIn(0, gridSize - 1)
                                val startIndex = (row * gridSize) + col
                                if (startIndex < pieces.size) {
                                    onDragStart(pieces[startIndex].id)
                                }
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, _ ->
                                onDrag(change.position)
                                change.consume()
                            }
                        )
                    }
            )
        }

        if (draggedPiece != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(
                    image = draggedPiece.bitmap,
                    topLeft = Offset(
                        x = fingerDragOffset.x - (draggedPiece.bitmap.width / 2),
                        y = fingerDragOffset.y - (draggedPiece.bitmap.height / 2)
                    ),
                    alpha = 0.8f
                )
            }
        }
    }
}

private data class PuzzlePiece(
    val id: Int,
    val bitmap: ImageBitmap,
    val correctOffset: Offset
)

private fun sliceBitmap(imageBitmap: ImageBitmap, gridSize: Int): List<PuzzlePiece> {
    val pieceWidth = imageBitmap.width / gridSize
    val pieceHeight = imageBitmap.height / gridSize
    val pieces = mutableListOf<PuzzlePiece>()
    var id = 0
    for (y in 0 until gridSize) {
        for (x in 0 until gridSize) {
            val correctOffset = Offset((x * pieceWidth).toFloat(), (y * pieceHeight).toFloat())
            val pieceBitmap = Bitmap.createBitmap(
                imageBitmap.asAndroidBitmap(),
                correctOffset.x.roundToInt(),
                correctOffset.y.roundToInt(),
                pieceWidth,
                pieceHeight
            ).asImageBitmap()

            pieces.add(
                PuzzlePiece(id = id++, bitmap = pieceBitmap, correctOffset = correctOffset)
            )
        }
    }
    return pieces
}
