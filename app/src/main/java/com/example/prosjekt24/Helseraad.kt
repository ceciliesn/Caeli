package com.example.prosjekt24

import android.content.Context
import org.json.JSONException
import org.json.JSONObject


class Helseraad(
    val title: String,
    val subtitle: String)


class LesInnFraFil {
    companion object {

        fun getHelseraadFromFile(filename: String, context: Context): ArrayList<Helseraad> {
            val helseraadList = ArrayList<Helseraad>()

            try {
                // Load data
                val jsonString = loadJsonFromAsset("Helseraad.json", context)
                val json = JSONObject(jsonString)
                val helseraad = json.getJSONArray("listeAvHelseraad")

                for(i in 0 until helseraad.length()) {
                    val item = helseraad.get(i) as JSONObject
                    helseraadList.add(Helseraad(item.getString("title"), item.getString("subtitle")))
                }


            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return helseraadList
        }


        //Leser inn json-filen
        private fun loadJsonFromAsset(filename: String, context: Context): String? {
            var json: String? = null

            try {
                val inputStream = context.assets.open(filename)
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                json = String(buffer, Charsets.UTF_8)
            } catch (ex: java.io.IOException) {
                ex.printStackTrace()
                return null
            }

            return json
        }
    }
}