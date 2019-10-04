package com.example.prosjekt24

import android.content.ContentValues.TAG
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

/*Tar inn en cache for å skrive resultatet til, tar inn en client for å gjøre apikall, og et søkeord fra bruekr*/
/*Gjør apicall med søkeord og legger resultatene inn i cache for senere bruk, returnerer om det gikk bra*/
fun getLocationsForSearchWord(cache: Cache, client: OkHttpClient, searchWord : String) : Boolean {
    if(searchWord == "") {
        return  false
    }

    val url = "https://ws.geonorge.no/SKWS3Index/ssr/json/sok?navn=${searchWord}*&epsgKode=4258"

    val request = Request.Builder()
        .url(url)
        .build()

    try {
        val response = client.newCall(request).execute()

        if(response.code() != 200) {

            response.body()!!.close()

            return false
        }

        val responseData = response.body()!!.string()

        response.body()!!.close()

        val json = JSONObject(responseData)

        var treff = json.getInt("totaltAntallTreff")

        if(treff > 1) {
            if(treff > 50) {
                treff = 50
            }
            val stedsnavn = json.get("stedsnavn") as JSONArray

            for(index in 0 until treff) {
                val sted = stedsnavn.get(index) as JSONObject
                val navnType = sted.getString("navnetype")
                val stedsNavn = sted.getString("stedsnavn")
                val kommuneNavn = sted.getString("kommunenavn")
                val lat = sted.getDouble("nord")
                val lon = sted.getDouble("aust")
                var type = "grunnkrets"
                if(navnType == "Kommune" || navnType == "By") {
                    type = "kommune"
                }

                if(addToCache(navnType,kommuneNavn)) {
                    cache.addLocation(searchWord, lat,lon,stedsNavn, kommuneNavn, type, navnType, hashMapOf())
                }
            }
        }
        else if (treff == 1) {
            val sted = json.get("stedsnavn") as JSONObject
            val navnType = sted.getString("navnetype")
            val stedsNavn = sted.getString("stedsnavn")
            val kommuneNavn = sted.getString("kommunenavn")
            val lat = sted.getDouble("nord")
            val lon = sted.getDouble("aust")

            var type = "grunnkrets"
            if(navnType == "Kommune" || navnType == "By") {
                type = "kommune"
            }

            if(addToCache(navnType,kommuneNavn)) {
                cache.addLocation(searchWord, lat,lon,stedsNavn, kommuneNavn, type, navnType, hashMapOf())
            }
        }
    } catch (e : IOException) {
        Log.e(TAG, "Error calling getAreas", e)
        return false
    }

    return true
}

/*Filtrerer stedsnavn som ikke er i norge og fylker, returnerer om det gikk bra*/
fun addToCache(navnType : String, kommunenavn : String) : Boolean {
    return navnType != "Fylke" && kommunenavn != "Utland" && kommunenavn != "Jan Mayen" && kommunenavn != "Svalbard"
}

/*Tar in cache for å skrive resultater, og en clinet for apikall*/
/*Henter alle stasjoner fra api og legger inn i cachen, returnerer om det gikk bra*/
fun getStationsLocations(cache : Cache, client: OkHttpClient) : Boolean {
    val url = "https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/stations"
    val request = Request.Builder()
        .url(url)
        .addHeader("user-agent","gruppe24")
        .build()

    try {
        val response = client.newCall(request).execute()

        if(response.code() != 200) {
            response.body()!!.close()
            return false
        }

        val responseData = response.body()!!.string()
        response.body()!!.close()

        val json = JSONArray(responseData)

        val type = "station"

        for(i in 0 until json.length()) {
            val value = json.get(i) as JSONObject
            val kommune = value.get("kommune") as JSONObject
            val id = value.getString("eoi")
            val county = kommune.getString("name")
            val name = value.getString("name")
            val lon= value.getString("longitude").toDouble()
            val lat = value.getString("latitude").toDouble()

            cache.addLocation(lat,lon,name, county, type , id, hashMapOf())
        }
    } catch (e : IOException) {
        Log.e(TAG, "Error calling getStations", e)
        return false
    }

    return true
}

/*Tar inn en lokasjon, henter luftkvalitet for denne dagen og i går for den lokasjonen*/
/*Tar inn en client for apikall, returnerer omo det gikk bra eller ikke*/
fun getAirQuality(location : LocationCaeli, client: OkHttpClient) : Boolean {
        val url = location.getUrl()
        val urlToday = url + getTimeString(true)
        val urlYesterday = url + getTimeString(false)
        val newAirQuality = hashMapOf<String,AirQuality>()

        val firstApiCall = getAirQuality(location, client, urlYesterday, newAirQuality)
        val secondApiCall = getAirQuality(location, client, urlToday, newAirQuality)

        return if(firstApiCall && secondApiCall) {
            location.updateAirQuality(newAirQuality)
            true
        } else {
            false
        }
}

/*Gjør apikall og parser json og lager et nytt airquality objekt som legges inn i newAirquality*/
/*Tar inn lokasjon og url for apicall smat client. returnerer om det gikk bra*/
private fun getAirQuality(location: LocationCaeli, client: OkHttpClient, url: String, newAirQuality : HashMap<String,AirQuality>) : Boolean {
    val request = Request.Builder()
        .addHeader("user-agent","gruppe24")
        .url(url)
        .build()

    try {
        val response = client.newCall(request).execute()

        if(response.code() != 200) {
            response.body()!!.close()
            return false
        }

        val responseData = response.body()!!.string()
        response.body()!!.close()

        val json = JSONObject(responseData)
        val data = json.get("data") as JSONObject
        val jsonArray = data.getJSONArray("time") as JSONArray

        val meta = json.get("meta") as JSONObject
        val locationJson = meta.get("location") as JSONObject
        val name = locationJson.getString("name")

        if(location.county == "MyLocation") {
            location.name = name
        }

        for(index in 0 until jsonArray.length()) {
            val value = jsonArray.get(index) as JSONObject
            val variables = value.get("variables") as JSONObject
            val no2 = variables.get("no2_concentration") as JSONObject
            val pm10 = variables.get("pm10_concentration") as JSONObject
            val o3 = variables.get("o3_concentration") as JSONObject
            val pm25 = variables.get("pm25_concentration") as JSONObject
            val aqi = variables.get("AQI") as JSONObject


            val time = value.getString("from")
            val no2_value = no2.getDouble("value")
            val pm10_value = pm10.getDouble("value")
            val o3_value = o3.getDouble("value")
            val pm25_value = pm25.getDouble("value")
            val aqi_value = aqi.getDouble("value")

            val airQuality = AirQuality(time, pm10_value , o3_value, no2_value, pm25_value, aqi_value)
            newAirQuality[time] = airQuality
        }

    } catch (e : IOException) {
        Log.e(TAG, "Error calling getAirquality")
        return false
    }
    return true
}

/*Henter tid fra enheten og returnerer formatert streng for bruk til MET*/
/*Tar inn en boolean for om det er i dag eller i går*/
private fun getTimeString(today: Boolean) : String {
    val calender = Calendar.getInstance()

    if(!today) {
        calender.add(Calendar.DATE, -1)
    }

    val yrString = SimpleDateFormat("yyyy-MM-dd'T12:00:00z'", Locale.ENGLISH)

    return yrString.format(calender.time)
}