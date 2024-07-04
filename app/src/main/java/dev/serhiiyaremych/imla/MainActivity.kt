/*
 * Copyright 2024, Serhii Yaremych
 * SPDX-License-Identifier: MIT
 */

package dev.serhiiyaremych.imla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tracing.trace
import dev.serhiiyaremych.imla.data.ApiClient
import dev.serhiiyaremych.imla.modifier.blurSource
import dev.serhiiyaremych.imla.ui.BackdropBlur
import dev.serhiiyaremych.imla.ui.theme.ImlaTheme
import dev.serhiiyaremych.imla.ui.userpost.SimpleImageViewer
import dev.serhiiyaremych.imla.ui.userpost.UserPostView
import dev.serhiiyaremych.imla.uirenderer.Style
import dev.serhiiyaremych.imla.uirenderer.UiLayerRenderer
import dev.serhiiyaremych.imla.uirenderer.rememberUiLayerRenderer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            ImlaTheme {
                val uiRenderer = rememberUiLayerRenderer(downSampleFactor = 2)
                val viewingImage = remember {
                    mutableStateOf("")
                }
                Box(modifier = Modifier.fillMaxWidth()) {

                    // Full height content
                    Surface(
                        Modifier
                            .fillMaxSize()
                            .blurSource(uiRenderer),
                    ) {
                        Content(modifier = Modifier
                            .fillMaxSize(),
                            contentPadding = PaddingValues(top = TopAppBarDefaults.MediumAppBarExpandedHeight),
                            onImageClick = { viewingImage.value = it },
                            onScroll = { /*uiRenderer.onUiLayerUpdated()*/ })
                    }
                    val showBottomSheet = remember { mutableStateOf(false) }
                    Column(modifier = Modifier.matchParentSize()) {
                        // Layer 0 above full height content
                        BlurryTopAppBar(uiRenderer)
                        Spacer(modifier = Modifier.weight(1f))
                        // Layer 1 full height content
                        BlurryBottomNavBar(uiRenderer) {
                            showBottomSheet.value = true
                        }
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .matchParentSize(),
                        visible = viewingImage.value.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        BackdropBlur(
                            modifier = Modifier.matchParentSize(),
                            uiLayerRenderer = uiRenderer,
                            style = Style(
                                blurRadius = 10.dp,
                                tint = Color.Transparent,
                                noiseAlpha = 0.3f
                            )
                        ) {
                            SimpleImageViewer(
                                modifier = Modifier
                                    .fillMaxSize(),
                                imageUrl = viewingImage.value,
                                onDismiss = { viewingImage.value = "" })
                        }
                        DisposableEffect(key1 = Unit) {
                            onDispose { uiRenderer.onUiLayerUpdated() }
                        }
                    }

                    if (showBottomSheet.value) {
                        val sheetState = rememberModalBottomSheetState()

                        val blur = remember {
                            mutableIntStateOf(1)
                        }
                        val sheetHeight = remember {
                            mutableIntStateOf(0)
                        }
                        val noiseAlpha = remember {
                            mutableFloatStateOf(0.1f)
                        }
                        BackdropBlur(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(2f),
                            style = Style(
                                blurRadius = blur.intValue.dp,
                                tint = Color.Transparent,
                                noiseAlpha = noiseAlpha.floatValue
                            ),
                            uiLayerRenderer = uiRenderer
                        )

                        ModalBottomSheet(
                            modifier = Modifier
                                .statusBarsPadding()
                                .onSizeChanged { sheetHeight.intValue = it.height },
                            sheetState = sheetState,
                            scrimColor = Color.Transparent,
                            onDismissRequest = { showBottomSheet.value = false },
                            containerColor = Color.White.copy(alpha = 0.4f)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .safeGesturesPadding(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Blur Settings")
                                Spacer(Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Noise")
                                    Spacer(Modifier.width(16.dp))
                                    Slider(
                                        noiseAlpha.floatValue,
                                        onValueChange = { noiseAlpha.floatValue = it },
                                        valueRange = 0.0f..1.0f
                                    )
                                }
                            }

                        }
                        LaunchedEffect(sheetState) {
                            snapshotFlow { sheetState.requireOffset() }
                                .distinctUntilChanged()
                                .collect {
                                    val expandFraction = 1.0f - (it / sheetHeight.intValue)
                                    blur.intValue =
                                        (32f * expandFraction).roundToInt().coerceAtLeast(1)
                                }
                        }

                    }

                }
            }
        }
    }

    @Composable
    private fun BlurryBottomNavBar(
        uiRenderer: UiLayerRenderer,
        onShowSettings: () -> Unit
    ) {

        BackdropBlur(
            modifier = Modifier
                .fillMaxWidth()
//                .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .border(
                    Dp.Hairline,
                    Color.DarkGray,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            uiLayerRenderer = uiRenderer,
            style = Style(
                blurRadius = 10.dp,
                noiseAlpha = 0.2f
            )
        ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                windowInsets = WindowInsets(bottom = 0.dp),
                containerColor = Color.Transparent
            ) {
                NavigationBarItem(selected = false, onClick = { /*TODO*/ }, icon = {
                    Icon(
                        imageVector = Icons.Filled.Home, contentDescription = null
                    )
                })
                NavigationBarItem(selected = false, onClick = { /*TODO*/ }, icon = {
                    Icon(
                        imageVector = Icons.Filled.Search, contentDescription = null
                    )
                })
                NavigationBarItem(selected = false, onClick = { /*TODO*/ }, icon = {
                    Icon(
                        imageVector = Icons.Filled.Notifications, contentDescription = null
                    )
                })
                NavigationBarItem(selected = false, onClick = onShowSettings, icon = {
                    Icon(
                        imageVector = Icons.Filled.Settings, contentDescription = null
                    )
                })

            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun BlurryTopAppBar(uiRenderer: UiLayerRenderer) {
        BackdropBlur(
            modifier = Modifier.requiredHeight(150.dp),
            uiLayerRenderer = uiRenderer,
            blurMask = /*Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 1.0f),
                    Color.White.copy(alpha = 1.0f),
                    Color.White.copy(alpha = 0.9f),
                    Color.White.copy(alpha = 0.6f),
                    Color.White.copy(alpha = 0.0f),
                ),
            )*/null,
            style = Style(
                blurRadius = 15.dp,
                noiseAlpha = 0.2f
            )
        ) {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Blur Demo") },
                windowInsets = WindowInsets(top = 0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = { /* "Open nav drawer" */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = null)
                    }
                }
            )
        }
    }

    @Composable
    private fun Content(
        modifier: Modifier,
        contentPadding: PaddingValues,
        onImageClick: (String) -> Unit,
        onScroll: (Int) -> Unit
    ) = trace("MainActivity#Content") {
        val scrollState = rememberLazyListState()
        val currentOnScroll = rememberUpdatedState(onScroll).value
        LaunchedEffect(key1 = scrollState, key2 = onScroll) {
            snapshotFlow { scrollState.firstVisibleItemScrollOffset }.distinctUntilChanged()
                .collect {
                    currentOnScroll(it)
                }
        }
        val posts =
            ApiClient.getPosts().collectAsStateWithLifecycle(initialValue = persistentListOf())
        LazyColumn(modifier = modifier, state = scrollState, contentPadding = contentPadding) {
            items(posts.value, key = { it.id }) { item ->
                UserPostView(
                    modifier = Modifier.fillMaxWidth(), post = item, onImageClick = onImageClick
                )
            }
        }

    }
}