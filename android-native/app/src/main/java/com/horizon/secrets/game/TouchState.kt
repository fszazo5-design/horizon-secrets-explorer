package com.horizon.secrets.game

import kotlin.math.sqrt

class TouchState {
    @Volatile var moveX: Float = 0f
    @Volatile var moveY: Float = 0f
    @Volatile var jumpPressed: Boolean = false
    @Volatile var pausePressed: Boolean = false
    @Volatile var mapRequested: Boolean = false

    fun setMove(x: Float, y: Float) {
        val length = sqrt(x * x + y * y).coerceAtLeast(1f)
        moveX = (x / length).coerceIn(-1f, 1f)
        moveY = (y / length).coerceIn(-1f, 1f)
    }

    fun clearMove() {
        moveX = 0f
        moveY = 0f
    }

    fun cancelAll() {
        clearMove()
        jumpPressed = false
        pausePressed = false
        mapRequested = false
    }

    fun consumeJump(): Boolean = jumpPressed.also { jumpPressed = false }
}
