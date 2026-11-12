package com.apexfission.android.auth.capture

import java.text.Normalizer
import java.util.Locale

/** Offline suggestions; never rejects manual locations or infers missing OCR values. */
internal class JurisdictionCatalogue private constructor(rows: List<List<String>>) {
    private val countries = rows.groupBy { it[0] }.mapValues { (_, places) ->
        places.groupBy { it[1] }.mapValues { (_, cities) -> cities.map { it[2] }.filter(String::isNotBlank).distinct().sorted() }
    }
    fun regions(country: String, locale: Locale): List<String> {
        val code = countryCode(country, locale) ?: return emptyList()
        return countries[code]?.keys?.sorted().orEmpty()
    }

    fun cities(country: String, region: String, locale: Locale): List<String> {
        val code = countryCode(country, locale) ?: return emptyList()
        val query = normalized(region)
        return countries[code]?.entries?.firstOrNull { normalized(it.key) == query }?.value.orEmpty()
    }

    companion object {
        fun read(lines: Sequence<String>): JurisdictionCatalogue = JurisdictionCatalogue(
            lines.filterNot { it.startsWith("#") }.map { it.split('\t') }.filter { it.size == 3 }.toList()
        )
        fun normalized(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "").lowercase(Locale.ROOT)

        private fun countryCode(value: String, locale: Locale): String? {
            val query = normalized(value)
            return Locale.getISOCountries().firstOrNull { code ->
                val country = Locale.Builder().setRegion(code).build()
                query in listOf(code, country.getDisplayCountry(locale), country.getDisplayCountry(Locale.ENGLISH), country.getDisplayCountry(Locale.forLanguageTag("es")))
                    .map(::normalized)
            }
        }
    }
}
