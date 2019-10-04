package com.example.prosjekt24

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import kotlinx.android.synthetic.main.activity_contact_us.*
import android.widget.Toast



class ContactUs : GlobalClass() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_us)

        setUpToolbar()

        if (supportActionBar != null) {
            supportActionBar!!.title = getString(R.string.tittel_contactus)
        }

        sendMail()
    }

    /*Setter opp toolbar og lukker tastaturet når menyen åpnes*/
    private fun setUpToolbar() {
        val toolbar2: Toolbar =findViewById(R.id.toolbar2)
        setSupportActionBar(toolbar2)
        val ctx = this
        val drawer : DrawerLayout =findViewById(R.id.drawer_layout)
        val toggle = object : ActionBarDrawerToggle(this, drawer, toolbar2,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            override fun onDrawerSlide(drawerView: View, sideOffset : Float) {
                closeKeyboard(ctx)
                super.onDrawerOpened(drawerView)
            }
        }

        drawer.addDrawerListener(toggle)

        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener {
            onClickNavigationItem(this,it,drawer)
            false
        }

    }

    /*Tar inn input fra bruker og sender brukeren til enhetens mail tjeneste*/
    private fun sendMail(){
        //sender e-post ved knappeklikk
        btnOK.setOnClickListener {

            val nFelt = findViewById<EditText>(R.id.navnefelt)
            val navn = nFelt.text.toString()

            val tmFelt = findViewById<EditText>(R.id.tilbakemelding)
            val tilbakemelding = tmFelt.text.toString()

            val velgtema = findViewById<Spinner>(R.id.velgtema)
            val tema = velgtema.selectedItem.toString()

            val svarboks = findViewById<CheckBox>(R.id.onskersvar)
            val svarbool = svarboks.isChecked

            val svar: String
            svar = if (svarbool) {
                "Ja"
            } else {
                "Nei"
            }

            //sjekker at alle felt er utfylt og fyller inn i e-post som sendes via brukers epost-applikasjon
            if (navn.trim().isNotEmpty() && tilbakemelding.trim().isNotEmpty()) {

                val mail = Intent(Intent.ACTION_SEND)
                mail.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>("kontaktcaeli@gmail.com"))
                mail.putExtra(Intent.EXTRA_SUBJECT, tema)
                val mailInfo =
                    String.format("Avsender: $navn\n \n Din melding: $tilbakemelding\n \n Ønsker tilbakemelding: $svar")
                mail.putExtra(Intent.EXTRA_TEXT, mailInfo)
                mail.type = "text/html"
                startActivity(mail)
                finish()
            }
            else {
                Toast.makeText(this, "Vennligst fyll ut alle felt", Toast.LENGTH_LONG).show()
            }
        }
    }
}
