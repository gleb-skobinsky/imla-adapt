package dev.serhiiyaremych.imla.ui

import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.window.Popup
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
        ImlaPopupPositionProvider(alignment, offset) {
            contentRect = it
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = properties
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

