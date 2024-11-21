package com.udacity

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat.getColor
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var widthSize = 0
    private var heightSize = 0
    private var progress = 0f

    private var buttonBackgroundColor: Int = getColor(context, R.color.colorPrimaryDark)
    private var progressColor: Int = getColor(context, R.color.colorPrimary)
    private var textColor: Int = Color.WHITE
    private var errorColor: Int = Color.RED
    private var buttonText: String = context.getString(R.string.state_default)

    private var buttonState: ButtonState by Delegates.observable<ButtonState>(ButtonState.Default) { p, old, new ->
        invalidate()
    }

    private val paintBackground = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 48f
    }

    private val valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000
        addUpdateListener {
            progress = it.animatedFraction
            invalidate()
        }
    }

    init {
        // Get custom attributes
        context.theme.obtainStyledAttributes(attrs, R.styleable.LoadingButton, 0, 0).apply {
            try {
                buttonBackgroundColor = getColor(R.styleable.LoadingButton_buttonBackgroundColor, Color.GRAY)
                progressColor = getColor(R.styleable.LoadingButton_progressColor, Color.BLUE)
                textColor = getColor(R.styleable.LoadingButton_textColor, Color.WHITE)
                buttonText = getString(R.styleable.LoadingButton_buttonText) ?: context.getString(R.string.state_default)
                errorColor = getColor(R.styleable.LoadingButton_errorColor, Color.RED)
            } finally {
                recycle()
            }
        }

        paintBackground.color = buttonBackgroundColor
        paintProgress.color = progressColor
        paintText.color = textColor

        isClickable = true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // Draw Background
        canvas?.drawRect(0f, 0f, widthSize.toFloat(), heightSize.toFloat(), paintBackground)

        // Draw the loading animation progress
        if (buttonState == ButtonState.Loading) {
            canvas?.drawRect(0f, 0f, progress * widthSize, heightSize.toFloat(), paintProgress)
        }

        if (buttonState == ButtonState.Failed) {
            paintBackground.color = errorColor  // Apply the error color to the background
            invalidate()  // Redraw the view with the new color
        }

        // Draw text
        val text = when (buttonState) {
            ButtonState.Default -> context.getString(R.string.state_default)
            ButtonState.Pending -> context.getString(R.string.state_pending)
            ButtonState.Loading -> context.getString(R.string.state_loading)
            ButtonState.Failed -> context.getString(R.string.state_failed)
            ButtonState.Completed -> context.getString(R.string.state_completed)
        }

        canvas?.drawText(
            text,
            (widthSize / 2).toFloat(),
            (heightSize / 2 - (paintText.descent() + paintText.ascent()) / 2),
            paintText
        )
    }

    fun setLoadingState(state: ButtonState) {
        when (state) {
            ButtonState.Loading -> {
                // Disable button to prevent spamming clicks
                isClickable = false
                buttonState = state
                valueAnimator.start()
            }
            ButtonState.Pending -> {
                buttonState = state
                valueAnimator.pause()
            }
            ButtonState.Failed -> {
                buttonState = state
                valueAnimator.cancel()
            }
            ButtonState.Completed -> {
                if (isAnimationCompleted()) {
                    buttonState = state
                    valueAnimator.cancel()
                    // Trigger notification if animation is already completed
                    (context as MainActivity).sendNotification(
                        context.getString(R.string.notification_title),
                        context.getString(R.string.notification_description),
                        true
                    )
                    isClickable = true
                } else {
                    // Delay setting to Completed until animation finishes
                    valueAnimator.addListener(object : Animator.AnimatorListener {
                        override fun onAnimationEnd(animation: Animator) {
                            buttonState = ButtonState.Completed
                            // Trigger notification when animation finishes
                            (context as MainActivity).sendNotification(
                                context.getString(R.string.notification_title),
                                context.getString(R.string.notification_description),
                                true
                            )
                            isClickable = true
                            valueAnimator.removeListener(this) // Clean up listener
                        }

                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                }
            }
            else -> {
                buttonState = state
                invalidate()
            }
        }
    }

    private fun isAnimationCompleted(): Boolean {
        return !valueAnimator.isRunning && progress >= 1f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }
}