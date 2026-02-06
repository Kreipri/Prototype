package com.example.prototype.ui.parent

// --- ANDROID & CORE ---
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

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

// --- UTILS ---
import java.text.SimpleDateFormat
import java.util.*

/**
 * ParentDashboardActivity: Main entry point for the Parent View.
 * Displays real-time monitoring stats and logs fetched from Firestore.
 */
class ParentDashboardActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()

    // Observable states: Changing these values automatically triggers UI recomposition
    private var incidentList = mutableStateListOf<FirebaseSyncManager.LogEntry>()
    private var currentTargetId = mutableStateOf("NOT_LINKED")
    private var isRefreshing = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadSavedTargetId()
        refreshDashboard()

        setContent {
            MaterialTheme {
                // Local UI state for showing/hiding the pairing dialog
                var showDialog by remember { mutableStateOf(false) }

                ParentDashboardScreen(
                    targetId = currentTargetId.value,
                    incidents = incidentList,
                    refreshing = isRefreshing.value,
                    onRefresh = { refreshDashboard() },
                    onSettingsClick = { showDialog = true }
                )

                if (showDialog) {
                    LinkDeviceDialog(
                        onDismiss = { showDialog = false },
                        onConfirm = { newId ->
                            updateTargetId(newId)
                            showDialog = false
                        }
                    )
                }
            }
        }
    }

    /** * Loads the paired child device ID from SharedPreferences
     */
    private fun loadSavedTargetId() {
        val prefs = getSharedPreferences("AppConfig", MODE_PRIVATE)
        currentTargetId.value = prefs.getString("target_id", "NOT_LINKED") ?: "NOT_LINKED"
    }

    /** * Updates the target ID in local storage and refreshes the data
     */
    private fun updateTargetId(newId: String) {
        getSharedPreferences("AppConfig", MODE_PRIVATE).edit()
            .putString("target_id", newId).apply()
        currentTargetId.value = newId
        refreshDashboard()
    }

    /** * Fetches the latest 50 logs from Firestore for the currently linked child device
     */
    private fun refreshDashboard() {
        if (currentTargetId.value == "NOT_LINKED") return

        isRefreshing.value = true
        db.collection("monitor_sessions").document(currentTargetId.value).collection("logs")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get()
            .addOnSuccessListener { documents ->
                incidentList.clear() // Clear previous state to populate new data
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
}

// --- GLOBAL UI DESIGN CONSTANTS ---
object Variables {
    val BackgroundDefaultDefault: Color = Color(0xFFFFFFFF)
    val BorderDefaultDefault: Color = Color(0xFFD9D9D9)
    val StrokeBorder: Dp = 1.dp
    val Space600: Dp = 24.dp
    val columnGap: Dp = 16.dp
}

// --- COMPOSABLE UI SCREENS ---

/**
 * Main Layout Structure using a Scaffold for Bottom Navigation
 */
@Composable
fun ParentDashboardScreen(
    targetId: String,
    incidents: List<FirebaseSyncManager.LogEntry>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        bottomBar = { ParentBottomNavigation() },
        containerColor = Color(0xFFF0F7FF)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Child Info & Settings Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.LightGray)) {
                        Icon(Icons.Default.Person, null, Modifier.align(Alignment.Center))
                    }
                    Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(targetId, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Status: Active", color = Color(0xFF4CAF50), fontSize = 12.sp)
                    }
                    IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, "Settings") }
                }
            }

            // Stats: Circular Progress and Severity Breakdown
            StatsCard(incidents)

            // Log Section: Date-grouped tables
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFD9D9D9), shape = RoundedCornerShape(12.dp))
                    .background(color = Color.White, shape = RoundedCornerShape(12.dp))
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

/**
 * Summary Card displaying a Risk Score and linear progress for Low/Med/High counts
 */
@Composable
fun StatsCard(incidents: List<FirebaseSyncManager.LogEntry>) {
    val high = incidents.count { it.severity == "HIGH" }
    val med = incidents.count { it.severity == "MEDIUM" }
    val low = incidents.count { it.severity == "LOW" }
    val total = incidents.size.coerceAtLeast(1)

    // Risk score focuses on High and Medium incidents
    val riskScore = (high + med).toFloat() / total

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { riskScore },
                    modifier = Modifier.size(90.dp),
                    strokeWidth = 8.dp,
                    color = if (riskScore > 0.5f) Color.Red else Color.Green,
                    trackColor = Color(0xFFE0E0E0)
                )
                Text("${(riskScore * 100).toInt()}%", fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.padding(start = 20.dp).weight(1f)) {
                SeverityBar("Low", low.toFloat() / total, Color(0xFF4CAF50))
                SeverityBar("Med", med.toFloat() / total, Color(0xFFFFA000))
                SeverityBar("High", high.toFloat() / total, Color(0xFFD32F2F))
            }
        }
    }
}

@Composable
fun SeverityBar(label: String, progress: Float, color: Color) {
    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).height(6.dp).clip(CircleShape),
        color = color,
        trackColor = Color(0xFFF0F0F0)
    )
}

/**
 * Groups logs by date and displays them in separate cards
 */
@Composable
fun LogTable(incidents: List<FirebaseSyncManager.LogEntry>) {
    val groupedIncidents = incidents.groupBy {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(it.timestamp)).uppercase()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        groupedIncidents.forEach { (date, logsForDate) ->
            Text(
                text = date,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                fontWeight = FontWeight.ExtraBold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Column {
                    TableHeader()
                    logsForDate.forEach { incident ->
                        LogItem(incident)
                        if (incident != logsForDate.last()) {
                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Aligns labels using Weights to ensure they match LogItem rows
 */
@Composable
fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(Variables.columnGap),
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
        horizontalArrangement = Arrangement.spacedBy(Variables.columnGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SeverityBadge(severity = incident.severity, modifier = Modifier.weight(0.8f))
        Text(text = incident.word, modifier = Modifier.weight(1.2f), fontSize = 12.sp, maxLines = 1)
        Text(text = incident.app, modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray)

        val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(incident.timestamp))
        Text(text = timeString, modifier = Modifier.weight(0.7f), fontSize = 12.sp, textAlign = TextAlign.End)
    }
}

/**
 * Pill-style badge for Severity text with custom background/content colors
 */
@Composable
fun SeverityBadge(severity: String, modifier: Modifier = Modifier) {
    val (backgroundColor, contentColor) = when (severity.uppercase()) {
        "HIGH" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        "MEDIUM" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        else -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    }

    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = severity.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LinkDeviceDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Child Device") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Child Device ID") }) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Link") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ParentBottomNavigation() {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = true, onClick = {})
        NavigationBarItem(icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }, selected = false, onClick = {})
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_6")
@Composable
fun DashboardPreview() {
    val mockIncidents = listOf(
        FirebaseSyncManager.LogEntry("scam", "HIGH", "Facebook", System.currentTimeMillis()),
        FirebaseSyncManager.LogEntry("bully", "MEDIUM", "Messenger", System.currentTimeMillis() - 600000),
        FirebaseSyncManager.LogEntry("hello", "LOW", "TikTok", System.currentTimeMillis() - 120000000)
    )
    MaterialTheme {
        ParentDashboardScreen("Pixel 6 (Mock)", mockIncidents, false, {}, {})
    }
}