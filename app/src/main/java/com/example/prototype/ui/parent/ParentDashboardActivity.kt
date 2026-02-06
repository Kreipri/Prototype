package com.example.prototype.ui.parent

// --- ANDROID & CORE ---
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate

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

// --- FIREBASE & DATA ---
import com.example.prototype.data.remote.FirebaseSyncManager
import com.google.firebase.firestore.*

// --- PROJECT SPECIFIC ---
import com.example.prototype.ui.theme.AppTheme
import com.example.prototype.ui.welcome.RoleSelectionActivity // Or LoginActivity if you created it

// --- UTILS ---
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

/**
 * ParentDashboardActivity: Main entry point for the Parent View.
 */
class ParentDashboardActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()

    // --- STATE MANAGEMENT ---
    private var incidentList = mutableStateListOf<FirebaseSyncManager.LogEntry>()
    private var currentTargetId = mutableStateOf("NOT_LINKED")
    private var isRefreshing = mutableStateOf(false)

    // --- LIFECYCLE ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadSavedTargetId()
        refreshDashboard()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContent {
            MaterialTheme {
                // UI States for Dialogs
                var showLinkDialog by remember { mutableStateOf(false) }
                var showLogoutDialog by remember { mutableStateOf(false) }

                ParentDashboardScreen(
                    targetId = currentTargetId.value,
                    incidents = incidentList,
                    refreshing = isRefreshing.value,
                    onRefresh = { refreshDashboard() },
                    onHeaderSettingsClick = { showLinkDialog = true }, // Gear icon in header
                    onBottomNavSettingsClick = { showLogoutDialog = true } // Settings in Bottom Nav
                )

                // Dialog 1: Link Device
                if (showLinkDialog) {
                    LinkDeviceDialog(
                        onDismiss = { showLinkDialog = false },
                        onConfirm = { newId ->
                            updateTargetId(newId)
                            showLinkDialog = false
                        }
                    )
                }

                // Dialog 2: Logout / Settings Menu
                if (showLogoutDialog) {
                    LogoutDialog(
                        onDismiss = { showLogoutDialog = false },
                        onLogout = {
                            showLogoutDialog = false
                            performLogout()
                        }
                    )
                }
            }
        }
    }

    // --- DATA LOGIC ---

    private fun loadSavedTargetId() {
        val prefs = getSharedPreferences("AppConfig", MODE_PRIVATE)
        currentTargetId.value = prefs.getString("target_id", "NOT_LINKED") ?: "NOT_LINKED"
    }

    private fun updateTargetId(newId: String) {
        getSharedPreferences("AppConfig", MODE_PRIVATE).edit {
            putString("target_id", newId)
        }
        currentTargetId.value = newId
        refreshDashboard()
    }

    private fun refreshDashboard() {
        if (currentTargetId.value == "NOT_LINKED") return

        isRefreshing.value = true
        db.collection("monitor_sessions").document(currentTargetId.value).collection("logs")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get()
            .addOnSuccessListener { documents ->
                incidentList.clear()
                for (doc in documents) {
                    incidentList.add(FirebaseSyncManager.LogEntry(
                        word = doc.getString("word") ?: "?",
                        severity = doc.getString("severity") ?: "LOW",
                        app = doc.getString("app") ?: "Unknown",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    ))
                }
                isRefreshing.value = false
            }
            .addOnFailureListener {
                isRefreshing.value = false
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performLogout() {
        // Clear all session data
        getSharedPreferences("AppConfig", MODE_PRIVATE).edit {
            clear() // Removes role, target_id, is_logged_in, etc.
        }

        // Redirect to Entry Point (RoleSelection or Login)
        val intent = Intent(this, RoleSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
        startActivity(intent)
        finish()
    }
}

// --- COMPOSE UI SCREENS ---

@Composable
fun ParentDashboardScreen(
    targetId: String,
    incidents: List<FirebaseSyncManager.LogEntry>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onHeaderSettingsClick: () -> Unit,
    onBottomNavSettingsClick: () -> Unit
) {
    Scaffold(
        bottomBar = { ParentBottomNavigation(onSettingsClick = onBottomNavSettingsClick) },
        containerColor = AppTheme.Background,
        contentColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.PaddingDefault),
            verticalArrangement = Arrangement.spacedBy(AppTheme.PaddingDefault)
        ) {
            // 1. Profile Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppTheme.CardCorner),
                colors = CardDefaults.cardColors(containerColor = AppTheme.Surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.LightGray)) {
                        Icon(Icons.Default.Person, null, Modifier.align(Alignment.Center))
                    }
                    Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(targetId, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Status: Active", color = AppTheme.Success, fontSize = 12.sp)
                    }
                    // Gear icon for Linking (Header)
                    IconButton(onClick = onHeaderSettingsClick) { Icon(Icons.Default.Link, "Link Device") }
                }
            }

            // 2. Statistics Card
            StatsCard(incidents)

            // 3. Log Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = AppTheme.Border, shape = RoundedCornerShape(12.dp))
                    .background(color = AppTheme.Surface, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text("Recent Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                LogTable(incidents)

                Button(
                    onClick = onRefresh,
                    enabled = !refreshing,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (refreshing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else Text("Refresh Data")
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// --- REUSABLE COMPONENTS ---

@Composable
fun LogTable(incidents: List<FirebaseSyncManager.LogEntry>) {
    val grouped = incidents.groupBy {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(it.timestamp)).uppercase()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        grouped.forEach { (date, logs) ->
            Text(
                text = date,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                fontWeight = FontWeight.ExtraBold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AppTheme.Border),
                colors = CardDefaults.cardColors(containerColor = AppTheme.Surface),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Column {
                    TableHeader()
                    logs.forEach { incident ->
                        LogItem(incident)
                        if (incident != logs.last()) {
                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.ColumnGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Severity", Modifier.weight(0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Word", Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("App", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Time", Modifier.weight(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
fun LogItem(incident: FirebaseSyncManager.LogEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.ColumnGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SeverityBadge(severity = incident.severity, modifier = Modifier.weight(0.8f))
        Text(text = incident.word, modifier = Modifier.weight(1.2f), fontSize = 12.sp, maxLines = 1)
        Text(text = incident.app, modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray)

        val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(incident.timestamp))
        Text(text = timeString, modifier = Modifier.weight(0.7f), fontSize = 12.sp, textAlign = TextAlign.End)
    }
}

@Composable
fun StatsCard(incidents: List<FirebaseSyncManager.LogEntry>) {
    val high = incidents.count { it.severity == "HIGH" }
    val med = incidents.count { it.severity == "MEDIUM" }
    val low = incidents.count { it.severity == "LOW" }
    val total = incidents.size.coerceAtLeast(1)
    val riskScore = (high + med).toFloat() / total

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(AppTheme.CardCorner),
        colors = CardDefaults.cardColors(containerColor = AppTheme.Surface)
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { riskScore },
                    modifier = Modifier.size(90.dp),
                    strokeWidth = 8.dp,
                    color = if (riskScore > 0.5f) AppTheme.Error else AppTheme.Success,
                    trackColor = Color(0xFFE0E0E0)
                )
                Text("${(riskScore * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Column(modifier = Modifier.padding(start = 20.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SeverityBar("Low", low.toFloat() / total, AppTheme.Success)
                SeverityBar("Med", med.toFloat() / total, AppTheme.Warning)
                SeverityBar("High", high.toFloat() / total, AppTheme.Error)
            }
        }
    }
}

@Composable
fun SeverityBar(label: String, progress: Float, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = color,
            trackColor = Color(0xFFF0F0F0)
        )
    }
}

@Composable
fun SeverityBadge(severity: String, modifier: Modifier = Modifier) {
    val (bg, text) = when (severity.uppercase()) {
        "HIGH" -> Color(0xFFFFEBEE) to AppTheme.Error
        "MEDIUM" -> Color(0xFFFFF3E0) to AppTheme.Warning
        else -> Color(0xFFE8F5E9) to AppTheme.Success
    }
    Surface(modifier = modifier, color = bg, shape = RoundedCornerShape(AppTheme.BadgeCorner)) {
        Text(
            text = severity.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = text,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// --- DIALOGS & NAVIGATION ---

@Composable
fun ParentBottomNavigation(onSettingsClick: () -> Unit) {
    NavigationBar(containerColor = AppTheme.Surface) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") },
            selected = true,
            onClick = {} // Already on Home
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings") },
            selected = false,
            onClick = onSettingsClick // Opens the Logout/Settings Dialog
        )
    }
}

@Composable
fun LinkDeviceDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pair Child Device") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Child ID") }) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Link") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LogoutDialog(onDismiss: () -> Unit, onLogout: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = { Text("Do you want to log out of your parent account?") },
        confirmButton = {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Error)
            ) {
                Text("Log Out")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// --- PREVIEWS ---

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    val mockIncidents = listOf(
        FirebaseSyncManager.LogEntry("scam", "HIGH", "Facebook", System.currentTimeMillis()),
        FirebaseSyncManager.LogEntry("bully", "MEDIUM", "Messenger", System.currentTimeMillis() - 600000)
    )
    MaterialTheme {
        ParentDashboardScreen("Pixel 6 (Mock)", mockIncidents, false, {}, {}, {})
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun SettingsDialogPreview() {
    MaterialTheme {
        // We use a Box with a semi-transparent background to simulate the
        // "dim" effect that occurs when a dialog is open.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            LogoutDialog(
                onDismiss = {},
                onLogout = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LinkDialogPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            LinkDeviceDialog(
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}