 package com.example.prosjekt24

import com.example.prosjekt24.GlobalClass.Companion.cache
import com.example.prosjekt24.GlobalClass.Companion.client
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

data class LocationCaeli(val lat : Double,
                         val lon : Double,
                         var name : String,
                         val county : String,
                         val type : String,
                         val nameType: String,
                         var airQuality : HashMap<String, AirQuality>) : Serializable {

    private var lastUpdated : Date = Calendar.getInstance().time

    fun getId() : String {
        return name + county + nameType
    }

    private fun hasAirQuality() : Boolean {
        return airQuality.isNotEmpty()
    }

    /*Returnerer et par med om det gikk bra og luftkvaliteten for en gitt tid*/
    /*Hvis lufkvaliteten trenger oppdatering gjøres det først hvis enheten har nett*/
    fun getAirQuality(tid : Int, hasInternet : Boolean = false) : Pair<Boolean, AirQuality?> {
        val time = getTimeString(tid)

        if(!needsUpdate()) {
            return Pair(true,airQuality[time]!!)
        } else if(hasInternet){
            val apicall = getAirQuality(this, client)

            if(airQuality.containsKey(time)) {
                return Pair(apicall, airQuality[time]!!)
            }
        }

        return Pair(false, null)

    }

    /*Tar inn en ny luftkvalitet HashMap og erstatter den som er i lokasjonen*/
    fun updateAirQuality(newAirQuality : HashMap<String, AirQuality>) {
        val dateNow = Calendar.getInstance().time
        airQuality = newAirQuality
        lastUpdated = dateNow
    }

    /*Henter tiden fra enheten og sjekker om det har gått mer enn et døgn siden sist oppdatering*/
     private fun needsUpdate() : Boolean {
         val dateNow = Calendar.getInstance().time
         if(!hasAirQuality()) {
            return true
         }

        val diff = dateNow.time - lastUpdated.time
        val diffSeconds = diff/1000
        val diffMinutes = diffSeconds/60
        val diffHours = diffMinutes/60
        return diffHours > 24
    }

    /*Rturnerer formatert streng med tiden, med tid som offset*/
    /*24 er santid, 0 er samme tid i går og 48 er samme tid i morgen*/
    private fun getTimeString(tid : Int) : String {
        val nyDate = lastUpdated.time + 3600 * 1000 * (tid - GRAPH_MAX_VALUE/2)
        val yrString = SimpleDateFormat("yyyy-MM-dd'T'HH':00:00Z'", Locale.ENGLISH)

        return yrString.format(nyDate)
    }

    fun getUrl() : String {
        return if(type == "station") {
            "https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/?station=$nameType&reftime="
        } else {
            "https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/?lat=$lat&lon=$lon&areaclass=$type&reftime="
        }
    }

}

data class AirQuality(val time : String,
                      val pm10_concentration: Double,
                      val o3_concentration : Double,
                      val n02_concentration : Double,
                      val pm25 : Double,
                      val aqi : Double) : Serializable


/*Container for å lagre dataen fra apiet*/
data class Cache(val locations : HashMap<String,LocationCaeli>,
                 val searchWords : HashMap<String, MutableList<LocationCaeli>>) {
    private var myLocation = LocationCaeli(0.0,0.0,"","MyLocation","grunnkrets","MyLocation", hashMapOf())

    fun getMyLocation() : LocationCaeli? {
        if(myLocation.lat == 0.0) {
            return null
        }
        return myLocation
    }

    fun updateMyLocation(lat: Double, lon: Double) : LocationCaeli {
        val latMax3Digits : Double = Math.round(lat * 1000.0) / 1000.0
        val lonMax3Digits : Double = Math.round(lon * 1000.0) / 1000.0

        if(latMax3Digits != myLocation.lat || lonMax3Digits != myLocation.lon) {
            myLocation = LocationCaeli(latMax3Digits,lonMax3Digits,"","MyLocation","grunnkrets","MyLocation", hashMapOf())
        }

        return myLocation
    }

    /*legger til location fra GPS*/
    /*Tar inn de nødvendige parameterene som trengs for å lage et lokasjons objekt*/
    fun addLocation(lat : Double, lon : Double, name : String, county : String, type : String, nameType: String, airQuality : HashMap<String, AirQuality>) : LocationCaeli {
        val id = name + county + nameType
        lateinit var location : LocationCaeli
        if(locations.containsKey(id)) {
            location = locations[id]!!
        } else {
            val latMax3Digits : Double = Math.round(lat * 1000.0) / 1000.0
            val lonMax3Digits : Double = Math.round(lon * 1000.0) / 1000.0
            location = LocationCaeli(latMax3Digits,lonMax3Digits,name,county,type,nameType,airQuality)
            locations[id] = location
        }

        return location
    }

    /*legger til location fra søkeord*/
    /*Tar inn de nødvendige parameterene som trengs for å lage et lokasjons objekt*/
    fun addLocation(searchWord : String, lat : Double, lon : Double, name : String, county : String, type : String, nameType: String, airQuality : HashMap<String, AirQuality>) {

        val location = addLocation(lat,lon,name,county,type,nameType,airQuality)

        if(searchWords.containsKey(searchWord)) {
            val listOfAreas = searchWords[searchWord]
            if(!listOfAreas!!.contains(location)) {
                listOfAreas.add(location)
            }
        } else {
            searchWords[searchWord] = mutableListOf(location)
        }

    }

    fun getLocation(id : String) : LocationCaeli? {
        return locations[id]
    }

    /*Returnerer en liste med resultater for søkeord, hvis det ikke finnes gjøres et apikall som legger det inn*/
    fun getLocationsForSearchWord(searchWord : String) : Pair<Boolean, MutableList<LocationCaeli>> {
        if(searchWords.containsKey(searchWord)) {
            return Pair(true, searchWords[searchWord]!!)
        } else {
            val apicall = getLocationsForSearchWord(cache,client,searchWord)
            
            if(searchWords.containsKey(searchWord)) {
                return Pair(apicall,searchWords[searchWord]!!)
            }
            
            return Pair(apicall, mutableListOf())
        }
    }
}
