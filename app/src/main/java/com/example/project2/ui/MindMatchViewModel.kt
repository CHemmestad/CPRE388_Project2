package com.example.project2.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project2.data.AuthRepository
import com.example.project2.data.DailyChallenge
import com.example.project2.data.FirebaseMindMatchRepository
import com.example.project2.data.LeaderboardEntry
import com.example.project2.data.PlayerProfile
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleProgress
import com.google.firebase.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Central app state holder: loads data from Firebase-backed repositories and exposes UI-facing state.
 *
 * @param authRepo authentication data source
 * @param repository Firebase implementation for puzzles, progress, and leaderboards
 */
class MindMatchViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val repository: FirebaseMindMatchRepository = FirebaseMindMatchRepository()
) : ViewModel() {

    var isLoading by mutableStateOf(true)
        private set

    var profile: PlayerProfile? = null
        private set

    var puzzles by mutableStateOf<List<PuzzleDescriptor>>(emptyList())
        private set

    var userPuzzles by mutableStateOf<List<PuzzleDescriptor>>(emptyList())
        private set

    var progressByPuzzle by mutableStateOf<Map<String, PuzzleProgress>>(emptyMap())
        private set

    var dailyChallenge by mutableStateOf<DailyChallenge?>(null)
        private set

    var leaderboard by mutableStateOf<Map<String, List<LeaderboardEntry>>>(emptyMap())
        private set

    init {
        viewModelScope.launch {
            loadInitialData()
        }
    }

    /** Reload everything after auth or significant actions. */
    fun reloadData() {
        viewModelScope.launch {
            loadInitialData()
        }
    }

    /**
     * Load profile, puzzles, progress, daily challenge, and leaderboard from Firebase.
     */
    private suspend fun loadInitialData() {
        isLoading = true
        try {
            // 1) load profile (also loads progress inside repository.loadActiveProfile)
            repository.loadActiveProfile(authRepo)
            profile = repository.activeProfile

            // 2) load puzzles and progress
            repository.loadPuzzlesFromFirebase()
            puzzles = repository.puzzles
            progressByPuzzle = repository.progressByPuzzle
            userPuzzles = puzzles.filter { it.creatorId == profile?.id }

            // 3) load latest daily challenge and merge its puzzle into the list
            mergeDailyChallenge(repository.loadLatestDailyChallenge())

            // 4) leaderboard
            repository.loadLeaderboard()
            leaderboard = repository.leaderboard
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    /**
     * Called by puzzle screens when the user makes progress or exits.
     * This saves to Firestore and updates the local cache.
     */
    fun saveProgress(progress: PuzzleProgress) {
        val userId = authRepo.getCurrentUserId() ?: return
        viewModelScope.launch {
            repository.saveProgress(userId, progress)
            progressByPuzzle = repository.progressByPuzzle
            repository.recordPuzzlePlayed(userId, progress.puzzleId)
            profile = repository.activeProfile
        }
    }

    /**
     * Submit a leaderboard score and refresh cached leaderboards.
     *
     * @param puzzleId target puzzle id
     * @param score numeric score or time depending on puzzle type
     */
    fun submitLeaderboardScore(puzzleId: String, score: Int) {
        val name = profile?.displayName ?: "Player"
        viewModelScope.launch {
            try {
                repository.submitLeaderboardEntry(
                    puzzleId = puzzleId,
                    playerName = name,
                    score = score
                )
                repository.loadLeaderboard()
                leaderboard = repository.leaderboard
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Reload puzzles and associated progress from Firebase.
     */
    fun refreshPuzzles() {
        viewModelScope.launch {
            try {
                repository.loadPuzzlesFromFirebase()
                puzzles = repository.puzzles
                progressByPuzzle = repository.progressByPuzzle
                userPuzzles = puzzles.filter { it.creatorId == profile?.id }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Record a puzzle play timestamp for the active user.
     *
     * @param puzzleId id of the played puzzle
     */
    fun markPuzzlePlayed(puzzleId: String) {
        val userId = authRepo.getCurrentUserId() ?: return
        viewModelScope.launch {
            repository.recordPuzzlePlayed(userId, puzzleId)
            profile = repository.activeProfile
        }
    }

    /** Remove a user-created puzzle and refresh local state. */
    fun deleteUserPuzzle(puzzleId: String) {
        viewModelScope.launch {
            try {
                repository.deletePuzzleFromFirebase(puzzleId)
                puzzles = repository.puzzles
                userPuzzles = puzzles.filter { it.creatorId == profile?.id }
                progressByPuzzle = repository.progressByPuzzle
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Fetch and merge the latest daily challenge.
     */
    fun refreshDailyChallenge() {
        viewModelScope.launch {
            try {
                mergeDailyChallenge(repository.loadLatestDailyChallenge())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Update profile fields for the current user.
     *
     * @param displayName new display name
     * @param bio profile bio text
     */
    fun updateProfile(displayName: String, bio: String) {
        val userId = authRepo.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                val updated = authRepo.updateProfile(userId, displayName, bio)
                if (updated != null) {
                    profile = updated
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Delete the current account and clear local state.
     *
     * @param onDeleted callback invoked after successful deletion
     */
    fun deleteAccount(onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val success = authRepo.deleteAccount()
            if (success) {
                profile = null
                puzzles = emptyList()
                userPuzzles = emptyList()
                progressByPuzzle = emptyMap()
                dailyChallenge = null
                leaderboard = emptyMap()
                authRepo.logout()
                onDeleted()
            }
        }
    }

    /**
     * Merge the latest daily challenge into existing puzzles/state.
     *
     * @param latest daily challenge payload to cache
     */
    private fun mergeDailyChallenge(latest: DailyChallenge?) {
        dailyChallenge = latest
        val challengePuzzle = latest?.puzzle ?: return

        // Replace any existing entry with the same id
        puzzles = puzzles.filterNot { it.id == challengePuzzle.id } + challengePuzzle
    }

    /**
     * Submit a jigsaw completion time to the leaderboard.
     *
     * @param timeInSeconds completion time
     * @param gridSize size of the jigsaw grid
     */
    fun saveJigsawScore(timeInSeconds: Long, gridSize: Int) {
        val name = profile?.displayName ?: "Anonymous"

        val puzzleIdForLeaderboard = "JIGSAW_${gridSize}X${gridSize}"

        submitLeaderboardScore(
            puzzleId = puzzleIdForLeaderboard,
            score = timeInSeconds.toInt()
        )
    }
}
