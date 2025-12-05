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

    var puzzles: List<PuzzleDescriptor> = emptyList()
        private set

    var progressByPuzzle: Map<String, PuzzleProgress> = emptyMap()
        private set

    var dailyChallenge: DailyChallenge? = null
        private set

    var leaderboard: Map<String, List<LeaderboardEntry>> = emptyMap()
        private set

    init {
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

            // 2) load puzzles
            repository.loadPuzzlesFromFirebase()
            puzzles = repository.puzzles

            // 3) expose progress to UI
            progressByPuzzle = repository.progressByPuzzle

            // 4) if/when you implement these in the repo, hook them here:
            // dailyChallenge = repository.dailyChallenge
            // leaderboard = repository.leaderboard
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
}
