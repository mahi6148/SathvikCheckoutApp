package com.example.sathvikwidget.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.BitmapImageProvider
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.sathvikwidget.MainActivity
import com.example.sathvikwidget.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext


class MyAppWidget : GlanceAppWidget() {
    companion object {
        val currentPhotoIndexKey = intPreferencesKey("current_photo_index")
        const val TAG = "MyAppWidget"

        // Define maximum bitmap dimensions for widget
        const val MAX_BITMAP_WIDTH = 800
        const val MAX_BITMAP_HEIGHT = 800

        // Define corner radius (in dp) - adjust to match your widget's corner radius
        const val CORNER_RADIUS_DP = 16f
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Switch to IO dispatcher for data loading operations
        withContext(Dispatchers.IO) {
            val photoUriManager = PhotoUriManager(context)
            val photoUris: List<Uri> = try {
                photoUriManager.widgetPhotoUrisFlow.first()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching initial photo URIs", e)
                emptyList()
            }

            Log.d(TAG, "Providing content with ${photoUris.size} URIs")

            // Pre-load bitmaps for smoother widget loading
            val bitmaps = photoUris.mapIndexedNotNull { index, uri ->
                loadBitmapFromUri(context, uri, index)
            }.toMap()

            provideContent {
                val currentIndex = currentState(currentPhotoIndexKey) ?: 0
                Log.d(TAG, "Current index from state: $currentIndex")

                GlanceTheme {
                    WidgetContent(
                        photoUris = photoUris,
                        currentIndex = currentIndex,
                        preloadedBitmaps = bitmaps,
                        context = context
                    )
                }
            }
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri, index: Int): Pair<Int, Bitmap>? {
        return try {
            if (uri.scheme == "content") {
                try {
                    // Try to take persistent permission
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not take persistent permission for URI: $uri", e)
                    // Continue anyway, as we might still be able to read it
                }

                // Get image dimensions first without loading the full bitmap
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                }

                // Calculate sample size to reduce memory usage
                val sampleSize = calculateInSampleSize(options, MAX_BITMAP_WIDTH, MAX_BITMAP_HEIGHT)

                // Load bitmap with sampling to reduce memory usage
                val loadOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }

                // Try to load the bitmap with sampling
                val input = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(input, null, loadOptions)
                input?.close()

                if (bitmap != null) {
                    // Further scale down if still too large
                    val scaledBitmap = if (bitmap.width > MAX_BITMAP_WIDTH || bitmap.height > MAX_BITMAP_HEIGHT) {
                        scaleBitmap(bitmap, MAX_BITMAP_WIDTH, MAX_BITMAP_HEIGHT)
                    } else {
                        bitmap
                    }

                    // Apply rounded corners
                    val roundedBitmap = applyRoundedCorners(context, scaledBitmap)

                    // If scaling was performed, we can recycle the intermediate bitmap
                    if (scaledBitmap != bitmap && scaledBitmap != roundedBitmap) {
                        scaledBitmap.recycle()
                    }

                    Log.d(TAG, "Loaded bitmap for index $index: ${roundedBitmap.width}x${roundedBitmap.height}, " +
                            "memory: ${roundedBitmap.byteCount / 1024}KB")

                    index to roundedBitmap
                } else {
                    Log.e(TAG, "Failed to decode bitmap for URI: $uri")
                    null
                }
            } else if (uri.scheme == "android.resource") {
                // Handle resource URIs
                Log.d(TAG, "Loading resource URI: $uri")

                // Parse resource ID from URI
                val resId = uri.toString().split("/").last().toIntOrNull() ?: R.drawable.ic_launcher_foreground

                // Get image dimensions first
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeResource(context.resources, resId, options)

                // Calculate sample size
                val sampleSize = calculateInSampleSize(options, MAX_BITMAP_WIDTH, MAX_BITMAP_HEIGHT)

                // Load with sampling
                val loadOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val bitmap = BitmapFactory.decodeResource(context.resources, resId, loadOptions)

                if (bitmap != null) {
                    // Further scale down if still too large
                    val scaledBitmap = if (bitmap.width > MAX_BITMAP_WIDTH || bitmap.height > MAX_BITMAP_HEIGHT) {
                        scaleBitmap(bitmap, MAX_BITMAP_WIDTH, MAX_BITMAP_HEIGHT)
                    } else {
                        bitmap
                    }

                    // Apply rounded corners
                    val roundedBitmap = applyRoundedCorners(context, scaledBitmap)

                    // If scaling was performed, we can recycle the intermediate bitmap
                    if (scaledBitmap != bitmap && scaledBitmap != roundedBitmap) {
                        scaledBitmap.recycle()
                    }

                    Log.d(TAG, "Loaded resource bitmap: ${roundedBitmap.width}x${roundedBitmap.height}, " +
                            "memory: ${roundedBitmap.byteCount / 1024}KB")

                    index to roundedBitmap
                } else {
                    null
                }
            } else {
                Log.d(TAG, "Unsupported URI scheme: ${uri.scheme}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap for URI: $uri", e)
            null
        }
    }

    // Apply rounded corners to a bitmap
    private fun applyRoundedCorners(context: Context, sourceBitmap: Bitmap): Bitmap {
        // Convert dp to pixels for the corner radius
        val density = context.resources.displayMetrics.density
        val cornerRadiusPx = CORNER_RADIUS_DP * density

        try {
            // Create output bitmap with same dimensions and ARGB_8888 config
            val outputBitmap = Bitmap.createBitmap(
                sourceBitmap.width,
                sourceBitmap.height,
                Bitmap.Config.ARGB_8888
            )

            // Create canvas with output bitmap
            val canvas = android.graphics.Canvas(outputBitmap)

            // Create paint with anti-aliasing
            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }

            // Create rectangular path with rounded corners
            val rect = RectF(0f, 0f, sourceBitmap.width.toFloat(), sourceBitmap.height.toFloat())
            val path = Path().apply {
                addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
            }

            // Clip canvas to the rounded rectangle
            canvas.clipPath(path)

            // Draw the bitmap
            canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)

