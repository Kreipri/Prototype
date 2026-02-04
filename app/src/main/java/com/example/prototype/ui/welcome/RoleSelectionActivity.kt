package com.example.prototype.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.prototype.R
import com.example.prototype.ui.child.ChildDashboardActivity
import com.example.prototype.ui.parent.ParentDashboardActivity
import androidx.core.content.edit

/**
 * Entry point of the application ("Gatekeeper Activity").
 *
 * Responsibilities:
 * 1. Auto-Login: Checks if a valid session exists and redirects immediately.
 * 2. Role Selection: Allows the user to choose between Parent or Child modes.
 * 3. Identity Management: Generates/Persists a unique Device ID for the Child role.
 */
class RoleSelectionActivity : AppCompatActivity() {

    // --- CONSTANTS ---
    // Industry Standard: Use a 'companion object' for constants.
    // Why? It prevents "Magic Strings" (hardcoded text) scattered across your app.
    // If you ever need to change "AppConfig" to "SecureConfig", you only change it here once.
    companion object {
        private const val PREFS_NAME = "AppConfig"
        private const val KEY_ROLE = "role"
        private const val KEY_DEVICE_ID = "device_id"
        private const val ROLE_CHILD = "CHILD"
        private const val ROLE_PARENT = "PARENT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. AUTO-LOGIN GATEKEEPER
        // We check this *before* setting the layout to avoid screen flickering.
        if (attemptAutoLogin()) {
            return // Stop execution; we are redirecting.
        }

        // 2. INITIALIZE UI
        // Only show this screen if no user is logged in.
        setContentView(R.layout.activity_role_selection)
        setupClickListeners()
    }

    /**
     * Checks SharedPreferences for an existing session.
     * @return true if a redirection occurred, false if the user must log in.
     */
    private fun attemptAutoLogin(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val role = prefs.getString(KEY_ROLE, null)

        return when (role) {
            ROLE_CHILD -> {
                navigateToDashboard(ChildDashboardActivity::class.java)
                true
            }
            ROLE_PARENT -> {
                navigateToDashboard(ParentDashboardActivity::class.java)
                true
            }
            else -> false // No role saved, stay here.
        }
    }

    /**
     * Sets up the UI interactions.
     * Keeps 'onCreate' clean and readable.
     */
    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnRoleChild).setOnClickListener {
            handleChildLogin()
        }

        findViewById<Button>(R.id.btnRoleParent).setOnClickListener {
            handleParentLogin()
        }
    }

    /**
     * Logic for initializing a Child Session.
     * Includes "Device ID" generation/persistence.
     */
    private fun handleChildLogin() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // LOGIC: Hardware ID Simulation
        // We check if an ID exists. If not, we generate one.
        // We do NOT overwrite existing IDs, so the parent doesn't have to re-link every time.
        if (!prefs.contains(KEY_DEVICE_ID)) {
            val newId = (100000..999999).random().toString()
            prefs.edit { putString(KEY_DEVICE_ID, newId) }
        }

        saveRoleAndRedirect(ROLE_CHILD, ChildDashboardActivity::class.java)
    }

    /**
     * Logic for initializing a Parent Session.
     */
    private fun handleParentLogin() {
        // Parents don't need a generated ID; they act as the "Viewer".
        saveRoleAndRedirect(ROLE_PARENT, ParentDashboardActivity::class.java)
    }

    /**
     * Helper to save the role and move to the next screen.
     * DRY Principle (Don't Repeat Yourself).
     */
    private fun saveRoleAndRedirect(role: String, targetActivity: Class<*>) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit {
                putString(KEY_ROLE, role)
            }

        navigateToDashboard(targetActivity)
    }

    /**
     * Standard navigation helper.
     * Ensures the user cannot press "Back" to return to the selection screen.
     */
    private fun navigateToDashboard(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish() // DESTROY this activity so it's removed from the Back Stack.
    }
}