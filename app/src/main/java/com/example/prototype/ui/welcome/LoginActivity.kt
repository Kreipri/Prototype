package com.example.prototype.ui.welcome

// --- ANDROID & CORE ---
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.edit

// --- JETPACK COMPOSE UI ---
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- PROJECT SPECIFIC ---
import com.example.prototype.ui.child.ChildDashboardActivity
import com.example.prototype.ui.parent.ParentDashboardActivity
import com.example.prototype.ui.theme.AppTheme

/**
 * LoginActivity: The NEW Entry Point.
 * 1. Checks if user is already logged in.
 * 2. If yes -> Redirects to Dashboard or Role Selection.
 * 3. If no -> Shows Login UI.
 */
class LoginActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "AppConfig"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_ROLE = "role"
        private const val ROLE_CHILD = "CHILD"
        private const val ROLE_PARENT = "PARENT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. AUTO-LOGIN CHECK
        if (checkAutoLogin()) return

        // 2. SHOW LOGIN UI
        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginSuccess = { performLogin() }
                )
            }
        }
    }

    /**
     * Checks if a session exists. Redirects if true.
     * Returns TRUE if redirection happened.
     */
    private fun checkAutoLogin(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

        if (isLoggedIn) {
            val role = prefs.getString(KEY_ROLE, null)
            when (role) {
                ROLE_CHILD -> navigateTo(ChildDashboardActivity::class.java)
                ROLE_PARENT -> navigateTo(ParentDashboardActivity::class.java)
                else -> navigateTo(RoleSelectionActivity::class.java) // Logged in, but no role yet
            }
            return true
        }
        return false
    }

    private fun performLogin() {
        // Save session flag
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            putBoolean(KEY_IS_LOGGED_IN, true)
        }
        // Go to Role Selection next
        navigateTo(RoleSelectionActivity::class.java)
    }

    private fun navigateTo(target: Class<*>) {
        startActivity(Intent(this, target))
        finish()
    }
}

// --- UI COMPONENTS ---

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Background),
        contentAlignment = Alignment.Center
    ) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome Back",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.Primary
                )
                Text(
                    text = "Sign in to continue",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Login Button
                Button(
                    onClick = onLoginSuccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LOG IN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    MaterialTheme { LoginScreen {} }
}