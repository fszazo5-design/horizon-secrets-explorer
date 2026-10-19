package com.horizon.secrets.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.room.Room
import com.horizon.secrets.data.AppDatabase
import com.horizon.secrets.data.ProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

class ProgressSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val playerId = applicationContext.getSharedPreferences("horizon", Context.MODE_PRIVATE).getString("player_id", "guest-session") ?: "guest-session"
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "horizon-secrets.db").build()
        val progress = ProgressRepository(db.progressDao(), playerId).load()
        val endpoint = applicationContext.getSharedPreferences("horizon", Context.MODE_PRIVATE).getString("sync_url", "") ?: ""
        val ok = CloudflareSyncApi(HttpClient(OkHttp), endpoint).sync(progress)
        if (ok) Result.success() else Result.retry()
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<ProgressSyncWorker>().setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
