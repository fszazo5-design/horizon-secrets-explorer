package com.horizon.secrets.data

import com.horizon.secrets.domain.PlayerProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProgressRepository(private val dao: ProgressDao, private val playerId: String) {
    suspend fun load(): PlayerProgress = withContext(Dispatchers.IO) {
        dao.find(playerId)?.toDomain() ?: PlayerProgress(playerId = playerId).also { save(it) }
    }

    suspend fun save(progress: PlayerProgress) = withContext(Dispatchers.IO) {
        dao.save(ProgressEntity(playerId, progress.unlockedLevels, progress.completedLevels.joinToString(",") { if (it) "1" else "0" }, progress.bestScore, progress.updatedAt))
    }

    private fun ProgressEntity.toDomain() = PlayerProgress(
        playerId = playerId,
        unlockedLevels = unlockedLevels,
        completedLevels = completedLevels.split(",").map { it == "1" }.let { it + List((4 - it.size).coerceAtLeast(0)) { false } },
        bestScore = bestScore,
        updatedAt = updatedAt,
    )
}
