package com.example.nosignalalertsystem.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nosignalalertsystem.R
import com.example.nosignalalertsystem.data.AppDatabase
import kotlinx.coroutines.launch

class LogsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var logsAdapter: LogsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init DB
        db = AppDatabase.getDatabase(requireContext())

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.logsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        logsAdapter = LogsAdapter(emptyList())
        recyclerView.adapter = logsAdapter

        // Load logs
        loadLogs()
    }

    private fun loadLogs() {
        lifecycleScope.launch {
            val logs = db.weakSignalDao().getAllLogs()
            logsAdapter.updateData(logs)
        }
    }
}
