package com.elektrik.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.elektrik.MainActivity
import com.elektrik.R

/**
 * Zero-dependency widget provider.
 * Reads project data from SharedPreferences via WidgetDataHelper.
 * No Hilt, no Room, no coroutines — guaranteed to never crash.
 */
class ElektrikWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val projectName = WidgetDataHelper.getProjectName(context)
        val projectId = WidgetDataHelper.getProjectId(context)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_elektrik)
            views.setTextViewText(R.id.widget_project_name, projectName)

            // SOL: Proje ismine tıklayınca → Ana Menü
            val homeIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.elektrik.ACTION_WIDGET_HOME"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            views.setOnClickPendingIntent(
                R.id.widget_left_area,
                PendingIntent.getActivity(
                    context, appWidgetId + 1000, homeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // ORTA: Görsellere tıklayınca → Proje Detay
            val projectIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.elektrik.ACTION_WIDGET_PROJECT"
                putExtra("PROJECT_ID", projectId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            views.setOnClickPendingIntent(
                R.id.widget_middle_area,
                PendingIntent.getActivity(
                    context, appWidgetId + 2000, projectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // SAĞ: Kameraya tıklayınca → Kamera (bugünün tarihine fotoğraf)
            val cameraIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.elektrik.ACTION_WIDGET_CAMERA"
                putExtra("PROJECT_ID", projectId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            views.setOnClickPendingIntent(
                R.id.widget_camera_btn,
                PendingIntent.getActivity(
                    context, appWidgetId, cameraIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        /** Convenience for triggering updates from anywhere. */
        fun sendUpdateBroadcast(context: Context) {
            WidgetDataHelper.notifyWidgets(context)
        }
    }
}
