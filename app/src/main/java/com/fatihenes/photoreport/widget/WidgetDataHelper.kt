package com.fatihenes.photoreport.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

import androidx.core.content.edit

/**
 * Lightweight helper to bridge app data → widget via SharedPreferences.
 * No Hilt, no Room, no coroutines needed on the widget side.
 */
object WidgetDataHelper {

    private const val PREFS_NAME = "santiye_gunlugu_widget_prefs"
    private const val KEY_PROJECT_NAME = "latest_project_name"
    private const val KEY_PROJECT_ID = "latest_project_id"

    /** Called from the app (repository) whenever a project changes. */
    fun saveLatestProject(context: Context, projectId: Long, projectName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(KEY_PROJECT_ID, projectId)
            putString(KEY_PROJECT_NAME, projectName)
        }

        // Trigger widget refresh
        notifyWidgets(context)
    }

    fun getProjectName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROJECT_NAME, "Proje Yok") ?: "Proje Yok"
    }

    fun getProjectId(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_PROJECT_ID, -1L)
    }

    /** Force-refresh every Şantiye Günlüğü widget on the home screen. */
    fun notifyWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, PhotoReportWidgetProvider::class.java)
        )
        if (ids.isNotEmpty()) {
            val intent = Intent(context, PhotoReportWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
