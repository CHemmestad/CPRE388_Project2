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

class FirebaseMindMatchRepository : MindMatchRepository {

    private val db = Firebase.firestore

    // --------- Core MindMatchRepository properties ---------

    override lateinit var activeProfile: PlayerProfile
        private set

    private var cachedPuzzles: List<PuzzleDescriptor> = emptyList()
    private var cachedProgress: Map<String, PuzzleProgress> = emptyMap()
    private var cachedLeaderboard: Map<String, List<LeaderboardEntry>> = emptyMap()

    override val puzzles: List<PuzzleDescriptor>
        get() = cachedPuzzles

    override val progressByPuzzle: Map<String, PuzzleProgress>
        get() = cachedProgress

    override val leaderboard: Map<String, List<LeaderboardEntry>>
        get() = cachedLeaderboard



    override val dailyChallenge: DailyChallenge
        get() = throw NotImplementedError("Firebase daily challenge not implemented yet.")

    // --------- Puzzles ---------

    suspend fun loadPuzzlesFromFirebase() {
        val snapshot = db.collection("puzzles").get().await()

        cachedPuzzles = snapshot
            .toObjects(FirebasePuzzle::class.java)
            .map { it.toPuzzle() }
    }

    suspend fun savePuzzleToFirebase(puzzle: PuzzleDescriptor) {
        val firebasePuzzle = puzzle.toFirebase()

        db.collection("puzzles")
            .document(firebasePuzzle.id)
            .set(firebasePuzzle)
            .await()
    }

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

    // --------- Profile + progress ---------

    /**
     * Loads the active profile from Firestore using AuthRepository,
     * and also loads this user's puzzle progress.
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
     * Loads the most recent daily challenge generated and maps it to a Mastermind puzzle descriptor.
     * Returns null if none exist or the payload is malformed.
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
     * Reads all puzzle progress documents for a given user:
     * users/{userId}/progress/{puzzleId}
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
     * Saves or updates progress for a single puzzle for this user.
     * users/{userId}/progress/{puzzleId}
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
     * Records that a puzzle was played at a given time for the active user.
     * Stores under users/{userId}/puzzlesPlayed map in Firestore and updates cache.
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



    suspend fun loadPuzzleTypes(): List<String> {
        val snapshot = db.collection("puzzleTypes").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.getString("name")?.takeIf { it.isNotBlank() }
        }
    }

    /**
     * Convenience helper for developers: seed puzzleTypes collection if empty.
     * Call this from a one-off debug action; do NOT leave it on a hot path.
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
     * Add a new leaderboard entry for a puzzle.
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
}
