package com.example.project2.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.concurrent.TimeUnit
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project2.data.PuzzleDescriptor
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.project2.ui.MindMatchViewModel

/**
 * Drag-and-drop jigsaw experience with timing and leaderboard submission.
 *
 * @param puzzle descriptor for the puzzle being played
 * @param onBack callback to exit to previous screen
 * @param gridSize dimension of the board (e.g., 3 for 3x3)
 * @param viewModel shared view model used to submit leaderboard scores
 */
@Composable
fun JigsawPuzzleScreen(
    puzzle: PuzzleDescriptor,
    onBack: () -> Unit,
    gridSize: Int,
    viewModel: MindMatchViewModel
) {
    var elapsedTime by remember { mutableStateOf(0L) }
    var puzzlePieces by remember { mutableStateOf<List<PuzzlePiece>>(emptyList()) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    var boardTopLeft by remember { mutableStateOf(Offset.Zero) }
    var draggedPieceId by remember { mutableStateOf<Int?>(null) }
    var targetDropIndex by remember { mutableStateOf<Int?>(null) }
    var isSolved by remember { mutableStateOf(false) }
    var fingerDragOffset by remember { mutableStateOf(Offset.Zero) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var scoreSaved by remember { mutableStateOf(false) }

    fun checkForSolution() {
        if (puzzlePieces.isEmpty()) return
        val solved = puzzlePieces.all { piece ->
            piece.id == puzzlePieces.indexOf(piece)
        }
        if (solved) {
            isSolved = true
        }
    }

    LaunchedEffect(key1 = isSolved) {
        if (isSolved && !scoreSaved) {
            scoreSaved = true
            val timeInSeconds = elapsedTime

            val difficulty = when (gridSize) {
                3 -> com.example.project2.data.Difficulty.EASY
                4 -> com.example.project2.data.Difficulty.MEDIUM
                5 -> com.example.project2.data.Difficulty.HARD
                else -> com.example.project2.data.Difficulty.EXPERT
            }

            val puzzleIdForLeaderboard = "JIGSAW_${difficulty.name}"

            scope.launch {
                viewModel.submitLeaderboardScore(
                    puzzleId = puzzleIdForLeaderboard,
                    score = timeInSeconds.toInt()
                )
                android.widget.Toast.makeText(context, "Score saved!", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else if (!isSolved) {
            // Simple ticker to track elapsed time while the board is unsolved.
            while (true) {
                delay(1000L)
                elapsedTime++
            }
        }
    }

    /**
     * Format elapsed seconds as mm:ss for display.
     *
     * @param seconds total elapsed seconds
     * @return formatted timer string
     */
    fun formatTime(seconds: Long): String {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }


    val availableImages = listOf(
        com.example.project2.R.drawable.jigsaw_image1,
        com.example.project2.R.drawable.jigsaw_image2,
        com.example.project2.R.drawable.jigsaw_image3,
        com.example.project2.R.drawable.jigsaw_image4
    )

    val imageToUse = remember(puzzle.id) {
        // Randomize image for the template; otherwise honor provided resource id.
        if (puzzle.id == "play_jigsaw_template") {
            availableImages.random()
        } else {
            puzzle.localImageResId ?: com.example.project2.R.drawable.jigsaw_image1
        }
    }

    LaunchedEffect(puzzle.id, boardSize, gridSize) {
        if (boardSize != IntSize.Zero && puzzlePieces.isEmpty()) {

            val originalBitmap = BitmapFactory.decodeResource(context.resources, imageToUse)

            val scaled = Bitmap.createScaledBitmap(
                originalBitmap, boardSize.width, boardSize.height, false
            ).asImageBitmap()

            val newPieces = sliceBitmap(scaled, gridSize = gridSize)
            puzzlePieces = newPieces.shuffled()
            checkForSolution()
        }
    }

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

            Text(
                text = "Time: ${formatTime(elapsedTime)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(text = puzzle.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
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
                        gridSize = gridSize,
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


            if (isSolved) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("You Win!", style = MaterialTheme.typography.displayMedium, color = Color.White)

                        Button(onClick = {
                            puzzlePieces = puzzlePieces.shuffled()
                            isSolved = false
                        }) { Text("Play Again") }

                        Button(onClick = onBack) { Text("Switch Difficulty") }

                        Button(onClick = {
                            puzzlePieces = emptyList()
                            boardSize = IntSize.Zero
                            isSolved = false
                            onBack()
                        }) { Text("Switch Puzzle") }


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

/**
 * Renders the jigsaw grid, handles drag gestures, and draws the floating piece.
 *
 * @param pieces current shuffled pieces
 * @param gridSize dimension of the grid (e.g., 3 = 3x3)
 * @param draggedPieceId id of the piece being dragged
 * @param targetDropIndex index of the hovered drop target
 * @param isSolved flag to disable interactions when completed
 * @param fingerDragOffset last known finger position relative to board
 * @param onDragStart called with piece id when drag begins
 * @param onDrag called with pointer position as drag continues
 * @param onDragEnd called when drag ends or cancels
 * @param modifier layout modifier passed from parent
 */
@Composable
private fun PuzzleBoard(
    pieces: List<PuzzlePiece>,
    gridSize: Int,
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
                            if (isDropTarget && !isSolved) MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f
                            )
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
                                // Resolve which piece was pressed so we can move it.
                                val pieceSize = size.width / gridSize
                                val col =
                                    (startOffset.x / pieceSize).toInt().coerceIn(0, gridSize - 1)
                                val row =
                                    (startOffset.y / pieceSize).toInt().coerceIn(0, gridSize - 1)
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

/** Represents one sliced jigsaw tile and its original location. */
private data class PuzzlePiece(
    val id: Int,
    val bitmap: ImageBitmap,
    val correctOffset: Offset
)

/**
 * Slice a bitmap into equal square pieces for the jigsaw board.
 *
 * @param imageBitmap source image to slice
 * @param gridSize dimension of the grid (e.g., 4 = 4x4)
 * @return list of puzzle pieces annotated with their original offsets
 */
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
