package com.apexfission.android.auth.capture

import kotlin.reflect.KProperty1

internal val cardFields = listOf(
    CardData::fullName, CardData::givenNames, CardData::surnames, CardData::birthDate,
    CardData::documentNumber, CardData::personalNumber, CardData::address, CardData::issueDate,
    CardData::expiryDate, CardData::issuingCountry, CardData::documentType, CardData::documentName,
    CardData::issuingRegion, CardData::issuingCity, CardData::issuingAuthority,
)

internal fun CardData.mergeOCR(result: CardData, protected: Set<KProperty1<CardData, String>>): CardData {
    val value = { field: KProperty1<CardData, String> ->
        if (field in protected) field.get(this) else field.get(result).ifBlank { field.get(this) }
    }

    return copy(
        fullName = value(CardData::fullName),
        givenNames = value(CardData::givenNames),
        surnames = value(CardData::surnames),
        birthDate = value(CardData::birthDate),
        documentNumber = value(CardData::documentNumber),
        personalNumber = value(CardData::personalNumber),
        address = value(CardData::address),
        issueDate = value(CardData::issueDate),
        expiryDate = value(CardData::expiryDate),
        issuingCountry = value(CardData::issuingCountry),
        documentType = value(CardData::documentType), documentName = value(CardData::documentName),
        issuingRegion = value(CardData::issuingRegion), issuingCity = value(CardData::issuingCity),
        issuingAuthority = value(CardData::issuingAuthority),
    )
}
