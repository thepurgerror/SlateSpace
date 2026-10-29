package com.slatespace.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray

/** Handles the explicit Mark done notification action without opening the app UI. */
class TaskReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_DONE) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)?.takeIf { it.isNotBlank() } ?: return
        val prefs = context.getSharedPreferences("slatespace", Context.MODE_PRIVATE)
        val raw = prefs.getString("todos", null) ?: return
        val tasks = try { JSONArray(raw) } catch (_: Exception) { return }
        var changed = false

        for (index in 0 until tasks.length()) {
            val task = tasks.optJSONObject(index) ?: continue
            if (task.optString("id") == taskId && !task.optBoolean("done", false)) {
                task.put("done", true)
                changed = true
                break
            }
        }

        if (changed) {
            prefs.edit().putString("todos", tasks.toString()).apply()
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            SlateSpaceWidgetProvider.updateAll(context)
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.slatespace.app.action.MARK_REMINDER_TASK_DONE"
        const val EXTRA_TASK_ID = "com.slatespace.app.extra.REMINDER_TASK_ID"
        private const val NOTIFICATION_ID = 2401
    }
}
