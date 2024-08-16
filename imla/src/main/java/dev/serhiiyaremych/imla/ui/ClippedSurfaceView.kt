package dev.serhiiyaremych.imla.ui

import android.content.Context
import android.graphics.Canvas
import android.view.SurfaceView
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath

internal class ClippedSurfaceView(
    context: Context
) : SurfaceView(context) {

    internal var clipPath: Path? = null

    override fun dispatchDraw(canvas: Canvas) {
        clipPath?.let {
            canvas.clipPath(it.asAndroidPath())
        }
        super.dispatchDraw(canvas)
    }
}