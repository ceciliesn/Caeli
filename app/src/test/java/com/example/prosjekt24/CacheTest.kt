package com.example.prosjekt24

import com.example.prosjekt24.GlobalClass.Companion.cache
import org.junit.Test

import org.junit.Assert.*

class CacheTest {
    val locationMyLocation = LocationCaeli(59.351, 9.58, "", "MyLocation", "grunnkrets","MyLocation", hashMapOf())

    val testLocation = LocationCaeli(10.0,10.0,"Test","Test2","Test3","test4", hashMapOf())

    @Test
    fun getMyLocationAndUpdateMyLocation() {
        val location = cache.getMyLocation()

        assertEquals(null,location)

        cache.updateMyLocation(locationMyLocation.lat,locationMyLocation.lon)

        assertEquals(locationMyLocation, cache.getMyLocation())
    }

    @Test
    fun addAndGetLocation_Test() {
        val locationsInCache = cache.locations.size
        cache.addLocation(10.0,10.0,"Test","Test2","Test3","test4", hashMapOf())

        assertEquals(locationsInCache + 1, cache.locations.size)

        val location = cache.getLocation(testLocation.getId())

        assertEquals(location,testLocation)

        cache.addLocation("sokeOrd", 11.0,11.0,"Test2","Test2","Test2","test2", hashMapOf())

        assertEquals(locationsInCache + 2, cache.locations.size)

        cache.addLocation("sokeOrd", 11.0,11.0,"Test2","Test2","Test2","test2", hashMapOf())

        assertEquals(locationsInCache + 2, cache.locations.size) //Sjekker at samme lat og lon ikke lager flere objekter

        assertEquals(true, cache.getLocation("TULLBALL") == null)
    }

    @Test
    fun test_getLocationsForSearchWord() {

        val ret = cache.getLocationsForSearchWord("o")

        val apiCall = ret.first

        val locations = ret.second

        assertEquals(true,apiCall)
        assertEquals(true, locations.size > 0)

        assertEquals(true,cache.locations.size > 0)

        assertEquals(true ,cache.getLocation(locations[0].getId()) != null)

        assertEquals(true, cache.searchWords.containsKey("o"))

        assertEquals(false, cache.searchWords.containsKey("b"))
    }
}