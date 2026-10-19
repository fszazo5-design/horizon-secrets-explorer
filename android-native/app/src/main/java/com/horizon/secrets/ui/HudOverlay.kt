package com.horizon.secrets.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.horizon.secrets.domain.HudState
import com.horizon.secrets.game.TouchState

class HudOverlay(
    context: Context,
    private val touch: TouchState,
    private val onLevelSelected: (Int) -> Unit,
    private val onCharacterSelected: (Int) -> Unit,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var state = HudState()
    private var mapVisible = true
    private var characterPickerVisible = false
    private var unlocked = 1
    private var completed = listOf(true, false, false, false)
    private var selectedCharacter = 0
    private var joystickPointer = -1
    private var joystickCenterX = 150f
    private var joystickCenterY = 0f
    private val characterNames = listOf("رائد النور", "حارس المدار", "رحّالة الرمال", "حارسة البلور")
    private val characterRoles = listOf("عباءة الطاقة", "درع بنفسجي", "خوذة الرمال", "تاج البلور")
    private val characterColors = listOf(
        Color.rgb(42, 184, 245),
        Color.rgb(170, 112, 255),
        Color.rgb(250, 158, 54),
        Color.rgb(77, 231, 184),
    )

    init { isClickable = true; setBackgroundColor(Color.TRANSPARENT) }

    fun updateHud(next: HudState) { state = next; postInvalidateOnAnimation() }
    fun updateProgress(unlockedLevels: Int, completedLevels: List<Boolean>) { unlocked = unlockedLevels; completed = completedLevels; postInvalidate() }
    fun showMap() { mapVisible = true; characterPickerVisible = false; touch.clearMove(); postInvalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        joystickCenterY = height - 150f
        if (mapVisible) {
            if (characterPickerVisible) drawCharacterPicker(canvas) else drawMap(canvas)
        } else drawHud(canvas)
    }

    private fun drawMap(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(235, 4, 15, 29); canvas.drawRect(24f, 24f, width - 24f, height - 24f, paint)
        paint.color = Color.rgb(98, 230, 255); paint.textSize = 42f; paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("خريطة العوالم", width - 54f, 92f, paint)
        paint.color = Color.rgb(255, 200, 107); paint.textSize = 18f
        canvas.drawText("مسارات الأفق · تقدمك محفوظ أوفلاين", width - 54f, 125f, paint)

        paint.color = Color.argb(220, 17, 57, 83)
        canvas.drawRoundRect(RectF(50f, 70f, 282f, 136f), 12f, 12f, paint)
        paint.color = characterColors[selectedCharacter]
        canvas.drawCircle(76f, 103f, 12f, paint)
        paint.textAlign = Paint.Align.LEFT; paint.color = Color.LTGRAY; paint.textSize = 14f
        canvas.drawText("شخصيتك", 98f, 96f, paint)
        paint.color = Color.WHITE; paint.textSize = 20f
        canvas.drawText(characterNames[selectedCharacter], 98f, 121f, paint)

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

    private fun drawCharacterPicker(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(245, 4, 15, 29)
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, paint)
        paint.textAlign = Paint.Align.RIGHT; paint.color = Color.rgb(98, 230, 255); paint.textSize = 38f
        canvas.drawText("اختر شخصيتك", width - 54f, 88f, paint)
        paint.color = Color.rgb(255, 200, 107); paint.textSize = 17f
        canvas.drawText("أربع شخصيات ثلاثية الأبعاد قابلة للعب", width - 54f, 119f, paint)

        val cardWidth = (width - 134f) / 2f
        val cardHeight = 108f
        for (i in characterNames.indices) {
            val row = i / 2; val col = i % 2
            val left = 54f + col * (cardWidth + 24f)
            val top = 146f + row * 124f
            val right = left + cardWidth
            paint.color = if (i == selectedCharacter) Color.argb(235, 27, 69, 92) else Color.argb(210, 15, 39, 61)
            canvas.drawRoundRect(RectF(left, top, right, top + cardHeight), 12f, 12f, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = if (i == selectedCharacter) 3f else 1.5f
            paint.color = characterColors[i]
            canvas.drawRoundRect(RectF(left + 1f, top + 1f, right - 1f, top + cardHeight - 1f), 12f, 12f, paint)
            paint.style = Paint.Style.FILL

            // A compact silhouette preview; the selected hero is rendered as a real 3D model in the world.
            paint.color = characterColors[i]
            canvas.drawCircle(left + 42f, top + 35f, 15f, paint)
            canvas.drawRoundRect(RectF(left + 27f, top + 52f, left + 57f, top + 91f), 8f, 8f, paint)
            paint.color = Color.rgb(12, 24, 38)
            canvas.drawRoundRect(RectF(left + 32f, top + 31f, left + 52f, top + 36f), 2f, 2f, paint)

            paint.textAlign = Paint.Align.RIGHT; paint.color = Color.WHITE; paint.textSize = 21f
            canvas.drawText(characterNames[i], right - 16f, top + 42f, paint)
            paint.color = Color.LTGRAY; paint.textSize = 15f
            canvas.drawText(characterRoles[i], right - 16f, top + 74f, paint)
            paint.color = characterColors[i]; paint.textSize = 13f
            canvas.drawText(if (i == selectedCharacter) "محددة" else "المس للاختيار", right - 16f, top + 96f, paint)
        }
        paint.textAlign = Paint.Align.CENTER; paint.color = Color.LTGRAY; paint.textSize = 15f
        canvas.drawText("اختر شخصية ثم المس خارج البطاقات للعودة إلى العوالم", width / 2f, height - 42f, paint)
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

    private fun characterAt(x: Float, y: Float): Int? {
        val cardWidth = (width - 134f) / 2f
        val row = when {
            y in 146f..254f -> 0
            y in 270f..378f -> 1
            else -> return null
        }
        val col = when {
            x in 54f..(54f + cardWidth) -> 0
            x in (78f + cardWidth)..(78f + cardWidth * 2f) -> 1
            else -> return null
        }
        return row * 2 + col
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
                    if (characterPickerVisible) {
                        val selected = characterAt(event.x, event.y)
                        if (selected != null) {
                            selectedCharacter = selected
                            onCharacterSelected(selected)
                        }
                        characterPickerVisible = false
                    } else if (event.x in 50f..282f && event.y in 70f..136f) {
                        characterPickerVisible = true
                    } else {
                        val col = if (event.x > width / 2f) 0 else 1
                        val row = if (event.y > height / 2f) 1 else 0
                        val selected = row * 2 + col
                        if (selected < unlocked) { mapVisible = false; onLevelSelected(selected) }
                    }
                } else { joystickPointer = -1; touch.clearMove() }
            }
        }
        invalidate(); return true
    }
}
