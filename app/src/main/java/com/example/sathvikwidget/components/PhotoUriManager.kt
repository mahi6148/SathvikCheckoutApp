package com.example.sathvikwidget.components

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "photo_uris")

class PhotoUriManager(private val context: Context) {

    companion object {
        // Define preference keys
        private val MAIN_PHOTO_URI_KEY = stringPreferencesKey("main_photo_uri")
        private val WIDGET_PHOTO_URIS_KEY = stringPreferencesKey("widget_photo_uris")
        private val PHOTO_ROTATION_KEY = floatPreferencesKey("photo_rotation")
    }

    // Save the main photo URI
    suspend fun saveMainPhotoUri(uri: Uri?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[MAIN_PHOTO_URI_KEY] = uri.toString()
            } else {
                preferences.remove(MAIN_PHOTO_URI_KEY)
            }
        }
    }

    // Get the main photo URI flow
    val mainPhotoUriFlow: Flow<Uri?> = context.dataStore.data.map { preferences ->
        val uriString = preferences[MAIN_PHOTO_URI_KEY]
        if (uriString != null) Uri.parse(uriString) else null
    }

    // Save the list of widget photo URIs
    suspend fun saveWidgetPhotoUris(uris: List<Uri>) {
        val uriStrings = uris.map { it.toString() }
        context.dataStore.edit { preferences ->
            preferences[WIDGET_PHOTO_URIS_KEY] = Json.encodeToString(uriStrings)
        }
    }

    // Get the widget photo URIs flow
    val widgetPhotoUrisFlow: Flow<List<Uri>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[WIDGET_PHOTO_URIS_KEY] ?: "[]"
        try {
            Json.decodeFromString<List<String>>(jsonString).map { Uri.parse(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }


    // Save the photo rotation angle
    suspend fun savePhotoRotation(angle: Float) {
        context.dataStore.edit { preferences ->
            preferences[PHOTO_ROTATION_KEY] = angle
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

    // Get the photo rotation angle flow
    val photoRotationFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PHOTO_ROTATION_KEY] ?: 0f
    }

}