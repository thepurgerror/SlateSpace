package com.slatespace.app

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.util.Calendar

/** Quiet-hours-aware reminders that name a real unfinished task instead of showing a generic count. */
class TaskReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_REMINDERS_ENABLED, true)) return Result.success()

        // Don't interrupt someone who is already actively using SlateSpace.
        val processInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(processInfo)
        if (processInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            return Result.success()
        }

        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val quietStart = prefs.getInt(KEY_QUIET_START, 22).coerceIn(0, 23)
        val quietEnd = prefs.getInt(KEY_QUIET_END, 7).coerceIn(0, 23)
        val inQuietHours = when {
            quietStart == quietEnd -> false
            quietStart > quietEnd -> hour >= quietStart || hour < quietEnd
            else -> hour >= quietStart && hour < quietEnd
        }
        if (inQuietHours) return Result.success()

        val raw = prefs.getString(KEY_TODOS, null) ?: return Result.success()
        val unfinishedTasks = try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                if (obj.optBoolean("done", false)) return@mapNotNull null
                val title = obj.optString("text", "").trim()
                if (title.isBlank()) return@mapNotNull null
                ReminderTask(
                    id = obj.optString("id", ""),
                    title = title,
                    priority = obj.optString("priority", "Normal")
                )
            }.sortedByDescending { task -> priorityWeight(task.priority) }
        } catch (_: Exception) {
            emptyList()
        }
        if (unfinishedTasks.isEmpty()) return Result.success()

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Unfinished task reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "A gentle reminder about a specific unfinished SlateSpace task"
                }
            )
        }

        val featured = unfinishedTasks.first()
        val count = unfinishedTasks.size
        val taskLines = unfinishedTasks.take(3).joinToString("\n") { task -> "• ${task.title}" }
        val moreLine = if (count > 3) "\n+ ${count - 3} more unfinished ${if (count - 3 == 1) "task" else "tasks"}" else ""
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_TAB, TAB_TASKS_FOR_NOTIFICATION)
        }
        val openPendingIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_OPEN_TASKS,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(appContext, TaskReminderActionReceiver::class.java).apply {
            action = TaskReminderActionReceiver.ACTION_MARK_DONE
            putExtra(TaskReminderActionReceiver.EXTRA_TASK_ID, featured.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            appContext,
            featured.id.hashCode(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (count == 1) "One task is still open" else "$count tasks are still open")
            .setContentText(featured.title)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Pick up where you left off:\n$taskLines$moreLine\n\nTap to open your tasks, or mark the highlighted task done."
                )
            )
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_save, "Mark done", donePendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        return try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
            Result.success()
        } catch (_: SecurityException) {
            Result.success()
        }
    }

    private data class ReminderTask(val id: String, val title: String, val priority: String)

    private fun priorityWeight(priority: String): Int = when (priority.lowercase()) {
        "urgent" -> 4
        "high" -> 3
        "normal" -> 2
        "low" -> 1
        else -> 0
    }

    companion object {
        const val UNIQUE_WORK_NAME = "slatespace-task-reminders"
        private const val CHANNEL_ID = "slatespace_focus_reminders"
        private const val NOTIFICATION_ID = 2401
        private const val REQUEST_OPEN_TASKS = 2403
        private const val KEY_TODOS = "todos"
        private const val KEY_REMINDERS_ENABLED = "remindersEnabled"
        private const val KEY_QUIET_START = "quietStartHour"
        private const val KEY_QUIET_END = "quietEndHour"
        private const val PREFS_NAME = "slatespace"
        private const val TAB_TASKS_FOR_NOTIFICATION = "Tasks"
    }
}
