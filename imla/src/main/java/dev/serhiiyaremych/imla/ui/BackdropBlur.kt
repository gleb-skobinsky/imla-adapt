/*
 * Copyright 2024, Serhii Yaremych
 * SPDX-License-Identifier: MIT
 */

package dev.serhiiyaremych.imla.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.toIntSize
import dev.serhiiyaremych.imla.uirenderer.Style
import dev.serhiiyaremych.imla.uirenderer.UiLayerRenderer
import java.util.UUID


@Composable
@NonSkippableComposable
private fun rememberNewId() = remember { UUID.randomUUID().toString() }

@Composable
public fun BackdropBlur(
    modifier: Modifier,
    style: Style = Style.default,
    uiLayerRenderer: UiLayerRenderer,
    blurMask: Brush? = null,
    clipShape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit = {}
) {
    var contentBoundingBox by remember { mutableStateOf(Rect.Zero) }
    val id = rememberNewId()

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
        // Render the external surface
        ImlaExternalSurface(
            modifier = Modifier.clipToShape(clipShape),
            isOpaque = false,
            onUpdate = { surface ->
                if (uiLayerRenderer.isInitialized) {
                    uiLayerRenderer.attachRendererSurface(
                        surface = surface,
                        id = id,
                        size = IntSize(width, height),
                    )
                }
                uiLayerRenderer.updateOffset(id, getTopOffset())
                uiLayerRenderer.updateStyle(id, style)
                uiLayerRenderer.updateMask(id, blurMask)
            },
            surfaceSize = contentBoundingBox.size.toIntSize(),
        ) {
            onSurface { surface, _, _ ->
                //surfaceReference = surface
                surface.onDestroyed {
                    uiLayerRenderer.detachRenderObject(id)
                }
            }
        }

        // Render the content and handle offset changes
        content()
    }
}

context(BoxScope)
private fun Modifier.clipToShape(shape: Shape) = this
    .matchParentSize()
    .drawWithCache {
        val clipPath = Path()
        val outline = shape.createOutline(size, layoutDirection, this)
        clipPath.rewind()
        clipPath.addOutline(outline)
        onDrawWithContent {
            clipPath(path = clipPath) {
                this@onDrawWithContent.drawContent()
            }
        }
    }