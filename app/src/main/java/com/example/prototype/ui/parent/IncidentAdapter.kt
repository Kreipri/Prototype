package com.example.prototype.ui.parent

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prototype.R
import com.example.prototype.data.remote.FirebaseSyncManager.LogEntry
import java.text.SimpleDateFormat
import java.util.*

class IncidentAdapter(private var incidents: List<LogEntry>) :
    RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder>() {

    class IncidentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val indicator: View = view.findViewById(R.id.indicatorSeverity)
        val word: TextView = view.findViewById(R.id.rowWord)
        val app: TextView = view.findViewById(R.id.rowApp)
        val time: TextView = view.findViewById(R.id.rowTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return IncidentViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        val item = incidents[position]

        // 1. Set Text
        holder.word.text = item.word
        holder.app.text = item.app

        // 2. Format Time
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.time.text = sdf.format(Date(item.timestamp))

        // 3. Set Color Indicator based on Severity
        val color = when (item.severity) {
            "HIGH" -> "#D32F2F"   // Red
            "MEDIUM" -> "#FFA000" // Orange
            else -> "#4CAF50"     // Green
        }
        holder.indicator.setBackgroundColor(Color.parseColor(color))
    }

    override fun getItemCount() = incidents.size

    // Helper to update the list when new data arrives from Firestore
    fun updateData(newItems: List<LogEntry>) {
        this.incidents = newItems
        notifyDataSetChanged()
    }
}