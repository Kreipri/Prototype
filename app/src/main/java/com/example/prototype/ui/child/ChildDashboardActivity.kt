package com.example.prototype.ui.child

// --- ANDROID & CORE ---
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.*
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit

// --- JETPACK COMPOSE UI ---
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*

// --- COMPOSE MATERIAL & ICONS ---
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

// --- COMPOSE RUNTIME ---
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*

// --- PROJECT SPECIFIC ---
import com.example.prototype.data.remote.FirebaseSyncManager
import com.example.prototype.service.FacebookAccessibilityService
import com.example.prototype.ui.theme.AppTheme
import com.example.prototype.ui.welcome.RoleSelectionActivity
import com.google.firebase.FirebaseApp
import com.example.prototype.service.ScreenCaptureService

// --- UTILS ---
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

class ChildDashboardActivity : ComponentActivity() {

    // --- STATE MANAGEMENT ---
    private var deviceId = mutableStateOf("...")
    private var consoleLogs = mutableStateListOf<String>()

    // Permission & Health States
    private var isAccessibilityOn = mutableStateOf(false)
    private var isNotificationOn = mutableStateOf(false)
    private var isOverlayOn = mutableStateOf(false)
    private var isInternetOn = mutableStateOf(false)
    private var isFirebaseReady = mutableStateOf(false)
    private var isScreenCaptureActive = mutableStateOf(false)

    private val consoleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("message") ?: return
            addToConsole(message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter("com.example.prototype.CONSOLE_UPDATE")
        registerReceiver(consoleReceiver, filter, RECEIVER_NOT_EXPORTED)
        loadDeviceInfo()
        performHealthCheck()
        addToConsole("System Initialized.")

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContent {
            MaterialTheme {
                // UI State for Dialogs
                var showEditDialog by remember { mutableStateOf(false) }

                // Monitor Facebook status in real-time
                val fbStatus = ScreenCaptureService.ScreenState.isFacebookOpen
                LaunchedEffect(fbStatus) {
                    addToConsole(if (fbStatus) "App Event: Facebook OPENED" else "App Event: Facebook CLOSED")
                }

                // Check system requirements
                val allChecksOk = isAccessibilityOn.value && isNotificationOn.value &&
                        isOverlayOn.value && isInternetOn.value &&
                        isFirebaseReady.value && isScreenCaptureActive.value

                ChildDashboardScreen(
                    deviceId = deviceId.value,
                    consoleLogs = consoleLogs,
                    isReady = allChecksOk,
                    checks = mapOf(
                        "Accessibility" to isAccessibilityOn.value,
                        "Notifications" to isNotificationOn.value,
                        "Overlay" to isOverlayOn.value,
                        "Internet" to isInternetOn.value,
                        "Firebase" to isFirebaseReady.value,
                        "Capture" to isScreenCaptureActive.value
                    ),
                    onFixPermission = { label -> navigateToSetting(label) },
                    onForceSync = { triggerSync() },
                    onLogout = { performLogout() },
                    onExit = { killApp() },
                    onEditIdClick = { showEditDialog = true } // 🟢 Open Edit Dialog
                )

                // 🟢 EDIT ID DIALOG
                if (showEditDialog) {
                    EditDeviceIdDialog(
                        currentId = deviceId.value,
                        onDismiss = { showEditDialog = false },
                        onConfirm = { newId ->
                            saveNewDeviceId(newId)
                            showEditDialog = false
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        performHealthCheck()
    }

    override fun onDestroy() {
        // 🟢 3. Unregister to prevent memory leaks
        unregisterReceiver(consoleReceiver)
        super.onDestroy()
    }

    // --- LOGIC ---

    private fun loadDeviceInfo() {
        val prefs = getSharedPreferences("AppConfig", MODE_PRIVATE)
        // If ID is missing, set a default, but don't generate one here to avoid overwrites
        deviceId.value = prefs.getString("device_id", "NOT_SET") ?: "NOT_SET"
    }

    private fun saveNewDeviceId(newId: String) {
        if (newId.isBlank()) return
        getSharedPreferences("AppConfig", MODE_PRIVATE).edit {
            putString("device_id", newId)
        }
        deviceId.value = newId
        addToConsole("Configuration: Device ID changed to '$newId'")
    }

    private fun performHealthCheck() {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        isAccessibilityOn.value = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }

        isNotificationOn.value = NotificationManagerCompat.from(this).areNotificationsEnabled()
        isOverlayOn.value = Settings.canDrawOverlays(this)

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isInternetOn.value = cm.activeNetworkInfo?.isConnected == true
        isFirebaseReady.value = FirebaseApp.getApps(this).isNotEmpty()
        isScreenCaptureActive.value = ScreenCaptureService.CaptureState.isRunning
    }

    private fun navigateToSetting(label: String) {
        val intent = when (label) {
            "Accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            "Notifications" -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            "Overlay" -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            else -> null
        }
        intent?.let { startActivity(it) }
    }

    private fun triggerSync() {
        FirebaseSyncManager.syncPendingLogs(this)
        addToConsole("Sync Event: Cloud synchronization completed.")
    }

    private fun addToConsole(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        consoleLogs.add(0, "[$timestamp] $message")

        // Keep only the last 50 logs to save memory
        if (consoleLogs.size > 50) consoleLogs.removeAt(50)
    }


    private fun performLogout() {
        // 🟢 LOGIC: Only remove the 'role', keep 'device_id' so it persists!
        getSharedPreferences("AppConfig", MODE_PRIVATE).edit {
            remove("role")
            // We intentionally do NOT remove "device_id" here.
        }
        startActivity(Intent(this, RoleSelectionActivity::class.java))
        finish()
    }

    private fun killApp() {
        finishAffinity()
        exitProcess(0)
    }
}

// --- MAIN SCREEN ---

@Composable
fun ChildDashboardScreen(
    deviceId: String,
    consoleLogs: List<String>,
    isReady: Boolean,
    checks: Map<String, Boolean>,
    onFixPermission: (String) -> Unit,
    onForceSync: () -> Unit,
    onLogout: () -> Unit,
    onExit: () -> Unit,
    onEditIdClick: () -> Unit // 🟢 New Callback
) {
    Scaffold(
        containerColor = AppTheme.Background,
        bottomBar = { StickyExitButton(onExit) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.PaddingDefault),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Child Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            // Status Section
            ReadyBanner(visible = isReady)
            ChildHeader(deviceId, onEditIdClick) // 🟢 Pass click handler

            // Connected Devices
            ConnectedDevicesCard()

            // Health & Activity
            HealthCheckSection(checks, onFixPermission)
            ActivityConsole(consoleLogs)

            // Bottom Actions
            ActionRow(onForceSync, onLogout)
            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- COMPONENT FUNCTIONS ---

@Composable
fun ReadyBanner(visible: Boolean) {
    AnimatedVisibility(visible = visible) {
        Surface(
            color = Color(0xFFE8F5E9),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, AppTheme.Success, RoundedCornerShape(12.dp))
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = AppTheme.Success)
                Spacer(Modifier.width(12.dp))
                Text("Ready for monitoring", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
        }
    }
}

@Composable
fun ChildHeader(deviceId: String, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppTheme.CardCorner),
        colors = CardDefaults.cardColors(containerColor = AppTheme.Surface)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.LightGray)) {
                Icon(Icons.Default.Face, null, Modifier.align(Alignment.Center))
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text("My Device ID", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                // 🟢 Header Text
                Text(deviceId, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            // 🟢 Edit Button
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, "Edit ID", tint = AppTheme.Primary)
            }
        }
    }
}

@Composable
fun ConnectedDevicesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.Surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Connected to:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint = AppTheme.Success, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Parent Device (Active)", fontSize = 14.sp)
            }
            Button(
                onClick = { /* Pair logic */ },
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Background, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Link New Device", fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthCheckSection(checks: Map<String, Boolean>, onFix: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("System Health", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            checks.forEach { (label, isOk) ->
                CompactHealthBadge(label, isOk, onClick = { if (!isOk) onFix(label) })
            }
        }
    }
}

