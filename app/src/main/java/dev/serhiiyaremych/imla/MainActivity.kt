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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tracing.trace
import dev.serhiiyaremych.imla.data.ApiClient
import dev.serhiiyaremych.imla.modifier.blurSource
import dev.serhiiyaremych.imla.ui.BackdropBlur
import dev.serhiiyaremych.imla.ui.BlurredBottomSheet
import dev.serhiiyaremych.imla.ui.BlurredPopup2
import dev.serhiiyaremych.imla.ui.theme.ImlaTheme
import dev.serhiiyaremych.imla.ui.userpost.SimpleImageViewer
import dev.serhiiyaremych.imla.ui.userpost.UserPostView
import dev.serhiiyaremych.imla.uirenderer.Style
import dev.serhiiyaremych.imla.uirenderer.UiLayerRenderer
import dev.serhiiyaremych.imla.uirenderer.rememberUiLayerRenderer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            ImlaTheme {
                val coroutineScope = rememberCoroutineScope()
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val uiRenderer = rememberUiLayerRenderer()
                val uiRenderer2 = rememberUiLayerRenderer()
                var viewingImage by remember { mutableStateOf("") }
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Full height content
                    Surface(
                        Modifier
                            .fillMaxSize()
                            .blurSource(uiRenderer2)
                            .blurSource(uiRenderer),
                    ) {
                        Content(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = TopAppBarDefaults.MediumAppBarExpandedHeight),
                            onImageClick = { viewingImage = it }
                        )
                    }
                    var showDialog by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.matchParentSize()) {
                        // Layer 0 above full height content
                        BlurryTopAppBar(uiRenderer)
                        Spacer(modifier = Modifier.weight(1f))
                        // Layer 1 full height content
                        BlurryBottomNavBar(
                            uiRenderer = uiRenderer,
                            onShowDialog = {
                                showDialog = !showDialog
                            },
                            onShowSettings = {
                                coroutineScope.launch {
                                    sheetState.show()
                                }
                            }
                        )
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .matchParentSize(),
                        visible = viewingImage.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        BackdropBlur(
                            modifier = Modifier.matchParentSize(),
                            style = Style(
                                blurRadius = 10.dp,
                                tint = Color.Transparent,
                                noiseAlpha = 0.3f
                            ),
                            uiLayerRenderer = uiRenderer
                        ) {
                            SimpleImageViewer(
                                modifier = Modifier.fillMaxSize(),
                                imageUrl = viewingImage,
                                onDismiss = { viewingImage = "" }
                            )
                        }
                        DisposableEffect(Unit) {
                            onDispose { uiRenderer.onUiLayerUpdated() }
                        }
                    }


                    BlurredBottomSheet(
                        uiLayerRenderer = uiRenderer2,
                        sheetState = sheetState
                    ) {
                        Box(
                            Modifier.fillMaxSize(), Alignment.Center
                        ) {
                            Text("Test bottom sheet content")
                        }
                    }


                    if (showDialog) {
                        BlurredPopup2(
                            alignment = Alignment.Center,
                            blurStyle = Style(blurRadius = 10.dp, tint = Color.Transparent),
                            uiLayerRenderer = uiRenderer,
                            clipShape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp, 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Test text",
                                    fontSize = 20.sp
                                )
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
        onShowDialog: () -> Unit,
        onShowSettings: () -> Unit
    ) {
        BackdropBlur(
            modifier = Modifier.fillMaxWidth(),
//                .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            style = Style(
                blurRadius = 6.dp,
                noiseAlpha = 0.2f,
                tint = Color.Black.copy(alpha = 0.5f)
            ),
            clipShape = RoundedCornerShape(50),
            uiLayerRenderer = uiRenderer
        ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                windowInsets = WindowInsets(bottom = 0.dp),
                containerColor = Color.Transparent
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = { /*TODO*/ },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Home, contentDescription = null
                        )
                    }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /*TODO*/ },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Search, contentDescription = null
                        )
                    }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onShowDialog,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Notifications, contentDescription = null
                        )
                    }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onShowSettings,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Settings, contentDescription = null
                        )
                    }
                )
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun BlurryTopAppBar(uiRenderer: UiLayerRenderer) {
        BackdropBlur(
            modifier = Modifier.requiredHeight(100.dp),
            style = Style(
                blurRadius = 1.dp,
                noiseAlpha = 0.0f,
                tint = Color.White.copy(alpha = 0.5f),
            ),
            uiLayerRenderer = uiRenderer
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
        onImageClick: (String) -> Unit
    ) = trace("MainActivity#Content") {
        val scrollState = rememberLazyListState()
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