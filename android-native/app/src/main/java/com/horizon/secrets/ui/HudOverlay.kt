package com.horizon.secrets.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.content.Context
import android.view.MotionEvent
import android.view.View
import com.horizon.secrets.domain.HudState
import com.horizon.secrets.game.TouchState

class HudOverlay(
    context: Context,
    private val touch: TouchState,
    private val onLevelSelected: (Int) -> Unit,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var state = HudState()
    private var mapVisible = true
    private var unlocked = 1
    private var completed = listOf(false)
    private var joystickPointer = -1
    private var joystickCenterX = 150f
    private var joystickCenterY = 0f

    init { isClickable = true; setBackgroundColor(Color.TRANSPARENT) }

    fun updateHud(next: HudState) { state = next; postInvalidateOnAnimation() }
    fun updateProgress(unlockedLevels: Int, completedLevels: List<Boolean>) { unlocked = unlockedLevels; completed = completedLevels; postInvalidate() }
    fun showMap() { mapVisible = true; touch.clearMove(); postInvalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        joystickCenterY = height - 150f
        if (mapVisible) drawMap(canvas) else drawHud(canvas)
    }

    private fun drawMap(canvas: Canvas) {
        paint.color = Color.rgb(28, 42, 34); canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.rgb(235, 219, 176); paint.textAlign = Paint.Align.CENTER; paint.textSize = 38f
        canvas.drawText("رحلة المستكشف", width / 2f, height * .28f, paint)
        paint.color = Color.rgb(190, 220, 177); paint.textSize = 19f
        canvas.drawText("مستوى واحد · قرية واقعية", width / 2f, height * .36f, paint)
        paint.color = Color.rgb(56, 126, 76)
        canvas.drawRoundRect(RectF(width * .28f, height * .46f, width * .72f, height * .64f), 24f, 24f, paint)
        paint.color = Color.WHITE; paint.textSize = 30f
        canvas.drawText("▶  ابدأ اللعب", width / 2f, height * .575f, paint)
        paint.color = Color.rgb(205, 215, 205); paint.textSize = 16f
        canvas.drawText("المس الزر للبدء", width / 2f, height * .78f, paint)
    }

    private fun drawHud(canvas: Canvas) {
        paint.textAlign = Paint.Align.RIGHT; paint.color = Color.WHITE; paint.textSize = 24f
        canvas.drawText("شظايا النور ${state.collected}/${state.total}", width - 28f, 42f, paint)
        paint.color = Color.rgb(255, 200, 107); paint.textSize = 21f
        canvas.drawText("الرصيد ${state.score}   ${state.timeLeft}ث", width - 28f, 75f, paint)
        paint.color = Color.rgb(98, 230, 255); paint.textSize = 16f
        canvas.drawText("العالم ${state.level + 1} · ${state.message}", width - 28f, 106f, paint)
        paint.color = Color.argb(105, 98, 160, 120); canvas.drawCircle(joystickCenterX, joystickCenterY, 86f, paint)
        paint.color = Color.argb(220, 235, 245, 225); canvas.drawCircle(joystickCenterX + touch.moveX * 46f, joystickCenterY + touch.moveY * 46f, 34f, paint)
        paint.color = Color.argb(200, 255, 200, 107); canvas.drawCircle(width - 100f, height - 135f, 48f, paint)
        paint.color = Color.WHITE; paint.textAlign = Paint.Align.CENTER; paint.textSize = 17f; canvas.drawText("قفز", width - 100f, height - 129f, paint)
        paint.color = Color.argb(180, 167, 139, 250); canvas.drawCircle(width - 205f, height - 100f, 34f, paint); paint.color = Color.WHITE; canvas.drawText("خريطة", width - 205f, height - 95f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (mapVisible) return true
                if (event.x < width * .45f) { if (joystickPointer == -1) joystickPointer = event.getPointerId(event.actionIndex); if (event.getPointerId(event.actionIndex) == joystickPointer) touch.setMove((event.x - joystickCenterX) / 86f, (event.y - joystickCenterY) / 86f) }
                else if (event.x > width - 180f && event.y > height - 220f) touch.jumpPressed = true
                else if (event.x > width - 260f && event.y > height - 180f) showMap()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mapVisible && event.actionMasked == MotionEvent.ACTION_UP) {
                    if (event.x > width * .24f && event.x < width * .76f && event.y > height * .42f && event.y < height * .68f) { mapVisible = false; onLevelSelected(0) }
                } else if (event.getPointerId(event.actionIndex) == joystickPointer) { joystickPointer = -1; touch.clearMove() }
            }
        }
        invalidate(); return true
    }
}
