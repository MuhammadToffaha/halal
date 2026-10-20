package com.toffaha.halal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultScreen(r: ScanResult, state: AppState) {
    val ctx = LocalContext.current
    val color = Color(r.verdict.color)
    var showIngredients by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Verdict card
        Card(
            colors = CardDefaults.cardColors(containerColor = color),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(r.verdict.emoji, fontSize = 56.sp)
                Text(r.verdict.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(r.verdict.subtitle, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
            }
        }

        // Product
        val name = r.product?.name?.ifBlank { null } ?: r.listed?.name?.ifBlank { null } ?: "Unknown product"
        val brand = listOfNotNull(
            r.product?.brand?.ifBlank { null } ?: r.listed?.brand?.ifBlank { null },
            r.product?.quantity?.ifBlank { null }
        ).joinToString(" · ")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("🛒 $name", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (brand.isNotBlank()) Text("🏭 $brand")
                Text("🔢 ${r.barcode}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (r.reasons.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("💡 Why", fontWeight = FontWeight.Bold)
                    r.reasons.forEach { Text(it) }
                    r.listed?.url?.takeIf { it.isNotBlank() }?.let { u ->
                        TextButton(onClick = { ctx.openUrl(u) }) { Text("📄 Open certificate / source") }
                    }
                }
            }
        }

        if (r.findings.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🧪 Ingredients flagged", fontWeight = FontWeight.Bold)
                    r.findings.forEach { f ->
                        Row {
                            Text(if (f.level == Level.HARAM) "🔴 " else "🟠 ")
                            Column {
                                Text("${f.label}  ·  “${f.matched}”", fontWeight = FontWeight.SemiBold)
                                if (f.note.isNotBlank()) Text(f.note, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        r.product?.ingredients?.takeIf { it.isNotBlank() }?.let { ing ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    TextButton(onClick = { showIngredients = !showIngredients }) {
                        Text(if (showIngredients) "📋 Hide ingredients" else "📋 Show ingredients")
                    }
                    if (showIngredients) Text(ing, fontSize = 13.sp)
                }
            }
        }

        Button(onClick = { state.close() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("📷 Scan next product", fontSize = 16.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { ctx.openUrl(OpenFoodFacts.productUrl(r.barcode)) }, modifier = Modifier.weight(1f)) {
                Text("🌐 Open Food Facts")
            }
            OutlinedButton(onClick = { ctx.openUrl(reportUrl(r)) }, modifier = Modifier.weight(1f)) {
                Text("📝 Add / correct")
            }
        }

        Text(
            "⚖️ Guidance only. Certified status comes from the certifier (logo on pack or your Halal database). " +
                "Ingredient checks follow common European halal-certifier criteria; when in doubt, ask the certifier or a scholar.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** Opens a pre-filled GitHub issue so the product can be added to data/products.json */
fun reportUrl(r: ScanResult): String {
    val name = r.product?.name ?: r.listed?.name ?: ""
    val brand = r.product?.brand ?: r.listed?.brand ?: ""
    return "${Config.REPO_URL}/issues/new?template=product.yml" +
        "&title=${Net.enc("Product: $name ${r.barcode}".trim())}" +
        "&barcode=${Net.enc(r.barcode)}&name=${Net.enc(name)}&brand=${Net.enc(brand)}" +
        "&app_verdict=${Net.enc(r.verdict.title)}"
}
