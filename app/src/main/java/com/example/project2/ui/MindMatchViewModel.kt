package com.example.project2.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project2.data.AuthRepository
import com.example.project2.data.DailyChallenge
import com.example.project2.data.FakeMindMatchRepository
import com.example.project2.data.FirebaseMindMatchRepository
import com.example.project2.data.LeaderboardEntry
import com.example.project2.data.MindMatchRepository
import com.example.project2.data.PlayerProfile
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleProgress
import kotlinx.coroutines.launch

/**
 * Simple ViewModel that exposes fake data for the UI while the real data layer is built.
 */
/*
class MindMatchViewModel(
    private val repository: MindMatchRepository = FakeMindMatchRepository()
) : ViewModel() {

    val profile: PlayerProfile get() = repository.activeProfile
    val puzzles: List<PuzzleDescriptor> get() = repository.puzzles
    val progressByPuzzle: Map<String, PuzzleProgress> get() = repository.progressByPuzzle
    val dailyChallenge: DailyChallenge get() = repository.dailyChallenge
    val leaderboard: Map<String, List<LeaderboardEntry>> get() = repository.leaderboard
}
*/
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
            try {
                repository.loadActiveProfile(authRepo)
                profile = repository.activeProfile

                repository.loadPuzzlesFromFirebase()
                puzzles = repository.puzzles
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}
