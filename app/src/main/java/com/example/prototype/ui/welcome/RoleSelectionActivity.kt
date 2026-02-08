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
 * RoleSelectionActivity
 * * LOGIC FLOW SUMMARY:
 * 1. App Launch -> onCreate().
 * 2. Check Persistence -> attemptAutoLogin() checks if a role exists in SharedPreferences.
 * 3. Decision Point:
 * - IF ROLE EXISTS: Direct navigation to specific Dashboard (Flow ends here).
 * - IF NO ROLE: Render RoleSelectionScreen UI.
 * 4. User Interaction:
 * - Child Selected -> handleChildLogin() -> Generate ID -> Save Role -> Dashboard.
 * - Parent Selected -> handleParentLogin() -> Save Role -> Dashboard.
 */
class RoleSelectionActivity : ComponentActivity() {

    // --- CONSTANTS ---
    companion object {
        private const val PREFS_NAME = "AppConfig" // The physical XML file name in storage.
        private const val KEY_ROLE = "role"        // Key for storing the user type.
        private const val KEY_DEVICE_ID = "device_id" // Key for the child's unique identifier.
        private const val ROLE_CHILD = "CHILD"
        private const val ROLE_PARENT = "PARENT"
    }

    // --- LIFECYCLE ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("AppConfig", MODE_PRIVATE)
        val user = prefs.getString("name", "User") ?: "User"

        /**
         * SEQUENCE STEP 1: Auto-Login Check.
         * We do this BEFORE setContent to prevent the UI from "flashing" the selection
         * screen if the user is already logged in.
         */
        if (attemptAutoLogin()) return

        /**
         * SEQUENCE STEP 2: Render UI.
         * Only reached if attemptAutoLogin returns false (no session found).
         */
        setContent {
            MaterialTheme {
                RoleSelectionScreen(
                    user = user,
                    onSelectChild = { handleChildLogin() },
                    onSelectParent = { handleParentLogin() }
                )
            }
        }
    }

    // --- LOGIC: SESSION MANAGEMENT ---

    /**
     * DATA FLOW: SharedPreferences -> App Logic.
     * Reads the 'AppConfig' file to see if a role has been persisted.
     */
    private fun attemptAutoLogin(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Read the stored role; returns null if it's the first run.
        return when (prefs.getString(KEY_ROLE, null)) {
            ROLE_CHILD -> {
                // If a child role exists, redirect to Child Dashboard.
                navigateToDashboard(ChildDashboardActivity::class.java)
                true
            }
            ROLE_PARENT -> {
                // If a parent role exists, redirect to Parent Dashboard.
                navigateToDashboard(ParentDashboardActivity::class.java)
                true
            }
            else -> false // No session found; proceeds to show the UI.
        }
    }

    // --- LOGIC: ROLE HANDLERS ---

    /**
     * DATA FLOW: Logic -> SharedPreferences.
     * Specific logic for setting up a Child device.
     */
    private fun handleChildLogin() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        /**
         * IDENTITY GENERATION:
         * A Child device needs a persistent ID so the Parent can link to it.
         * We check if it exists first to avoid overwriting a previous ID.
         */
        if (!prefs.contains(KEY_DEVICE_ID)) {
            val newId = (100000..999999).random().toString() // Generate a 6-digit link code.
            prefs.edit { putString(KEY_DEVICE_ID, newId) }   // Save to storage.
        }

        saveRoleAndRedirect(ROLE_CHILD, ChildDashboardActivity::class.java)
    }

    private fun handleParentLogin() {
        // Parent devices don't need a generated ID as they are the "viewers".
        saveRoleAndRedirect(ROLE_PARENT, ParentDashboardActivity::class.java)
    }

    /**
     * PERSISTENCE:
     * Commits the selected role to SharedPreferences so attemptAutoLogin()
     * finds it next time the app starts.
     */
    private fun saveRoleAndRedirect(role: String, targetActivity: Class<*>) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            putString(KEY_ROLE, role)
        }
        navigateToDashboard(targetActivity)
    }

    /**
     * NAVIGATION SEQUENCE:
     * 1. Starts the target activity (Parent or Child Dashboard).
     * 2. Calls finish().
     * * STRATEGIC NOTE: By calling finish(), we remove RoleSelectionActivity from the
     * Android Back Stack. This means if the user presses "Back" from the Dashboard,
     * they exit the app instead of coming back to this selection screen.
     */
    private fun navigateToDashboard(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }
}

// --- COMPOSE UI COMPONENTS ---
// These are "stateless" UI components that purely handle the visual presentation.

@Composable
fun RoleSelectionScreen(
    user: String,
    onSelectChild: () -> Unit,
    onSelectParent: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Background)
            .padding(57.dp, 103.dp, 57.dp, 103.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top)){
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Hello, $user!",
                    style = AppTheme.TitlePageStyle,
                    textAlign = TextAlign.Left
                )
                Text(
                    text = "Which device are you currently using?",
                    style = AppTheme.BodyBase,
                )
            }
            Spacer(modifier = Modifier.height(90.dp))
            Column(
                Modifier
                    .width(200.dp),
                verticalArrangement = Arrangement.spacedBy(50.dp, Alignment.Top)
            ){
                Column(horizontalAlignment = Alignment.CenterHorizontally){
                    Icon(
                        modifier = Modifier.size(88.dp),
                        imageVector = Icons.Default.Face,
                        contentDescription = "OverSee Icon",
                        tint = AppTheme.Success // Or any color from your theme
                    )
                    // Triggers handleChildLogin() on click.
                    RoleButton(
                        text = "Child Device",
                        color = AppTheme.Success,
                        onClick = onSelectChild
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally){
                    Icon(
                        modifier = Modifier.size(88.dp),
                        imageVector = Icons.Default.Person,
                        contentDescription = "OverSee Icon",
                        tint = AppTheme.Primary // Or any color from your theme
                    )
                    // Triggers handleParentLogin() on click.
                    RoleButton(
                        text = "Parent Device",
                        color = AppTheme.Primary,
                        onClick = onSelectParent
                    )
                }
            }
        }

    }
}

@Composable
fun RoleButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(4.dp)
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_6")
@Composable
fun RoleSelectionScreenPreview() {
    // Replace with your actual Theme wrapper if you have one
    MaterialTheme {
        RoleSelectionScreen(
            user = "Admin",
            onSelectChild = {},
            onSelectParent = {}
        )
    }
}