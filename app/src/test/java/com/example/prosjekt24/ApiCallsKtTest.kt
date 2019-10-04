package com.example.prosjekt24

import com.example.prosjekt24.GlobalClass.Companion.cache
import com.example.prosjekt24.GlobalClass.Companion.client
import org.junit.Test

import org.junit.Assert.*

class ApiCallsKtTest {

    val locationMyLocation = LocationCaeli(59.351, 9.58, "MyLocation", "MyLocation", "grunnkrets","MyLocation", hashMapOf())

    @Test
    fun test_getLocationsForSearchWord() {
        val ret = getLocationsForSearchWord(cache,client,"z")

        assertEquals(true,ret)

        assertEquals(true,cache.searchWords.containsKey("z"))
        assertEquals(true, cache.searchWords["z"] != null)

        val ret2 = getLocationsForSearchWord(cache,client,"")

        assertEquals(false,ret2)

        assertEquals(false, cache.searchWords.containsKey(""))

        assertEquals(true, cache.searchWords[""] == null)
    }

    @Test
    fun test_addToCache() {

        assertEquals(false,addToCache("Fylke", "Test"))

        assertEquals(false, addToCache("Test", "Svalbard"))

        assertEquals(false, addToCache("Fylke", "Svalbard"))

        assertEquals(false, addToCache("Test", "Jan Mayen"))

        assertEquals(false, addToCache("Test", "Utland"))

        assertEquals(true, addToCache("Test", "Test"))
    }

    @Test
    fun test_getAirQuality() {

        assertEquals(true, locationMyLocation.airQuality.isEmpty())
        val ret = getAirQuality(locationMyLocation, client)

        assertEquals(true, ret)

        assertEquals(true, locationMyLocation.airQuality.isNotEmpty())
    }
}