            // If the source bitmap is not the same as our result, recycle it
            if (sourceBitmap != outputBitmap) {
                sourceBitmap.recycle()
            }

            return outputBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error applying rounded corners", e)
            // Return original bitmap if there was an error
            return sourceBitmap
        }
    }

    // Calculate appropriate sample size for loading bitmaps
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        Log.d(TAG, "Original size: ${width}x${height}, sample size: $inSampleSize")
        return inSampleSize
    }

    // Scale bitmap to fit within the maximum dimensions while preserving aspect ratio
    private fun scaleBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height

        val ratio = minOf(
            maxWidth.toFloat() / sourceWidth,
            maxHeight.toFloat() / sourceHeight
        )

        val targetWidth = (sourceWidth * ratio).toInt()
        val targetHeight = (sourceHeight * ratio).toInt()

        Log.d(TAG, "Scaling bitmap from ${sourceWidth}x${sourceHeight} to ${targetWidth}x${targetHeight}")

        val scaledBitmap = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)

        // Don't recycle here as we'll handle that after applying rounded corners
        return scaledBitmap
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(
    photoUris: List<Uri>,
    currentIndex: Int,
    preloadedBitmaps: Map<Int, Bitmap>,
    context: Context
) {
    val TAG = "WidgetContent"
    val imageCount = photoUris.size
    Log.d(TAG, "WidgetContent rendering. Count: $imageCount, Index: $currentIndex")

    val mainActivityIntent = createMainActivityIntent(context)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (photoUris.isEmpty()) {
            Log.d(TAG, "No photos to display.")
            Text(
                text = "No photos selected for widget.",
                style = TextStyle(color = GlanceTheme.colors.onBackground)
            )
        } else {
            // Ensure index is valid
            val validIndex = currentIndex.coerceIn(0, maxOf(0, imageCount - 1))
            val currentUri = photoUris[validIndex]
            Log.d(TAG, "Displaying image at index $validIndex: $currentUri")

            // Use preloaded bitmap if available, otherwise use fallback
            val imageProvider = getImageProvider(preloadedBitmaps, validIndex, context)
            val showErrorMessage = !preloadedBitmaps.containsKey(validIndex)

            // Display the image - note that we don't need to add corner radius here
            // as the bitmap already has rounded corners applied
            Image(
                provider = imageProvider,
                contentDescription = if (showErrorMessage)
                    "Fallback image" else "Widget photo $validIndex",
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(bottom = 8.dp)
                    .clickable(
                        actionRunCallback<LogAndLaunchAction>()
                    ) ,
                contentScale = ContentScale.Fit
            )

            // Show error message if needed
            if (showErrorMessage) {
                Text(
                    "Image preview unavailable",
                    style = TextStyle(color = GlanceTheme.colors.error)
                )
            }
        }
    }
}

class LogAndLaunchAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("widget log", "clicked")
        val intent = createMainActivityIntent(context)
        context.startActivity(intent)
    }
}

class ChangePageAction : ActionCallback {
    companion object {
        val directionParamKey = ActionParameters.Key<String>("direction")
        const val NEXT = "next"
        const val PREV = "prev"
        const val TAG = "ChangePageAction"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val direction = parameters[directionParamKey]
        Log.d(TAG, "onAction called for direction: $direction")

        if (direction == null) {
            Log.w(TAG, "Direction parameter is missing.")
            return
        }

        withContext(Dispatchers.IO) {
            val photoUriManager = PhotoUriManager(context)
            val photoUris = try {
                photoUriManager.widgetPhotoUrisFlow.first()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching photo URIs in action", e)
                emptyList<Uri>()
            }
            val imageCount = photoUris.size

            if (imageCount <= 1) {
                Log.d(TAG, "Not changing page, image count is $imageCount")
                return@withContext
            }

            updateAppWidgetState(context, glanceId) { prefs ->
                val currentIndex = prefs[MyAppWidget.currentPhotoIndexKey] ?: 0
                Log.d(TAG, "Current index in action: $currentIndex")

                val nextIndex = when (direction) {
                    NEXT -> (currentIndex + 1) % imageCount
                    PREV -> (currentIndex - 1 + imageCount) % imageCount
                    else -> currentIndex
                }
                Log.d(TAG, "Calculated next index: $nextIndex")
                prefs[MyAppWidget.currentPhotoIndexKey] = nextIndex
            }

            // Update the widget
            MyAppWidget().update(context, glanceId)
            Log.d(TAG, "Requested widget update for glanceId: $glanceId")
        }
    }
}

private fun createMainActivityIntent(context: Context): Intent {
    return Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}


// Helper function to safely get image provider outside composable
@SuppressLint("RestrictedApi")
private fun getImageProvider(
    preloadedBitmaps: Map<Int, Bitmap>,
    index: Int,
    context: Context
): ImageProvider {
    return if (preloadedBitmaps.containsKey(index)) {
        BitmapImageProvider(preloadedBitmaps[index]!!)
    } else {
        // Fallback to app icon
        androidx.glance.ImageProvider(R.drawable.ic_launcher_foreground)
    }
}