@Composable
fun CompactHealthBadge(label: String, isOk: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isOk) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isOk) AppTheme.Success else AppTheme.Error,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isOk) Color(0xFF2E7D32) else Color(0xFFD32F2F))
        }
    }
}

@Composable
fun ActivityConsole(logs: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Activity Console", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AppTheme.Border),
            colors = CardDefaults.cardColors(containerColor = AppTheme.Surface.copy(alpha = 0.9f))
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                items(logs) { log ->
                    Text(
                        text = log,
                        color = if (log.contains("Event")) AppTheme.Primary else Color.DarkGray,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionRow(onForceSync: () -> Unit, onLogout: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onForceSync, Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
            Text("Sync Now")
        }
        OutlinedButton(onClick = onLogout, Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
            Text("Log Out")
        }
    }
}

@Composable
fun StickyExitButton(onExit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppTheme.Background.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Button(
            onClick = onExit,
            modifier = Modifier.padding(16.dp).fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ExitToApp, null)
            Spacer(Modifier.width(8.dp))
            Text("Deactivate & Exit App", fontWeight = FontWeight.Bold)
        }
    }
}

// 🟢 NEW DIALOG FOR EDITING ID
@Composable
fun EditDeviceIdDialog(currentId: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(currentId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Device ID") },
        text = {
            Column {
                Text("Enter a unique name or code for this device:", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Device ID") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// --- PREVIEWS ---

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChildDashboardPreview() {
    MaterialTheme {
        ChildDashboardScreen(
            deviceId = "A7-9B2-C4",
            consoleLogs = listOf("[10:00:01] App Event: Facebook OPENED"),
            isReady = false,
            checks = mapOf("Accessibility" to true, "Notifications" to true),
            onFixPermission = {},
            onForceSync = {},
            onLogout = {},
            onExit = {},
            onEditIdClick = {}
        )
    }
}