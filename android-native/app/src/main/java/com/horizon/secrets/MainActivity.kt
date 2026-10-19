package com.horizon.secrets

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.horizon.secrets.game.HorizonGame
import com.horizon.secrets.game.TouchState
import com.horizon.secrets.sync.ProgressSyncWorker
import com.horizon.secrets.ui.HudOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AndroidApplication() {
    private lateinit var container: AppContainer
    private lateinit var game: HorizonGame
    private lateinit var hud: HudOverlay
    private val touch = TouchState()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer(this)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
            // Avoid EGL/MSAA crashes on devices that do not expose multisampling.
            numSamples = 0
        }
        hud = HudOverlay(
            this,
            touch,
            onLevelSelected = { index ->
                if (::game.isInitialized) Gdx.app.postRunnable { game.startLevel(index) }
            },
            onCharacterSelected = { index ->
                if (::game.isInitialized) Gdx.app.postRunnable { game.selectCharacter(index) }
            },
        )
        val root = FrameLayout(this)
        root.layoutDirection = ViewGroup.LAYOUT_DIRECTION_RTL
        root.addView(hud, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        game = HorizonGame(touch, { hud.updateHud(it) }) { level, score ->
            scope.launch {
                val current = container.progressRepository.load()
                val completed = current.completedLevels.toMutableList().apply { this[level] = true }
                val next = current.copy(unlockedLevels = maxOf(current.unlockedLevels, (level + 2).coerceAtMost(4)), completedLevels = completed, bestScore = maxOf(current.bestScore, score), updatedAt = System.currentTimeMillis())
                container.progressRepository.save(next)
                hud.updateProgress(next.unlockedLevels, next.completedLevels)
                ProgressSyncWorker.enqueue(this@MainActivity)
            }
        }
        val gameView = initializeForView(game, config)
        root.addView(gameView, 0, FrameLayout.LayoutParams(-1, -1))
        scope.launch {
            val progress = container.progressRepository.load()
            hud.updateProgress(progress.unlockedLevels, progress.completedLevels)
        }
    }

    override fun onPause() { super.onPause(); touch.clearMove() }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
