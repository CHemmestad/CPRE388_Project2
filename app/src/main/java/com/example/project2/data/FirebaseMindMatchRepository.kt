package com.example.project2.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resumeWithException

class FirebaseMindMatchRepository : MindMatchRepository {

    private val db = Firebase.firestore

    override lateinit var activeProfile: PlayerProfile
        private set

    // Load puzzles from Firestore
    override val puzzles: List<PuzzleDescriptor>
        get() = cachedPuzzles

    override val progressByPuzzle: Map<String, PuzzleProgress>
        get() = emptyMap() // TODO

    override val leaderboard: Map<String, List<LeaderboardEntry>>
        get() = emptyMap()

    override val dailyChallenge: DailyChallenge
        get() = throw NotImplementedError("Firebase daily challenge not implemented yet.")

    private var cachedPuzzles: List<PuzzleDescriptor> = emptyList()

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

    suspend fun loadActiveProfile(authRepo: AuthRepository) {
        val userId = authRepo.getCurrentUserId()
            ?: throw Exception("User not logged in")

        // Convert callback → suspend (cleaner)
        val profile = suspendCancellableCoroutine<PlayerProfile> { cont ->
            authRepo.loadUserProfile(userId) { loaded, error ->
                if (loaded != null) cont.resume(loaded) {}
                else cont.resumeWithException(Exception(error ?: "Unknown error"))
            }
        }
        activeProfile = profile
    }

}
