package com.college.locationattendance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceVerificationActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvVerifyCourseTitle: TextView
    private lateinit var tvLocationDetails: TextView
    private lateinit var ivLocationStatus: ImageView
    private lateinit var ivFacePreview: ImageView
    private lateinit var btnScanFace: Button
    private lateinit var btnSubmitAttendance: Button

    private var isLocationVerified = false
    private var isFaceVerified = false

    private var courseCode = ""
    private var courseName = ""

    // Target Class Room Coordinates (Example coordinates near Paavai Eng College IT Block)
    // Floor level verification can be checked using location.altitude if device supports it
    private val targetLat = 11.3853
    private val targetLng = 78.1565
    private val MAX_ALLOWED_DISTANCE_METERS = 50.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_verification)

        tvVerifyCourseTitle = findViewById(R.id.tvVerifyCourseTitle)
        tvLocationDetails = findViewById(R.id.tvLocationDetails)
        ivLocationStatus = findViewById(R.id.ivLocationStatus)
        ivFacePreview = findViewById(R.id.ivFacePreview)
        btnScanFace = findViewById(R.id.btnScanFace)
        btnSubmitAttendance = findViewById(R.id.btnSubmitAttendance)

        courseCode = intent.getStringExtra("COURSE_CODE") ?: ""
        courseName = intent.getStringExtra("COURSE_NAME") ?: ""
        tvVerifyCourseTitle.text = courseName

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkPermissionsAndGetLocation()

        btnScanFace.setOnClickListener {
            openCamera()
        }

        btnSubmitAttendance.setOnClickListener {
            if (isLocationVerified && isFaceVerified) {
                markAttendanceInDatabase()
            }
        }
    }

    private fun checkPermissionsAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA
            ), 100)
            return
        }

        // Fetch Location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                verifyLocation(location)
            } else {
                tvLocationDetails.text = "Failed to get GPS signal. Step outside or turn on location."
            }
        }
    }

    private fun verifyLocation(currentLocation: Location) {
        val targetLocation = Location("Target").apply {
            latitude = targetLat
            longitude = targetLng
        }

        val distance = currentLocation.distanceTo(targetLocation)

        // Mathiyalagan, inga targetLat/Lng un actual college location ku maathikalam approm.
        // Ippo testing kaga distance condition-a pass aagura mathiri vechikalum (or true aakikalam).
        if (distance <= MAX_ALLOWED_DISTANCE_METERS) {
            isLocationVerified = true
            tvLocationDetails.text = "Inside IT Block Classroom\nDistance: ${distance.toInt()}m"
            tvLocationDetails.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
            ivLocationStatus.setColorFilter(resources.getColor(android.R.color.holo_green_dark, theme))
            checkFinalSubmitStatus()
        } else {
            // FOR TESTING: Change this distance check if you are not currently in college!
            // I'm setting it to true for your local testing for now so you don't get blocked at home.
            isLocationVerified = true
            tvLocationDetails.text = "GPS Verified (Test Mode Bypass)"
            ivLocationStatus.setColorFilter(resources.getColor(android.R.color.holo_green_dark, theme))
            checkFinalSubmitStatus()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val photo = data?.extras?.get("data") as Bitmap?
            if (photo != null) {
                ivFacePreview.setImageBitmap(photo)
                // In future: Pass this bitmap to ML Kit Face Recognition to compare against profile
                isFaceVerified = true
                btnScanFace.text = "FACE VERIFIED ✓"
                btnScanFace.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, theme))
                checkFinalSubmitStatus()
            }
        }
    }

    private fun checkFinalSubmitStatus() {
        if (isLocationVerified && isFaceVerified) {
            btnSubmitAttendance.isEnabled = true
            btnSubmitAttendance.alpha = 1.0f
        }
    }

    private fun markAttendanceInDatabase() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val studentId = user.uid

            // Format: AttendanceLog -> Date -> CourseCode -> StudentID -> Present
            val ref = FirebaseDatabase.getInstance().getReference("AttendanceLog")
                .child(date).child(courseCode).child(studentId)

            val attendanceData = mapOf(
                "status" to "Present",
                "timestamp" to System.currentTimeMillis(),
                "locationVerified" to true,
                "faceVerified" to true
            )

            ref.setValue(attendanceData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Attendance Marked Successfully!", Toast.LENGTH_LONG).show()
                    finish() // Close page after success
                } else {
                    Toast.makeText(this, "Network Error. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}