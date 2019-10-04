package com.example.prosjekt24

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.location.LocationManager
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import com.google.maps.android.ui.IconGenerator

class Maps : GlobalClass(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        setUpToolbar(this)

        if(supportActionBar != null) {
            supportActionBar!!.title = getString(R.string.tittel_maps)
        }

        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)

    }

    /*Setter kameraet sin posisjon over kartet og zoomer inn*/
    override fun onMapReady(map: GoogleMap) {

        map.uiSettings.isRotateGesturesEnabled = false

        val norway = LatLng(65.0, 17.0)
        val zoom = 4.5f
        if(checkLocationPermission(this)) {

            val locationManager = getLocationManager(this)

            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if(location != null) {
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(location.latitude,location.longitude))
                    .zoom(10.5f)
                    .bearing(0f)
                    .tilt(0f)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                map.setOnMarkerClickListener(this)
                addStations(map)
            }
        } else {

            val cameraPosition = CameraPosition.Builder()
                .target(norway)
                .zoom(zoom)
                .bearing(0f)
                .tilt(0f)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            map.setOnMarkerClickListener(this)
            addStations(map)
        }

        val refresh = findViewById<ImageView>(R.id.refresh)

        refresh.setOnClickListener {
            addStations(map)
        }

    }

    /*Går gjennom alle lokasjoner og kaller på addmarker på stasjonene*/
    private fun addStations(map: GoogleMap){
        val handler = Handler(Looper.getMainLooper())

        val spinner = findViewById<RelativeLayout>(R.id.loadingPanel)
        spinner.visibility = View.VISIBLE

        val ctx = this
        var apiCall : Boolean = false

        GlobalScope.launch {
            threadGetStations.join()
            for (location in cache.locations.values) {
                if (location.type == "station"){
                    apiCall = true
                    handler.post{ mapAddMarker(map,location) }
                }
            }

            handler.post{ spinner.visibility = View.GONE }

            if(!apiCall) {
                threadGetStations = startGetStations()
                handler.post { Toast.makeText(ctx,getString(R.string.wrong_apicall), Toast.LENGTH_SHORT).show() }
            }
        }
    }

    /*Legger inn en marker for hver stasjon basert på koordinater*/
    private fun mapAddMarker(map: GoogleMap, location: LocationCaeli) {
        val iconFactory = IconGenerator(this)


        val marker = map.addMarker(
            MarkerOptions()
                .position(LatLng(location.lat, location.lon))
                .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(location.name)))
        ) as Marker

        marker.tag = location
    }

    /*Sender videre til ShowAirQuality når man trykker på en stasjon*/
    override fun onMarkerClick(marker:Marker):Boolean {
        val clicked = marker.getTag() as LocationCaeli
        val intent = Intent(this, ShowAirQuality::class.java)
        intent.putExtra(SELECTED_LOCATION, clicked.getId())
        intent.putExtra(FLAGG_STRING, FLAGG_LOCATION)
        startActivity(intent)
        return true
    }

}
