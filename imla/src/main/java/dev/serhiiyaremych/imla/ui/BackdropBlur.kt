/*
 * Copyright 2024, Serhii Yaremych
 * SPDX-License-Identifier: MIT
 */

package dev.serhiiyaremych.imla.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntSize
import dev.serhiiyaremych.imla.uirenderer.Style
import dev.serhiiyaremych.imla.uirenderer.UiLayerRenderer
import java.util.UUID

@Composable
@NonRestartableComposable
private fun rememberNewId() = rememberSaveable { UUID.randomUUID().toString() }

@Composable
public fun BackdropBlur(
    modifier: Modifier,
    style: Style = Style.default,
    uiLayerRenderer: UiLayerRenderer,
    blurMask: Brush? = null,
    clipShape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit
) {
    var contentRect by remember { mutableStateOf(Rect.Zero) }

    val getTopOffset = {
        IntOffset(
            x = contentRect.left.toInt(),
            y = contentRect.top.toInt()
        )
    }

    MeasuredSurface(
        modifier = modifier.onPlaced { contentRect = it.boundsInRoot() },
        uiLayerRenderer = uiLayerRenderer,
        contentRect = contentRect,
        onTopOffset = getTopOffset,
        blurMask = blurMask,
        clipShape = clipShape,
        style = style,
        content = content,
    )
}

@Composable
internal fun MeasuredSurface(
    uiLayerRenderer: UiLayerRenderer,
    contentRect: Rect,
    style: Style,
    clipShape: Shape,
    modifier: Modifier = Modifier,
    blurMask: Brush? = null,
    onTopOffset: () -> IntOffset = {
        IntOffset(
            x = contentRect.left.toInt(),
            y = contentRect.top.toInt()
        )
    },
    content: @Composable BoxScope.() -> Unit
) {
    val id = rememberNewId()
    var updateOffsetComplete by remember { mutableStateOf(false) }
    var updateMaskComplete by remember { mutableStateOf(false) }
    var updateStyleComplete by remember { mutableStateOf(false) }
    val allComplete =
        updateOffsetComplete && updateMaskComplete && updateStyleComplete
    Box(modifier.isVisible(allComplete)) {
        var attachedToSurface = remember { false }
        // Render the external surface
        ImlaExternalSurface(
            modifier = Modifier
                .matchParentSize()
                .clipToShape(clipShape),
            surfaceSize = contentRect.size.toIntSize(),
            onUpdate = { surface ->
                if (uiLayerRenderer.isInitialized && !attachedToSurface) {
                    uiLayerRenderer.attachRendererSurface(
                        surface = surface,
                        id = id,
                        size = IntSize(width, height),
                    )
                    attachedToSurface = true
                }
                uiLayerRenderer.updateOffset(id, onTopOffset()) {
                    updateOffsetComplete = true
                }
                uiLayerRenderer.updateStyle(id, style) {
                    updateStyleComplete = true
                }
                uiLayerRenderer.updateMask(id, blurMask) {
                    updateMaskComplete = true
                }
            },
        ) {
            onSurface { surface, _, _ ->
                surface.onDestroyed {
                    uiLayerRenderer.detachRenderObject(id)
                }
            }
        }

        content()
    }
}

internal fun Modifier.clipToShape(
    shape: Shape
) = this
    .drawWithCache {
        onDrawWithContent {
            clipPath(path = shape.toPath(size, layoutDirection, this)) {
                this@onDrawWithContent.drawContent()
            }
        }
    }

internal fun Shape.toPath(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density
): Path {
    val clipPath = Path()
    val outline = createOutline(size, layoutDirection, density)
    clipPath.rewind()
    clipPath.addOutline(outline)
    clipPath.close()
    return clipPath
}

private fun Modifier.isVisible(condition: Boolean) = this.alpha(if (condition) 1f else 0f)