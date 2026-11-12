package com.apexfission.android.auth.capture

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class JurisdictionCatalogueTest {
    private val catalogue = JurisdictionCatalogue.read(sequenceOf(
        "# attribution", "MX\tChihuahua\t", "MX\tChihuahua\tCiudad Juárez", "MX\tSonora\tHermosillo", "US\tTexas\tEl Paso"
    ))
    @Test fun suggestionsRespectBothParents() {
        assertEquals(listOf("Chihuahua", "Sonora"), catalogue.regions("México", Locale.ENGLISH))
        assertEquals(listOf("Ciudad Juárez"), catalogue.cities("MX", "Chihuahua", Locale.ENGLISH))
        assertTrue(catalogue.cities("US", "Chihuahua", Locale.ENGLISH).isEmpty())
        assertTrue(catalogue.regions("Custom jurisdiction", Locale.ENGLISH).isEmpty())
    }
    @Test fun accentInsensitiveSearchPreservesPrintedValues() {
        assertEquals("ciudad juarez", JurisdictionCatalogue.normalized("Ciudad Juárez"))
    }
}
