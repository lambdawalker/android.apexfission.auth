package com.apexfission.android.auth.capture

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class DocumentReviewTest {
    @Test fun legacyRecordsHaveBlankJurisdiction() {
        val data = Json.decodeFromString<CardData>("""{"full_name":"Test Person"}""")
        assertEquals("", data.issuingRegion)
        assertEquals("", data.documentType)
    }
    @Test fun jurisdictionAndCustomTypeSurviveWireRoundTrip() {
        val data = CardData(documentType = "other", documentName = "Municipal credential", issuingCountry = "Mexico", issuingRegion = "Chihuahua", issuingCity = "Ciudad Juárez")
        assertEquals(data, Json.decodeFromString<CardData>(Json.encodeToString(data)))
        assertTrue(data.isValid())
        assertFalse(data.copy(issuingCity = "a".repeat(513)).isValid())
    }
    @Test fun changingParentsClearsOnlyDescendants() {
        val place = IssuingJurisdiction("Mexico", "Chihuahua", "Ciudad Juárez")
        assertEquals(place, place.withCountry("Mexico"))
        assertEquals(IssuingJurisdiction("Canada"), place.withCountry("Canada"))
        assertEquals(IssuingJurisdiction("Mexico", "Sonora"), place.withRegion("Sonora"))
    }
    @Test fun editedJurisdictionAndTypeAreProtectedFromLateOcr() {
        val draft = CardData(documentType = "other", issuingRegion = "User region", issuingCity = "")
        val ocr = CardData(documentType = "passport", issuingRegion = "OCR region", issuingCity = "OCR city")
        val merged = draft.mergeOCR(ocr, setOf(CardData::documentType, CardData::issuingRegion, CardData::issuingCity))
        assertEquals(draft, merged)
    }
    @Test fun unknownTypesUseGenericPresetWithoutLosingTheirValue() {
        assertEquals(DocumentKind.OTHER, DocumentKind.forValue("Consular document"))
        assertEquals(DocumentKind.PASSPORT, DocumentKind.forValue("passport"))
    }
}
