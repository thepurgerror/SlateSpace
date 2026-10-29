package com.slatespace.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import org.json.JSONArray

/** Battery-friendly home-screen summary of open tasks, backed by the existing local task store. */
class SlateSpaceWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateOne(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        updateOne(context, manager, appWidgetId)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SlateSpaceWidgetProvider::class.java))
            ids.forEach { updateOne(context, manager, it) }
        }

        private data class WidgetTask(val id: String, val title: String, val priority: String)

        private fun updateOne(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences("slatespace", Context.MODE_PRIVATE)
            val raw = prefs.getString("todos", "[]") ?: "[]"
            var total = 0
            var done = 0
            val openTasks = mutableListOf<WidgetTask>()
            try {
                val todos = JSONArray(raw)
                for (i in 0 until todos.length()) {
                    val item = todos.optJSONObject(i) ?: continue
                    total++
                    if (item.optBoolean("done", false)) {
                        done++
                    } else {
                        val title = item.optString("text", "").trim().ifBlank { "Untitled task" }
                        openTasks += WidgetTask(
                            id = item.optString("id", ""),
                            title = title,
                            priority = item.optString("priority", "Normal")
                        )
                    }
                }
            } catch (_: Exception) {
                // A malformed local value should show a safe empty state, never crash the launcher.
                total = 0
                done = 0
                openTasks.clear()
            }
            val priorityRank: (String) -> Int = { priority ->
                when (priority.lowercase()) {
                    "urgent" -> 4
                    "high" -> 3
                    "normal" -> 2
                    "low" -> 1
                    else -> 0
                }
            }
            val ordered = openTasks.sortedByDescending { priorityRank(it.priority) }
            val open = ordered.size
            val views = RemoteViews(context.packageName, R.layout.widget_slatespace)
            views.setTextViewText(R.id.widget_progress, "$done/$total")
            val status = when {
                total == 0 -> "No tasks yet"
                open == 0 -> "You're all caught up"
                open == 1 -> "1 task left"
                else -> "$open tasks left"
            }
            views.setTextViewText(R.id.widget_status, status)
            views.setTextViewText(
                R.id.widget_first_task,
                ordered.getOrNull(0)?.title ?: if (total == 0) "Capture your first task" else "Take a breather."
            )
            views.setTextViewText(
                R.id.widget_second_task,
                ordered.getOrNull(1)?.title ?: if (open == 0) "Nothing waiting on you." else "Tap below to plan your next step."
            )
            views.setViewVisibility(R.id.widget_second_task, if (ordered.size > 1 || open == 0) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_done, if (open > 0 && ordered.firstOrNull()?.id?.isNotBlank() == true) View.VISIBLE else View.GONE)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_START_TAB, "Tasks")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val openPending = PendingIntent.getActivity(
                context, 1200 + widgetId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_open, openPending)
            views.setOnClickPendingIntent(R.id.widget_card, openPending)

            ordered.firstOrNull()?.takeIf { it.id.isNotBlank() }?.let { task ->
                val doneIntent = Intent(context, SlateSpaceWidgetActionReceiver::class.java).apply {
                    action = SlateSpaceWidgetActionReceiver.ACTION_MARK_DONE
                    data = Uri.parse("slatespace://widget/mark-done/${Uri.encode(task.id)}/$widgetId")
                    putExtra(SlateSpaceWidgetActionReceiver.EXTRA_TASK_ID, task.id)
                }
                val donePending = PendingIntent.getBroadcast(
                    context, (task.id.hashCode() * 31 + widgetId), doneIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_done, donePending)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
