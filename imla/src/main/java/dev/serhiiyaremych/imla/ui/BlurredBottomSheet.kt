package dev.serhiiyaremych.imla.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import dev.serhiiyaremych.imla.uirenderer.Style
import dev.serhiiyaremych.imla.uirenderer.UiLayerRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BoxScope.BlurredBottomSheet(
    uiLayerRenderer: UiLayerRenderer,
    sheetState: SheetState = rememberModalBottomSheetState(),
    dismissOnClickOutside: Boolean = true,
    paddings: PaddingValues = PaddingValues.Absolute(0.dp, 0.dp, 0.dp, 0.dp),
    blurStyle: Style = Style.default,
    shape: Shape = RectangleShape,
    content: @Composable () -> Unit
) {
    if (sheetState.isVisible) {
        var rect by remember { mutableStateOf(Rect.Zero) }
        BackdropBlur(
            style = blurStyle,
            modifier = Modifier
                .drawWithCache {
                    onDrawWithContent {
                        clipRect(
                            left = rect.left,
                            top = rect.top,
                            right = rect.right,
                            bottom = rect.bottom
                        ) {
                            clipPath(path = shape.toPath(size, layoutDirection, this)) {
                                this@onDrawWithContent.drawContent()
                            }
                        }
                    }
                },
            uiLayerRenderer = uiLayerRenderer
        ) {
            Box(
                Modifier
                    .padding(paddings)
                    .fillMaxSize()
            )
        }


        ModalBottomSheet(
            containerColor = Color.Transparent,
            onDismissRequest = {},
            scrimColor = Color.Transparent,
            sheetState = sheetState
        ) {
            Column(Modifier.onPlaced { rect = it.boundsInRoot() }) {
                content()
            }
        }
    }
}
