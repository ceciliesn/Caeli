package com.example.prosjekt24

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.SystemClock
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import kotlinx.android.synthetic.main.activity_fragment.*
import java.lang.Exception

const val GRAPH_MAX_VALUE = 48
const val SHARED_PREFS_SETTINGS = "innstillinger"
const val FLAGG_STRING = "FLAGG"
const val SELECTED_LOCATION = "LOCATION"

const val FLAGG_LOCATION = 1
const val FLAGG_POSITION = 0


open class GlobalClass() : AppCompatActivity() {

    fun closeKeyboard(context : Activity) {
        val view = context.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun setUpToolbar(context: Activity)  {
        val toolbar2: Toolbar = findViewById(R.id.toolbar2)
        setSupportActionBar(toolbar2)
        val drawer : DrawerLayout = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(context, drawer, toolbar2,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close)

        drawer.addDrawerListener(toggle)

        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener {
            onClickNavigationItem(context,it,drawer)
            false
        }

    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null
    }

    /*Tar inn en requestCode og overrider metoden for å selv kunne bestemme hvor brukeren skal bli sent*/
    /*Spør om GPS før en aktivitet som trenger GPS starter hvis det ikke er tillat*/
    /*Sender med flagg til ny aktivitet*/
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 200) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, ShowAirQuality::class.java)
                intent.putExtra(FLAGG_STRING, FLAGG_POSITION)
                startActivity(intent)
            }
        }else if(requestCode == 300) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setAlarm(true,this)

            } else {
                getSharedPreferences(SHARED_PREFS_SETTINGS, Context.MODE_PRIVATE).edit().putBoolean(
                    "Notifikasjon", false).apply()
                setAlarm(false,this)
            }
        } else if(requestCode == 0) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                container.setCurrentItem(2,true)
            } else {
                container.setCurrentItem(2,true)
            }

        } else if(requestCode == 100) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setAlarm(true,this)
                getSharedPreferences(SHARED_PREFS_SETTINGS, Context.MODE_PRIVATE).edit().putBoolean(
                    "Notifikasjon", true).apply()

                val intent = Intent(this, ShowAirQuality::class.java)
                intent.putExtra(FLAGG_STRING, FLAGG_POSITION)
                startActivity(intent)

            } else {
                val intent = Intent(this, Search::class.java)
                startActivity(intent)

            }
        } else if(requestCode == 205) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, ShowAirQuality::class.java)
                intent.putExtra(FLAGG_STRING, FLAGG_POSITION)
                overridePendingTransition(R.anim.abc_slide_in_top, R.anim.abc_slide_out_bottom)
                startActivity(intent)
            }
        } else {
            println("Wrong requestCode: $requestCode")
        }
    }

    /*Skrur av og på alarm for å gi notifikasjoner ut i fra setAlarm-verdien*/
    fun setAlarm(setAlarm : Boolean, context: Context) {

        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        var alarmIntent = Intent(context,LocationReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(context.applicationContext, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        }
        if(setAlarm) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val filter = IntentFilter("update airquality")
                context.applicationContext.registerReceiver(NotificationReceiver(),filter)
            }
            alarmMgr.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 10*60*1000,
                (10*60*1000).toLong(),
                alarmIntent
            )
        } else  {
            try {
                context.applicationContext.unregisterReceiver(NotificationReceiver())
            } catch (e : Exception) {
                Log.i("UnregisterReceiver", e.toString())
            }
            alarmMgr.cancel(alarmIntent)
            alarmIntent.cancel()
        }

        alarmIntent = Intent(context,NotificationReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        }

        if(setAlarm) {
            alarmMgr.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60*60*1000,
                (60*60*1000).toLong(),
                alarmIntent
            )
        } else if(!setAlarm) {
            alarmMgr.cancel(alarmIntent)
            alarmIntent.cancel()
        }
    }

    /*Sjekker om du har gitt tillatelse for posisjon*/
    fun checkLocationPermission(context : Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*Sjekker om du har gitt tillatelse for posisjon. Hvis du ikke har det spør den om tillatelse*/
    fun checkLocationPermissionAndPromptResponse(context : Activity, requestCode : Int): Boolean {

        val ret = checkLocationPermission(context)

        if(!ret) {
            val message = getString(R.string.gdpr)
            AlertDialog.Builder(context, R.style.CaeliAlertDialogTheme)
                .setTitle("Tilgang til lokasjonen din")
                .setMessage(message)
                .setPositiveButton("Ok", DialogInterface.OnClickListener { _, _ ->
                    //Prompt the user once explanation has been shown
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        requestCode
                    )
                })
                .create()
                .show()
        }

        return ret
    }

    /*Gjør alternativene i menyen klikkbare og bestemmer hva som skjer når du trykker på dem*/
    fun onClickNavigationItem(ctx  : Activity, menuItem: MenuItem, drawer : DrawerLayout) {
        val id = menuItem.itemId
        if (id == R.id.nav_map) {
            val intent = Intent(ctx, Maps::class.java)
            ctx.startActivity(intent)
        } else if (id == R.id.nav_showairquality) {
            var requestCode = 200
            if(ctx.localClassName == "ShowAirQuality") {
                requestCode = 205
            }
            if(checkLocationPermissionAndPromptResponse(ctx,requestCode)) {
                val intent = Intent(ctx, ShowAirQuality::class.java)
                intent.putExtra(FLAGG_STRING, FLAGG_POSITION)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                ctx.startActivity(intent)
            }
        } else if (id == R.id.nav_settings) {
            val intent = Intent(ctx, Settings::class.java)
            ctx.startActivity(intent)
        } else if (id == R.id.nav_aboutus) {
            val intent = Intent(ctx, AboutUs::class.java)
            ctx.startActivity(intent)
        } else if (id == R.id.nav_infopage) {
            val intent = Intent(ctx, HealthInfo::class.java)
            ctx.startActivity(intent)
        } else if (id == R.id.nav_sok) {
            val intent = Intent(ctx, Search::class.java)
            ctx.startActivity(intent)
        }

        if(ctx.localClassName != "ShowAirQuality"  && ctx.localClassName != "Search") {
            if(checkLocationPermission(ctx)) {
                ctx.finish()
            }
        }

        ctx.overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out)
        drawer.closeDrawer(GravityCompat.START, false)
    }

    /*Er et statisk objekt slik at vi kan legge inn globalke verdier som alle kan bruke*/
    companion object  {
        private lateinit var locationManager : LocationManager

        val client = OkHttpClient()
        val cache = Cache(hashMapOf(), hashMapOf())
        var threadGetStations = startGetStations()

        fun getLocationManager(ctx:Context) : LocationManager {
            if(!::locationManager.isInitialized) {
                locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            }
            return locationManager
        }

        fun startGetStations() : Job {
            return GlobalScope.launch {
                getStationsLocations(cache, client)
            }
        }
    }
}