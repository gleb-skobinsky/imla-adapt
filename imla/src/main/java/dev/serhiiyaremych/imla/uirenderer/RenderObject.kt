/*
 * Copyright 2024, Serhii Yaremych
 * SPDX-License-Identifier: MIT
 */

@file:Suppress("unused", "CanBeParameter")

package dev.serhiiyaremych.imla.uirenderer

import android.graphics.PixelFormat
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.view.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.util.trace
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import dev.serhiiyaremych.imla.renderer.Framebuffer
import dev.serhiiyaremych.imla.renderer.SubTexture2D
import dev.serhiiyaremych.imla.renderer.Texture2D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class RenderObject internal constructor(
    internal val id: String,
    internal var highResRect: Rect,
    internal val lowResRect: Rect,
    internal var highResFBO: Framebuffer,
    internal var lowResLayer: SubTexture2D,
    internal val renderableScope: RenderableScope,
) {
    private var renderCallback: ((RenderObject) -> Unit)? = null

    private val openGLCallback = object : GLRenderer.RenderCallback {
        override fun onDrawFrame(eglManager: EGLManager) {
            trace("RenderObject#onRender") {
                renderCallback?.invoke(this@RenderObject)
            }
        }
    }

    internal var style: Style = Style.default

    internal fun updateStyle(newStyle: Style, onRenderComplete: () -> Unit) {
        if (style == newStyle) {
            onRenderComplete()
            return
        }
        style = newStyle
        invalidate {
            onRenderComplete()
        }
    }

    internal var mask: Texture2D? = null

    public var renderTarget: GLRenderer.RenderTarget? = null

    public fun invalidate(onRenderComplete: (GLRenderer.RenderTarget) -> Unit) {
        renderTarget?.requestRender(onRenderComplete)
    }

    public fun setRenderCallback(onRender: ((RenderObject) -> Unit)?) {
        this.renderCallback = onRender
    }

    public fun updateOffset(offset: IntOffset, onRenderComplete: () -> Unit): Unit =
        trace("RenderObject#updateOffset") {
            val (x, y) = offset
            val scaledTranslateY =
                lowResLayer.texture.height - (y * renderableScope.scale) - lowResRect.height
            val scaledRect = lowResRect.translate(
                translateX = x.toFloat() * renderableScope.scale,
                translateY = scaledTranslateY
            )
            // todo: update coordinates in place
            lowResLayer = SubTexture2D.createFromCoords(
                texture = lowResLayer.texture,
                rect = scaledRect
            )

            val rect = highResRect.translate(
                translateX = x.toFloat(),
                translateY = y.toFloat()
            )
            highResRect = rect

            invalidate {
                onRenderComplete()
            }
        }

    override fun toString(): String {
        return """RenderObject(
            id='$id', rect='$highResRect', 
            layer='${lowResLayer.id}, ${lowResLayer.subTextureSize}',
            style=$style
        )""".trimIndent()
    }


    public fun detachFromRenderer() {
        renderTarget?.detach(true)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RenderObject

        if (id != other.id) return false
        if (highResRect != other.highResRect) return false
        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + highResRect.hashCode()
        result = 31 * result + style.hashCode()
        return result
    }


    public companion object {

        private fun createImageReader(width: Int, height: Int): ImageReader {
            val maxImages = 2
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ImageReader.newInstance(
                    /* width = */ width,
                    /* height = */ height,
                    /* format = */ PixelFormat.RGBA_8888,
                    /* maxImages = */ maxImages,
                    /* usage = */
                    HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
                )
            } else {
                ImageReader.newInstance(
                    /* width = */ width,
                    /* height = */ height,
                    /* format = */ PixelFormat.RGBA_8888,
                    /* maxImages = */ maxImages
                )
            }
        }

        internal fun createFromSurface(
            id: String,
            renderableLayer: RenderableRootLayer,
            glRenderer: GLRenderer,
            surface: Surface,
            rect: Rect,
        ): RenderObject {
            val scaledTexture = renderableLayer.lowResTexture
            val scaledRegion = Matrix().apply {
                scale(renderableLayer.scale, renderableLayer.scale)
            }.map(rect)

            val renderObject = RenderObject(
                id = id,
                highResRect = rect,
                lowResRect = scaledRegion,
                highResFBO = renderableLayer.highResFBO,
                lowResLayer = SubTexture2D.createFromCoords(
                    texture = scaledTexture,
                    rect = scaledRegion
                ),
                renderableScope = RenderableScope(
                    scale = renderableLayer.scale,
                    originalSizeInt = rect.size.toIntSize(),
                    renderer = renderableLayer.renderer2D
                ),
            ).apply {
                renderTarget = glRenderer.attach(
                    surface = surface,
                    width = rect.width.toInt(),
                    height = rect.height.toInt(),
                    renderer = openGLCallback
                )
            }
            return renderObject
        }
    }
}