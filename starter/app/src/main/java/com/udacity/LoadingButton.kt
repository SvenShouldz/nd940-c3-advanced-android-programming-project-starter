package com.udacity

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var widthSize = 0
    private var heightSize = 0

    private val valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000
        addUpdateListener {
            invalidate()
        }
    }

    private var buttonState: ButtonState by Delegates.observable<ButtonState>(ButtonState.Unclicked) { p, old, new ->
        invalidate()
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 48f
        color = context.getColor(R.color.colorPrimaryDark)
    }

    init {
        isClickable = true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawRect(0f, 0f, widthSize.toFloat(),  heightSize.toFloat(), paint)

        // Draw the loading animation progress
        if (buttonState == ButtonState.Loading) {
            val progress = (valueAnimator.animatedValue as Float) * widthSize
            canvas?.drawRect(progress, 0f, widthSize.toFloat(),  heightSize.toFloat(), paint.apply {
                color = context.getColor(R.color.colorPrimaryDark)
            })
            canvas?.drawRect(0f, 0f, progress, heightSize.toFloat(), paint.apply {
                color = context.getColor(R.color.colorPrimary)
            })
        }

        // Draw text
        val text = when (buttonState) {
            ButtonState.Loading -> context.getString(R.string.button_loading)
            ButtonState.Completed -> context.getString(R.string.button_completed)
            else -> context.getString(R.string.button_name)
        }

        canvas?.drawText(
            text,
            (widthSize / 2).toFloat(),
            (heightSize / 2 - (paint.descent() + paint.ascent()) / 2),
            paint.apply {
                color = Color.WHITE
            }
        )
    }

    fun setLoadingState(state: ButtonState) {
        buttonState = state
        when (state) {
            ButtonState.Loading -> valueAnimator.start()
            ButtonState.Completed -> valueAnimator.cancel()
            else -> invalidate()
        }
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