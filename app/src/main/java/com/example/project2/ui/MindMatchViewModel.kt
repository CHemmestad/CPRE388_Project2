package com.example.project2.ui

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
import kotlinx.coroutines.launch

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

    fun reloadData() {
        viewModelScope.launch {
            loadInitialData()
        }
    }

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
        }
    }

    fun submitLeaderboardScore(puzzleId: String, score: Int) {
        val name = profile?.displayName ?: "Player"
        viewModelScope.launch {
            try {
                repository.submitLeaderboardEntry(
                    puzzleId = puzzleId,
                    playerName = name,
                    score = score
                )
                leaderboard = repository.leaderboard
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
    fun markPuzzlePlayed(puzzleId: String) {
        val userId = authRepo.getCurrentUserId() ?: return
        viewModelScope.launch {
            repository.recordPuzzlePlayed(userId, puzzleId)
            profile = repository.activeProfile
        }
    }

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

    fun refreshDailyChallenge() {
        viewModelScope.launch {
            try {
                mergeDailyChallenge(repository.loadLatestDailyChallenge())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun mergeDailyChallenge(latest: DailyChallenge?) {
        dailyChallenge = latest
        val challengePuzzle = latest?.puzzle ?: return

        // Replace any existing entry with the same id
        puzzles = puzzles.filterNot { it.id == challengePuzzle.id } + challengePuzzle
    }
}
