package com.college.locationattendance

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UpdateTeacherProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_teacher_profile)

        val etTName = findViewById<EditText>(R.id.etTName)
        val etTDept = findViewById<EditText>(R.id.etTDept)
        val etTYear = findViewById<EditText>(R.id.etTYear)
        val etTSec = findViewById<EditText>(R.id.etTSec)
        val etTSem = findViewById<EditText>(R.id.etTSem)
        val btnSave = findViewById<Button>(R.id.btnSaveTeacherProfile)

        val user = FirebaseAuth.getInstance().currentUser
        val database = FirebaseDatabase.getInstance().getReference("Users")

        // Pre-fill existing data if any
        if (user != null) {
            database.child(user.uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    etTName.setText(snapshot.child("name").value?.toString() ?: "")
                    etTDept.setText(snapshot.child("department").value?.toString() ?: "")
                    etTYear.setText(snapshot.child("handlingYear").value?.toString() ?: "")
                    etTSec.setText(snapshot.child("section").value?.toString() ?: "")
                    etTSem.setText(snapshot.child("semester").value?.toString() ?: "")
                }
            }
        }

        btnSave.setOnClickListener {
            val name = etTName.text.toString().trim()
            val dept = etTDept.text.toString().trim()
            val year = etTYear.text.toString().trim()
            val sec = etTSec.text.toString().trim()
            val sem = etTSem.text.toString().trim()

            if (name.isEmpty() || dept.isEmpty() || year.isEmpty() || sec.isEmpty() || sem.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user != null) {
                val updates = mapOf(
                    "name" to name,
                    "department" to dept,
                    "handlingYear" to year,
                    "section" to sec,
                    "semester" to sem
                )

                database.child(user.uid).updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close page and go back to dashboard
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}