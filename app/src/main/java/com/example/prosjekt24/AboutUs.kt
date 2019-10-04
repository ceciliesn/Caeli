package com.example.prosjekt24


import android.os.Bundle
import android.content.Intent
import android.support.v4.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_about_us.*


class AboutUs : GlobalClass() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        setUpToolbar(this)

        if(supportActionBar != null) {
            supportActionBar!!.title = getString(R.string.tittel_aboutus)
        }

        //knapp som åpner epost-aktiviteten
        kontaktknapp.setOnClickListener {
            val intent = Intent(this, ContactUs::class.java)
            startActivity(intent)
        }
    }
}
