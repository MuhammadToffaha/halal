package com.toffaha.halal

import org.json.JSONArray
import org.json.JSONObject

enum class Level { HARAM, DOUBTFUL }

/** One ingredient rule from data/rules.json */
data class Rule(
    val id: String,
    val level: Level,
    val label: String,
    val note: String,
    val match: List<String>,
    val except: List<String>,
    val veganOk: Boolean,
)

/** One verified entry from data/products.json */
data class Listed(
    val barcode: String,
    val name: String,
    val brand: String,
    val status: String,
    val source: String,
    val note: String,
    val url: String,
)

/** One source from data/resources.json */
data class Resource(
    val name: String,
    val country: String,
    val description: String,
    val url: String,
    val search: String,
)

/** Product data from Open Food Facts */
data class Product(
    val code: String,
    val name: String,
    val brand: String,
    val quantity: String,
    val ingredients: String,
    val labels: List<String>,
    val additives: List<String>,
    val analysis: List<String>,
    val categories: List<String>,
)

data class Finding(val level: Level, val label: String, val note: String, val matched: String)

enum class Verdict(val emoji: String, val title: String, val subtitle: String, val color: Long) {
    HALAL_CERTIFIED("✅", "HALAL", "Halal certification label on product", 0xFF1E8E3E),
    HALAL_LISTED("✅", "HALAL", "Confirmed in Halal database", 0xFF1E8E3E),
    NO_HARAM_FOUND("🟢", "No haram ingredients found", "Not certified · based on ingredient check", 0xFF558B2F),
    DOUBTFUL("🟠", "DOUBTFUL · Mashbooh", "Contains questionable ingredients", 0xFFE67E00),
    HARAM("⛔", "NOT HALAL", "Contains haram ingredients", 0xFFC62828),
    UNKNOWN("❔", "Unknown", "No ingredient list available", 0xFF607D8B),
    NOT_FOUND("❔", "Product not found", "Not in Open Food Facts or Halal database", 0xFF607D8B),
}

data class ScanResult(
    val barcode: String,
    val verdict: Verdict,
    val product: Product?,
    val listed: Listed?,
    val findings: List<Finding>,
    val reasons: List<String>,
)

// ---------- JSON helpers ----------
fun JSONObject.str(k: String): String = if (!has(k) || isNull(k)) "" else optString(k, "").trim()

fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it, "") }.filter { it.isNotBlank() }

fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
