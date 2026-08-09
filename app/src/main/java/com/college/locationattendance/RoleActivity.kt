package com.college.locationattendance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class RoleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role)

        val btnRoleStudent = findViewById<Button>(R.id.btnRoleStudent)
        val btnRoleTeacher = findViewById<Button>(R.id.btnRoleTeacher)
        val btnBackToLogin = findViewById<Button>(R.id.btnBackToLogin)

        btnRoleStudent.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnRoleTeacher.setOnClickListener {
            startActivity(Intent(this, TeacherRegisterActivity::class.java))
        }

        btnBackToLogin.setOnClickListener {
            finish() // Goes back to Login page
        }
    }
}