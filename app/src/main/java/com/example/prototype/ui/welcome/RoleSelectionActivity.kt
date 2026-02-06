package com.example.prototype.ui.welcome

// --- ANDROID & CORE ---
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.edit

// --- JETPACK COMPOSE UI ---
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*

// --- COMPOSE MATERIAL & ICONS ---
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

// --- COMPOSE RUNTIME ---
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*

// --- PROJECT SPECIFIC ---
import com.example.prototype.ui.child.ChildDashboardActivity
import com.example.prototype.ui.parent.ParentDashboardActivity
import com.example.prototype.ui.theme.AppTheme

/**
 * RoleSelectionActivity: The "Gatekeeper" of the application.
 * * Responsibilities:
 * 1. Auto-Login: Checks for existing sessions to bypass this screen.
 * 2. Role UI: Displays the "Parent" vs "Child" selection.
 * 3. Identity: Generates unique Device IDs for child devices.
 */
class RoleSelectionActivity : ComponentActivity() {

    // --- CONSTANTS ---
    companion object {
        private const val PREFS_NAME = "AppConfig"
        private const val KEY_ROLE = "role"
        private const val KEY_DEVICE_ID = "device_id"
        private const val ROLE_CHILD = "CHILD"
        private const val ROLE_PARENT = "PARENT"
    }

    // --- LIFECYCLE ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. AUTO-LOGIN CHECK
        // If a role is already saved, skip UI and go straight to Dashboard
        if (attemptAutoLogin()) return

        // 2. RENDER UI
        setContent {
            MaterialTheme {
                RoleSelectionScreen(
                    onSelectChild = { handleChildLogin() },
                    onSelectParent = { handleParentLogin() }
                )
            }
        }
    }

    // --- LOGIC: SESSION MANAGEMENT ---

    /**
     * Checks SharedPreferences for an active session.
     * Returns TRUE if redirection happened (stopping UI rendering).
     */
    private fun attemptAutoLogin(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        return when (prefs.getString(KEY_ROLE, null)) {
            ROLE_CHILD -> {
                navigateToDashboard(ChildDashboardActivity::class.java)
                true
            }
            ROLE_PARENT -> {
                navigateToDashboard(ParentDashboardActivity::class.java)
                true
            }
            else -> false // No session found, show selection screen
        }
    }

    // --- LOGIC: ROLE HANDLERS ---

    private fun handleChildLogin() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Generate a permanent Device ID if one doesn't exist.
        // We do this once so the parent doesn't have to re-link if the app restarts.
        if (!prefs.contains(KEY_DEVICE_ID)) {
            val newId = (100000..999999).random().toString() // Simple 6-digit code
            prefs.edit { putString(KEY_DEVICE_ID, newId) }
        }

        saveRoleAndRedirect(ROLE_CHILD, ChildDashboardActivity::class.java)
    }

    private fun handleParentLogin() {
        // Parents act as "Viewers" and don't need a generated ID.
        saveRoleAndRedirect(ROLE_PARENT, ParentDashboardActivity::class.java)
    }

    private fun saveRoleAndRedirect(role: String, targetActivity: Class<*>) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            putString(KEY_ROLE, role)
        }
        navigateToDashboard(targetActivity)
    }

    private fun navigateToDashboard(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish() // Kill this activity so "Back" button exits the app instead of returning here
    }
}

// --- COMPOSE UI COMPONENTS ---

@Composable
fun RoleSelectionScreen(
    onSelectChild: () -> Unit,
    onSelectParent: () -> Unit
) {
    // Root container using AppTheme Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Background),
        contentAlignment = Alignment.Center
    ) {

        // Central Card for Focus
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(AppTheme.CardCorner),
            colors = CardDefaults.cardColors(containerColor = AppTheme.Surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo / Title
                Text(
                    text = "OverSee",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppTheme.Primary
                )
                Text(
                    text = "Select device role",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                // Role Buttons
                RoleButton(
                    text = "Child Device",
                    icon = Icons.Default.Face,
                    color = AppTheme.Success,
                    onClick = onSelectChild
                )

                Spacer(modifier = Modifier.height(16.dp))

                RoleButton(
                    text = "Parent Device",
                    icon = Icons.Default.Person,
                    color = AppTheme.Primary,
                    onClick = onSelectParent
                )
            }
        }
    }
}

/**
 * Reusable Button Component to ensure consistent styling.
 */
@Composable
fun RoleButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
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