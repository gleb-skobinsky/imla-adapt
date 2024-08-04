/*
 * Copyright 2024, Serhii Yaremych
 * SPDX-License-Identifier: MIT
 */

package dev.serhiiyaremych.imla.ui

import android.graphics.PixelFormat
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.trace
import androidx.compose.ui.viewinterop.AndroidView
import dev.serhiiyaremych.imla.uirenderer.Style
import dev.serhiiyaremych.imla.uirenderer.UiLayerRenderer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
public fun BackdropBlur(
    modifier: Modifier,
    style: Style = Style.default,
    uiLayerRenderer: UiLayerRenderer,
    blurMask: Brush? = null,
    clipShape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var contentBoundingBox by remember { mutableStateOf(Rect.Zero) }
    val id = remember { trace("BlurBehindView#id") { UUID.randomUUID().toString() } }

    val getTopOffset = {
        IntOffset(
            x = contentBoundingBox.left.toInt(),
            y = contentBoundingBox.top.toInt()
        )
    }

    Box(
        modifier = modifier
            .onPlaced { layoutCoordinates ->
                contentBoundingBox = layoutCoordinates.boundsInParent()
            }
    ) {
        val clipPath = remember { Path() }
        // Render the external surface
        AndroidView(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val outline = clipShape.createOutline(size, layoutDirection, this)
                    clipPath.rewind()
                    clipPath.addOutline(outline)
                    onDrawWithContent {
                        clipPath(path = clipPath) {
                            this@onDrawWithContent.drawContent()
                        }
                    }
                },
            factory = { context ->
                SurfaceView(context).apply {
                    holder.setFormat(PixelFormat.TRANSLUCENT)
                    coroutineScope.launch {
                        snapshotFlow { uiLayerRenderer.isInitialized }
                            .filter { it }
                            .collect {
                                uiLayerRenderer.attachRendererSurface(
                                    surface = holder.surface,
                                    id = id,
                                    size = IntSize(width, height),
                                )
                                uiLayerRenderer.updateOffset(id, getTopOffset())
                                uiLayerRenderer.updateStyle(id, style)
                                uiLayerRenderer.updateMask(id, blurMask)
                            }

                    }
                }
            },
            update = { view ->
                uiLayerRenderer.updateMask(id, blurMask)
                uiLayerRenderer.updateOffset(id, getTopOffset())
                trace("BackdropBlurView#renderObject.style") {
                    uiLayerRenderer.updateStyle(id, style)
                }
            }
        )

        // Render the content and handle offset changes
        content()
    }
}