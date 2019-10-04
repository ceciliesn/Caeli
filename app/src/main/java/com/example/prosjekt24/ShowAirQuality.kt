package com.example.prosjekt24

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_show_air_quality.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerImage
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout


class ShowAirQuality : GlobalClass() {
    lateinit var helseraadListen : ArrayList<Helseraad>
    lateinit var adapter : HelseraadAdapter
    var location : LocationCaeli? = null

    /*Finner ut om brukeren vil se luftkvalitet for posisjon eller for ett valgt sted før vi kaller på
    * updateLocation hvis det er posisjon eller refresh hvis det er et valgt sted
    * setter også opp alle nødvendige onclickListners*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_air_quality)
        helseraadListen = LesInnFraFil.getHelseraadFromFile("Helseraad.json", this)
        adapter = HelseraadAdapter(this, helseraadListen)
        setUpToolbar(this)

        if(supportActionBar != null) {
            supportActionBar!!.title = getString(R.string.tittel_showairquality)
        }

        val view = findViewById<RelativeLayout>(R.id.chartBox)
        view.visibility = View.GONE

        val backToCurrent: ImageView = findViewById(R.id.pollutionImgArea)
        backToCurrent.visibility = View.GONE


        val flagg = intent.getIntExtra(FLAGG_STRING,-1)
        val handler = Handler(Looper.getMainLooper())

        when (flagg) {
            -1 -> println("Feil ved uthenting av flagg showAirQuality")
            FLAGG_POSITION -> {
                location = cache.getMyLocation()
                updateLocation(handler)
            }
            FLAGG_LOCATION -> {
                location = cache.getLocation(intent.getStringExtra(SELECTED_LOCATION))
                refresh(handler)
            }
        }


        settingsChart(chart)
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onNothingSelected() {
            }
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if(location == null) return
                if(h != null && e != null) {
                    val airQuality = location!!.getAirQuality(e.x.toInt()).second
                    displayAirQuality(airQuality)
                }
            }
        })


        val refresh = findViewById<ImageView>(R.id.refresh)

        refresh.setOnClickListener {
            if(flagg == FLAGG_LOCATION) {
                refresh(handler)
            } else if(flagg == FLAGG_POSITION) {
                updateLocation(handler)
            }
        }

        findViewById<View>(R.id.mapCover).setOnTouchListener(View.OnTouchListener { v, event ->
            if (sliding_layout.panelState !== SlidingUpPanelLayout.PanelState.COLLAPSED) {
                sliding_layout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
                return@OnTouchListener true
            }

            false
        })

        backToSearch.setOnClickListener {
            val intent = Intent(this, Search::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }


        backToCurrent.setOnClickListener{
            chart.highlightValue((GRAPH_MAX_VALUE/2).toFloat(),0,false)
            val airQuality = location!!.getAirQuality(GRAPH_MAX_VALUE/2).second
            displayAirQuality(airQuality)
        }
    }

    /*Passer på at vi refresh blir kalt på når man kommer tilbake til aktiviteten
    * Dette er slik at viewene skal oppdateres*/
    override fun onResume() {
        val handler = Handler(Looper.getMainLooper())
        refresh(handler)
        super.onResume()
    }

    /*Her endrer vi instillingene til grafen*/
    private fun settingsChart(chart : LineChart) {
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.axisLeft.isEnabled = false
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.description.text = ""
        chart.legend.isEnabled = false
        chart.isHighlightPerDragEnabled = true
        chart.isHighlightPerTapEnabled = true
        chart.setTouchEnabled(true)
        val myMarkerImage = MarkerImage(this, R.drawable.graph_pointer)
        myMarkerImage.setOffset(-(24).toFloat(),-(24).toFloat())
        chart.marker = myMarkerImage
    }

