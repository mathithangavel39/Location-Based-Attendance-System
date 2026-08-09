package com.college.locationattendance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var ivMenu: ImageView
    private lateinit var navView: NavigationView
    private lateinit var pieChartAttendance: PieChart
    private lateinit var ivEditProfile: ImageView
    private lateinit var btnLogout: Button

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileDept: TextView
    private lateinit var tvProfileYear: TextView
    private lateinit var tvProfileSection: TextView
    private lateinit var tvProfileSemester: TextView
    private lateinit var ivNavProfilePic: ImageView

    private lateinit var tvOverallPercentage: TextView
    private lateinit var subjectListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        drawerLayout = findViewById(R.id.drawerLayout)
        ivMenu = findViewById(R.id.ivMenu)
        navView = findViewById(R.id.navView)
        pieChartAttendance = findViewById(R.id.pieChartAttendance)
        ivEditProfile = findViewById(R.id.ivEditProfile)
        btnLogout = findViewById(R.id.btnLogout)

        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileDept = findViewById(R.id.tvProfileDept)
        tvProfileYear = findViewById(R.id.tvProfileYear)
        tvProfileSection = findViewById(R.id.tvProfileSection)
        tvProfileSemester = findViewById(R.id.tvProfileSemester)
        ivNavProfilePic = findViewById(R.id.ivNavProfilePic)

        tvOverallPercentage = findViewById(R.id.tvOverallPercentage)
        subjectListContainer = findViewById(R.id.subjectListContainer)

        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        setupPieChartDesign()

        ivEditProfile.setOnClickListener {
            val intent = Intent(this@MainActivity, UpdateProfileActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnDay1).setOnClickListener { forceOpenDaySchedule("Day1") }
        findViewById<Button>(R.id.btnDay2).setOnClickListener { forceOpenDaySchedule("Day2") }
        findViewById<Button>(R.id.btnDay3).setOnClickListener { forceOpenDaySchedule("Day3") }
        findViewById<Button>(R.id.btnDay4).setOnClickListener { forceOpenDaySchedule("Day4") }
        findViewById<Button>(R.id.btnDay5).setOnClickListener { forceOpenDaySchedule("Day5") }
        findViewById<Button>(R.id.btnDay6).setOnClickListener { forceOpenDaySchedule("Day6") }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadUserProfileData()
        fetchAttendanceData()
    }

    private fun loadUserProfileData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val ref = FirebaseDatabase.getInstance().getReference("Users").child(user.uid)
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        tvProfileName.text = "Name: ${snapshot.child("name").getValue(String::class.java) ?: "N/A"}"
                        tvProfileDept.text = "Dept: ${snapshot.child("department").getValue(String::class.java) ?: "N/A"}"
                        tvProfileYear.text = "Year: ${snapshot.child("year").getValue(String::class.java) ?: "N/A"}"
                        tvProfileSection.text = "Section: ${snapshot.child("section").getValue(String::class.java) ?: "N/A"}"
                        tvProfileSemester.text = "Semester: ${snapshot.child("semester").getValue(String::class.java) ?: "N/A"}"

                        val photoUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                        if (photoUrl.isNotEmpty()) {
                            Glide.with(this@MainActivity).load(photoUrl).circleCrop().into(ivNavProfilePic)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun forceOpenDaySchedule(dayKeyStr: String) {
        val intent = Intent(this@MainActivity, PeriodListActivity::class.java)
        intent.putExtra("DAY_KEY", dayKeyStr)
        startActivity(intent)
    }

    private fun setupPieChartDesign() {
        pieChartAttendance.isDrawHoleEnabled = true
        pieChartAttendance.setHoleColor(Color.parseColor("#121212"))
        pieChartAttendance.setTransparentCircleAlpha(0)
        pieChartAttendance.centerText = "Overall\nAttendance"
        pieChartAttendance.setCenterTextColor(Color.WHITE)
        pieChartAttendance.setCenterTextSize(14f)
        pieChartAttendance.description.isEnabled = false
        pieChartAttendance.legend.isEnabled = false // 🚀 Hide legend since we have the list below
    }

    private fun fetchAttendanceData() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val timetableRef = FirebaseDatabase.getInstance().getReference("Timetable")
        val attendanceRef = FirebaseDatabase.getInstance().getReference("Attendance").child(user.uid)

        timetableRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(timetableSnapshot: DataSnapshot) {
                val subjectAttendanceMap = HashMap<String, Int>()

                for (daySnapshot in timetableSnapshot.children) {
                    for (periodSnapshot in daySnapshot.children) {
                        val cName = periodSnapshot.child("courseName").getValue(String::class.java)
                        if (!cName.isNullOrEmpty()) {
                            subjectAttendanceMap[cName.trim()] = 0
                        }
                    }
                }

                attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(attSnapshot: DataSnapshot) {
                        var totalPresent = 0f
                        var totalAbsent = 0f

                        for (dateSnapshot in attSnapshot.children) {
                            for (subjectSnapshot in dateSnapshot.children) {
                                val rawSavedName = subjectSnapshot.child("courseName").getValue(String::class.java) ?: ""
                                val status = subjectSnapshot.child("status").getValue(String::class.java)

                                val cleanSubjectName = if (rawSavedName.contains(" : ")) {
                                    rawSavedName.substringAfter(" : ").trim()
                                } else {
                                    rawSavedName.trim()
                                }

                                if (!subjectAttendanceMap.containsKey(cleanSubjectName) && cleanSubjectName.isNotEmpty()) {
                                    subjectAttendanceMap[cleanSubjectName] = 0
                                }

                                when (status) {
                                    "Present" -> {
                                        totalPresent++
                                        if (cleanSubjectName.isNotEmpty()) {
                                            subjectAttendanceMap[cleanSubjectName] = subjectAttendanceMap[cleanSubjectName]!! + 1
                                        }
                                    }
                                    "Absent" -> totalAbsent++
                                }
                            }
                        }

                        // 🚀 MASTER SORT: Alphabetical sort guarantees colors match perfectly
                        val sortedSubjectMap = subjectAttendanceMap.toSortedMap()

                        updatePieChart(sortedSubjectMap)
                        buildSubjectList(sortedSubjectMap)

                        val totalTracked = totalPresent + totalAbsent
                        if (totalTracked > 0) {
                            val overallPercent = (totalPresent / totalTracked) * 100
                            tvOverallPercentage.text = "Overall Attendance: ${String.format("%.1f", overallPercent)}%"
                            if(overallPercent >= 75) tvOverallPercentage.setTextColor(Color.parseColor("#4CAF50"))
                            else tvOverallPercentage.setTextColor(Color.parseColor("#F44336"))
                        } else {
                            tvOverallPercentage.text = "Overall Attendance: 0.0%"
                            tvOverallPercentage.setTextColor(Color.parseColor("#A0A0A0"))
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🚀 NEW: Subject-Wise Multi-Colored Pie Chart
    private fun updatePieChart(sortedSubjectMap: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        val colorsList = ArrayList<Int>()

        // Expanded color palette for multiple subjects
        val colorsHex = arrayOf("#00BCD4", "#9C27B0", "#FFC107", "#E91E63", "#3F51B5", "#8BC34A", "#FF5722", "#795548", "#009688", "#4CAF50")
        var colorIndex = 0
        var hasData = false

        for ((subjectName, presentCount) in sortedSubjectMap) {
            if (subjectName.isEmpty()) continue

            // Only add to PieChart if attended at least once
            if (presentCount > 0) {
                entries.add(PieEntry(presentCount.toFloat(), "")) // Empty string keeps the pie chart ring clean
                colorsList.add(Color.parseColor(colorsHex[colorIndex % colorsHex.size]))
                hasData = true
            }
            // Increase index regardless of count to keep color sync with the list below
            colorIndex++
        }

        // If completely empty (no attendance marked yet)
        if (!hasData) {
            entries.add(PieEntry(1f, ""))
            colorsList.add(Color.DKGRAY)
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colorsList
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        pieChartAttendance.data = data
        pieChartAttendance.invalidate()
        pieChartAttendance.animateY(1000)
    }

    private fun buildSubjectList(sortedSubjectMap: Map<String, Int>) {
        subjectListContainer.removeAllViews()

        val colorsHex = arrayOf("#00BCD4", "#9C27B0", "#FFC107", "#E91E63", "#3F51B5", "#8BC34A", "#FF5722", "#795548", "#009688", "#4CAF50")
        var colorIndex = 0

        for ((cleanSubjectName, presentCount) in sortedSubjectMap) {
            if (cleanSubjectName.isEmpty()) continue

            val MAX_PERIODS = 45.0
            val percentage = (presentCount / MAX_PERIODS) * 100
            val colorStr = colorsHex[colorIndex % colorsHex.size]

            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 15, 0, 15)
            }

            val colorDot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(40, 40).apply {
                    setMargins(0, 15, 20, 0)
                }
                setBackgroundColor(Color.parseColor(colorStr))
            }

            val tvDetails = TextView(this).apply {
                text = "$cleanSubjectName\nAttended: $presentCount / 45 (${String.format("%.1f", percentage)}%)"
                setTextColor(Color.WHITE)
                textSize = 16f
            }

            rowLayout.addView(colorDot)
            rowLayout.addView(tvDetails)
            subjectListContainer.addView(rowLayout)

            colorIndex++
        }
    }
}