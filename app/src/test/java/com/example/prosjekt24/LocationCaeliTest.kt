package com.example.prosjekt24

import org.junit.Test

import org.junit.Assert.*

class LocationCaeliTest {
    val locationMyLocation = LocationCaeli(59.351, 9.58, "MyLocation", "MyLocation", "grunnkrets","MyLocation", hashMapOf())
    val locationStation = LocationCaeli(59.92767, 10.84655, "Alnabru", "MyLocation", "station","NO0057A", hashMapOf())

    val badLocation = LocationCaeli(-52.32,-123.0, "TEST", "TEST" , "TEST","TEST" , hashMapOf())

    @Test
    fun test_updateAirQuality() {
        val newAirQuality = hashMapOf<String,AirQuality>()
        newAirQuality["2019-05-19T15:00:00Z"] = AirQuality("2019-05-19T15:00:00Z",10.0,10.0,10.0,10.0,2.0)

        assertEquals(true, locationMyLocation.airQuality.isEmpty())

        locationMyLocation.updateAirQuality(newAirQuality)

        assertEquals(true,locationMyLocation.airQuality.isNotEmpty())

        assertEquals(true,locationMyLocation.airQuality == newAirQuality)

    }

    @Test
    fun getAirQuality_MyLocation_hasInternet() {
        val returnValue = locationMyLocation.getAirQuality(24,true)
        assertEquals(true, returnValue.first) // Tester om api kallet gikk bra
        assertEquals(true, returnValue.second != null) // Tester om luftkvaliteten ikke er null
    }

    @Test
    fun getAirQuality_MyLocation_noInternet() {
        val returnValue = locationMyLocation.getAirQuality(24)
        assertEquals(false, returnValue.first) // Tester om api kallet gikk dårlig
        assertEquals(true, returnValue.second == null) // Tester om at vi får null
    }

    @Test
    fun getAirQuality_Station_hasInternet() {
        val returnValue = locationStation.getAirQuality(24, true)
        assertEquals(true, returnValue.first)
        assertEquals(true, returnValue.second != null)
    }

    @Test
    fun getAirQuality_Station_noInternet() {
        val returnValue = locationStation.getAirQuality(24)
        assertEquals(false, returnValue.first)
        assertEquals(true, returnValue.second == null)
    }

    @Test
    fun getAirQuality_BadLocation_noInternet() {
        val returnValue = badLocation.getAirQuality(24)
        assertEquals(false, returnValue.first)
        assertEquals(true, returnValue.second == null)
    }

    @Test
    fun getAirQuality_BadLocation_hasInternet() {
        val returnValue = badLocation.getAirQuality(24, true)
        assertEquals(false, returnValue.first)
        assertEquals(true, returnValue.second == null)
    }

    @Test
    fun getUrlStation() {
       assertEquals("https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/?station=NO0057A&reftime=",locationStation.getUrl())
    }

    @Test
    fun getUrlMyLocation() {
        assertEquals("https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/?lat=59.351&lon=9.58&areaclass=grunnkrets&reftime="
            ,locationMyLocation.getUrl())
    }

    @Test
    fun getUrlBadLocation() {
        assertEquals("https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/?lat=-52.32&lon=-123.0&areaclass=TEST&reftime="
            ,badLocation.getUrl())
    }


}