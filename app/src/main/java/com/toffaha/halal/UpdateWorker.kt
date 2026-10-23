package com.toffaha.halal

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Settings for silent updates */
object AppPrefs {
    /** true while a screen of the app is visible — never replace the app under the user's finger */
    @Volatile var inForeground = false

    private fun p(ctx: Context) = ctx.getSharedPreferences("settings", MODE_PRIVATE)
    fun autoUpdate(ctx: Context) = p(ctx).getBoolean("auto_update", true)
    fun wifiOnly(ctx: Context) = p(ctx).getBoolean("wifi_only", true)
    fun setAutoUpdate(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("auto_update", v).apply(); UpdateWorker.schedule(ctx) }
    fun setWifiOnly(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("wifi_only", v).apply(); UpdateWorker.schedule(ctx) }
    fun lastUpdateError(ctx: Context, msg: String) = p(ctx).edit().putString("update_error", msg).apply()
}

/** Background job: check GitHub Releases, download, install silently while the app is closed. */
class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!AppPrefs.autoUpdate(ctx)) return Result.success()
        Lang.init(ctx)
        // also refresh the halal data silently
        runCatching { withContext(Dispatchers.Main) { DataRepo(ctx) }.refresh() }

        val release = try {
            withContext(Dispatchers.IO) { Updater.latest() }
        } catch (e: Exception) {
            return Result.retry()
        } ?: return Result.success()
        if (release.code <= BuildConfig.VERSION_CODE) return Result.success()
        if (AppPrefs.inForeground) return Result.success() // try again next run
        if (!Updater.canInstall(ctx)) {
            Notify.show(
                ctx, 3,
                L("⬆️ Halal update available", "⬆️ Halal-Update verfügbar"),
                L("Open the app to allow automatic updates.", "App öffnen, um automatische Updates zu erlauben."),
                ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            )
            return Result.success()
        }
        return try {
            val file = withContext(Dispatchers.IO) { Updater.download(ctx, release.apkUrl) {} }
            if (AppPrefs.inForeground) return Result.success()
            withContext(Dispatchers.IO) { Updater.install(ctx, file) }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC = "silent-update"
        private const val NOW = "silent-update-now"

        private fun constraints(ctx: Context) = Constraints.Builder()
            .setRequiredNetworkType(if (AppPrefs.wifiOnly(ctx)) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        /** once a day */
        fun schedule(ctx: Context) {
            val wm = WorkManager.getInstance(ctx)
            if (!AppPrefs.autoUpdate(ctx)) {
                wm.cancelUniqueWork(PERIODIC); return
            }
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints(ctx))
                .build()
            wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        /** run soon (e.g. right after the user leaves the app) */
        fun runSoon(ctx: Context) {
            if (!AppPrefs.autoUpdate(ctx)) return
            val req = OneTimeWorkRequestBuilder<UpdateWorker>()
                .setConstraints(constraints(ctx))
                .setInitialDelay(20, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, req)
        }
    }
}
