package com.example.sathvikwidget.screens

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.sathvikwidget.components.BottomNavBar
import com.example.sathvikwidget.components.PhotoUriManager
import com.example.sathvikwidget.components.Route
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val photoUriManager = remember { PhotoUriManager(context) }
    val scope = rememberCoroutineScope()

    // State for main photo URI
    val mainPhotoUri by photoUriManager.mainPhotoUriFlow.collectAsState(initial = null)
    val rotationAngle by photoUriManager.photoRotationFlow.collectAsState(initial = 0f)

    // Animated rotation value
    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        label = "rotationAnimation"
    )

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Transformable state for handling zoom and pan gestures
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        // Update scale with constraints (min 1f, max 5f)
        scale = (scale * zoomChange).coerceIn(1f, 5f)

        // Update offset with constraints based on scale
        // This creates a "rubberbanding" effect when dragging beyond bounds
        val maxX = (scale - 1f) * 500 // Approximation, adjust based on your image dimensions
        val maxY = (scale - 1f) * 500

        offset = Offset(
            x = (offset.x + offsetChange.x).coerceIn(-maxX, maxX),
            y = (offset.y + offsetChange.y).coerceIn(-maxY, maxY)
        )
    }

    Box(
        modifier = modifier.fillMaxSize().transformable(transformableState)
    ) {
        // Display main photo as background with rotation
//        AsyncImage(
//            model = ImageRequest.Builder(LocalContext.current)
//                .data(mainPhotoUri)
//                .crossfade(true)
//                .build(),
//            contentDescription = "Main Photo",
//            modifier = Modifier
//                .fillMaxSize()
//                .graphicsLayer {
//                    scaleX = scale
//                    scaleY = scale
//                    translationX = offset.x
//                    translationY = offset.y
//                    rotationZ = animatedRotation
//                }
//                ,
//            contentScale = ContentScale.Fit
//        )

        PinchToZoomView(modifier = modifier,mainPhotoUri=mainPhotoUri, offset = offset, animatedRotation = animatedRotation)

        // Rotation button with semi-transparent background
        if (mainPhotoUri != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rotation button
                FloatingActionButton(
                    onClick = {
                        // Rotate 90 degrees clockwise and save to DataStore
                        val newAngle = (rotationAngle + 90f) % 360f
                        scope.launch {
                            photoUriManager.savePhotoRotation(newAngle)
                        }
                    },
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rotate Image"
                    )
                }

                // Reset zoom button
                FloatingActionButton(
                    onClick = {
                        // Reset zoom and pan
                        scale = 1f
                        offset = Offset.Zero
                    },
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Reset Zoom"
                    )
                }
            }
        }

        // If no main photo is selected, show a message
        if (mainPhotoUri == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No main photo selected",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        // Bottom navigation stays on top of everything
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BottomNavBar(navController = navController)
        }
    }
}


@Composable
fun PinchToZoomView(
    modifier: Modifier,
    imageContentDescription: String = "",
    mainPhotoUri:Uri?,
    offset:Offset,
    animatedRotation:Float
) {
    // Mutable state variables to hold scale and offset values
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val minScale = 1f
    val maxScale = 4f

    // Remember the initial offset
    var initialOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Coefficient for slowing down movement
    val slowMovement = 0.5f

    // Box composable containing the image
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Update scale with the zoom
                    val newScale = scale * zoom
                    scale = newScale.coerceIn(minScale, maxScale)

                    // Calculate new offsets based on zoom and pan
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val offsetXChange = (centerX - offsetX) * (newScale / scale - 1)
                    val offsetYChange = (centerY - offsetY) * (newScale / scale - 1)

                    // Calculate min and max offsets
                    val maxOffsetX = (size.width / 2) * (scale - 1)
                    val minOffsetX = -maxOffsetX
                    val maxOffsetY = (size.height / 2) * (scale - 1)
                    val minOffsetY = -maxOffsetY

                    // Update offsets while ensuring they stay within bounds
                    if (scale * zoom <= maxScale) {
                        offsetX = (offsetX + pan.x * scale * slowMovement + offsetXChange)
                            .coerceIn(minOffsetX, maxOffsetX)
                        offsetY = (offsetY + pan.y * scale * slowMovement + offsetYChange)
                            .coerceIn(minOffsetY, maxOffsetY)
                    }

                    // Store initial offset on pan
                    if (pan != Offset(0f, 0f) && initialOffset == Offset(0f, 0f)) {
                        initialOffset = Offset(offsetX, offsetY)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Reset scale and offset on double tap
                        if (scale != 1f) {
                            scale = 1f
                            offsetX = initialOffset.x
                            offsetY = initialOffset.y
                        } else {
                            scale = 2f
                        }
                    }
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
    ) {
        // Image to be displayed with pinch-to-zoom functionality
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(mainPhotoUri)
                .crossfade(true)
                .build(),
            contentDescription = "Main Photo",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    rotationZ = animatedRotation
                }
            ,
            contentScale = ContentScale.Fit
        )
    }
}