    /*Refresh prøver å hente nyeste data fra LocationCaeli objektet og sender det videre til displayAirQuality
      Tar med en handler for å passe på at endringer i views ikke skjer i en tråd*/
    private fun refresh(handler : Handler) {
        val hasLocation = location != null
        val hasInternet = isNetworkAvailable(this)

        if(!hasLocation) {
            displayAirQuality(null, hasLocation, hasInternet, false)
            return
        }

        val spinner = findViewById<RelativeLayout>(R.id.loadingPanel)
        spinner.visibility = View.VISIBLE

        GlobalScope.launch {
            val returnValue = location!!.getAirQuality(GRAPH_MAX_VALUE/2, hasInternet)
            val apiCall = returnValue.first
            val airQuality = returnValue.second
            handler.post {
                if(apiCall) {
                    updateData()
                }
                displayAirQuality(airQuality,hasLocation, hasInternet, apiCall)
            }
        }
    }

    /*Metode som blir kalt fra displayAirQuality hvis noe har gått galt. Tar inn 3 boolske verdier slik at vi kan
    * gi best mulig feilmelding til brukeren*/
    private fun somethingWentWrong(hasLocation : Boolean, hasInternet : Boolean, apiCall : Boolean)  {
        val textView: TextView = findViewById(R.id.text)

        if(!hasLocation) {
            textView.text = getString(R.string.no_location)
        } else if(!hasInternet) {
            textView.text = getString(R.string.no_internet)
        } else if(!apiCall) {
            textView.text = getString(R.string.wrong_apicall)
        }
    }

    /*DisplayAirQuality oppdaterer views slik ut i fra de forskjellige verdiene til airQuality objektet den får inn*/
    fun displayAirQuality(airQuality : AirQuality?, hasLocation: Boolean = true, hasInternet: Boolean = true, apiCall: Boolean = true) {
        val spinner = findViewById<RelativeLayout>(R.id.loadingPanel)
        spinner.visibility = View.GONE

        if(airQuality == null) {
            somethingWentWrong(hasLocation,hasInternet,apiCall)
            return
        }

        displayTime(airQuality)
        startHelseRad(airQuality.aqi)

        val textView: TextView = findViewById(R.id.text)
        textView.text = location!!.name

        val backToCurrent: ImageView = findViewById(R.id.pollutionImgArea)
        backToCurrent.visibility = View.VISIBLE

        //if-sjekk som endrer logen dersom en av de ulike målingene er over et visst nivå.
        val pollutionDescription = findViewById<TextView>(R.id.pollutionDescription)
        when {
            airQuality.aqi >= 4 -> {
                pollutionImgArea.setImageResource(R.drawable.purple_cropped)
                pollutionDescription.text = getString(R.string.pollution_description_very_high)
            }
            airQuality.aqi >= 3 -> {
                pollutionImgArea.setImageResource(R.drawable.red_cropped)
                pollutionDescription.text = getString(R.string.pollution_description_high)
            }
            airQuality.aqi >= 2 -> {
                pollutionImgArea.setImageResource(R.drawable.yellow_cropped)
                pollutionDescription.text = getString(R.string.pollution_description_moderat)
            }
            else -> {
                pollutionImgArea.setImageResource(R.drawable.green_cropped)
                pollutionDescription.text = getString(R.string.pollution_description_low)
            }
        }
    }

    /*Formaterer datoFormatet til målingen slik at det står i dag, i morgen og i går med klokkeslett*/
    private fun displayTime(airQuality : AirQuality) {
        val tekst: TextView = findViewById(R.id.lastUpdated)
        val calender = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("dd", Locale.ENGLISH)
        val today = dayFormat.format(calender.time)
        calender.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrow = dayFormat.format(calender.time)
        val kl = airQuality.time.subSequence(11,16)
        val day = airQuality.time.subSequence(8,10)
        var timeString : String

        if (day == today) {
            timeString = "I dag"
        }else if(day == tomorrow){
            timeString = "I morgen"
        }else {
            timeString = "I går"
        }
        timeString += "\nKl: $kl"
        tekst.text = timeString
        val chart = findViewById<RelativeLayout>(R.id.chartBox)
        chart.visibility = View.VISIBLE
    }

