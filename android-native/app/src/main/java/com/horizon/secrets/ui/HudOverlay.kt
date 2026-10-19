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
    private var completed = listOf(true, false, false, false)
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
        paint.color = Color.argb(235, 4, 15, 29); canvas.drawRect(24f, 24f, width - 24f, height - 24f, paint)
        paint.color = Color.rgb(98, 230, 255); paint.textSize = 42f; paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("خريطة العوالم", width - 54f, 92f, paint)
        paint.color = Color.rgb(255, 200, 107); paint.textSize = 18f
        canvas.drawText("مسارات الأفق · تقدمك محفوظ أوفلاين", width - 54f, 125f, paint)
        val names = listOf("مدار الاتزان", "بصائر الظلال", "ميزان الماء", "مرآة الحقيقة")
        val glyphs = listOf("◈", "◐", "≈", "✦")
        for (i in 0..3) {
            val row = i / 2; val col = i % 2
            val left = 54f + col * ((width - 118f) / 2f); val top = 160f + row * 142f; val right = left + (width - 142f) / 2f
            val available = i < unlocked
            paint.color = if (available) Color.argb(220, 17, 57, 83) else Color.argb(150, 26, 30, 42)
            canvas.drawRoundRect(RectF(left, top, right, top + 112f), 8f, 8f, paint)
            paint.color = if (available) Color.rgb(98, 230, 255) else Color.GRAY; paint.textAlign = Paint.Align.RIGHT; paint.textSize = 27f
            canvas.drawText(glyphs[i], right - 18f, top + 38f, paint)
            paint.textSize = 21f; canvas.drawText(names[i], right - 56f, top + 38f, paint)
            paint.textSize = 15f; paint.color = if (completed.getOrElse(i) { false }) Color.rgb(85, 240, 179) else Color.LTGRAY
            canvas.drawText(if (completed.getOrElse(i) { false }) "مكتمل" else if (available) "متاح الآن" else "مغلق", right - 56f, top + 76f, paint)
        }
        paint.textAlign = Paint.Align.CENTER; paint.color = Color.LTGRAY; paint.textSize = 16f
        canvas.drawText("المس: اختر عالماً · زر الرجوع: العودة", width / 2f, height - 50f, paint)
    }

    private fun drawHud(canvas: Canvas) {
        paint.textAlign = Paint.Align.RIGHT; paint.color = Color.WHITE; paint.textSize = 24f
        canvas.drawText("شظايا النور ${state.collected}/${state.total}", width - 28f, 42f, paint)
        paint.color = Color.rgb(255, 200, 107); paint.textSize = 21f
        canvas.drawText("الرصيد ${state.score}   ${state.timeLeft}ث", width - 28f, 75f, paint)
        paint.color = Color.rgb(98, 230, 255); paint.textSize = 16f
        canvas.drawText("العالم ${state.level + 1} · ${state.message}", width - 28f, 106f, paint)
        paint.color = Color.argb(90, 98, 230, 255); canvas.drawCircle(joystickCenterX, joystickCenterY, 72f, paint)
        paint.color = Color.argb(190, 98, 230, 255); canvas.drawCircle(joystickCenterX + touch.moveX * 38f, joystickCenterY + touch.moveY * 38f, 30f, paint)
        paint.color = Color.argb(200, 255, 200, 107); canvas.drawCircle(width - 100f, height - 135f, 48f, paint)
        paint.color = Color.WHITE; paint.textAlign = Paint.Align.CENTER; paint.textSize = 17f; canvas.drawText("قفز", width - 100f, height - 129f, paint)
        paint.color = Color.argb(180, 167, 139, 250); canvas.drawCircle(width - 205f, height - 100f, 34f, paint); paint.color = Color.WHITE; canvas.drawText("خريطة", width - 205f, height - 95f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (mapVisible) return true
                if (event.x < width * .45f) { if (joystickPointer == -1) joystickPointer = event.getPointerId(event.actionIndex); touch.setMove((event.x - joystickCenterX) / 70f, (event.y - joystickCenterY) / 70f) }
                else if (event.x > width - 180f && event.y > height - 220f) touch.jumpPressed = true
                else if (event.x > width - 260f && event.y > height - 180f) showMap()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mapVisible && event.actionMasked == MotionEvent.ACTION_UP) {
                    val col = if (event.x > width / 2f) 0 else 1; val row = if (event.y > height / 2f) 1 else 0; val selected = row * 2 + col
                    if (selected < unlocked) { mapVisible = false; onLevelSelected(selected) }
                } else { joystickPointer = -1; touch.clearMove() }
            }
        }
        invalidate(); return true
    }
}
