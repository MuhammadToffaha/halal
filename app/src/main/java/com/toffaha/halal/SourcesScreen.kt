package com.toffaha.halal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SourcesScreen(state: AppState) {
    val ctx = LocalContext.current
    val d = state.data
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("📚 Halal sources", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
            Text("European certifiers & references. Use 🔎 Search tab to search inside them.", fontSize = 13.sp)
        }
        items(d.resources) { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${r.country} ${r.name}", fontWeight = FontWeight.SemiBold)
                    if (r.description.isNotBlank()) Text(r.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (r.url.isNotBlank()) TextButton(onClick = { ctx.openUrl(r.url) }) { Text("🌐 Open website") }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⚙️ App & data", fontWeight = FontWeight.Bold)
                    Text("📱 Version ${BuildConfig.VERSION_NAME}")
                    Text("📗 Halal database: ${d.products.size} products" + (if (d.productsUpdated.isNotBlank()) " · ${d.productsUpdated}" else ""))
                    Text("🏷️ Halal-labelled in Germany (auto): ${d.imported.size}" + (if (d.importedUpdated.isNotBlank()) " · ${d.importedUpdated}" else ""))
                    Text("🧪 Ingredient rules: ${d.rules.size}" + (if (d.rulesVersion.isNotBlank()) " · ${d.rulesVersion}" else ""))
                    d.lastSync?.let { Text("🔄 Synced from GitHub: $it", fontSize = 13.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { state.syncData() }) { Text("🔄 Sync data") }
                        OutlinedButton(onClick = { state.checkUpdate(true) }) { Text("⬆️ Check update") }
                    }
                    state.updateMsg?.let { Text(it) }
                    TextButton(onClick = { ctx.openUrl(Config.REPO_URL) }) { Text("🐙 GitHub repo") }
                }
            }
        }
        item {
            Text(
                "⚖️ How verdicts work: 1) your verified Halal database on GitHub, 2) halal label recorded on Open Food Facts, " +
                    "3) ingredient check (pork, alcohol, gelatine, carmine, animal emulsifiers, meat…). " +
                    "“No haram ingredients found” is NOT a certificate.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun UpdateBanner(state: AppState) {
    val r = state.release ?: return
    Surface(color = Color(0xFFFFF3CD), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎉 New version: ${r.name}", color = Color(0xFF5D4300), modifier = Modifier.weight(1f))
                TextButton(onClick = { state.installUpdate() }, enabled = state.updateProgress == null) {
                    Text("⬇️ Update")
                }
            }
            state.updateProgress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
            state.updateMsg?.let { Text(it, color = Color(0xFF5D4300), fontSize = 13.sp) }
        }
    }
}
