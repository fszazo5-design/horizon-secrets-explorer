package com.horizon.secrets

import android.content.Context
import androidx.room.Room
import com.horizon.secrets.data.AppDatabase
import com.horizon.secrets.data.ProgressRepository
import java.util.UUID

class AppContainer(context: Context) {
    private val prefs = context.getSharedPreferences("horizon", Context.MODE_PRIVATE)
    private val playerId = prefs.getString("player_id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("player_id", it).apply() }
    val database: AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "horizon-secrets.db").build()
    val progressRepository = ProgressRepository(database.progressDao(), playerId)
}
