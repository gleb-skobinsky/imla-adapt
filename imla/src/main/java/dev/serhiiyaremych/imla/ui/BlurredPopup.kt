package dev.serhiiyaremych.imla.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.serhiiyaremych.imla.uirenderer.Style
import dev.serhiiyaremych.imla.uirenderer.UiLayerRenderer

@Composable
public fun BlurredPopup(
    alignment: Alignment,
    uiLayerRenderer: UiLayerRenderer,
    offset: IntOffset = IntOffset.Zero,
    properties: PopupProperties = PopupProperties(
        usePlatformDefaultWidth = false
    ),
    onDismissRequest: () -> Unit,
    blurStyle: Style = Style.default,
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
    val positionProvider = remember(alignment, offset) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val anchorAlignmentPoint = alignment.align(
                    IntSize.Zero,
                    anchorBounds.size,
                    layoutDirection
                )
                // Note the negative sign. Popup alignment point contributes negative offset.
                val popupAlignmentPoint = -alignment.align(
                    IntSize.Zero,
                    popupContentSize,
                    layoutDirection
                )
                val resolvedUserOffset = IntOffset(
                    offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1),
                    offset.y
                )

                val completeOffset = anchorBounds.topLeft +
                        anchorAlignmentPoint +
                        popupAlignmentPoint +
                        resolvedUserOffset

                contentRect = Rect(
                    offset = completeOffset.toOffset(),
                    size = popupContentSize.toSize()
                )

                return completeOffset
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp)
        ) {
            MeasuredSurface(
                uiLayerRenderer = uiLayerRenderer,
                contentRect = contentRect,
                onTopOffset = getTopOffset,
                blurMask = blurMask,
                clipShape = clipShape,
                style = blurStyle,
                content = content,
            )
        }
    }
}

@Composable
public fun BoxScope.BlurredPopup2(
    alignment: Alignment = Alignment.Center,
    uiLayerRenderer: UiLayerRenderer,
    blurStyle: Style = Style.default,
    blurMask: Brush? = null,
    clipShape: Shape = RectangleShape,
    content: @Composable () -> Unit
) {
    BackdropBlur(
        modifier = Modifier.align(alignment),
        style = blurStyle,
        blurMask = blurMask,
        clipShape = clipShape,
        uiLayerRenderer = uiLayerRenderer
    ) {
        content()
    }
}