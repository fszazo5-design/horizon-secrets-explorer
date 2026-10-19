package com.horizon.secrets.domain

data class PlayerProgress(
    val playerId: String,
    val unlockedLevels: Int = 1,
    val completedLevels: List<Boolean> = listOf(true, false, false, false),
    val bestScore: Int = 0,
    val updatedAt: Long = 0L,
)

data class HudState(
    val level: Int = 0,
    val collected: Int = 0,
    val total: Int = 0,
    val score: Int = 0,
    val timeLeft: Int = 145,
    val message: String = "استكشف المدار واجمع شظايا النور",
)
