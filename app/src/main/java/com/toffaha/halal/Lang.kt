package com.toffaha.halal

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** App language (English / Deutsch). Saved on the phone. Compose screens redraw when it changes. */
object Lang {
    var german by mutableStateOf(false)
        private set

    fun init(ctx: Context) {
        val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        german = p.getString("lang", null)?.let { it == "de" }
            ?: (java.util.Locale.getDefault().language == "de")
    }

    fun set(ctx: Context, de: Boolean) {
        german = de
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("lang", if (de) "de" else "en").apply()
    }
}

/** Pick text for current language */
fun L(en: String, de: String): String = if (Lang.german) de else en
