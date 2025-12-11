package com.example.project2.data

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.firebase.Timestamp
import org.json.JSONObject
import java.time.Instant
import com.google.firebase.storage.ktx.storage
import com.google.firebase.auth.ktx.auth

/**
 * Firebase-backed implementation of the MindMatch repository, responsible for
 * loading/saving puzzles, profiles, progress, leaderboards, and daily challenges.
 */
class FirebaseMindMatchRepository : MindMatchRepository {

    private val db = Firebase.firestore

    // --------- Core MindMatchRepository properties ---------

    /** Profile for the authenticated user; populated after [loadActiveProfile]. */
    override lateinit var activeProfile: PlayerProfile
        private set

    /** Cached list of puzzles combining defaults and any fetched from Firestore. */
    private var cachedPuzzles: List<PuzzleDescriptor> = emptyList()
    /** Cached progress keyed by puzzle id for the active user. */
    private var cachedProgress: Map<String, PuzzleProgress> = emptyMap()
    /** Cached leaderboard entries grouped by puzzle id. */
    private var cachedLeaderboard: Map<String, List<LeaderboardEntry>> = emptyMap()


    private val defaultPuzzles = listOf(
        PuzzleDescriptor(
            id = "play_jigsaw_template",
            title = "Jigsaw Puzzle",
            description = "A classic jigsaw puzzle. Choose your difficulty and solve it as fast as you can!",
            type = PuzzleType.JIGSAW,
            creatorId = "mindmatch_official",
            difficulty = Difficulty.MEDIUM
        ),
        PuzzleDescriptor(
            id = "JIGSAW_EASY",
            title = "Jigsaw (Easy)",
            type = PuzzleType.JIGSAW,
            difficulty = Difficulty.EASY,
            creatorId = "mindmatch_official",
            description = "Leaderboard for 3x3 puzzles."
        ),
        PuzzleDescriptor(
            id = "JIGSAW_MEDIUM",
            title = "Jigsaw (Medium)",
            type = PuzzleType.JIGSAW,
            difficulty = Difficulty.MEDIUM,
            creatorId = "mindmatch_official",
            description = "Leaderboard for 4x4 puzzles."
        ),
        PuzzleDescriptor(
            id = "JIGSAW_HARD",
            title = "Jigsaw (Hard)",
            type = PuzzleType.JIGSAW,
            difficulty = Difficulty.HARD,
            creatorId = "mindmatch_official",
            description = "Leaderboard for 5x5 puzzles."
        ),
        PuzzleDescriptor(
            id = "JIGSAW_EXPERT",
            title = "Jigsaw (Expert)",
            type = PuzzleType.JIGSAW,
            difficulty = Difficulty.EXPERT,
            creatorId = "mindmatch_official",
            description = "Leaderboard for 6x6 puzzles."
        )
    )

    override val puzzles: List<PuzzleDescriptor>
        get() = cachedPuzzles

    override val progressByPuzzle: Map<String, PuzzleProgress>
        get() = cachedProgress

    override val leaderboard: Map<String, List<LeaderboardEntry>>
        get() = cachedLeaderboard



    /** Latest daily challenge placeholder until Firestore-backed daily challenges are wired up. */
    override val dailyChallenge: DailyChallenge
        get() = throw NotImplementedError("Firebase daily challenge not implemented yet.")

    // --------- Puzzles ---------

    /**
     * Fetch all puzzles from Firestore, merge with defaults, and cache locally.
     */
    suspend fun loadPuzzlesFromFirebase() {
        val snapshot = db.collection("puzzles").get().await()

        val firebasePuzzles = snapshot
            .toObjects(FirebasePuzzle::class.java)
            .map { it.toPuzzle() }

        // Combine the default puzzles with the ones from Firebase
        cachedPuzzles = defaultPuzzles + firebasePuzzles
    }

    /**
     * Persist a puzzle descriptor to Firestore and update cache.
     *
     * @param puzzle domain descriptor to save
     */
    suspend fun savePuzzleToFirebase(puzzle: PuzzleDescriptor) {
        val firebasePuzzle = puzzle.toFirebase()

        db.collection("puzzles")
            .document(firebasePuzzle.id)
            .set(firebasePuzzle)
            .await()
    }

    /**
     * Delete a puzzle document and evict it from caches.
     *
     * @param puzzleId identifier of the puzzle to remove
     */
    suspend fun deletePuzzleFromFirebase(puzzleId: String) {
        db.collection("puzzles")
            .document(puzzleId)
            .delete()
            .await()

        // Update cache so UI reflects removal immediately
        cachedPuzzles = cachedPuzzles.filterNot { it.id == puzzleId }
        cachedProgress = cachedProgress - puzzleId
        cachedLeaderboard = cachedLeaderboard - puzzleId
    }

