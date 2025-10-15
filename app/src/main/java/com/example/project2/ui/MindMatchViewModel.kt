package com.example.project2.ui

import androidx.lifecycle.ViewModel
import com.example.project2.data.DailyChallenge
import com.example.project2.data.FakeMindMatchRepository
import com.example.project2.data.LeaderboardEntry
import com.example.project2.data.MindMatchRepository
import com.example.project2.data.PlayerProfile
import com.example.project2.data.PuzzleDescriptor
import com.example.project2.data.PuzzleProgress

/**
 * Simple ViewModel that exposes fake data for the UI while the real data layer is built.
 */
class MindMatchViewModel(
    private val repository: MindMatchRepository = FakeMindMatchRepository()
) : ViewModel() {

    val profile: PlayerProfile get() = repository.activeProfile
    val puzzles: List<PuzzleDescriptor> get() = repository.puzzles
    val progressByPuzzle: Map<String, PuzzleProgress> get() = repository.progressByPuzzle
    val dailyChallenge: DailyChallenge get() = repository.dailyChallenge
    val leaderboard: Map<String, List<LeaderboardEntry>> get() = repository.leaderboard
}
