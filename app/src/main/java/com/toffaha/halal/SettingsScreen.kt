package com.toffaha.halal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: AppState, onSources: () -> Unit) {
    val ctx = LocalContext.current
    val d = state.data
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 📚 Sources (moved here from its own tab)
        Card(
            onClick = onSources,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Gold),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionTitle(L("📚 Halal sources", "📚 Halal-Quellen"))
                    Text(
                        L("Certifiers & references (${d.resources.size})", "Zertifizierer & Referenzen (${d.resources.size})"),
                        fontSize = 13.sp
                    )
                }
                Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }

        // 🌐 Language
        Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Gold.copy(alpha = 0.45f)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(L("🌐 Language", "🌐 Sprache"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !Lang.german, onClick = { Lang.set(ctx, false) }, label = { Text("🇬🇧 English") })
                    FilterChip(selected = Lang.german, onClick = { Lang.set(ctx, true) }, label = { Text("🇩🇪 Deutsch") })
                }
            }
        }

        // ⬆️ App update
        Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Gold.copy(alpha = 0.45f)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(L("⬆️ App update", "⬆️ App-Update"))
                Text(L("📱 Installed: ", "📱 Installiert: ") + BuildConfig.VERSION_NAME)
                val r = state.release
                if (r != null) {
                    Text(L("🎉 New version available: ", "🎉 Neue Version verfügbar: ") + r.name)
                    if (r.notes.isNotBlank()) Text(r.notes.take(400), fontSize = 13.sp)
                    Button(onClick = { state.installUpdate() }, enabled = state.updateProgress == null, modifier = Modifier.fillMaxWidth()) {
                        Text(L("⬇️ Download & install", "⬇️ Herunterladen & installieren"))
                    }
                    state.updateProgress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
                } else {
                    Button(onClick = { state.checkUpdate(true) }, modifier = Modifier.fillMaxWidth()) {
                        Text(L("🔄 Check for updates", "🔄 Nach Updates suchen"))
                    }
                }
                state.updateMsg?.let { Text(it) }
            }
        }

        // 📗 Data
        Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Gold.copy(alpha = 0.45f)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle(L("📗 Halal database", "📗 Halal-Datenbank"))
                Text(L("🏷️ Halal-labelled products (Germany): ", "🏷️ Halal-gekennzeichnete Produkte (DE): ") +
                    d.imported.size + (if (d.importedUpdated.isNotBlank()) " · ${d.importedUpdated}" else ""))
                Text(L("✍️ Your verified products: ", "✍️ Deine geprüften Produkte: ") + d.products.size)
                Text(L("🧪 Ingredient rules: ", "🧪 Zutaten-Regeln: ") + d.rules.size +
                    (if (d.rulesVersion.isNotBlank()) " · ${d.rulesVersion}" else ""))
                d.lastSync?.let { Text(L("🔄 Last sync: ", "🔄 Zuletzt synchronisiert: ") + it, fontSize = 13.sp) }
                OutlinedButton(onClick = { state.syncData() }, modifier = Modifier.padding(top = 4.dp)) {
                    Text(L("🔄 Sync data now", "🔄 Daten jetzt synchronisieren"))
                }
            }
        }

        GoldDivider(Modifier.padding(vertical = 4.dp))
        TextButton(onClick = { ctx.openUrl(Config.REPO_URL) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("🐙 GitHub") }
    }
}
