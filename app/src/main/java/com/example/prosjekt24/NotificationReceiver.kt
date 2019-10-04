package com.example.prosjekt24

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.widget.RemoteViews
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import kotlin.math.absoluteValue
import android.app.NotificationChannel


class NotificationReceiver : BroadcastReceiver() {
    val handler = Handler(Looper.getMainLooper())

    private val channelId ="com.example.prosjekt24"

    var latestLocation : LocationCaeli? = null

    var oldAqi = 0.0

    /*kaller på getData og updateAirQuality*/
    override fun onReceive(context: Context?, intent: Intent?) {
        if(context != null && intent != null) {
            val bool = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED

            val sharedPreferences = context.getSharedPreferences("innstillinger",Context.MODE_PRIVATE)

            if(!sharedPreferences.getBoolean("Notifikasjon", false) or bool) {
                return
            }

            getData(context)
            updateAirQuality(context,intent)
        }


    }

    /*henter ny luftkvalitet data fra LocationCaeli objektet*/
    fun updateAirQuality(context: Context, intent: Intent) {
        if (latestLocation == null) return

        if(isNetworkAvailable(context)) {
            GlobalScope.launch {
                val hasInternet = isNetworkAvailable(context)
                val returnValue = latestLocation!!.getAirQuality(GRAPH_MAX_VALUE/2, hasInternet)
                val apiCall = returnValue.first
                val airQuality = returnValue.second
                if(apiCall) { handler.post(Runnable { sendNotification(context,intent, airQuality) }) }
            }
        }
    }

    /*Bestemmer om vi skal kalle på showNotification eller ikke basert på om AQI verdien har endret seg nok siden sist*/
    fun sendNotification(context: Context, intent: Intent, airQuality : AirQuality?) {
        if(airQuality == null) return


        val newAqi = airQuality.aqi

        if(oldAqi == 0.0) oldAqi = newAqi


        if((newAqi.toInt() - oldAqi.toInt()).absoluteValue >= 1) {
            showNotification(context,intent,newAqi)
        }


        oldAqi = newAqi
        saveData(context)
    }

    /*Viser notifikasjon basert på aqi verdien*/
    fun showNotification(context: Context, intent: Intent,  newAqi : Double) {
        val contentView = RemoteViews(context.packageName,R.layout.notification_layout)
        contentView.setTextColor(R.id.notification_content, Color.BLACK)
        contentView.setTextColor(R.id.notification_title, Color.BLACK)

        when {
            newAqi < 2 -> {
               return
            }
            newAqi < 3 -> {
                contentView.setTextViewText(R.id.notification_title,"Luftkvalitet moderat - ${latestLocation!!.name}")
                contentView.setTextViewText(R.id.notification_content,"Luftkvaliteten der du er har endret seg")
            }
            newAqi < 4 -> {
                contentView.setTextViewText(R.id.notification_title,"Luftkvalitet høy - ${latestLocation!!.name}")
                contentView.setTextViewText(R.id.notification_content,"Luftkvaliteten der du er har endret seg")
            }
            else -> {
                contentView.setTextViewText(R.id.notification_title,"Luftkvalitet ekstremt høy - ${latestLocation!!.name}")
                contentView.setTextViewText(R.id.notification_content,"Luftkvaliteten der du er har endret seg")
            }
        }


        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = 1
        val channelId = "channel-01"
        val channelName = "Prosjekt24"


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(
                channelId, channelName, importance
            )
            nManager.createNotificationChannel(mChannel)
        }

        val builder : NotificationCompat.Builder  =
            NotificationCompat.Builder(context,channelId)
                .setSmallIcon(R.drawable.notification_logo)
                .setContent(contentView)
                .setChannelId(channelId)
                .setAutoCancel(true)



        val targetIntent  = Intent(context, ShowAirQuality::class.java)
        targetIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        targetIntent.putExtra(FLAGG_STRING, FLAGG_POSITION)
        val contentIntent : PendingIntent = PendingIntent.getActivity(context, 0, targetIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        builder.setContentIntent(contentIntent)
        nManager.notify(notificationId, builder.build())

   }


    /*Henter LocationCaeli og AQI fra fil*/
    private fun getData(context: Context) : Boolean {
        val directory = context.filesDir
        val file = File(directory,"Data")

        try{
            val fileInputStream = FileInputStream(file)
            val objectInputStream = ObjectInputStream(fileInputStream)

            latestLocation = objectInputStream.readObject() as LocationCaeli?
            oldAqi = objectInputStream.readDouble()


            objectInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }

    /*Lagrer locationCaeli og AQI til fil*/
    private fun saveData(context: Context) : Boolean {

        try {
            val fos = context.openFileOutput("Data", Context.MODE_PRIVATE). use {
                val oos = ObjectOutputStream(it)
                oos.writeObject(latestLocation)
                oos.writeDouble(oldAqi)
                oos.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /*Sjekker om enheten er koblet til internet*/
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null
    }

}
