package net.odiak.camerapreview

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView

class CustomSurfaceView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        setMeasuredDimension(measuredWidth, Math.round(measuredWidth * 1.333f))
    }
}
