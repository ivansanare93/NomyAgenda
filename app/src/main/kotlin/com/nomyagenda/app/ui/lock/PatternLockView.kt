package com.nomyagenda.app.ui.lock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.nomyagenda.app.R
import kotlin.math.min

/**
 * A 3×3 pattern-lock grid view.
 *
 * Consumers set [onPatternComplete] to receive the drawn pattern (list of 0-based dot indices)
 * once the user lifts their finger. Call [setState] to show neutral / success / error feedback,
 * and [reset] to clear the current pattern.
 */
class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    enum class State { NEUTRAL, SUCCESS, ERROR }

    private val gridSize = 3
    private val dotCount = gridSize * gridSize

    // Centre coordinates for each dot (populated in onSizeChanged)
    private val dotCx = FloatArray(dotCount)
    private val dotCy = FloatArray(dotCount)

    private val selectedDots = mutableListOf<Int>()
    private var currentTouchX = 0f
    private var currentTouchY = 0f
    private var isDrawing = false

    var state: State = State.NEUTRAL
        private set

    /** Called when the user lifts their finger; receives the ordered list of dot indices. */
    var onPatternComplete: ((List<Int>) -> Unit)? = null
    private val colorSuccess = context.getColor(R.color.pattern_success)
    private val colorError = context.getColor(R.color.pattern_error)

    private val dotPaintOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotPaintInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Sizes (resolved in onSizeChanged)
    private var dotRadiusOuter = 0f
    private var dotRadiusInner = 0f

    // ─── Public API ───────────────────────────────────────────────────────────

    fun setState(newState: State) {
        state = newState
        invalidate()
    }

    fun reset() {
        selectedDots.clear()
        isDrawing = false
        state = State.NEUTRAL
        invalidate()
    }

    fun getPattern(): List<Int> = selectedDots.toList()

    // ─── Size / layout ────────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Force square
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cellW = w.toFloat() / gridSize
        val cellH = h.toFloat() / gridSize
        for (i in 0 until dotCount) {
            val col = i % gridSize
            val row = i / gridSize
            dotCx[i] = cellW * col + cellW / 2f
            dotCy[i] = cellH * row + cellH / 2f
        }
        val cellMin = min(cellW, cellH)
        dotRadiusOuter = cellMin * 0.22f
        dotRadiusInner = cellMin * 0.10f
        linePaint.strokeWidth = cellMin * 0.08f
    }

    // ─── Drawing ──────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val activeColor = when (state) {
            State.SUCCESS -> colorSuccess
            State.ERROR -> colorError
            State.NEUTRAL -> colorNeutral
        }

        // Lines between selected dots
        if (selectedDots.isNotEmpty()) {
            linePaint.color = activeColor
            linePaint.alpha = 160
            val path = Path()
            path.moveTo(dotCx[selectedDots[0]], dotCy[selectedDots[0]])
            for (k in 1 until selectedDots.size) {
                path.lineTo(dotCx[selectedDots[k]], dotCy[selectedDots[k]])
            }
            // Line from last dot to current finger position (while drawing)
            if (isDrawing) {
                path.lineTo(currentTouchX, currentTouchY)
            }
            canvas.drawPath(path, linePaint)
        }

        // Dots
        for (i in 0 until dotCount) {
            val selected = selectedDots.contains(i)
            dotPaintOuter.color = if (selected) activeColor else colorNeutral
            dotPaintOuter.alpha = if (selected) 60 else 30
            canvas.drawCircle(dotCx[i], dotCy[i], dotRadiusOuter, dotPaintOuter)

            dotPaintInner.color = if (selected) activeColor else colorNeutral
            dotPaintInner.alpha = if (selected) 255 else 100
            canvas.drawCircle(dotCx[i], dotCy[i], dotRadiusInner, dotPaintInner)
        }
    }

    // ─── Touch ────────────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                reset()
                isDrawing = true
                handleMove(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentTouchX = event.x
                currentTouchY = event.y
                handleMove(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDrawing = false
                if (selectedDots.size >= MIN_PATTERN_LENGTH) {
                    onPatternComplete?.invoke(selectedDots.toList())
                } else {
                    // Too short – show error briefly then reset
                    setState(State.ERROR)
                    postDelayed({ reset() }, 800)
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleMove(x: Float, y: Float) {
        val hit = hitTest(x, y) ?: return
        if (!selectedDots.contains(hit)) {
            selectedDots.add(hit)
        }
    }

    private fun hitTest(x: Float, y: Float): Int? {
        for (i in 0 until dotCount) {
            val dx = x - dotCx[i]
            val dy = y - dotCy[i]
            if (dx * dx + dy * dy <= dotRadiusOuter * dotRadiusOuter * 4) {
                return i
            }
        }
        return null
    }

    companion object {
        /** Minimum number of dots a valid pattern must contain. */
        const val MIN_PATTERN_LENGTH = 4
    }
}
