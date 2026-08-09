package com.college.locationattendance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // 🚀 FAST AUTO-LOGIN: Checks local memory and jumps instantly without showing Login UI!
        if (currentUser != null) {
            val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val savedRole = prefs.getString("SAVED_ROLE", "")

            if (savedRole == "Teacher") {
                startActivity(Intent(this, TeacherDashboardActivity::class.java))
                finish()
                return // Stops loading the login page entirely
            } else if (savedRole == "Student") {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return // Stops loading the login page entirely
            }
        }

        // If not logged in, show the normal Login Page
        setContentView(R.layout.activity_login)
        database = FirebaseDatabase.getInstance().reference

        val etEmail = findViewById<EditText>(R.id.etLoginEmail)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val cbShowPassword = findViewById<CheckBox>(R.id.cbShowPasswordLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegisterPrompt)

        // Show/Hide Password Logic
        cbShowPassword.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter Email and Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.text = "VERIFYING..."
            btnLogin.isEnabled = false

            // Authenticate with Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            checkUserRoleAndRedirect(userId)
                        }
                    } else {
                        btnLogin.text = "LOGIN"
                        btnLogin.isEnabled = true
                        Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Routes to Role Selection Page
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RoleActivity::class.java))
        }
    }

    // Role-Based Redirection Logic (Runs only on fresh manual login)
    private fun checkUserRoleAndRedirect(uid: String) {
        database.child("Users").child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val role = snapshot.child("role").value.toString()

                // 🚀 SAVE ROLE LOCALLY: This makes the next app open 10x faster
                val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("SAVED_ROLE", role).apply()

                if (role == "Teacher") {
                    Toast.makeText(this, "Welcome to Teacher Portal", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, TeacherDashboardActivity::class.java))
                } else {
                    Toast.makeText(this, "Welcome Student", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            } else {
                Toast.makeText(this, "User details not found in Database!", Toast.LENGTH_LONG).show()
                findViewById<Button>(R.id.btnLogin).apply {
                    text = "LOGIN"
                    isEnabled = true
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Database Error. Try Again.", Toast.LENGTH_SHORT).show()
            findViewById<Button>(R.id.btnLogin).apply {
                text = "LOGIN"
                isEnabled = true
            }
        }
    }
}