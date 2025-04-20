package com.example.sathvikwidget.screens


import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.navigation.NavController
import coil3.compose.AsyncImage // Import Coil's AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.sathvikwidget.components.BottomNavBar
import com.example.sathvikwidget.components.MyAppWidget
import com.example.sathvikwidget.components.PhotoUriManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun ConfigureScreen(controller: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val photoUriManager = remember { PhotoUriManager(context) }
    // State for the single main photo URI
    var selectedMainPhotoUri by remember { mutableStateOf<Uri?>(null) }
    // State list for multiple widget photo URIs
    val selectedWidgetPhotosUris = remember { mutableStateListOf<Uri>() }

    LaunchedEffect(Unit) {
        // Load main photo URI
        photoUriManager.mainPhotoUriFlow.collectLatest { uri ->
            if (uri != null) {
                selectedMainPhotoUri = uri
            }
        }
    }

    // Modify the LaunchedEffect that watches selectedWidgetPhotosUris
    LaunchedEffect(selectedWidgetPhotosUris.toList()) {
        if (selectedWidgetPhotosUris.isNotEmpty()) {
            photoUriManager.saveWidgetPhotoUris(selectedWidgetPhotosUris.toList())

            // Update the widget
            updateWidgets(context)
        }
    }

    val savedWidgetUris = photoUriManager.widgetPhotoUrisFlow.collectAsState(initial = emptyList())

    LaunchedEffect(savedWidgetUris.value) {
        if (selectedWidgetPhotosUris.isEmpty() && savedWidgetUris.value.isNotEmpty()) {
            selectedWidgetPhotosUris.clear()
            selectedWidgetPhotosUris.addAll(savedWidgetUris.value)
        }
    }

    // Save changes when URIs are updated
    LaunchedEffect(selectedMainPhotoUri) {
        photoUriManager.saveMainPhotoUri(selectedMainPhotoUri)
    }

    LaunchedEffect(selectedWidgetPhotosUris.toList()) {
        if (selectedWidgetPhotosUris.isNotEmpty()) {
            photoUriManager.saveWidgetPhotoUris(selectedWidgetPhotosUris.toList())
        }
    }


    // Launcher for SINGLE photo selection (Main Photo)
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = {
            uri -> selectedMainPhotoUri = uri
            if (uri != null) {
                // Take a persistent permission to read the URI across app restarts
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
    )

    // Launcher for MULTIPLE photo selection (Widget Photos)
    // You can optionally set a limit using ActivityResultContracts.PickMultipleVisualMedia(maxItems = N)
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(), // No limit by default
        onResult = { uris ->
            // Add all newly selected URIs to the list
            val newUris = uris.filter { newUri -> !selectedWidgetPhotosUris.any { it.toString() == newUri.toString() } }
            newUris.forEach { uri ->
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            selectedWidgetPhotosUris.addAll(newUris)

            if (newUris.isNotEmpty()) {
                CoroutineScope(Dispatchers.Unconfined).launch {
                    photoUriManager.updateWidgets(context)
                }
            }

        }
    )

    Column(
        modifier = modifier
            .fillMaxSize() ,// Fill the screen
        horizontalAlignment = Alignment.CenterHorizontally
        // Remove verticalArrangement = Arrangement.Center to allow content to flow from top
    ) {

        // --- Main Photo Section ---
        Text(
            "Main Photo",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Card for displaying/picking the main photo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Adjust height as needed
                .clickable(enabled = selectedMainPhotoUri == null) { // Only clickable if no photo selected
                    singlePhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = if (selectedMainPhotoUri == null) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedMainPhotoUri != null) {
                    // Display the selected main photo
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(selectedMainPhotoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected Main Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // Crop to fill the card bounds
                    )
                    // Close button for the main photo
                    IconButton(
                        onClick = { selectedMainPhotoUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f), // Semi-transparent background
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove Main Photo",
                            tint = Color.White
                        )
                    }
                } else {
                    // Placeholder to prompt selection
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.AccountBox, // Placeholder Icon
                            contentDescription = "Select Main Photo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap to select Main Photo", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // Space between sections

        // --- Widget Photos Section ---
        Text(
            "Widget Photos",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp), // Adjust size for desired grid look
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Allow grid to take available space
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Display selected widget photos
            items(
                items = selectedWidgetPhotosUris.toList(),
                key = { uri -> System.identityHashCode(uri) } // Better unique key based on object identity
            ) { uri ->
                ImageItem(
                    uri = uri,
                    onRemoveClick = {
                        selectedWidgetPhotosUris.remove(uri)
                        CoroutineScope(Dispatchers.Unconfined).launch {
                            photoUriManager.updateWidgets(context)
                        }
                                    },
                    modifier = Modifier.size(100.dp)
                )
            }


            // Add "+" Card at the end
            item {
                AddPhotoCard(
                    onClick = {
                        multiplePhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.size(100.dp) // Consistent item size
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Space before bottom nav bar

        // --- Bottom Nav Bar ---
        BottomNavBar(navController = controller)

    }
}

// Helper Composable for displaying an image with a close button
@Composable
fun ImageItem(
    uri: Uri,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Selected Widget Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Close button
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp) // Smaller padding for grid items
                    .size(24.dp) // Smaller icon button
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove Widget Photo",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp) // Smaller icon
                )
            }
        }
    }
}

// Helper Composable for the "+" Card
@Composable
fun AddPhotoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), // Subtle border
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Slightly different background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add Widget Photo",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

suspend fun updateWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(MyAppWidget::class.java)

    // Update all instances of the widget
    for (glanceId in glanceIds) {
        MyAppWidget().update(context, glanceId)
    }
}

