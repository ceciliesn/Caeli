package com.example.prosjekt24

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView

class LocationListAdapter(private var myDataset: List<LocationCaeli>, private val context: Context) :
    RecyclerView.Adapter<LocationListAdapter.MyViewHolder>() {

    // En variabel til view til hvert element
    class MyViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    // Lager det nye viewet
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): LocationListAdapter.MyViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.my_text_view, parent, false) as TextView
        return MyViewHolder(textView)
    }

    // Setter innholdet i viewet og sier hva som skal gjøres når den blir trykket på
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val location = myDataset.get(position)
        holder.textView.text = "${location.name} \n${location.nameType} (${location.county})"
        holder.textView.setOnClickListener{
            val intent = Intent(context, ShowAirQuality::class.java)
            intent.putExtra(FLAGG_STRING, FLAGG_LOCATION)
            intent.putExtra(SELECTED_LOCATION, location.getId())
            startActivity(context, intent, null)
        }
    }

    fun updateDataset(newDataset: List<LocationCaeli>){
        myDataset = newDataset
        notifyDataSetChanged()
    }

    override fun getItemCount() = myDataset.size
}