package com.example.prosjekt24

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.widget.SwitchCompat



class Settings : GlobalClass() {

    /*Passer på at notifikasjons ikke blir skrudd på hvis brukeren ikke godtar stedstjeneste*/
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 300) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setAlarm(true, this)

            } else {
                getSharedPreferences("innstillinger", Context.MODE_PRIVATE).edit().putBoolean(
                    "Notifikasjon", false
                ).apply()
                setAlarm(false, this)
                loadData()
            }
        } else {
            println("Wrong requestCode: $requestCode")
        }
    }

    /*Kaller på loadData og setter opp toolbaren*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setUpToolbar(this)

        if(supportActionBar != null) {
            supportActionBar!!.setTitle("Innstillinger")
        }

        //henter de ulike switch-knappene legger de i en list, lager shared prefs og en liste med switchens state.
        loadData()
    }

    /*Henter ut verdiene som ligger i sharedprefs og switches fra xml. Kobler opp onclick slik at de kaller på saveData, kaller så på updateView*/
    private fun loadData(){
        val switch1 = this.findViewById(R.id.switch1) as SwitchCompat
        val switch2 = this.findViewById(R.id.switch2) as SwitchCompat
        val switch3 = this.findViewById(R.id.switch3) as SwitchCompat
        val switch4 = this.findViewById(R.id.switch4) as SwitchCompat
        val switch5 = this.findViewById(R.id.switch5) as SwitchCompat
        val switch6 = this.findViewById(R.id.switch6) as SwitchCompat
        val list  = mutableListOf(switch1,switch2,switch3,switch4,switch5, switch6)


        val switchNames = mutableListOf<String>("Generell befolkning", "Gravide og barn","Hjerte- og karssykdommer","Astma og andre luftveissykdommer","Eldre", "Notifikasjon")


        val switchOnOff = mutableListOf<Boolean>(true,true,true,true,true,false)
        val sharedPreferences = getSharedPreferences(SHARED_PREFS_SETTINGS, Context.MODE_PRIVATE)
        for(i in 0 until list.size - 1){
            switchOnOff[i] = sharedPreferences.getBoolean(switchNames[i] , true)
        }
        switchOnOff[5] = sharedPreferences.getBoolean(switchNames[5], false)


        for(item in list){
            item.setOnClickListener(){
                saveData(switchNames,list)
            }
        }
        updateView(list, switchOnOff)
    }

    /*Passer på at switchene blir vist riktig ut ifra dataen vi hentet ut fra loadData*/
    private fun updateView(list : MutableList<SwitchCompat>, switchOnOff : MutableList<Boolean>){
        for(i in 0 until list.size ){
            list[i].isChecked = switchOnOff[i]
        }
    }

    /*Når man trykker på en switch lagrer vi den nye staten i sharedPrefrences og kaller på setAlarm hvis notifikasjons switchen har blitt endret*/
    private fun saveData(switchNames :MutableList<String>, list : MutableList<SwitchCompat>){
        val sharedPreferences = getSharedPreferences(SHARED_PREFS_SETTINGS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        for(i in 0 until list.size){
            editor.putBoolean(switchNames[i], list[i].isChecked)
        }
        editor.apply()

        if(sharedPreferences.getBoolean(switchNames[5], false)) {
            if(checkLocationPermissionAndPromptResponse(this, 300)) {
                setAlarm(true,this)
            }
        }
        else {
            setAlarm(false,this)
        }
    }
}







