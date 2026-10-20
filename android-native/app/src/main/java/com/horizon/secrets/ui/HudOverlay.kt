package com.horizon.secrets.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    private val density = resources.displayMetrics.density.coerceAtLeast(1f)
    private var state = HudState()
    private var mapVisible = true
    private var characterPickerVisible = false
    private var unlocked = 1
    private var completed = listOf(true, false, false, false)
    private var selectedCharacter = 0
    private var joystickPointer = -1
    private var jumpPointer = -1
    private var ignoreMapOpenPointer = -1
    private var joystickCenterX = 0f
    private var joystickCenterY = 0f
    private val characterNames = listOf("هاربر", "سام", "مايا")
    private val characterRoles = listOf("بدلة رسمية · 7 حركات", "ملابس يومية · 7 حركات", "ملابس رياضية · 7 حركات")
    private val characterColors = listOf(
        Color.rgb(98, 230, 255),
        Color.rgb(114, 204, 170),
        Color.rgb(227, 123, 213),
    )
    private val characterPortraits: List<Bitmap?> by lazy {
        characterNames.indices.map { index ->
            val assetName = listOf("harper", "sam", "maya")[index]
            runCatching { context.assets.open("characters/portrait_$assetName.png").use { stream -> BitmapFactory.decodeStream(stream) } }.getOrNull()
        }
    }

    init {
        isClickable = true
        setBackgroundColor(Color.TRANSPARENT)
        isFocusableInTouchMode = true
    }

    private fun dp(value: Float): Float = value * density

    fun updateHud(next: HudState) { state = next; postInvalidateOnAnimation() }
    fun updateProgress(unlockedLevels: Int, completedLevels: List<Boolean>) {
        unlocked = unlockedLevels
        completed = completedLevels
        postInvalidate()
    }

    fun showMap() {
        mapVisible = true
        characterPickerVisible = false
        joystickPointer = -1
        jumpPointer = -1
        ignoreMapOpenPointer = -1
        touch.cancelAll()
        postInvalidate()
    }

    fun cancelTouchPointers() {
        joystickPointer = -1
        jumpPointer = -1
        ignoreMapOpenPointer = -1
        touch.cancelAll()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (joystickPointer == -1) {
            joystickCenterX = width * 0.16f
            joystickCenterY = height - dp(112f)
        }
        if (mapVisible) {
            if (characterPickerVisible) drawCharacterPicker(canvas) else drawMap(canvas)
        } else {
            drawHud(canvas)
        }
    }

    private fun drawMap(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(235, 4, 15, 29)
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, paint)
        paint.color = Color.rgb(98, 230, 255)
        paint.textSize = 42f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("خريطة العوالم", width - 54f, 92f, paint)
        paint.color = Color.rgb(255, 200, 107)
        paint.textSize = 18f
        canvas.drawText("مسارات الأفق · تقدمك محفوظ أوفلاين", width - 54f, 125f, paint)

        paint.color = Color.argb(220, 17, 57, 83)
        canvas.drawRoundRect(RectF(50f, 70f, 282f, 136f), 12f, 12f, paint)
        paint.color = characterColors[selectedCharacter]
        canvas.drawCircle(76f, 103f, 12f, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.LTGRAY
        paint.textSize = 14f
        canvas.drawText("شخصيتك", 98f, 96f, paint)
        paint.color = Color.WHITE
        paint.textSize = 20f
        canvas.drawText(characterNames[selectedCharacter], 98f, 121f, paint)

        val names = listOf("مدار الاتزان", "بصائر الظلال", "ميزان الماء", "مرآة الحقيقة")
        val glyphs = listOf("◈", "◐", "≈", "✦")
        for (i in 0..3) {
            val row = i / 2
            val col = i % 2
            val left = 54f + col * ((width - 118f) / 2f)
            val top = 160f + row * 142f
            val right = left + (width - 142f) / 2f
            val available = i < unlocked
            paint.color = if (available) Color.argb(220, 17, 57, 83) else Color.argb(150, 26, 30, 42)
            canvas.drawRoundRect(RectF(left, top, right, top + 112f), 8f, 8f, paint)
            paint.color = if (available) Color.rgb(98, 230, 255) else Color.GRAY
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 27f
            canvas.drawText(glyphs[i], right - 18f, top + 38f, paint)
            paint.textSize = 21f
            canvas.drawText(names[i], right - 56f, top + 38f, paint)
            paint.textSize = 15f
            paint.color = if (completed.getOrElse(i) { false }) Color.rgb(85, 240, 179) else Color.LTGRAY
            canvas.drawText(if (completed.getOrElse(i) { false }) "مكتمل" else if (available) "متاح الآن" else "مغلق", right - 56f, top + 76f, paint)
        }
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.LTGRAY
        paint.textSize = 16f
        canvas.drawText("المس: اختر عالماً · زر الرجوع: العودة", width / 2f, height - 50f, paint)
    }

    private fun drawCharacterPicker(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(245, 4, 15, 29)
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(98, 230, 255)
        paint.textSize = 38f
        canvas.drawText("اختر شخصيتك", width - 54f, 88f, paint)
        paint.color = Color.rgb(255, 200, 107)
        paint.textSize = 17f
        canvas.drawText("ثلاث شخصيات بشرية ثلاثية الأبعاد ومتحركة", width - 54f, 119f, paint)

        val cardWidth = (width - 134f) / 2f
        val cardHeight = 108f
        for (i in characterNames.indices) {
            val row = i / 2
            val col = i % 2
            val left = if (i == 2) (width - cardWidth) / 2f else 54f + col * (cardWidth + 24f)
            val top = 146f + row * 124f
            val right = left + cardWidth
            paint.color = if (i == selectedCharacter) Color.argb(235, 27, 69, 92) else Color.argb(210, 15, 39, 61)
            canvas.drawRoundRect(RectF(left, top, right, top + cardHeight), 12f, 12f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (i == selectedCharacter) 3f else 1.5f
            paint.color = characterColors[i]
            canvas.drawRoundRect(RectF(left + 1f, top + 1f, right - 1f, top + cardHeight - 1f), 12f, 12f, paint)
            paint.style = Paint.Style.FILL

            drawCharacterPortrait(canvas, i, left, top, cardHeight)

            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.WHITE
            paint.textSize = 21f
            canvas.drawText(characterNames[i], right - 16f, top + 42f, paint)
            paint.color = Color.LTGRAY
            paint.textSize = 15f
            canvas.drawText(characterRoles[i], right - 16f, top + 74f, paint)
            paint.color = characterColors[i]
            paint.textSize = 13f
            canvas.drawText(if (i == selectedCharacter) "محددة" else "المس للاختيار", right - 16f, top + 96f, paint)
        }
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.LTGRAY
        paint.textSize = 15f
        canvas.drawText("اختر شخصية ثم المس خارج البطاقات للعودة إلى العوالم", width / 2f, height - 42f, paint)
    }

    private fun drawHud(canvas: Canvas) {
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.WHITE
        paint.textSize = 24f
        canvas.drawText("شظايا النور ${state.collected}/${state.total}", width - 28f, 42f, paint)
        paint.color = Color.rgb(255, 200, 107)
        paint.textSize = 21f
        canvas.drawText("الرصيد ${state.score}   ${state.timeLeft}ث", width - 28f, 75f, paint)
        paint.color = Color.rgb(98, 230, 255)
        paint.textSize = 16f
        canvas.drawText("العالم ${state.level + 1} · ${state.message}", width - 28f, 106f, paint)

        val stickRadius = dp(56f)
        paint.color = Color.argb(90, 98, 230, 255)
        canvas.drawCircle(joystickCenterX, joystickCenterY, stickRadius, paint)
        paint.color = Color.argb(190, 98, 230, 255)
        canvas.drawCircle(
            joystickCenterX + touch.moveX * dp(30f),
            joystickCenterY + touch.moveY * dp(30f),
            dp(24f),
            paint,
        )

        val jumpX = width - dp(82f)
        val jumpY = height - dp(108f)
        paint.color = Color.argb(200, 255, 200, 107)
        canvas.drawCircle(jumpX, jumpY, dp(40f), paint)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = dp(17f)
        canvas.drawText("قفز", jumpX, jumpY + dp(6f), paint)

        val mapX = width - dp(158f)
        val mapY = height - dp(72f)
        paint.color = Color.argb(180, 167, 139, 250)
        canvas.drawCircle(mapX, mapY, dp(30f), paint)
        paint.color = Color.WHITE
        paint.textSize = dp(12f)
        canvas.drawText("خريطة", mapX, mapY + dp(4f), paint)
    }

    private fun isJumpButton(x: Float, y: Float): Boolean {
        val cx = width - dp(82f)
        val cy = height - dp(108f)
        val r = dp(52f)
        val dx = x - cx
        val dy = y - cy
        return dx * dx + dy * dy <= r * r
    }

    private fun isMapButton(x: Float, y: Float): Boolean {
        val cx = width - dp(158f)
        val cy = height - dp(72f)
        val r = dp(39f)
        val dx = x - cx
        val dy = y - cy
        return dx * dx + dy * dy <= r * r
    }

    private fun drawCharacterPortrait(canvas: Canvas, index: Int, left: Float, top: Float, cardHeight: Float) {
        val bitmap = characterPortraits.getOrNull(index) ?: return
        val maxWidth = 52f
        val maxHeight = cardHeight - 10f
        val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height)
        val targetWidth = bitmap.width * scale
        val targetHeight = bitmap.height * scale
        val bounds = RectF(left + 7f, top + (cardHeight - targetHeight) / 2f, left + 7f + targetWidth, top + (cardHeight + targetHeight) / 2f)
        paint.isFilterBitmap = true
        paint.alpha = 255
        canvas.drawBitmap(bitmap, null, bounds, paint)
    }

    private fun characterAt(x: Float, y: Float): Int? {
        val cardWidth = (width - 134f) / 2f
        val cardHeight = 108f
        for (i in characterNames.indices) {
            val row = i / 2
            val col = i % 2
            val left = if (i == 2) (width - cardWidth) / 2f else 54f + col * (cardWidth + 24f)
            val top = 146f + row * 124f
            if (x in left..(left + cardWidth) && y in top..(top + cardHeight)) return i
        }
        return null
    }

    private fun levelAt(x: Float, y: Float): Int? {
        for (i in 0..3) {
            val row = i / 2
            val col = i % 2
            val left = 54f + col * ((width - 118f) / 2f)
            val top = 160f + row * 142f
            val right = left + (width - 142f) / 2f
            if (x in left..right && y in top..(top + 112f)) return i
        }
        return null
    }

    private fun startPointer(event: MotionEvent, index: Int) {
        if (mapVisible) return
        val x = event.getX(index)
        val y = event.getY(index)
        val pointerId = event.getPointerId(index)
        when {
            isJumpButton(x, y) -> {
                if (jumpPointer == -1) {
                    jumpPointer = pointerId
                    touch.jumpPressed = true
                }
            }
            isMapButton(x, y) -> { showMap(); ignoreMapOpenPointer = pointerId }
            x < width * 0.48f && joystickPointer == -1 -> {
                joystickPointer = pointerId
                joystickCenterX = x
                joystickCenterY = y
                updateJoystick(x, y)
            }
        }
    }

    private fun updateJoystick(x: Float, y: Float) {
        val travel = dp(56f).coerceAtLeast(1f)
        touch.setMove((x - joystickCenterX) / travel, (y - joystickCenterY) / travel)
    }

    private fun finishMapTap(x: Float, y: Float) {
        if (characterPickerVisible) {
            val selected = characterAt(x, y)
            if (selected != null) {
                selectedCharacter = selected
                onCharacterSelected(selected)
            }
            characterPickerVisible = false
            return
        }
        if (x in 50f..282f && y in 70f..136f) {
            characterPickerVisible = true
            return
        }
        val selected = levelAt(x, y) ?: return
        if (selected < unlocked) {
            mapVisible = false
            onLevelSelected(selected)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (mapVisible) return true
                startPointer(event, event.actionIndex)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!mapVisible && joystickPointer != -1) {
                    val index = event.findPointerIndex(joystickPointer)
                    if (index >= 0) updateJoystick(event.getX(index), event.getY(index))
                }
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                if (mapVisible) {
                    if (pointerId == ignoreMapOpenPointer) ignoreMapOpenPointer = -1
                    else if (event.actionMasked == MotionEvent.ACTION_UP) finishMapTap(event.x, event.y)
                } else {
                    if (pointerId == joystickPointer) {
                        joystickPointer = -1
                        touch.clearMove()
                    }
                    if (pointerId == jumpPointer) jumpPointer = -1
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        joystickPointer = -1
                        jumpPointer = -1
                        touch.clearMove()
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                joystickPointer = -1
                jumpPointer = -1
                ignoreMapOpenPointer = -1
                touch.cancelAll()
            }
        }
        invalidate()
        return true
    }
}
