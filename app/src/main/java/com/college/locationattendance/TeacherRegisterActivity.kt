package com.college.locationattendance

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class TeacherRegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val etEmail = findViewById<EditText>(R.id.etTeacherEmail)
        val etPhone = findViewById<EditText>(R.id.etTeacherPhone)
        val etPassword = findViewById<EditText>(R.id.etTeacherPassword)
        val etRePassword = findViewById<EditText>(R.id.etTeacherRePassword)
        val cbShowPassword = findViewById<CheckBox>(R.id.cbShowPasswordTeacherRegister)
        val btnRegister = findViewById<Button>(R.id.btnTeacherRegister)

        cbShowPassword.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                etRePassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                etRePassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            etPassword.setSelection(etPassword.text.length)
            etRePassword.setSelection(etRePassword.text.length)
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val rePassword = etRePassword.text.toString().trim()

            if (email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != rePassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🚀 MASTER LOGIC: Checking the correct "PreAuthTeachers" node
            database.child("PreAuthTeachers").child(phone).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Mobile Number is authorized! Proceed to create account.
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid ?: ""
                                val userMap = hashMapOf(
                                    "role" to "Teacher",
                                    "email" to email,
                                    "phoneNumber" to phone
                                )

                                database.child("Users").child(userId).setValue(userMap)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Teacher Registration Successful! Please Login.", Toast.LENGTH_LONG).show()
                                        auth.signOut()
                                        val intent = Intent(this, LoginActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                            } else {
                                Toast.makeText(this, "Auth Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    // Unauthorized Phone Number block
                    Toast.makeText(this, "Access Denied: Mobile Number not pre-approved by Admin!", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener { exception ->
                // 🚀 ERROR TRACKING: Shows the exact error message from Firebase
                Toast.makeText(this, "DB Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}