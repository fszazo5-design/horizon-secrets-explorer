package com.horizon.secrets.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProgressDao {
    @Query("SELECT * FROM player_progress WHERE playerId = :playerId LIMIT 1")
    suspend fun find(playerId: String): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(progress: ProgressEntity)
}
