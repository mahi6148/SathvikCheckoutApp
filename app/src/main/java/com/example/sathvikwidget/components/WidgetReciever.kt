package com.example.sathvikwidget.components

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MyAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyAppWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Start periodic updates when the first widget instance is added
        WidgetUpdateWorker.enqueuePeriodicWork(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel periodic updates when the last widget instance is removed
        WidgetUpdateWorker.cancelPeriodicWork(context)
    }

}
