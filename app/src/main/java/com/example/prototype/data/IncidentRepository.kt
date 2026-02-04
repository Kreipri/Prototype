package com.example.prototype.data

import android.content.Context
import com.example.prototype.data.local.IncidentLogger
import com.example.prototype.data.remote.FirebaseSyncManager
import com.example.prototype.data.model.Incident

// This class coordinates Local and Cloud data
object IncidentRepository {

    fun saveIncident(context: Context, incident: Incident) {
        // 1. Save to Local File
        IncidentLogger.logIncident(context, incident.word, incident.severity, incident.appName)

        // 2. If HIGH severity, trigger immediate upload (Business Rule)
        if (incident.severity == "HIGH") {
            FirebaseSyncManager.syncPendingLogs(context)
        }
    }

    fun syncData(context: Context) {
        FirebaseSyncManager.syncPendingLogs(context)
    }
}