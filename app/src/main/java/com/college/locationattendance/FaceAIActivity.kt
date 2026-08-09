package com.college.locationattendance

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

class FaceAIActivity : AppCompatActivity() {

    private lateinit var ivFacePreview: ImageView
    private lateinit var btnScanFace: Button
    private lateinit var tvVerificationStatus: TextView
    private lateinit var btnSubmitAttendance: Button
    private lateinit var tvWindowTime: TextView
    private lateinit var tvGpsStatus: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val ALLOWED_RADIUS_METERS = 50.0f
    private var isGpsVerified = false

    private var classLatitude: Double = 0.0
    private var classLongitude: Double = 0.0

    private var registeredPhotoUrl: String = ""
    private var dbFaceSignature1: Float? = null
    private var dbFaceSignature2: Float? = null

    private var currentSafeNodeKey: String = ""
    private var activeTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_ai)

        ivFacePreview = findViewById(R.id.ivFacePreview)
        btnScanFace = findViewById(R.id.btnScanFace)
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus)
        btnSubmitAttendance = findViewById(R.id.btnSubmitAttendance)
        tvWindowTime = findViewById(R.id.tvWindowTime)
        tvGpsStatus = findViewById(R.id.tvGpsStatus)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val courseName = intent.getStringExtra("COURSE_NAME") ?: "Unknown Subject"
        val courseCodeRaw = intent.getStringExtra("COURSE_CODE") ?: "UNKNOWN_CODE"
        val facultyName = intent.getStringExtra("FACULTY_NAME") ?: "Unknown Faculty"
        val timeString = intent.getStringExtra("TIME") ?: "00:00 - 00:00"

        currentSafeNodeKey = courseCodeRaw.replace(Regex("[.#$\\[\\]/]"), "")

        findViewById<TextView>(R.id.tvCourseTitle).text = courseName
        findViewById<TextView>(R.id.tvCourseDetails).text = "$courseCodeRaw | $timeString"
        findViewById<TextView>(R.id.tvFacultyName).text = "Faculty: $facultyName"

        calculateGracePeriod(timeString)

        btnScanFace.isEnabled = false
        btnScanFace.alpha = 0.5f

        classLatitude = intent.getDoubleExtra("LATITUDE", 11.398781)
        classLongitude = intent.getDoubleExtra("LONGITUDE", 78.162111)

        checkLocationPermissionAndFetch()
        fetchRegisteredFaceData()

        btnScanFace.setOnClickListener {
            if (!isGpsVerified) {
                Toast.makeText(this, "GPS Verification Pending! Please go inside the classroom.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (dbFaceSignature1 != null) {
                checkCameraPermission()
            } else {
                Toast.makeText(this, "Securing network validation, please wait...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndResumeTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeTimer?.cancel()
    }

    private fun checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
        } else {
            fetchLiveLocation()
        }
    }

    private fun fetchLiveLocation() {
        tvGpsStatus.text = "Tracking live coordinates..."
        tvGpsStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(classLatitude, classLongitude, location.latitude, location.longitude, results)
                    val distanceInMeters = results[0]

                    if (distanceInMeters <= ALLOWED_RADIUS_METERS) {
                        isGpsVerified = true
                        tvGpsStatus.text = "GPS Verified ✓\nYou are inside the classroom zone."
                        tvGpsStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                    } else {
                        isGpsVerified = false
                        val distFormatted = String.format("%.2f", distanceInMeters)
                        tvGpsStatus.text = "OUT OF RANGE ❌\nPlease go inside the classroom. You are $distFormatted meters away."
                        tvGpsStatus.setTextColor(Color.RED)
                    }
                } else {
                    tvGpsStatus.text = "Location sensor error! Turn on high-accuracy GPS."
                    tvGpsStatus.setTextColor(Color.RED)
                }
            }
        } catch (e: SecurityException) {
            tvGpsStatus.text = "Critical: GPS Permission Denied."
        }
    }

    private fun calculateGracePeriod(timeString: String) {
        try {
            val startTime = timeString.split(" - ")[0].trim()
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = sdf.parse(startTime)
            if (date != null) {
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.add(Calendar.MINUTE, 5)
                val endTimeGrace = sdf.format(calendar.time)
                tvWindowTime.text = "Time: $startTime to $endTimeGrace"
            }
        } catch (e: Exception) {
            tvWindowTime.text = "Time Window Lock Enabled"
        }
    }

    private fun fetchRegisteredFaceData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val ref = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.uid)
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChild("profileImageUrl")) {
                        registeredPhotoUrl = snapshot.child("profileImageUrl").value.toString()
                        downloadAndAnalyzeDbPhoto(registeredPhotoUrl)
                    } else {
                        tvVerificationStatus.text = "Security Warning: No reference image found in profile!"
                        tvVerificationStatus.setTextColor(Color.RED)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun downloadAndAnalyzeDbPhoto(urlString: String) {
        Thread {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val bitmap = BitmapFactory.decodeStream(connection.inputStream)

                if (bitmap != null) {
                    runOnUiThread {
                        extractMultiPointSignatures(bitmap) { sig1, sig2 ->
                            dbFaceSignature1 = sig1
                            dbFaceSignature2 = sig2
                            if (dbFaceSignature1 != null && dbFaceSignature2 != null) {
                                tvVerificationStatus.text = "Profile Biometrics Ready! Scan your face."
                                tvVerificationStatus.setTextColor(Color.WHITE)
                                btnScanFace.isEnabled = true
                                btnScanFace.alpha = 1.0f
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }.start()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 102)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLiveLocation()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 102 && resultCode == RESULT_OK) {
            val livePhoto = data?.extras?.get("data") as Bitmap?
            if (livePhoto != null) {
                ivFacePreview.setImageBitmap(livePhoto)
                processLiveCameraPhoto(livePhoto)
            }
        }
    }

    // 🚀 MASTER FIX: Relaxed Threshold (0.30f) to stop Mismatches
    private fun processLiveCameraPhoto(liveBitmap: Bitmap) {
        tvVerificationStatus.text = "Matching identity matrix against server locks..."
        tvVerificationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))

        extractMultiPointSignatures(liveBitmap) { liveSig1, liveSig2 ->
            if (liveSig1 != null && liveSig2 != null && dbFaceSignature1 != null && dbFaceSignature2 != null) {
                val diff1 = abs(liveSig1 - dbFaceSignature1!!)
                val diff2 = abs(liveSig2 - dbFaceSignature2!!)
                val STRICT_THRESHOLD = 0.30f

                if (diff1 <= STRICT_THRESHOLD && diff2 <= STRICT_THRESHOLD) {
                    faceVerifiedSuccessfully()
                } else {
                    val d1Str = String.format("%.3f", diff1)
                    val d2Str = String.format("%.3f", diff2)
                    tvVerificationStatus.text = "IDENTITY MISMATCH!\nEyes Diff: $d1Str | Mouth Diff: $d2Str\n(Keep phone straight at eye level)"
                    tvVerificationStatus.setTextColor(Color.RED)
                    btnScanFace.text = "RETRY SCAN"
                    btnScanFace.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                    btnSubmitAttendance.isEnabled = false
                    btnSubmitAttendance.alpha = 0.5f
                }
            } else {
                tvVerificationStatus.text = "Authentication Error: Geometric points not captured. Bring phone closer."
                tvVerificationStatus.setTextColor(Color.RED)
            }
        }
    }

    private fun extractMultiPointSignatures(bitmap: Bitmap, callback: (Float?, Float?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).build()
        val detector = FaceDetection.getClient(options)
        detector.process(image).addOnSuccessListener { faces ->
            if (faces.size == 1) {
                val face = faces[0]
                val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
                val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

                if (leftEye != null && rightEye != null && nose != null && mouthLeft != null && mouthRight != null) {
                    val eyeDistance = hypot((leftEye.x - rightEye.x).toDouble(), (leftEye.y - rightEye.y).toDouble())
                    val midEyeX = (leftEye.x + rightEye.x) / 2
                    val midEyeY = (leftEye.y + rightEye.y) / 2
                    val noseHeight = hypot((midEyeX - nose.x).toDouble(), (midEyeY - nose.y).toDouble())
                    val signature1 = (eyeDistance / noseHeight).toFloat()
                    val mouthWidth = hypot((mouthLeft.x - mouthRight.x).toDouble(), (mouthLeft.y - mouthRight.y).toDouble())
                    val signature2 = (mouthWidth / noseHeight).toFloat()
                    callback(signature1, signature2)
                } else { callback(null, null) }
            } else { callback(null, null) }
        }.addOnFailureListener { callback(null, null) }
    }

    private fun faceVerifiedSuccessfully() {
        tvVerificationStatus.text = "IDENTITY VERIFIED ✓\nAccess Granted"
        tvVerificationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        btnScanFace.isEnabled = false
        btnScanFace.text = "VERIFIED"
        btnScanFace.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        btnSubmitAttendance.isEnabled = true
        btnSubmitAttendance.alpha = 1.0f
        btnSubmitAttendance.setOnClickListener { uploadAttendanceToFirebase() }
    }

    private fun uploadAttendanceToFirebase() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            btnSubmitAttendance.text = "CONNECTING TO SECURE NODE..."
            btnSubmitAttendance.isEnabled = false

            val calendar = Calendar.getInstance()
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val courseNameRaw = intent.getStringExtra("COURSE_NAME") ?: "Unknown"
            val courseCodeRaw = intent.getStringExtra("COURSE_CODE") ?: "UNKNOWN_CODE"

            val attendanceRef = FirebaseDatabase.getInstance().getReference("Attendance")
                .child(user.uid).child(currentDate).child(currentSafeNodeKey)

            val attendanceData = mapOf(
                "courseName" to courseNameRaw,
                "courseCode" to courseCodeRaw,
                "time" to (intent.getStringExtra("TIME") ?: ""),
                "status" to "In Process",
                "timestamp" to System.currentTimeMillis(),
                "gpsLatitude" to classLatitude,
                "gpsLongitude" to classLongitude
            )

            attendanceRef.setValue(attendanceData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startInProcessCountdown(currentSafeNodeKey, 30000)
                } else {
                    Toast.makeText(this, "Network error! Sync failed.", Toast.LENGTH_SHORT).show()
                    btnSubmitAttendance.text = "MARK ATTENDANCE"
                    btnSubmitAttendance.isEnabled = true
                }
            }
        }
    }

    // 🚀 MASTER FIX: Instant update using updateChildren & Auto-Close Disabled
    private fun startInProcessCountdown(safeNodeKey: String, millisRemaining: Long) {
        activeTimer?.cancel()

        btnSubmitAttendance.isEnabled = false
        btnSubmitAttendance.setBackgroundColor(Color.parseColor("#FF9800"))

        val prefs = getSharedPreferences("AttendanceTimerPrefs", Context.MODE_PRIVATE)

        if (millisRemaining == 30000L) {
            val targetEndTime = System.currentTimeMillis() + 30000
            prefs.edit().putLong("TIMER_END_$safeNodeKey", targetEndTime).apply()
        }

        activeTimer = object : CountDownTimer(millisRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                btnSubmitAttendance.text = "IN PROCESS (${secondsRemaining}s)"
            }

            override fun onFinish() {
                prefs.edit().remove("TIMER_END_$safeNodeKey").apply()

                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

                    val attendanceRef = FirebaseDatabase.getInstance().getReference("Attendance")
                        .child(user.uid).child(currentDate).child(safeNodeKey)

                    val updateData = mapOf("status" to "Present")

                    // Direct node optimization to avoid background freezes
                    attendanceRef.updateChildren(updateData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            btnSubmitAttendance.setBackgroundColor(Color.parseColor("#388E3C"))
                            btnSubmitAttendance.text = "ATTENDANCE MARKED SUCCESSFULLY ✓"
                            Toast.makeText(this@FaceAIActivity, "Success! Status updated to PRESENT.", Toast.LENGTH_SHORT).show()

                            // 🚀 Button resets after 20 seconds. Clean execution without killing the view state.
                            btnSubmitAttendance.postDelayed({
                                btnSubmitAttendance.isEnabled = true
                                btnSubmitAttendance.alpha = 1.0f
                                btnSubmitAttendance.setBackgroundColor(Color.parseColor("#388E3C"))
                                btnSubmitAttendance.text = "MARK ATTENDANCE"
                            }, 20000)
                        } else {
                            btnSubmitAttendance.setBackgroundColor(Color.RED)
                            btnSubmitAttendance.text = "NETWORK ERROR"
                            Toast.makeText(this@FaceAIActivity, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }.start()
    }

    private fun checkAndResumeTimer() {
        if (currentSafeNodeKey.isEmpty()) return

        val prefs = getSharedPreferences("AttendanceTimerPrefs", Context.MODE_PRIVATE)
        val savedEndTime = prefs.getLong("TIMER_END_$currentSafeNodeKey", 0L)

        if (savedEndTime > 0) {
            val currentTime = System.currentTimeMillis()

            if (currentTime < savedEndTime) {
                val remainingTime = savedEndTime - currentTime
                btnScanFace.isEnabled = false
                btnScanFace.text = "VERIFIED"
                btnScanFace.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                tvVerificationStatus.text = "IDENTITY VERIFIED ✓\nAccess Granted"
                tvVerificationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

                startInProcessCountdown(currentSafeNodeKey, remainingTime)
            } else {
                prefs.edit().remove("TIMER_END_$currentSafeNodeKey").apply()
                startInProcessCountdown(currentSafeNodeKey, 1000)
            }
        }
    }
}