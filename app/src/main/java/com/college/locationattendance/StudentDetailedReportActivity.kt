package com.college.locationattendance

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StudentDetailedReportActivity : AppCompatActivity() {

    private lateinit var tvReportStudentName: TextView
    private lateinit var llAttendanceRecords: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_detailed_report)

        tvReportStudentName = findViewById(R.id.tvReportStudentName)
        llAttendanceRecords = findViewById(R.id.llAttendanceRecords)

        val studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        val studentName = intent.getStringExtra("STUDENT_NAME") ?: "Unknown Student"

        tvReportStudentName.text = "Report: $studentName"

        if (studentId.isNotEmpty()) {
            fetchStudentAttendance(studentId)
        } else {
            Toast.makeText(this, "Error loading student details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchStudentAttendance(studentId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("Attendance").child(studentId)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                llAttendanceRecords.removeAllViews()
                var recordFound = false

                if (snapshot.exists()) {
                    for (dateSnapshot in snapshot.children) {
                        val date = dateSnapshot.key ?: ""

                        for (recordSnapshot in dateSnapshot.children) {

                            // 🚀 EXACT MATCH WITH YOUR FIREBASE STRUCTURE
                            val courseName = recordSnapshot.child("courseName").getValue(String::class.java)
                                ?: recordSnapshot.child("courseCode").getValue(String::class.java)
                                ?: "Subject Unknown"

                            val time = recordSnapshot.child("time").getValue(String::class.java) ?: ""

                            // Combine Course Name and Time nicely
                            val displaySubject = if (time.isNotEmpty()) {
                                "$courseName\n($time)"
                            } else {
                                courseName
                            }

                            recordFound = true
                            addRecordToView(date, displaySubject, "PRESENT")
                        }
                    }
                }

                if (!recordFound) {
                    addRecordToView("-", "No Records", "-")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@StudentDetailedReportActivity, "DB Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addRecordToView(date: String, subjectDetails: String, status: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            setPadding(0, 20, 0, 20)
        }

        val tvDate = TextView(this).apply {
            text = date
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvSubject = TextView(this).apply {
            text = subjectDetails
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 13f // Slightly adjusted to fit "Period 1 : Virtual Reality..."
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvStatus = TextView(this).apply {
            text = status
            setTextColor(Color.parseColor("#4CAF50"))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(tvDate)
        row.addView(tvSubject)
        row.addView(tvStatus)

        llAttendanceRecords.addView(row)

        val line = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(Color.parseColor("#222222"))
        }
        llAttendanceRecords.addView(line)
    }
}