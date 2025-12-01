package com.example.nosignalalertsystem.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nosignalalertsystem.R
import com.example.nosignalalertsystem.data.AppDatabase
import com.example.nosignalalertsystem.data.WeakSignalEntity
import kotlinx.coroutines.launch
import java.io.File

class LogsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var logsAdapter: LogsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        recyclerView = view.findViewById(R.id.logsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        logsAdapter = LogsAdapter(emptyList()) { log -> deleteSingleLog(log) }
        recyclerView.adapter = logsAdapter

        view.findViewById<Button>(R.id.btnDeleteAll).setOnClickListener {
            lifecycleScope.launch {
                db.weakSignalDao().deleteAllLogs()
                logsAdapter.updateData(emptyList())
                Toast.makeText(requireContext(), "All logs deleted", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.btnExportCSV).setOnClickListener {
            exportCsv()
        }

        loadLogs()
    }

    private fun loadLogs() {
        lifecycleScope.launch {
            val logs = db.weakSignalDao().getAllLogs()
            logsAdapter.updateData(logs)
        }
    }

    private fun deleteSingleLog(log: WeakSignalEntity) {
        lifecycleScope.launch {
            db.weakSignalDao().deleteLog(log)
            loadLogs()
        }
    }

    private fun exportCsv() {
        lifecycleScope.launch {
            val logs = db.weakSignalDao().getAllLogs()
            if (logs.isEmpty()) {
                Toast.makeText(requireContext(), "No logs to export", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val sb = StringBuilder()
            sb.append("timestamp,dbm,latitude,longitude\n")
            logs.forEach { sb.append("${it.timestamp},${it.dbm},${it.latitude},${it.longitude}\n") }

            val filename = "weak_signal_logs.csv"
            val file = File(requireContext().getExternalFilesDir(null), filename)
            file.writeText(sb.toString())
            Toast.makeText(requireContext(), "CSV exported: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }
}
