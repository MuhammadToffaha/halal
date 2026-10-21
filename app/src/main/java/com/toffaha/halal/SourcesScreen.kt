package com.toffaha.halal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun SourcesScreen(state: AppState, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val d = state.data
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp)) {
                Text(L("← ⚙️ Settings", "← ⚙️ Einstellungen"))
            }
            SectionTitle(L("📚 Halal sources", "📚 Halal-Quellen"))
            Text(L("European certifiers & references. Use 🔎 Search tab to search inside them.", "Europäische Zertifizierer & Referenzen. Im 🔎 Suche-Tab kannst du darin suchen."), fontSize = 13.sp)
        }
        items(d.resources) { r ->
            Card(
                Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Gold.copy(alpha = 0.45f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("${r.country} ${r.name}", fontWeight = FontWeight.SemiBold)
                    if (r.description.isNotBlank()) Text(r.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (r.url.isNotBlank()) TextButton(onClick = { ctx.openUrl(r.url) }) { Text(L("🌐 Open website", "🌐 Webseite öffnen")) }
                }
            }
        }
        item {
            Text(
                L(
                    "⚖️ How verdicts work: 1) your verified Halal database on GitHub, 2) halal label (auto list / Open Food Facts), " +
                        "3) ingredient check (pork, alcohol, gelatine, carmine, animal emulsifiers, meat…). " +
                        "“No haram ingredients found” is NOT a certificate.",
                    "⚖️ So entsteht das Ergebnis: 1) deine geprüfte Halal-Datenbank auf GitHub, 2) Halal-Siegel (Auto-Liste / Open Food Facts), " +
                        "3) Zutatenprüfung (Schwein, Alkohol, Gelatine, Karmin, tierische Emulgatoren, Fleisch…). " +
                        "„Keine Haram-Zutaten gefunden“ ist KEIN Zertifikat."
                ),
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
                Text(L("🎉 New version: ", "🎉 Neue Version: ") + r.name, color = Color(0xFF5D4300), modifier = Modifier.weight(1f))
                TextButton(onClick = { state.installUpdate() }, enabled = state.updateProgress == null) {
                    Text(L("⬇️ Update", "⬇️ Aktualisieren"))
                }
            }
            state.updateProgress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
            state.updateMsg?.let { Text(it, color = Color(0xFF5D4300), fontSize = 13.sp) }
        }
    }
}
