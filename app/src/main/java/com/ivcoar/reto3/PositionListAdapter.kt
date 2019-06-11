package com.ivcoar.reto3

import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

class PositionListAdapter(val context: Context, val data: List<Location>) : BaseAdapter() {
    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(index: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.position_row, parent, false)
        val positionText = view.findViewById<TextView>(R.id.positionText)
        val timeText = view.findViewById<TextView>(R.id.timeText)

        positionText.text = locationToString(data[index])
        timeText.text = DateFormat.getDateTimeInstance().format(Date(data[index].time))

        return view
    }

    override fun getItem(position: Int): Location {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return data[position].hashCode().toLong()
    }

    override fun getCount(): Int {
        return data.size
    }

    private fun locationToString(loc: Location): String {
        return "${loc.latitude.toAng()} ${if (loc.latitude >= 0) context.getString(R.string.north) else context.getString(R.string.south)}, " +
                "${loc.longitude.toAng()} ${if (loc.longitude >= 0) context.getString(R.string.east) else context.getString(R.string.west)}"
    }

    private fun Double.toAng(): String {
        val x = this
        val deg = x.absoluteValue.toInt()
        val min = (x.absoluteValue % 60).toInt()
        val sec = x.absoluteValue % 3600

        return "${deg}º $min' ${String.format("%.4f", sec)}\""
    }
}