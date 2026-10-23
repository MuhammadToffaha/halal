package com.toffaha.halal

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Gets the result of a self-update session from Android's PackageInstaller. */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // First update (or older Android): Android needs one confirmation tap.
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (AppPrefs.inForeground) {
                    runCatching { ctx.startActivity(confirm) }
                } else {
                    Notify.show(
                        ctx, 2,
                        L("⬆️ Halal update ready", "⬆️ Halal-Update bereit"),
                        L("Tap to install. Next updates install silently.", "Tippen zum Installieren. Weitere Updates laufen automatisch."),
                        confirm
                    )
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit // app restarts on the new version
            else -> {
                AppPrefs.lastUpdateError(ctx, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "failed")
            }
        }
    }
}

/** After the app was replaced by a newer version: short "updated" notification. */
class UpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        Lang.init(ctx)
        Notify.show(
            ctx, 1,
            L("✅ Halal updated", "✅ Halal aktualisiert"),
            L("Now on version ", "Jetzt Version ") + BuildConfig.VERSION_NAME,
            ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        )
        UpdateWorker.schedule(ctx)
    }
}

object Notify {
    private const val CHANNEL = "updates"

    fun show(ctx: Context, id: Int, title: String, text: String, open: Intent?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (Build.VERSION.SDK_INT >= 26) {
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "App updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val pi = open?.let {
            PendingIntent.getActivity(ctx, id, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(id, n) }
    }
}
