package com.toffaha.halal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.app.PendingIntent
import android.content.pm.PackageInstaller
import org.json.JSONObject
import java.io.File

/** Self-update from GitHub Releases (tag vN, asset halal.apk). */
object Updater {
    data class Release(val code: Int, val name: String, val notes: String, val apkUrl: String)

    fun latest(): Release? {
        val o = JSONObject(Net.get(Config.LATEST_RELEASE_API))
        val code = o.str("tag_name").filter { it.isDigit() }.toIntOrNull() ?: return null
        val apk = o.optJSONArray("assets").objects()
            .firstOrNull { it.str("name").endsWith(".apk") }?.str("browser_download_url") ?: return null
        return Release(code, o.str("name").ifBlank { "v$code" }, o.str("body"), apk)
    }

    fun download(ctx: Context, url: String, progress: (Float) -> Unit): File {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "halal-update.apk")
        val c = Net.open(url)
        try {
            if (c.responseCode !in 200..299) throw Net.HttpError(c.responseCode)
            val total = c.contentLengthLong
            var done = 0L
            c.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) progress(done.toFloat() / total)
                    }
                }
            }
        } finally {
            c.disconnect()
        }
        return file
    }

    fun canInstall(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    /** Opens Android's "install unknown apps" setting for this app */
    fun askInstallPermission(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Installs the APK with PackageInstaller.
     * Android 12+: no confirmation once this app is the "installer of record"
     * (i.e. after the first self-update). Otherwise Android shows one confirmation.
     * @return false if the user first has to allow "install unknown apps"
     */
    fun install(ctx: Context, file: File): Boolean {
        if (!canInstall(ctx)) {
            askInstallPermission(ctx)
            return false
        }
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(ctx.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setRequestUpdateOwnership(true)
            }
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("halal.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pi = PendingIntent.getBroadcast(
                ctx, id, Intent(ctx, InstallReceiver::class.java).setPackage(ctx.packageName), flags
            )
            session.commit(pi.intentSender)
        }
        return true
    }
}
