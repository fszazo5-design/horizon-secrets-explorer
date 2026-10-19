package com.horizon.secrets.sync

import com.horizon.secrets.domain.PlayerProgress
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class CloudflareSyncApi(private val client: HttpClient, private val endpoint: String) {
    suspend fun sync(progress: PlayerProgress): Boolean {
        if (endpoint.isBlank()) return false
        val completed = progress.completedLevels.joinToString(",") { if (it) "true" else "false" }
        val body = "{\"game\":\"horizon-secrets\",\"version\":1,\"progress\":{\"unlockedLevels\":${progress.unlockedLevels},\"completedLevels\":[$completed],\"leaderboard\":[{\"name\":\"مستكشف\",\"score\":${progress.bestScore},\"level\":0}]}}"
        return runCatching {
            client.post(endpoint) {
                contentType(ContentType.Application.Json)
                header("x-player-id", progress.playerId)
                setBody(body)
            }.status.value in 200..299
        }.getOrDefault(false)
    }
}
