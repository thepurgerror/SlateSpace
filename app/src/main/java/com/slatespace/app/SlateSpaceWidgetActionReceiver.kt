package com.slatespace.app

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray

/** Handles the widget's explicit, one-tap completion action without opening the app. */
class SlateSpaceWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_MARK_DONE) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)?.takeIf { it.isNotBlank() } ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TODOS, "[]") ?: "[]"
        val updated = try {
            val todos = JSONArray(raw)
            var changed = false
            for (index in 0 until todos.length()) {
                val task = todos.optJSONObject(index) ?: continue
                if (task.optString("id") == taskId && !task.optBoolean("done", false)) {
                    task.put("done", true)
                    changed = true
                    break
                }
            }
            if (changed) todos.toString() else null
        } catch (_: Exception) {
            null
        }
        if (updated != null) prefs.edit().putString(KEY_TODOS, updated).apply()
        SlateSpaceWidgetProvider.updateAll(context)
        // Refreshing via AppWidgetManager is safe even if no widget is currently placed.
        AppWidgetManager.getInstance(context)
    }

    companion object {
        const val ACTION_MARK_DONE = "com.slatespace.app.widget.MARK_DONE"
        const val EXTRA_TASK_ID = "com.slatespace.app.widget.TASK_ID"
        private const val PREFS_NAME = "slatespace"
        private const val KEY_TODOS = "todos"
    }
}
