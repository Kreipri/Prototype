package com.example.prototype.data

import android.content.Context
import com.example.prototype.data.local.LocalStorageManager
import com.example.prototype.data.model.Incident
import com.example.prototype.data.remote.FirebaseSyncManager

/**
 * The Single Source of Truth for Incident Data.
 *
 * This Repository Pattern abstracts the data sources (File vs Firebase)
 * from the rest of the application. The UI and Services talk ONLY to this class.
 */
object IncidentRepository {

    /**
     * Saves a new incident.
     *
     * Strategy:
     * 1. Always save to local storage immediately for offline resilience.
     * 2. If the severity is HIGH, trigger an immediate "Emergency Sync" to the cloud.
     */
    fun saveIncident(context: Context, incident: Incident) {
        // 1. Local Persistence (Primary)
        LocalStorageManager.logIncident(
            context,
            incident.word,
            incident.severity,
            incident.appName
        )

        // 2. Business Rule: Critical Alert Logic
        if (incident.severity == "HIGH") {
            // Trigger background upload immediately
            FirebaseSyncManager.syncPendingLogs(context)
        }
    }

    /**
     * Manually triggers a synchronization of all pending local logs to the cloud.
     * Usually called by a periodic background timer or user action.
     */
    fun syncData(context: Context) {
        FirebaseSyncManager.syncPendingLogs(context)
    }
}