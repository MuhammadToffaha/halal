package com.toffaha.halal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lang.init(this)
        setContent { HalalTheme { HalalApp() } }
    }
}

val Green = Color(0xFF1B7F4B)

@Composable
fun HalalTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme())
        darkColorScheme(primary = Color(0xFF6FD39B), secondary = Color(0xFF9CCFB2))
    else
        lightColorScheme(primary = Green, secondary = Color(0xFF3E6B53))
    MaterialTheme(colorScheme = scheme, content = content)
}

fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(this, L("No app to open link", "Keine App zum Öffnen des Links"), Toast.LENGTH_SHORT).show()
    }
}

/** App-wide state: lookups, data and updates. */
class AppState(private val ctx: Context, private val scope: CoroutineScope) {
    val data = DataRepo(ctx)
    var result by mutableStateOf<ScanResult?>(null)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    /** bumps whenever the camera may start scanning again */
    var scanEpoch by mutableIntStateOf(0)

    var release by mutableStateOf<Updater.Release?>(null)
    var updateProgress by mutableStateOf<Float?>(null)
    var updateMsg by mutableStateOf<String?>(null)

    fun lookup(raw: String) {
        val code = raw.filter { it.isDigit() }
        if (code.length < 6) { error = L("⚠️ Barcode looks invalid", "⚠️ Barcode scheint ungültig"); return }
        val listed = data.find(code)
        if (listed != null) {
            // ⚡ instant answer from the on-phone list, then enrich with ingredients in the background
            error = null
            result = Analyzer.analyze(code, null, listed, data.rules)
            scope.launch {
                val product = runCatching { withContext(Dispatchers.IO) { OpenFoodFacts.product(code) } }.getOrNull()
                    ?: return@launch
                if (result?.barcode == code) result = Analyzer.analyze(code, product, listed, data.rules)
            }
            return
        }
        scope.launch {
            loading = true; error = null
            try {
                val product = withContext(Dispatchers.IO) { OpenFoodFacts.product(code) }
                result = Analyzer.analyze(code, product, listed, data.rules)
            } catch (e: Exception) {
                if (listed != null) result = Analyzer.analyze(code, null, listed, data.rules)
                else error = L("📶 No connection to product database — try again", "📶 Keine Verbindung zur Produktdatenbank — nochmal versuchen")
            } finally {
                loading = false
                if (result == null) scanEpoch++
            }
        }
    }

    fun show(p: Product) {
        result = Analyzer.analyze(p.code, p, data.find(p.code), data.rules)
    }

    fun close() { result = null; error = null; scanEpoch++ }

    fun checkUpdate(manual: Boolean) {
        scope.launch {
            if (manual) updateMsg = L("🔄 Checking…", "🔄 Prüfe…")
            try {
                val r = withContext(Dispatchers.IO) { Updater.latest() }
                if (r != null && r.code > BuildConfig.VERSION_CODE) {
                    release = r; updateMsg = null
                } else if (manual) updateMsg = L("✅ You have the latest version", "✅ Du hast die neueste Version")
            } catch (e: Exception) {
                if (manual) updateMsg = L("📶 Could not reach GitHub", "📶 GitHub nicht erreichbar")
            }
        }
    }

    fun installUpdate() {
        val r = release ?: return
        if (updateProgress != null) return
        scope.launch {
            updateProgress = 0f; updateMsg = null
            try {
                val file = withContext(Dispatchers.IO) {
                    Updater.download(ctx, r.apkUrl) { p -> scope.launch { updateProgress = p } }
                }
                if (!Updater.install(ctx, file)) updateMsg = L("🔓 Allow installs from Halal, then tap Update again", "🔓 Installation für Halal erlauben, dann erneut auf Update tippen")
            } catch (e: Exception) {
                updateMsg = L("❌ Download failed", "❌ Download fehlgeschlagen")
            } finally {
                updateProgress = null
            }
        }
    }

    fun syncData() {
        scope.launch {
            val ok = data.refresh()
            Toast.makeText(ctx, if (ok) L("✅ Data updated from GitHub", "✅ Daten von GitHub aktualisiert") else L("📶 Using offline data", "📶 Offline-Daten werden genutzt"), Toast.LENGTH_SHORT).show()
        }
    }
}

enum class Tab(val icon: String, private val en: String, private val de: String) {
    SCAN("📷", "Scan", "Scannen"), SEARCH("🔎", "Search", "Suche"),
    SOURCES("📚", "Sources", "Quellen"), SETTINGS("⚙️", "Settings", "Einstellungen");

    val label get() = L(en, de)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalalApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { AppState(ctx.applicationContext, scope) }
    var tab by remember { mutableStateOf(Tab.SCAN) }

    LaunchedEffect(Unit) {
        state.data.loadLocal()
        state.checkUpdate(false)
        state.data.refresh()
    }
    BackHandler(enabled = state.result != null) { state.close() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("☪️ Halal Scanner", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Green, titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t; state.close() },
                        icon = { Text(t.icon, fontSize = 22.sp) },
                        label = { Text(t.label, maxLines = 1, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            UpdateBanner(state)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val r = state.result
                when {
                    r != null -> ResultScreen(r, state)
                    tab == Tab.SCAN -> ScanScreen(state)
                    tab == Tab.SEARCH -> SearchScreen(state)
                    tab == Tab.SOURCES -> SourcesScreen(state)
                    else -> SettingsScreen(state)
                }
            }
        }
    }
}
