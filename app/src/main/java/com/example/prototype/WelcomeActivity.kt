package com.example.prototype

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Auto-Login Check
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val role = prefs.getString("role", null)

        if (role == "CHILD") {
            startActivity(Intent(this, MainActivity::class.java)) // Child Dashboard
            finish()
            return
        } else if (role == "PARENT") {
            startActivity(Intent(this, ParentActivity::class.java)) // Parent Dashboard
            finish()
            return
        }

        // 2. Show Selection
        setContentView(R.layout.activity_welcome)

        findViewById<Button>(R.id.btnRoleChild).setOnClickListener {
            val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

            // 1. Check if we already have an ID from before
            var myId = prefs.getString("device_id", null)

            // 2. Only generate a new one if it's missing
            if (myId == null) {
                myId = (100000..999999).random().toString()
                prefs.edit().putString("device_id", myId).apply()
            }

            // 3. Save Role and Start
            prefs.edit().putString("role", "CHILD").apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnRoleParent).setOnClickListener {
            // Parent doesn't need an ID yet, they need to ENTER one later
            prefs.edit().putString("role", "PARENT").apply()

            startActivity(Intent(this, ParentActivity::class.java))
            finish()
        }
    }
}