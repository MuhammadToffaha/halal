package com.toffaha.halal

import org.json.JSONObject

/** Free open product database — many German supermarket products are in it. */
object OpenFoodFacts {
    private const val FIELDS =
        "code,product_name,product_name_de,product_name_en,brands,quantity," +
            "ingredients_text,ingredients_text_de,ingredients_text_en,ingredients_text_fr,ingredients_text_nl," +
            "labels_tags,additives_tags,ingredients_analysis_tags,categories_tags"

    /** @return null if the barcode is not in the database */
    fun product(code: String): Product? {
        val txt = try {
            Net.get("${Config.OFF}/api/v2/product/${Net.enc(code)}.json?fields=$FIELDS")
        } catch (e: Net.HttpError) {
            if (e.code == 404) return null else throw e
        }
        val o = JSONObject(txt)
        if (o.optInt("status", 0) != 1) return null
        return parse(o.optJSONObject("product") ?: return null, code)
    }

    fun search(q: String): List<Product> {
        val url = "${Config.OFF}/cgi/search.pl?search_terms=${Net.enc(q)}&search_simple=1&action=process&json=1" +
            "&page_size=30&tagtype_0=countries&tag_contains_0=contains&tag_0=germany&fields=$FIELDS"
        val o = JSONObject(Net.get(url))
        return o.optJSONArray("products").objects().map { parse(it, "") }.filter { it.code.isNotBlank() }
    }

    fun productUrl(code: String) = "${Config.OFF}/product/$code"

    private fun parse(p: JSONObject, fallbackCode: String): Product {
        val name = listOf("product_name_de", "product_name", "product_name_en")
            .map { p.str(it) }.firstOrNull { it.isNotBlank() } ?: ""
        val ingredients = listOf(
            "ingredients_text_de", "ingredients_text", "ingredients_text_en",
            "ingredients_text_fr", "ingredients_text_nl",
        ).map { p.str(it) }.filter { it.isNotBlank() }.distinct().joinToString("\n\n")
        return Product(
            code = p.str("code").ifBlank { fallbackCode },
            name = name,
            brand = p.str("brands"),
            quantity = p.str("quantity"),
            ingredients = ingredients,
            labels = p.optJSONArray("labels_tags").strings(),
            additives = p.optJSONArray("additives_tags").strings(),
            analysis = p.optJSONArray("ingredients_analysis_tags").strings(),
            categories = p.optJSONArray("categories_tags").strings(),
        )
    }
}
