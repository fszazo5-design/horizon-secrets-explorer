package com.horizon.secrets.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_progress")
data class ProgressEntity(
    @PrimaryKey val playerId: String,
    val unlockedLevels: Int,
    val completedLevels: String,
    val bestScore: Int,
    val updatedAt: Long,
)
