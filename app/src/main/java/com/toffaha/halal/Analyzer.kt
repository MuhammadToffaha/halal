package com.toffaha.halal

import java.util.Locale

/**
 * Decides the verdict. Order of trust:
 *  1. data/products.json (verified by you, with the certifier as source)
 *  2. Halal certification label recorded on Open Food Facts
 *  3. Ingredient rules from data/rules.json (haram / doubtful)
 */
object Analyzer {
    private val eNumber = Regex("(?<![\\p{L}\\d])e[\\s\\-]?(\\d{3,4}[a-f]?)(?![\\p{L}\\d])")
    private val regexCache = HashMap<String, Regex>()

    private val meatCategories = setOf(
        "en:meats", "en:poultry", "en:sausages", "en:meat-based-products",
        "en:prepared-meats", "en:hams", "en:meat-preparations", "en:chicken",
    )

    fun normalize(text: String): String =
        text.lowercase(Locale.ROOT)
            .replace('_', ' ')
            .replace(eNumber) { "e" + it.groupValues[1] }

    private fun matches(text: String, pattern: String): Boolean {
        if (!pattern.startsWith("=")) return text.contains(pattern)
        val word = pattern.substring(1)
        val re = regexCache.getOrPut(word) {
            Regex("(?<![\\p{L}\\d])" + Regex.escape(word) + "(?![\\p{L}\\d])")
        }
        return re.containsMatchIn(text)
    }

    fun findings(product: Product, rules: List<Rule>): List<Finding> {
        val additives = product.additives.joinToString(" ") { it.substringAfter(':') }
        val text = normalize(product.ingredients + " " + additives)
        val vegetarian = product.analysis.any { it == "en:vegan" || it == "en:vegetarian" }
        val out = mutableListOf<Finding>()
        for (rule in rules) {
            if (rule.veganOk && vegetarian) continue
            var t = text
            for (ex in rule.except) t = t.replace(ex, " ")
            val hit = rule.match.firstOrNull { matches(t, it) } ?: continue
            val level = if (product.cosmetic) rule.cosmeticLevel ?: rule.level else rule.level
            out += Finding(
                level,
                L(rule.label, rule.labelDe.ifBlank { rule.label }),
                L(rule.note, rule.noteDe.ifBlank { rule.note }),
                hit.removePrefix("="),
            )
        }
        val meatHit = out.any { it.label.contains("meat", true) || it.label.contains("fleisch", true) }
        if (!meatHit && product.categories.any { it in meatCategories }) {
            out += Finding(
                Level.DOUBTFUL, L("Meat product", "Fleischprodukt"),
                L("Meat & poultry must come from halal slaughter (Zabiha).", "Fleisch & Geflügel müssen aus Halal-Schlachtung (Zabiha) stammen."),
                "category"
            )
        }
        return out.sortedBy { it.level.ordinal }
    }

    fun analyze(barcode: String, product: Product?, listed: Listed?, rules: List<Rule>): ScanResult {
        val findings = product?.let { findings(it, rules) } ?: emptyList()
        val reasons = mutableListOf<String>()

        if (listed != null) {
            val auto = listed.source.contains("Facts", true)
            val v = when (listed.status) {
                "halal" -> if (auto) Verdict.HALAL_CERTIFIED else Verdict.HALAL_LISTED
                "haram" -> Verdict.HARAM
                else -> Verdict.DOUBTFUL
            }
            reasons += L("📗 Halal database: ", "📗 Halal-Datenbank: ") + listed.status.uppercase() +
                (if (listed.source.isNotBlank()) L(" · source: ", " · Quelle: ") + listed.source else "")
            if (listed.note.isNotBlank()) reasons += "📝 ${listed.note}"
            if (listed.stores.isNotBlank()) reasons += L("🏪 Sold at: ", "🏪 Erhältlich bei: ") + listed.stores
            if (auto) reasons += L("👀 Confirm the certifier logo on the package.", "👀 Zertifizierer-Logo auf der Packung prüfen.")
            return ScanResult(barcode, v, product, listed, findings, reasons)
        }

        if (product == null) return ScanResult(barcode, Verdict.NOT_FOUND, null, null, emptyList(), reasons)

        val halalLabels = product.labels.filter { it.contains("halal", true) }
        if (halalLabels.isNotEmpty()) {
            reasons += L("🏷️ Label on pack: ", "🏷️ Siegel auf Packung: ") + halalLabels.joinToString { it.substringAfter(':').replace('-', ' ') }
            reasons += L("👀 Confirm the certifier logo on the package.", "👀 Zertifizierer-Logo auf der Packung prüfen.")
            if (findings.isNotEmpty()) reasons += L("⚠️ Ingredient check still flagged items below — verify.", "⚠️ Zutatenprüfung hat unten Punkte markiert — bitte prüfen.")
            return ScanResult(barcode, Verdict.HALAL_CERTIFIED, product, null, findings, reasons)
        }

        val verdict = when {
            findings.any { it.level == Level.HARAM } -> Verdict.HARAM
            findings.isNotEmpty() -> Verdict.DOUBTFUL
            product.ingredients.isBlank() && product.additives.isEmpty() -> Verdict.UNKNOWN
            else -> Verdict.NO_HARAM_FOUND
        }
        if (verdict == Verdict.NO_HARAM_FOUND) {
            if (product.analysis.contains("en:vegan")) reasons += L("🌱 Marked vegan", "🌱 Als vegan markiert")
            if (product.cosmetic) reasons += L("💄 Cosmetic product (Open Beauty Facts)", "💄 Kosmetikprodukt (Open Beauty Facts)")
            reasons += L("ℹ️ No halal certificate known for this product.", "ℹ️ Kein Halal-Zertifikat für dieses Produkt bekannt.")
        }
        return ScanResult(barcode, verdict, product, null, findings, reasons)
    }
}
