package com.fatihenes.photoreport.ui.navigation

/**
 * Centralized navigation route constants.
 * Eliminates magic strings scattered across the codebase.
 */
object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val TRASH = "trash"
    const val DETAIL = "detail/{projectId}"
    const val CAMERA = "camera?logId={logId}&projectId={projectId}"

    fun detail(projectId: Long) = "detail/$projectId"
    fun camera(logId: Long? = null, projectId: Long? = null): String {
        val params = mutableListOf<String>()
        if (logId != null) params.add("logId=$logId")
        if (projectId != null) params.add("projectId=$projectId")
        return if (params.isEmpty()) "camera" else "camera?${params.joinToString("&")}"
    }
}
