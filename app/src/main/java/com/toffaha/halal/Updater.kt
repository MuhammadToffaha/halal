package com.toffaha.halal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
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

    /** @return false if the user first has to allow "install unknown apps" */
    fun install(ctx: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }
}
