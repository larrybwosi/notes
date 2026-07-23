package com.scryme.notes.receiver

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
