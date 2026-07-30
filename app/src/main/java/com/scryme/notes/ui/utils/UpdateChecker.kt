package com.scryme.notes.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/larrybwosi/notes/releases/latest"

    suspend fun checkForUpdates(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "ScrymeNotesApp")
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (connection.responseCode == 200) {
                    val reader = InputStreamReader(connection.inputStream)
                    val release = Gson().fromJson(reader, GitHubRelease::class.java)
                    reader.close()

                    val latestTag = release.tagName ?: return@withContext
                    val currentVersion = getCurrentVersion(context) ?: return@withContext

                    if (isNewerVersion(currentVersion, latestTag)) {
                        val apkUrl =
                            release.assets?.firstOrNull { it.name?.endsWith(".apk") == true }?.browserDownloadUrl
                                ?: release.htmlUrl
                                ?: return@withContext

                        showUpdateNotification(context, latestTag, apkUrl)
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCurrentVersion(context: Context): String? {
        return try {
            val packageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            packageInfo.versionName
        } catch (e: Exception) {
            null
        }
    }

    fun isNewerVersion(
        current: String,
        latest: String,
    ): Boolean {
        val currParts = current.replace("v", "").split(".").mapNotNull { it.toIntOrNull() }
        val lateParts = latest.replace("v", "").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currParts.size, lateParts.size)
        for (i in 0 until maxLen) {
            val currVal = currParts.getOrNull(i) ?: 0
            val lateVal = lateParts.getOrNull(i) ?: 0
            if (lateVal > currVal) return true
            if (currVal > lateVal) return false
        }
        return false
    }

    private fun showUpdateNotification(
        context: Context,
        latestTag: String,
        downloadUrl: String,
    ) {
        val channelId = "app_updates_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                android.app.NotificationChannel(
                    channelId,
                    "App Updates",
                    android.app.NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Notifications for new app updates"
                }
            notificationManager.createNotificationChannel(channel)
        }

        val updateIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        val pendingIntent =
            android.app.PendingIntent.getActivity(
                context,
                500,
                updateIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("New Update Available!")
                .setContentText("Version $latestTag is now available. Tap to download.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(500, notification)
    }

    private class GitHubRelease {
        @SerializedName("tag_name")
        val tagName: String? = null

        @SerializedName("html_url")
        val htmlUrl: String? = null

        val assets: List<GitHubAsset>? = null
    }

    private class GitHubAsset {
        val name: String? = null

        @SerializedName("browser_download_url")
        val browserDownloadUrl: String? = null
    }
}
