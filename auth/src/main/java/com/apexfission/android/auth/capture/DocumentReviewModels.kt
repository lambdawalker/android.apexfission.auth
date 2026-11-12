package com.apexfission.android.auth.capture

import com.apexfission.android.auth.library.R

/** Presentation presets; capture policy document_type is independent. */
internal enum class DocumentKind(val wireValue: String, val label: Int) {
    NATIONAL_ID("national_id", R.string.auth_type_national_id),
    PASSPORT("passport", R.string.auth_type_passport),
    DRIVERS_LICENSE("drivers_license", R.string.auth_type_drivers_license),
    RESIDENCE_PERMIT("residence_permit", R.string.auth_type_residence_permit),
    VOTER_ID("voter_id", R.string.auth_type_voter_id),
    OTHER("other", R.string.auth_type_other);

    companion object {
        fun forValue(value: String): DocumentKind = entries.firstOrNull { it.wireValue == value } ?: OTHER
    }
}

internal data class IssuingJurisdiction(val country: String = "", val region: String = "", val city: String = "") {
    fun withCountry(value: String) = if (value == country) this else IssuingJurisdiction(value)
    fun withRegion(value: String) = if (value == region) this else copy(region = value, city = "")
    fun display(): String = listOf(city, region, country).filter(String::isNotBlank).joinToString(", ")
}
