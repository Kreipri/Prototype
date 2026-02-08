package com.example.prototype.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.prototype.ui.parent.ParentDashboardActivity
import com.example.prototype.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// In LoginActivity.kt
class LoginActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // 1. AUTO-LOGIN CHECK: If Firebase user exists, skip login
        if (auth.currentUser != null) {
            syncCloudDataAndNavigate()
            return
        }

        setContent {
            MaterialTheme {
                LoginScreen(
                    onLogin = { email, pass -> loginUser(email, pass) },
                    onSignUpClick = { startActivity(Intent(this, SignupActivity::class.java)) }
                )
            }
        }
    }

    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email.trim(), pass.trim())
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                db.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        val linkedId = document.getString("linked_child_id") ?: "NOT_LINKED"

                        getSharedPreferences("AppConfig", MODE_PRIVATE).edit {
                            putString("target_id", linkedId)
                        }
                        navigateToRoleSelection()
                    }
                    .addOnFailureListener {
                        navigateToRoleSelection()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * 🟢 SLAPPED FUNCTION: Fetches the IDs from Firestore so they are
     * remembered immediately after login.
     */
    private fun syncCloudDataAndNavigate() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val prefs = getSharedPreferences("AppConfig", MODE_PRIVATE)

                    val cloudChildId = document.getString("device_id") ?: ""
                    val cloudLinkedId = document.getString("linked_child_id") ?: ""

                    prefs.edit {
                        // Restore Child Device ID if it exists in cloud
                        if (cloudChildId.isNotEmpty()) putString("device_id", cloudChildId)
                        // Restore Parent's Link if it exists in cloud
                        if (cloudLinkedId.isNotEmpty()) putString("target_id", cloudLinkedId)
                    }
                }
                navigateToRoleSelection()
            }
            .addOnFailureListener {
                // Even if sync fails, proceed to role selection
                navigateToRoleSelection()
            }
    }

    private fun navigateToRoleSelection() {
        startActivity(Intent(this, RoleSelectionActivity::class.java))
        finish() // Close login so back button exits app
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onSignUpClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(AppTheme.SecBackground), contentAlignment = Alignment.BottomCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally){
            Icon(
                modifier = Modifier.size(150.dp),
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "OverSee Icon",
                tint = AppTheme.Surface // Or any color from your theme
            )
            Text("Welcome!", Modifier.padding(36.dp), style = AppTheme.TitlePageStyle, color = AppTheme.Surface, textAlign = TextAlign.Center)
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(44.dp, 44.dp, 0.dp, 0.dp)
            ) {
                Column(Modifier.padding(57.dp, 71.dp, 57.dp, 57.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", style = AppTheme.BodyBase, color = AppTheme.TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", style = AppTheme.BodyBase, color = AppTheme.TextTertiary) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = { onLogin(email, password) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Primary)
                    ) {
                        Text("Login", style = AppTheme.BodyBase, color = AppTheme.Surface)
                    }
                    TextButton(onClick = onSignUpClick) {
                        Text("Don't have an account? Sign Up", style = AppTheme.BodyBase, textDecoration = TextDecoration.Underline)
                    }
                }
            }
        }

    }
}
// --- PREVIEWS ---

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            onLogin = { email, password ->
                // This block is empty for the preview
            },
            onSignUpClick = {
                // This block is empty for the preview
            }
        )
    }
}