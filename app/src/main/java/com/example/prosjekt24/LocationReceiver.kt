package com.example.prosjekt24

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.support.v4.content.ContextCompat
import java.io.*
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.example.prosjekt24.GlobalClass.Companion.cache
import com.example.prosjekt24.GlobalClass.Companion.getLocationManager
import com.google.android.gms.maps.model.LatLng
import java.util.*


class LocationReceiver : BroadcastReceiver() {

    var oldLocation: LocationCaeli? = null

    var oldAqi = 0.0


    /*Kaller på getData og updateCurrentLocation*/
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            getData(context)
            updateCurrentLocation(context,intent)
        }
    }

    /*Henter hvor brukeren er nå*/
    private fun updateCurrentLocation(context: Context, intent: Intent) {
        if(!checkLocationPermission(context)) return

        val locationManager = getLocationManager(context)
        val locationListener =  object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                if(location != null) {

                    val newLocation = cache.updateMyLocation(location.latitude,location.longitude)

                    if(oldLocation != null) {
                        if (LatLng(oldLocation!!.lat, oldLocation!!.lon) != LatLng(newLocation.lat, newLocation.lon)) {
                            sendBroadCast(context)
                        }
                    }

                    oldLocation = newLocation
                    saveData(context)
                    locationManager.removeUpdates(this)

                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                return
            }

            override fun onProviderEnabled(provider: String?) {
                return
            }

            override fun onProviderDisabled(provider: String?) {
                return
            }
        }

        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (location != null && location.time > (Calendar.getInstance().timeInMillis - 2*60*1000)) {
            val newLocation = cache.updateMyLocation(location.latitude,location.longitude)

            if(oldLocation != null) {
                if (LatLng(oldLocation!!.lat, oldLocation!!.lon) != LatLng(newLocation.lat, newLocation.lon)) {
                    sendBroadCast(context)
                }
            }

            oldLocation = newLocation
            saveData(context)

        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
        }

    }

    /*Ser om man har tillatelse for å bruke stedstjenester*/
    private fun checkLocationPermission(context: Context) : Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*Starter NotificationReceiver*/
    private fun sendBroadCast(context: Context) {
        val intent = Intent("update airquality")
        context.applicationContext.sendBroadcast(intent)
    }

    /*Lagrer LocationCaeli og QI til fil*/
    private fun saveData(context: Context) : Boolean {
        val file = File(context.filesDir, "Data")
        try {
            val fos = context.openFileOutput("Data", Context.MODE_PRIVATE). use {
                val oos = ObjectOutputStream(it)
                oos.writeObject(oldLocation)
                oos.writeDouble(oldAqi)
                oos.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }

    /*Henter LocationCaeli og AQI fra fil*/
    private fun getData(context: Context) : Boolean {
        val directory = context.filesDir
        val file = File(directory,"Data")

        try{
            val fileInputStream = FileInputStream(file)
            val objectInputStream = ObjectInputStream(fileInputStream)
            oldLocation = objectInputStream.readObject() as LocationCaeli?
            oldAqi = objectInputStream.readDouble()

            objectInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }

}