package com.toffaha.halal

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Loads the data JSON files: first from the on-phone cache (or the copy bundled in the APK),
 * then refreshes from GitHub so edits in the repo reach the app without a new release.
 */
class DataRepo(private val ctx: Context) {
    var rules by mutableStateOf<List<Rule>>(emptyList())
        private set
    var products by mutableStateOf<List<Listed>>(emptyList())
        private set
    var resources by mutableStateOf<List<Resource>>(emptyList())
        private set
    var rulesVersion by mutableStateOf("")
        private set
    var productsUpdated by mutableStateOf("")
        private set
    var lastSync by mutableStateOf<String?>(null)
        private set

    private val files = listOf("rules.json", "products.json", "resources.json")

    suspend fun loadLocal() {
        for (name in files) {
            val txt = withContext(Dispatchers.IO) {
                val cached = File(ctx.filesDir, name)
                val fromCache = runCatching { cached.readText().also { JSONObject(it) } }.getOrNull()
                fromCache ?: ctx.assets.open(name).bufferedReader().use { it.readText() }
            }
            runCatching { apply(name, txt) }
        }
    }

    /** @return true if every file was refreshed from GitHub */
    suspend fun refresh(): Boolean {
        var ok = true
        for (name in files) {
            try {
                val txt = withContext(Dispatchers.IO) { Net.get(Config.RAW_DATA + name) }
                apply(name, txt) // throws on bad JSON -> not cached
                withContext(Dispatchers.IO) { File(ctx.filesDir, name).writeText(txt) }
            } catch (e: Exception) {
                ok = false
            }
        }
        if (ok) lastSync = java.text.DateFormat.getDateTimeInstance().format(java.util.Date())
        return ok
    }

    fun find(code: String): Listed? {
        val c = code.trimStart('0')
        return products.firstOrNull { it.barcode.trimStart('0') == c }
    }

    fun searchListed(q: String): List<Listed> {
        val s = q.trim().lowercase()
        if (s.isEmpty()) return emptyList()
        return products.filter {
            it.name.lowercase().contains(s) || it.brand.lowercase().contains(s) || it.barcode.contains(s)
        }
    }

    private fun apply(name: String, txt: String) {
        val o = JSONObject(txt)
        when (name) {
            "rules.json" -> {
                rules = o.optJSONArray("rules").objects().map { r ->
                    Rule(
                        id = r.str("id"),
                        level = if (r.str("level").equals("haram", true)) Level.HARAM else Level.DOUBTFUL,
                        label = r.str("label"),
                        note = r.str("note"),
                        match = r.optJSONArray("match").strings().map { it.lowercase() },
                        except = r.optJSONArray("except").strings().map { it.lowercase() },
                        veganOk = r.optBoolean("vegan_ok", false),
                    )
                }
                rulesVersion = o.str("version")
            }
            "products.json" -> {
                products = o.optJSONArray("products").objects().map { p ->
                    Listed(
                        barcode = p.str("barcode"),
                        name = p.str("name"),
                        brand = p.str("brand"),
                        status = p.str("status").lowercase(),
                        source = p.str("source"),
                        note = p.str("note"),
                        url = p.str("url"),
                    )
                }.filter { it.barcode.isNotBlank() }
                productsUpdated = o.str("updated")
            }
            "resources.json" -> {
                resources = o.optJSONArray("resources").objects().map { r ->
                    Resource(r.str("name"), r.str("country"), r.str("description"), r.str("url"), r.str("search"))
                }
            }
        }
    }
}
