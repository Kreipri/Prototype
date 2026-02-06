package com.example.prototype.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.example.prototype.R
import com.example.prototype.ui.child.ChildDashboardActivity
import com.example.prototype.ui.parent.ParentDashboardActivity
import androidx.core.content.edit

// --- JETPACK COMPOSE UI ---
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*

// --- COMPOSE MATERIAL & ICONS ---
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

// --- COMPOSE RUNTIME & TOOLS ---
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*

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

// --- COMPOSABLE ---

@Composable
fun RoleSelectionScreen(
    onSelectChild: () -> Unit,
    onSelectParent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OverSee",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
        Text(
            text = "Choose which device you are using.",
            fontSize = 15.sp,
            color = Color(0xFF303030),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSelectChild,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text("👶 CHILD DEVICE", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSelectParent,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text("👨‍👩‍👧 PARENT DEVICE", fontSize = 18.sp)
        }
    }
}

// --- PREVIEW ---

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RoleSelectionPreview() {
    MaterialTheme {
        RoleSelectionScreen(
            onSelectChild = {},
            onSelectParent = {}
        )
    }
}