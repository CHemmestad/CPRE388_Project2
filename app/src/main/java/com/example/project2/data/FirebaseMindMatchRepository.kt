package com.example.project2.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    // --------- Optional: puzzle type helpers you already had ---------

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
}
