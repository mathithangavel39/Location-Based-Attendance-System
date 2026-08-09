package com.college.locationattendance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayoutTeacher: DrawerLayout
    private lateinit var ivTeacherMenu: ImageView
    private lateinit var llStudentContainer: LinearLayout

    // Drawer Views
    private lateinit var tvTName: TextView
    private lateinit var tvTDept: TextView
    private lateinit var tvTClass: TextView
    private lateinit var tvTSec: TextView
    private lateinit var tvTSem: TextView

    // 🚀 MASTER FIX: Changed Button to ImageView here!
    private lateinit var btnUpdateTeacherProfile: ImageView
    private lateinit var btnTeacherLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        drawerLayoutTeacher = findViewById(R.id.drawerLayoutTeacher)
        ivTeacherMenu = findViewById(R.id.ivTeacherMenu)
        llStudentContainer = findViewById(R.id.llStudentContainer)

        tvTName = findViewById(R.id.tvTName)
        tvTDept = findViewById(R.id.tvTDept)
        tvTClass = findViewById(R.id.tvTClass)
        tvTSec = findViewById(R.id.tvTSec)
        tvTSem = findViewById(R.id.tvTSem)

        btnUpdateTeacherProfile = findViewById(R.id.btnUpdateTeacherProfile)
        btnTeacherLogout = findViewById(R.id.btnTeacherLogout)

        ivTeacherMenu.setOnClickListener {
            drawerLayoutTeacher.openDrawer(GravityCompat.START)
        }

        btnUpdateTeacherProfile.setOnClickListener {
            drawerLayoutTeacher.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, UpdateTeacherProfileActivity::class.java))
        }

        btnTeacherLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            // Clear saved local memory so next user has to login
            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            prefs.edit().clear().apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Fetch all registered students
        loadStudentList()
    }

    override fun onResume() {
        super.onResume()
        loadTeacherProfile()
    }

    private fun loadStudentList() {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                llStudentContainer.removeAllViews() // Clear old data

                for (userSnapshot in snapshot.children) {
                    val role = userSnapshot.child("role").getValue(String::class.java) ?: ""
                    if (role == "Student") {
                        val name = userSnapshot.child("name").getValue(String::class.java) ?: "Unnamed Student"
                        val rollNo = userSnapshot.child("rollNumber").getValue(String::class.java) ?: ""
                        val studentId = userSnapshot.key ?: ""

                        // DYNAMIC BUTTON CREATION FOR EACH STUDENT
                        val btnStudent = Button(this@TeacherDashboardActivity).apply {
                            text = if (rollNo.isNotEmpty()) "$name - $rollNo" else name
                            isAllCaps = false
                            textSize = 16f
                            setTextColor(Color.WHITE)
                            setBackgroundColor(Color.parseColor("#3F51B5"))
                            gravity = Gravity.CENTER
                        }

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            160 // Height of button
                        )
                        params.setMargins(0, 0, 0, 30) // Margin between buttons
                        btnStudent.layoutParams = params

                        // ON CLICK -> Opens Full Detailed Report for that specific student
                        btnStudent.setOnClickListener {
                            Toast.makeText(this@TeacherDashboardActivity, "Opening Report for $name", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this@TeacherDashboardActivity, StudentDetailedReportActivity::class.java)
                            intent.putExtra("STUDENT_ID", studentId)
                            intent.putExtra("STUDENT_NAME", name)
                            startActivity(intent)
                        }

                        llStudentContainer.addView(btnStudent)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadTeacherProfile() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val ref = FirebaseDatabase.getInstance().getReference("Users").child(user.uid)
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        tvTName.text = "Name: ${snapshot.child("name").getValue(String::class.java) ?: "N/A"}"
                        tvTDept.text = "Dept: ${snapshot.child("department").getValue(String::class.java) ?: "N/A"}"
                        tvTClass.text = "Handling Year: ${snapshot.child("handlingYear").getValue(String::class.java) ?: "N/A"}"
                        tvTSec.text = "Section: ${snapshot.child("section").getValue(String::class.java) ?: "N/A"}"
                        tvTSem.text = "Semester: ${snapshot.child("semester").getValue(String::class.java) ?: "N/A"}"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}