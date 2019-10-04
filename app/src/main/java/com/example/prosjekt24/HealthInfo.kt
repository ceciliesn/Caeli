package com.example.prosjekt24

import android.os.Bundle

class HealthInfo : GlobalClass() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_healthinfo)

        setUpToolbar(this)

        if(supportActionBar != null) {
            supportActionBar!!.title = getString(R.string.tittel_healthinfo)
        }
    }
}
