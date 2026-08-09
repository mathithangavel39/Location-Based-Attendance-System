package com.college.locationattendance

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PeriodListActivity : AppCompatActivity() {

    private lateinit var periodsContainer: LinearLayout
    private lateinit var tvDayTitle: TextView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_timetable)

        periodsContainer = findViewById(R.id.periodsContainer)
        tvDayTitle = findViewById(R.id.tvDayTitle)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        val dayKey = intent.getStringExtra("DAY_KEY") ?: "Day1"
        tvDayTitle.text = "$dayKey - Schedule"

        loadPeriodsFromFirebase(dayKey)
    }

    private fun loadPeriodsFromFirebase(dayKey: String) {
        val ref = FirebaseDatabase.getInstance().getReference("Timetable").child(dayKey)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                periodsContainer.removeAllViews()

                if (snapshot.exists()) {
                    var periodNumber = 1

                    for (periodSnapshot in snapshot.children) {
                        // DB-la irunthu ellam details-aiyum edukkurom (Lat/Lng sethu)
                        val courseName = periodSnapshot.child("courseName").getValue(String::class.java) ?: ""
                        val courseCode = periodSnapshot.child("courseCode").getValue(String::class.java) ?: ""
                        val facultyName = periodSnapshot.child("facultyName").getValue(String::class.java) ?: ""
                        val startTime = periodSnapshot.child("startTime").getValue(String::class.java) ?: ""
                        val endTime = periodSnapshot.child("endTime").getValue(String::class.java) ?: ""

                        // 🚀 Fetch Latitude and Longitude (default to class anchor if null)
                        val latitude = periodSnapshot.child("latitude").getValue(Double::class.java) ?: 11.399772
                        val longitude = periodSnapshot.child("longitude").getValue(Double::class.java) ?: 78.157484

                        val periodTitle = "Period $periodNumber"

                        // 🚀 Ippo 8 parameters-ayum theliva anuppurom
                        addPeriodCard(periodTitle, courseName, courseCode, facultyName, startTime, endTime, latitude, longitude)

                        periodNumber++
                    }
                } else {
                    Toast.makeText(this@PeriodListActivity, "No schedule found for $dayKey", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PeriodListActivity, "Database Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Function definition eppadiyo apdiye irukku
    private fun addPeriodCard(periodTitle: String, courseName: String, courseCode: String, facultyName: String, startTime: String, endTime: String, latitude: Double, longitude: Double) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_period, periodsContainer, false)

        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val tvSubject = view.findViewById<TextView>(R.id.tvSubject)

        tvTime.text = "$startTime - $endTime"
        tvSubject.text = "$periodTitle : $courseName" // Puthusa UI-la course name kaatren

        view.setOnClickListener {
            val intent = Intent(this@PeriodListActivity, FaceAIActivity::class.java)
            intent.putExtra("COURSE_NAME", "$periodTitle : $courseName")
            intent.putExtra("COURSE_CODE", courseCode)
            intent.putExtra("FACULTY_NAME", facultyName)
            intent.putExtra("TIME", "$startTime - $endTime")

            // 🚀 DYNAMIC Lat/Lng-a FaceAIActivity-kku anupurom!
            intent.putExtra("LATITUDE", latitude)
            intent.putExtra("LONGITUDE", longitude)
            startActivity(intent)
        }

        periodsContainer.addView(view)
    }
}