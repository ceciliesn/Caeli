package com.example.prosjekt24

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class HelseraadAdapter(private val context: Context, private var dataSource: ArrayList<Helseraad>) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun updateData(list : ArrayList<Helseraad>) {
        dataSource = list
        this.notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {

            view = inflater.inflate(R.layout.helseraad_list_item, parent, false)

            holder = ViewHolder()
            holder.titleTextView = view.findViewById(R.id.helseraad_list_title) as TextView
            holder.subtitleTextView = view.findViewById(R.id.helseraad_list_subtitle) as TextView

            view.tag = holder

        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val titleTextView = holder.titleTextView
        val subtitleTextView = holder.subtitleTextView

        val helseraad = getItem(position) as Helseraad

        titleTextView.text = helseraad.title
        subtitleTextView.text = helseraad.subtitle

        return view
    }

    private class ViewHolder {
        lateinit var titleTextView: TextView
        lateinit var subtitleTextView: TextView
    }
}
