package com.example.prosjekt24

import android.content.Context
import android.content.Intent
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.os.Bundle
import android.view.View
import com.q42.android.scrollingimageview.ScrollingImageView
import kotlinx.android.synthetic.main.activity_fragment.*

class FragmentActivity : GlobalClass() {

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        container.adapter = mSectionsPagerAdapter
        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))

        //Åpner fragment-aktiviteten kun ved første kjøring
        val isFirstRun = getSharedPreferences("innstillinger", Context.MODE_PRIVATE)
                .getBoolean("isFirstRun", true)

        setAlarm(getSharedPreferences(SHARED_PREFS_SETTINGS, Context.MODE_PRIVATE).getBoolean("Notifikasjon",false),this)

        //Kontinuerlig loop med bakgrunnsbilde
        val scrollingBackground = findViewById<ScrollingImageView>(R.id.scrolling_foreground) as ScrollingImageView
        scrollingBackground.start()


        if ((!isFirstRun)) {
            var intent = Intent(this@FragmentActivity, Search::class.java)
            if(checkLocationPermission(this)) {
                intent = Intent(this@FragmentActivity, ShowAirQuality::class.java)
                intent.putExtra(FLAGG_STRING, FLAGG_POSITION)
            }

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            finish()
            startActivity(intent)
        } else {
            getSharedPreferences(SHARED_PREFS_SETTINGS, Context.MODE_PRIVATE).edit().putBoolean(
                "isFirstRun", false).apply()
        }
    }

    // OnClick-buttons for å navigere i fragments
    fun toLocation(v : View) {
        container.setCurrentItem(1,true)
    }

    /*Sjeklker om bruker tillater gps og setter notification og sender bruker til neste aktivitet*/
    fun setNotification(v : View) {
        if(checkLocationPermissionAndPromptResponse(this,100)) {
            val intent = Intent(this, ShowAirQuality::class.java)

            getSharedPreferences(SHARED_PREFS_SETTINGS, Context.MODE_PRIVATE).edit().putBoolean(
                "Notifikasjon", true).apply()

            intent.putExtra(FLAGG_STRING, FLAGG_POSITION)
            startActivity(intent)
        }
    }

    /*Sender bruker til søk hvis gps ikke er tillat ellers til showAirQuality*/
    fun toSearch(v : View) {
        if(checkLocationPermission(this)) {
            val intent = Intent(this, ShowAirQuality::class.java)
            intent.putExtra(FLAGG_STRING, FLAGG_POSITION)
            startActivity(intent)
        } else {
            val intent = Intent(this, Search::class.java)
            startActivity(intent)
        }

    }
    fun toNotification(v : View) {
        container.setCurrentItem(2,true)
    }
    fun findLocation(v : View) {
        checkLocationPermissionAndPromptResponse(this,0)
    }

    /*Setter hvilken fragment som skal vises for hver side*/
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment? {
            when(position) {
                0 -> {
                    return FPFragmentActivity()
                }
                1 -> {
                    return FPLocationActivity()
                }
                2 -> {
                    return FPNotificationActivity()
                }
                else -> return null
            }
        }
        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }
    }
}