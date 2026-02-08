package com.example.prototype.ui.welcome

// --- IMPORTS ---

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototype.ui.parent.ParentDashboardActivity
import com.example.prototype.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview


class SignupActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SignUpScreen(
                    onSignUp = { name, email, password ->
                        registerUser(name, email, password)
                    },
                    onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    private fun registerUser(name: String, email: String, pass: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Create Auth User (Auto-Hashes Password)
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // 2. Create User Map for Firestore
                val userMap = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to "PARENT",
                    "linked_child_id" to "",
                    "created_at" to System.currentTimeMillis()
                )

                // 3. Save to Firestore
                db.collection("users").document(uid).set(userMap)
                    .addOnSuccessListener {
                        // Save session locally
                        Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ParentDashboardActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Registration Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveSession(childId: String) {
        getSharedPreferences("AppConfig", MODE_PRIVATE).edit().apply {
            putString("role", "PARENT")
            putString("linked_child_id", childId) // Persist locally for speed
            apply()
        }
    }
}

@Composable
fun SignUpScreen(onSignUp: (String, String, String) -> Unit, onLoginClick: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 1. Main Background Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.SecBackground), // Matches Login Blue
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // 2. Logo & Header (Matches Login)
            Icon(
                modifier = Modifier.size(150.dp),
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "OverSee Icon",
                tint = AppTheme.Surface
            )



            // 3. Bottom Sheet Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp)
            ) {
                Column(
                    // Reduced top padding slightly (71dp -> 40dp) to fit the extra field comfortably
                    modifier = Modifier.padding(start = 57.dp, top = 40.dp, end = 57.dp, bottom = 57.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Sign Up",
                        modifier = Modifier.fillMaxWidth(),
                        style = AppTheme.TitlePageStyle,
                        textAlign = TextAlign.Left
                    )

                    // -- FIELDS --
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name", style = AppTheme.BodyBase, color = AppTheme.TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

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

                    Spacer(modifier = Modifier.height(10.dp))

                    // -- BUTTONS --
                    Button(
                        onClick = { onSignUp(name, email, password) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Primary)
                    ) {
                        Text("Create Account", style = AppTheme.BodyBase, color = AppTheme.Surface)
                    }

                    TextButton(onClick = onLoginClick) {
                        Text(
                            text = "Already have an account? Login",
                            style = AppTheme.BodyBase,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_6")
@Composable
fun SignUpScreenPreview() {
    MaterialTheme {
        SignUpScreen(
            onSignUp = { _, _, _ -> },
            onLoginClick = {}
        )
    }
}