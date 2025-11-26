package com.example.nosignalalertsystem.ui.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nosignalalertsystem.R
import com.example.nosignalalertsystem.data.WeakSignalEntity
import java.text.SimpleDateFormat
import java.util.Locale

class LogsAdapter(private var logs: List<WeakSignalEntity>) :
    RecyclerView.Adapter<LogsAdapter.LogViewHolder>() {

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtInfo: TextView = itemView.findViewById(R.id.txtLogInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]

        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        val time = formatter.format(log.timestamp)

        holder.txtInfo.text =
            "Time: $time\nSignal: ${log.dbm} dBm\nLat: ${log.latitude}\nLng: ${log.longitude}"
    }

    override fun getItemCount(): Int = logs.size

    fun updateData(newLogs: List<WeakSignalEntity>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}
