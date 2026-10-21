package com.toffaha.halal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(state: AppState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var listed by remember { mutableStateOf<List<Listed>>(emptyList()) }
    var found by remember { mutableStateOf<List<Product>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    fun run() {
        val q = query.trim()
        if (q.isEmpty()) return
        keyboard?.hide()
        if (q.length >= 8 && q.all(Char::isDigit)) { state.lookup(q); return }
        listed = state.data.searchListed(q)
        busy = true; msg = null
        scope.launch {
            try {
                found = withContext(Dispatchers.IO) { OpenFoodFacts.search(q) }
                if (found.isEmpty() && listed.isEmpty()) msg = L("🤷 Nothing found — try German name or brand", "🤷 Nichts gefunden — deutschen Namen oder Marke versuchen")
            } catch (e: Exception) {
                found = emptyList(); msg = L("📶 Open Food Facts not reachable", "📶 Open Food Facts nicht erreichbar")
            } finally { busy = false }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(L("🔎 Product, brand, shop or barcode", "🔎 Produkt, Marke, Laden oder Barcode")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { run() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(8.dp))
            Button(onClick = { run() }) { Text(L("Go", "Los")) }
        }

        // search the same text inside every halal source website
        if (query.isNotBlank() && state.data.resources.isNotEmpty()) {
            Text(L("📚 Search in sources:", "📚 In Quellen suchen:"), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.data.resources.filter { it.search.isNotBlank() }) { res ->
                    AssistChip(
                        onClick = { ctx.openUrl(res.search.replace("{q}", Net.enc(query.trim()))) },
                        label = { Text("${res.country} ${res.name}") }
                    )
                }
            }
        }

        if (busy) CircularProgressIndicator(Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
        msg?.let { Text(it, modifier = Modifier.padding(12.dp)) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            if (listed.isNotEmpty()) item { Text(L("📗 Halal database", "📗 Halal-Datenbank"), fontWeight = FontWeight.Bold) }
            items(listed) { l ->
                Card(Modifier.fillMaxWidth().clickable { state.lookup(l.barcode) }) {
                    Column(Modifier.padding(12.dp)) {
                        val icon = when (l.status) { "halal" -> "✅"; "haram" -> "⛔"; else -> "🟠" }
                        Text("$icon ${l.name}", fontWeight = FontWeight.SemiBold)
                        Text(
                            listOf(l.brand, l.stores.ifBlank { l.source }).filter { it.isNotBlank() }.joinToString(" · "),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (found.isNotEmpty()) item { Text(L("🌐 Open Food Facts (Germany)", "🌐 Open Food Facts (Deutschland)"), fontWeight = FontWeight.Bold) }
            items(found) { p ->
                val v = remember(p, state.data.rules) {
                    Analyzer.analyze(p.code, p, state.data.find(p.code), state.data.rules).verdict
                }
                Card(Modifier.fillMaxWidth().clickable { state.show(p) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(v.emoji, fontSize = 24.sp)
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(p.name.ifBlank { p.code }, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOf(p.brand, p.quantity).filter { it.isNotBlank() }.joinToString(" · ") + "  ·  ${v.titleL}",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
