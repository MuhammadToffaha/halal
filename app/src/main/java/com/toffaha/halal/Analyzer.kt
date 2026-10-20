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
            out += Finding(rule.level, rule.label, rule.note, hit.removePrefix("="))
        }
        val meatHit = out.any { it.label.contains("meat", true) || it.label.contains("Fleisch", true) }
        if (!meatHit && product.categories.any { it in meatCategories }) {
            out += Finding(Level.DOUBTFUL, "Meat product", "Meat & poultry must come from halal slaughter (Zabiha).", "category")
        }
        return out.sortedBy { it.level.ordinal }
    }

    fun analyze(barcode: String, product: Product?, listed: Listed?, rules: List<Rule>): ScanResult {
        val findings = product?.let { findings(it, rules) } ?: emptyList()
        val reasons = mutableListOf<String>()

        if (listed != null) {
            val v = when (listed.status) {
                "halal" -> Verdict.HALAL_LISTED
                "haram" -> Verdict.HARAM
                else -> Verdict.DOUBTFUL
            }
            reasons += "📗 Halal database: ${listed.status.uppercase()}" +
                (if (listed.source.isNotBlank()) " · source: ${listed.source}" else "")
            if (listed.note.isNotBlank()) reasons += "📝 ${listed.note}"
            return ScanResult(barcode, v, product, listed, findings, reasons)
        }

        if (product == null) return ScanResult(barcode, Verdict.NOT_FOUND, null, null, emptyList(), reasons)

        val halalLabels = product.labels.filter { it.contains("halal", true) }
        if (halalLabels.isNotEmpty()) {
            reasons += "🏷️ Label on pack: " + halalLabels.joinToString { it.substringAfter(':').replace('-', ' ') }
            reasons += "👀 Confirm the certifier logo on the package."
            if (findings.isNotEmpty()) reasons += "⚠️ Ingredient check still flagged items below — verify."
            return ScanResult(barcode, Verdict.HALAL_CERTIFIED, product, null, findings, reasons)
        }

        val verdict = when {
            findings.any { it.level == Level.HARAM } -> Verdict.HARAM
            findings.isNotEmpty() -> Verdict.DOUBTFUL
            product.ingredients.isBlank() && product.additives.isEmpty() -> Verdict.UNKNOWN
            else -> Verdict.NO_HARAM_FOUND
        }
        if (verdict == Verdict.NO_HARAM_FOUND) {
            if (product.analysis.contains("en:vegan")) reasons += "🌱 Marked vegan"
            reasons += "ℹ️ No halal certificate known for this product."
        }
        return ScanResult(barcode, verdict, product, null, findings, reasons)
    }
}
