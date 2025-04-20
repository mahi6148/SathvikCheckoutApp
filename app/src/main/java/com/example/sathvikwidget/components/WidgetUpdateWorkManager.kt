package com.example.sathvikwidget.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit


class WidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        const val TAG = "WidgetUpdateWorker"
        const val WORK_NAME = "widget_auto_update_work"

        // Schedule the periodic work
        fun enqueuePeriodicWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                1, TimeUnit.MINUTES, // Run every minute
                15, TimeUnit.SECONDS // Flex period - can run 15 seconds earlier
            )
                .addTag(TAG)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // Replace any existing work
                request
            )

            Log.d(TAG, "Periodic widget update work scheduled")
        }

        // Cancel the periodic work if needed
        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Periodic widget update work cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "WidgetUpdateWorker running")
        return try {
            val glanceId = GlanceAppWidgetManager(applicationContext)
                .getGlanceIds(MyAppWidget::class.java)

            for (id in glanceId) {
                advanceToNextImage(applicationContext, id)
            }

            Log.d(TAG, "Widget update completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget", e)
            Result.retry()
        }
    }

    private suspend fun advanceToNextImage(context: Context, glanceId: GlanceId) {
        withContext(Dispatchers.IO) {
            // Get the photo URIs
            val photoUriManager = PhotoUriManager(context)
            val photoUris = try {
                photoUriManager.widgetPhotoUrisFlow.first()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching photo URIs", e)
                emptyList<Uri>()
            }

            val imageCount = photoUris.size
            if (imageCount <= 1) {
                Log.d(TAG, "Not changing image, count is $imageCount")
                return@withContext
            }

            // Update the app widget state with the next index
            updateAppWidgetState(context, glanceId) { prefs ->
                val currentIndex = prefs[MyAppWidget.currentPhotoIndexKey] ?: 0
                val nextIndex = (currentIndex + 1) % imageCount
                Log.d(TAG, "Changing image from $currentIndex to $nextIndex")
                prefs[MyAppWidget.currentPhotoIndexKey] = nextIndex
            }

            // Update the widget UI
            MyAppWidget().update(context, glanceId)
        }
    }
}