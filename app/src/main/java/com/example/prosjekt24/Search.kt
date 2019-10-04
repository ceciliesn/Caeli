package com.example.prosjekt24

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.NavigationView
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.view.Gravity
import android.support.v4.view.GravityCompat



class Search : GlobalClass() {

    /*Override onBackPressed slik at vi kan alltid sende brukeren tilbake til showAirQuality med posisjon hvis brukeren har tillatt posisjon*/
    override fun onBackPressed() {
        if(checkLocationPermission(this)) {
            val intent = Intent(this, ShowAirQuality::class.java)
            intent.putExtra(FLAGG_STRING, FLAGG_POSITION)
            startActivity(intent)
            finish()
        } else {
            super.onBackPressed()
        }
    }

    /*Setter opp nødvendige onclickListneres*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)


        setUpToolbar()

        if(supportActionBar != null) {
            supportActionBar!!.title = getString(R.string.tittel_search)
        }

        val viewManager = LinearLayoutManager(this)
        val viewAdapter = LocationListAdapter(emptyList(), this)
        findViewById<android.support.v7.widget.RecyclerView>(R.id.my_recycler_view_test).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        val spinner = findViewById<RelativeLayout>(R.id.loadingPanel)
        spinner.visibility=View.GONE


        val handler = Handler(Looper.getMainLooper())
        var threadGetAreas = Job()
        threadGetAreas.cancel()

        val inputField: EditText = findViewById(R.id.editText)
        inputField.requestFocus()
        val ctx = this
        inputField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hasInternet = isNetworkAvailable(ctx)
                var apiCall = true

                if(!hasInternet) {
                    showList(hasInternet, apiCall, mutableListOf(), viewAdapter, "")
                    return
                }

                val input = reformatInput(inputField)


                if(input.isNotEmpty()) {
                    GlobalScope.launch {
                        threadGetAreas.join()

                        threadGetAreas = GlobalScope.launch {
                            handler.post {spinner.visibility=View.VISIBLE}
                            val returnValue = cache.getLocationsForSearchWord(input)
                            handler.post {spinner.visibility=View.GONE}
                            apiCall = returnValue.first
                            val list = returnValue.second
                            handler.post{
                                showList(hasInternet, apiCall, list, viewAdapter, input)
                            }

                        }
                    }

                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
               return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                return
            }

        })

        inputField.setOnEditorActionListener {v, actionId, event ->
            closeKeyboard(this)
            actionId == EditorInfo.IME_ACTION_DONE
        }

    }

    /*Reformaterer input fra brukeren slik at den alltid er i små bokstaver og fjerner unødvendig mellomrom på slutten*/
    private fun reformatInput(text : EditText) : String {
        var input = text.text.toString().toLowerCase()
        val index = input.lastIndex
        if(input.isNotEmpty()) {
            if(input[index] == ' ') {
                input = input.removeRange(index,index + 1)
            }
        }
        return input
    }

    /*Kobler opp toolbaren med menyen og passer på at tastaturet blir lukket */
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

    /*Endrer views ut i fra listen av LocationCaeli objekter vi får fra cachen og gir feilmeldinger ut i fra boolske verdier.
    * Tar også inn en adapter for å fylle opp et recyclerview med alle lokasjonene*/
    fun showList(hasInternet : Boolean, apiCall : Boolean, list : MutableList<LocationCaeli>, adapter: LocationListAdapter, input : String) {
        val noResult: TextView = findViewById(R.id.noResult)

        if(!hasInternet) {
            noResult.text = getString(R.string.no_internet)
        } else if(!apiCall){
            noResult.text = getString(R.string.wrong_apicall)
        } else if(list.isEmpty() && input.isNotEmpty()) {
            noResult.text = "Fant ingen lokasjoner for: $input"
        } else {
            noResult.text = ""
        }

        adapter.updateDataset(list)
    }
}
