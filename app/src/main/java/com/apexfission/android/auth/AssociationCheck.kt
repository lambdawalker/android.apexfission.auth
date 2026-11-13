package com.apexfission.android.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

private const val APP_LINK_RELATION = "delegate_permission/common.handle_all_urls"
private const val PASSKEY_RELATION = "delegate_permission/common.get_login_creds"

private fun normalizeFingerprint(value: String): String = value.replace(":", "").uppercase()

private fun displayFingerprint(value: String): String {
    val normalized = normalizeFingerprint(value)
    return if (normalized.length == 64 && normalized.all { it in '0'..'9' || it in 'A'..'F' }) {
        normalized.chunked(2).joinToString(":")
    } else {
        value.uppercase()
    }
}

/** Public website configuration only; this does not establish Android's device verification state. */
internal data class AssociationApproval(val appLinks: Boolean, val passkeys: Boolean) {
    fun warning(): String? = when {
        !appLinks && !passkeys -> "This build isn't approved for this domain. Passkeys and verified App Links won't work."
        !appLinks -> "This build isn't approved for verified App Links on this domain."
        !passkeys -> "This build isn't approved for passkeys on this domain."
        else -> null
    }
}

internal data class AssociationComparison(
    val packageName: String,
    val installedFingerprints: List<String>,
    val publishedFingerprints: List<String>,
    val fingerprintMatch: Boolean,
    val appLinks: Boolean,
    val passkeys: Boolean,
) {
    val passesAppLinks: Boolean get() = fingerprintMatch && appLinks

    fun appLinkSummary(): String = when {
        publishedFingerprints.isEmpty() ->
            "No assetlinks.json statement was found for $packageName."
        !fingerprintMatch ->
            "The installed signing certificate is not in the published fingerprint list."
        !appLinks ->
            "The certificate matches, but the App Links relation is missing."
        else ->
            "The installed build matches the published App Links association."
    }
}

internal fun associationComparison(
    document: String,
    packageName: String,
    fingerprints: Set<String>,
): AssociationComparison {
    require(fingerprints.isNotEmpty()) { "No signing certificates available" }
    val statements = Json.parseToJsonElement(document) as? JsonArray
        ?: error("Expected an association array")
    val installed = fingerprints
        .map(::displayFingerprint)
        .distinct()
        .sorted()
    val installedNormalized = installed.map(::normalizeFingerprint).toSet()
    val relationsByInstalled = installedNormalized.associateWith { mutableSetOf<String>() }
    val published = linkedSetOf<String>()

    for (statement in statements) {
        val item = statement as? JsonObject ?: error("Invalid association statement")
        val target = item["target"] as? JsonObject ?: error("Invalid association target")
        if (target["namespace"]?.jsonPrimitive?.content != "android_app" ||
            target["package_name"]?.jsonPrimitive?.content != packageName
        ) {
            continue
        }
        val certs = target["sha256_cert_fingerprints"] as? JsonArray
            ?: error("Missing fingerprints")
        val relations = item["relation"] as? JsonArray ?: error("Missing relations")
        val names = relations.map {
            (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content
                ?: error("Invalid relation")
        }

        for (cert in certs) {
            val value = (cert as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: error("Invalid certificate")
            val display = displayFingerprint(value)
            published += display
            relationsByInstalled[normalizeFingerprint(display)]?.addAll(names)
        }
    }

    val fingerprintMatch = installedNormalized.all { installedFingerprint ->
        published.any { normalizeFingerprint(it) == installedFingerprint }
    }
    return AssociationComparison(
        packageName = packageName,
        installedFingerprints = installed,
        publishedFingerprints = published.sorted(),
        fingerprintMatch = fingerprintMatch,
        appLinks = relationsByInstalled.values.all { APP_LINK_RELATION in it },
        passkeys = relationsByInstalled.values.all { PASSKEY_RELATION in it },
    )
}

internal fun associationApproval(
    document: String,
    packageName: String,
    fingerprints: Set<String>,
): AssociationApproval {
    val comparison = associationComparison(document, packageName, fingerprints)
    return AssociationApproval(
        appLinks = comparison.appLinks,
        passkeys = comparison.passkeys,
    )
}