    /**
     * Upload a puzzle image to Firebase Storage and return its public URL.
     *
     * @param imageUri local URI of the image to upload
     * @param puzzleId puzzle id used to name the storage object
     * @return download URL as a string once the upload completes
     */
    suspend fun uploadPuzzleImage(imageUri: android.net.Uri, puzzleId: String): String {
        val storageRef = Firebase.storage.reference
        val imageRef = storageRef.child("puzzle_images/${puzzleId}.jpg")
        // Upload the file and wait for the result
        imageRef.putFile(imageUri).await()
        // Get the download URL and wait for it
        return imageRef.downloadUrl.await().toString()
    }

    // --------- Profile + progress ---------

    /**
     * Load the active profile from Firestore via AuthRepository and hydrate progress.
     *
     * @param authRepo authentication repository providing the current user
     */
    suspend fun loadActiveProfile(authRepo: AuthRepository) {
        val userId = authRepo.getCurrentUserId()
            ?: throw Exception("User not logged in")

        // Convert callback → suspend
        val profile = suspendCancellableCoroutine<PlayerProfile> { cont ->
            authRepo.loadUserProfile(userId) { loaded, error ->
                if (loaded != null) {
                    cont.resume(loaded)
                } else {
                    cont.resumeWithException(Exception(error ?: "Unknown error"))
                }
            }
        }
        activeProfile = profile

        // also load progress for this user
        loadProgressForUser(userId)
    }

    /**
     * Load the most recent daily challenge document and map it to a Mastermind puzzle.
     *
     * @return a DailyChallenge or null if none/malformed
     */
    suspend fun loadLatestDailyChallenge(): DailyChallenge? {
        val snapshot = db.collection(DAILY_CHALLENGES_COLLECTION)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull() ?: return null
        val rawJson = doc.getString("rawJson") ?: return null
        val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
        val puzzleId = "${doc.id}_${createdAt.seconds}"

        val parsed = runCatching { JSONObject(rawJson) }.getOrNull() ?: return null
        val title = parsed.optString("title", "Daily Mastermind")
        val description = parsed.optString("description", "AI-generated Mastermind challenge")
        val configObj = parsed.optJSONObject("mastermindConfig") ?: return null
        val colors = configObj.optJSONArray("colors")?.let { arr ->
            List(arr.length()) { index -> arr.optString(index) }.filter { it.isNotBlank() }
        } ?: emptyList()
        val slots = configObj.optInt("slots", 4)
        val guesses = configObj.optInt("guesses", 8)
        val levels = configObj.optInt("levels", 1)
        val code = configObj.optJSONArray("code")?.let { arr ->
            List(arr.length()) { index -> arr.optString(index) }
        } ?: emptyList()

        if (colors.isEmpty() || code.isEmpty()) return null

        val mastermindConfig = MastermindConfig(
            colors = colors,
            slots = slots,
            guesses = guesses,
            levels = levels,
            code = code
        )

        val puzzle = PuzzleDescriptor(
            id = puzzleId,
            title = title,
            description = description,
            type = PuzzleType.MASTERMIND,
            creatorId = doc.getString("createdBy") ?: "daily-gemini",
            difficulty = Difficulty.MEDIUM,
            isUserCreated = true,
            mastermindConfig = mastermindConfig
        )

        // Stub DailyPuzzleContent to satisfy type; the actual play will use the Mastermind puzzle flow.
        val placeholderContent = DailyPuzzleContent(
            instructions = "Play today's Mastermind puzzle.",
            grid = emptyList(),
            controls = emptyList(),
            stats = PuzzleStats(target = 0, streak = 0, timeRemainingSeconds = 0)
        )

        return DailyChallenge(
            puzzle = puzzle,
            expiresAt = createdAt.toDate().toInstant().plusSeconds(86_400),
            content = placeholderContent
        )
    }

    /**
     * Read all progress documents for the user and cache them by puzzle id.
     *
     * @param userId Firebase auth uid whose progress should be loaded
     */
    suspend fun loadProgressForUser(userId: String) {
        val snapshot = db.collection("users")
            .document(userId)
            .collection("progress")
            .get()
            .await()

        cachedProgress = snapshot.documents
            .mapNotNull { it.toObject(FirebasePuzzleProgress::class.java)?.toProgress() }
            .associateBy { it.puzzleId }
    }

    /**
     * Save/update progress for a puzzle and refresh the cache.
     *
     * @param userId Firebase auth uid
     * @param progress progress state to persist
     */
    suspend fun saveProgress(userId: String, progress: PuzzleProgress) {
        val firebaseProgress = progress.toFirebase()

        db.collection("users")
            .document(userId)
            .collection("progress")
            .document(progress.puzzleId)
            .set(firebaseProgress)
            .await()

        // update local cache so UI sees latest data immediately
        cachedProgress = cachedProgress.toMutableMap().apply {
            put(progress.puzzleId, progress)
        }
    }

