package com.scryme.notes.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.scryme.notes.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        val prefs = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Abigail") ?: "Abigail"

        createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.BOOT_COMPLETED") {
            ReminderScheduler.rescheduleAllReminders(context)
            return
        }

        when (action) {
            "DAILY_REMINDER" -> {
                // Determine greeting based on current hour
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val greeting =
                    when {
                        hour < 12 -> "Good Morning"
                        hour < 17 -> "Good Afternoon"
                        else -> "Good Evening"
                    }

                val mainIntent =
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                val pendingIntent =
                    PendingIntent.getActivity(
                        context,
                        100,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                val notification =
                    NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("$greeting, $userName")
                        .setContentText("Let's capture something new today.")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build()

                notificationManager.notify(DAILY_NOTIFICATION_ID, notification)
            }
            "NOTE_REMINDER" -> {
                val noteId = intent.getStringExtra("NOTE_ID") ?: ""
                val noteTitle = intent.getStringExtra("NOTE_TITLE") ?: "Untitled Note"

                val mainIntent =
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("LAUNCH_NOTE_ID", noteId)
                    }
                // Use a unique request code for note reminders based on noteId hash
                val requestCode = noteId.hashCode()
                val pendingIntent =
                    PendingIntent.getActivity(
                        context,
                        requestCode,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                val notification =
                    NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Reminder: $noteTitle")
                        .setContentText("Check out your note blocks and update your thoughts.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build()

                notificationManager.notify(requestCode, notification)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminders"
            val descriptionText = "Notifications for note reminders and daily journal logs"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "note_reminders_channel"
        const val DAILY_NOTIFICATION_ID = 2024
    }
}

object ReminderScheduler {
    fun scheduleDailyReminder(
        context: Context,
        timeStr: String,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent =
            Intent(context, ReminderReceiver::class.java).apply {
                action = "DAILY_REMINDER"
            }
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val timeParts = timeStr.split(":")
        if (timeParts.size != 2) return
        val hour = timeParts[0].toIntOrNull() ?: 9
        val minute = timeParts[1].toIntOrNull() ?: 0

        val calendar =
            java.util.Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent,
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent,
                )
            }
        } catch (e: Exception) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent,
            )
        }
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent =
            Intent(context, ReminderReceiver::class.java).apply {
                action = "DAILY_REMINDER"
            }
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleNoteReminder(
        context: Context,
        noteId: String,
        noteTitle: String,
        timestamp: Long,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent =
            Intent(context, ReminderReceiver::class.java).apply {
                action = "NOTE_REMINDER"
                putExtra("NOTE_ID", noteId)
                putExtra("NOTE_TITLE", noteTitle)
            }
        val requestCode = noteId.hashCode()
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timestamp,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timestamp,
                        pendingIntent,
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timestamp,
                    pendingIntent,
                )
            }
        } catch (e: Exception) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timestamp,
                pendingIntent,
            )
        }
    }

    fun cancelNoteReminder(
        context: Context,
        noteId: String,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent =
            Intent(context, ReminderReceiver::class.java).apply {
                action = "NOTE_REMINDER"
            }
        val requestCode = noteId.hashCode()
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAllReminders(context: Context) {
        val prefs = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
        val dailyEnabled = prefs.getBoolean("daily_reminder_enabled", false)
        if (dailyEnabled) {
            val dailyTime = prefs.getString("daily_reminder_time", "09:00") ?: "09:00"
            scheduleDailyReminder(context, dailyTime)
        }

        // Reschedule note reminders
        val allPrefs = prefs.all
        for ((key, value) in allPrefs) {
            if (key.startsWith("reminder_note_") && value is Long) {
                val noteId = key.removePrefix("reminder_note_")
                val timestamp = value
                if (timestamp > System.currentTimeMillis()) {
                    val noteTitle = prefs.getString("reminder_title_note_$noteId", "Untitled Note") ?: "Untitled Note"
                    scheduleNoteReminder(context, noteId, noteTitle, timestamp)
                }
            }
        }
    }
}