    /*Her passer vi på at vi har den nyeste lokasjonen til enheten før vi kaller på refresh*/
    private fun updateLocation(handler : Handler)  {
        if(!checkLocationPermission(this)) return

        val locationManager = getLocationManager(this)
        val locationListener =  object : LocationListener {
            override fun onLocationChanged(newLocation: Location?) {
                if(newLocation != null) {

                    location = cache.updateMyLocation(newLocation.latitude,newLocation.longitude)
                    refresh(handler)
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

        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)


        if (lastKnownLocation != null && lastKnownLocation.time > (Calendar.getInstance().timeInMillis - 2*60*1000)) {
            location = cache.updateMyLocation(lastKnownLocation.latitude,lastKnownLocation.longitude)
            refresh(handler)
        } else {
            val spinner = findViewById<RelativeLayout>(R.id.loadingPanel)
            spinner.visibility = View.VISIBLE
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)

            if(lastKnownLocation != null) {
                location = cache.updateMyLocation(lastKnownLocation.latitude,lastKnownLocation.longitude)
                refresh(handler)
            }
        }
    }

    /*Filtrer ut helserådene ut i fra verdien til aqi og instillingene fra sharedPrefrences*/
    private fun startHelseRad(aqi : Double) {
        val listView = findViewById<ListView>(R.id.listView)
        val sharedPreferences = getSharedPreferences("innstillinger", Context.MODE_PRIVATE)
        val gb = sharedPreferences.getBoolean("Generell befolkning", true)
        val bg = sharedPreferences.getBoolean("Gravide og barn", true)
        val hk = sharedPreferences.getBoolean("Hjerte- og karssykdommer", true)
        val al = sharedPreferences.getBoolean("Astma og andre luftveissykdommer", true)
        val eldre = sharedPreferences.getBoolean("Eldre", true)
        val litIndexGb = (0..3)
        val litIndexAl = (4..7)
        val litIndexHk = (8..11)
        val litIndexEldre = (12..15)
        val litIndexBg = (16..19)
        var startIndex = -1

        val helse = findViewById<TextView>(R.id.helse)

        when {
            aqi == -1.0 -> println("Fant ikke aqi i startHelseRad")
            aqi < 2 -> {
                startIndex = 0
                helse.setTextColor(Color.parseColor("#acc38d"))
            }
            aqi < 3 -> {
                startIndex = 1
                helse.setTextColor(Color.parseColor("#fdc055"))
            }
            aqi < 4 -> {
                startIndex = 2
                helse.setTextColor(Color.parseColor("#fd5555"))
            }
            else -> {
                startIndex = 3
                helse.setTextColor(Color.parseColor("#c68db2"))
            }
        }

        val array = arrayListOf<Helseraad>()
        for(i in startIndex until helseraadListen.size step 4){
            if(!gb) {
                if(i in litIndexGb) continue
            }
            if(!bg) {
                if(i in litIndexBg) continue
            }
            if(!hk) {
                if(i in litIndexHk) continue
            }
            if(!al) {
                if(i in litIndexAl) continue
            }
            if(!eldre) {
                if(i in litIndexEldre) continue
            }
            array.add(helseraadListen[i])
        }

        if(array.isEmpty()) {
            array.add(Helseraad("Ingen aktiverte brukergrupper","Du kan endre aktiverte brukergrupper i instillinger"))
        }
        listView.adapter = adapter
        adapter.updateData(array)
    }

    /*Oppdaterer dataen til grafen, dette skjer hver gang refresh får ny luftkvalitet*/
    private fun updateData() {
        val chart = findViewById<LineChart>(R.id.chart)
        val entries = arrayListOf<Entry>()

        for(i in 0 .. GRAPH_MAX_VALUE) {
            entries.add(Entry(i.toFloat(), location!!.getAirQuality(i).second!!.aqi.toFloat()))
        }

        val dataSet = LineDataSet(entries, "")
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.lineWidth = 3f
        dataSet.setDrawHorizontalHighlightIndicator(false)
        dataSet.setDrawVerticalHighlightIndicator(false)
        dataSet.cubicIntensity = 0.2f
        dataSet.setDrawIcons(true)
        dataSet.isHighlightEnabled = true
        dataSet.color = Color.parseColor("#707070")
        chart.setScaleEnabled(false)
        chart.data = LineData(dataSet)
        chart.notifyDataSetChanged()
        chart.highlightValue((GRAPH_MAX_VALUE/2).toFloat(),0,false)
    }
}