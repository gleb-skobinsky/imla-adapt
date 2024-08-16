package dev.serhiiyaremych.imla.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.SurfaceView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb

private val redColor = Paint().apply { color = Color.Red.toArgb() }

internal class ClippedSurfaceView(
    context: Context
) : SurfaceView(context) {

    internal var clipPath: Path? = null

    override fun dispatchDraw(canvas: Canvas) {
        clipPath?.let {
            println("Canvas size: ${canvas.width} ${canvas.height}")
            for (segment in it) {
                println(segment)
            }
            canvas.clipRect(0f, 0f, 700f, -100f)
        }
        super.dispatchDraw(canvas)
    }
}