    /**
     * Record a puzzle play timestamp for a user and update the active profile cache.
     *
     * @param userId Firebase auth uid
     * @param puzzleId id of the puzzle that was played
     * @param playedAt timestamp when the play occurred (defaults to now)
     */
    suspend fun recordPuzzlePlayed(userId: String, puzzleId: String, playedAt: Timestamp = Timestamp.now()) {
        db.collection("users")
            .document(userId)
            .update(mapOf("puzzlesPlayed.$puzzleId" to playedAt))
            .await()

        // keep activeProfile in sync so UI can reflect immediately
        if (::activeProfile.isInitialized) {
            val updated = activeProfile.puzzlesPlayed.toMutableMap().apply {
                put(puzzleId, playedAt)
            }
            activeProfile = activeProfile.copy(puzzlesPlayed = updated)
        }
    }



    /**
     * Load available puzzle types from Firestore.
     *
     * @return list of puzzle type names
     */
    suspend fun loadPuzzleTypes(): List<String> {
        val snapshot = db.collection("puzzleTypes").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.getString("name")?.takeIf { it.isNotBlank() }
        }
    }

    /**
     * Seed default puzzle types if the collection is empty (dev helper).
     *
     * @param defaults names to insert when no puzzle types exist
     */
    suspend fun seedPuzzleTypesIfEmpty(
        defaults: List<String> = listOf("Mastermind", "Color Match", "Jigsaw")
    ) {
        val snapshot = db.collection("puzzleTypes").get().await()
        val existingNames = snapshot.documents.mapNotNull { it.getString("name") }.toSet()

        defaults.forEach { type ->
            if (existingNames.contains(type)) return@forEach
            db.collection("puzzleTypes")
                .document(type.lowercase().replace("\\s+".toRegex(), "_"))
                .set(mapOf("name" to type))
                .await()
        }
    }

    companion object {
        private const val DAILY_CHALLENGES_COLLECTION = "dailyChallenges"
    }
    // ------- LEADERBOARD -------

    /**
     * Load leaderboard entries from Firestore and group them by puzzle id.
     *
     * Caches the top 20 scores per puzzle for quick access.
     */
    suspend fun loadLeaderboard() {
        val snapshot = db.collection("leaderboard")
            .get()
            .await()

        val entries = snapshot
            .toObjects(FirebaseLeaderboardEntry::class.java)
            .map { it.toEntry() }

        // group by puzzleId and sort each list by score desc
        cachedLeaderboard = entries
            .groupBy { it.puzzleId }
            .mapValues { (_, list) ->
                list.sortedByDescending { it.score }
                    .take(20) // keep top 20 per puzzle
            }
    }

    /**
     * Add a leaderboard entry for a puzzle and refresh the cached leaderboard.
     *
     * @param puzzleId target puzzle id for the leaderboard
     * @param playerName display name to record
     * @param score higher is better; meaning depends on puzzle type
     */
    suspend fun submitLeaderboardEntry(
        puzzleId: String,
        playerName: String,
        score: Int
    ) {
        val entry = LeaderboardEntry(
            puzzleId = puzzleId,
            playerName = playerName,
            score = score,
            recordedAt = java.time.Instant.now()
        )

        db.collection("leaderboard")
            .add(entry.toFirebase())
            .await()

        // refresh cache \
        loadLeaderboard()
    }

    /**
     * Add a leaderboard entry for a jigsaw completion time and refresh leaderboard.
     *
     * @param timeInSeconds completion time for the puzzle
     * @param gridSize dimension of the jigsaw grid used to infer difficulty
     */
    suspend fun saveJigsawScore(timeInSeconds: Long, gridSize: Int) {
        val auth = com.google.firebase.ktx.Firebase.auth
        val currentUser = auth.currentUser ?: return

        val difficulty = when (gridSize) {
            3 -> Difficulty.EASY
            4 -> Difficulty.MEDIUM
            5 -> Difficulty.HARD
            else -> Difficulty.EXPERT
        }

        val puzzleIdForLeaderboard = "JIGSAW_${difficulty.name}"

        val entry = LeaderboardEntry(
            puzzleId = puzzleIdForLeaderboard,
            playerName = currentUser.displayName ?: "Anonymous",
            score = timeInSeconds.toInt(),
            recordedAt = java.time.Instant.now()
        )

        db.collection("leaderboard")
            .add(entry.toFirebase())
            .await()

        loadLeaderboard()
    }
}
