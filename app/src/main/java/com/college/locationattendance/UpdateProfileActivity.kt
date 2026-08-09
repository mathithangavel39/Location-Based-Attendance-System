package com.college.locationattendance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class UpdateProfileActivity : AppCompatActivity() {

    private lateinit var ivProfilePreview: ImageView
    private lateinit var progressUpload: ProgressBar
    private lateinit var btnSaveProfile: Button

    private var selectedImageUri: Uri? = null

    // Modern Launcher to pick image from Gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            Glide.with(this)
                .load(selectedImageUri)
                .circleCrop()
                .into(ivProfilePreview)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_profile)

        ivProfilePreview = findViewById(R.id.ivUpdateProfilePicPreview)
        val tvChangePhoto = findViewById<TextView>(R.id.tvChangePhoto)
        progressUpload = findViewById(R.id.progressUpload)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etDepartment = findViewById<EditText>(R.id.etDepartment)
        val etYear = findViewById<EditText>(R.id.etYear)
        val etSection = findViewById<EditText>(R.id.etSection)
        val etSemester = findViewById<EditText>(R.id.etSemester)

        // Load existing data when page opens
        loadExistingUserProfile(etFullName, etDepartment, etYear, etSection, etSemester)

        // Click listeners for photo upload
        tvChangePhoto.setOnClickListener { openGalleryIntent() }
        ivProfilePreview.setOnClickListener { openGalleryIntent() }

        // Save Button Click
        btnSaveProfile.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val dept = etDepartment.text.toString().trim()

            if (name.isEmpty() || dept.isEmpty()) {
                Toast.makeText(this, "Name and Department are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val dbMap = mutableMapOf(
                    "name" to name,
                    "department" to dept,
                    "year" to etYear.text.toString().trim(),
                    "section" to etSection.text.toString().trim(),
                    "semester" to etSemester.text.toString().trim()
                )

                if (selectedImageUri != null) {
                    uploadImageToStorageAndSaveAllDetails(user.uid, selectedImageUri!!, dbMap)
                } else {
                    saveProfileToDatabase(user.uid, dbMap)
                }
            }
        }
    }

    private fun loadExistingUserProfile(name: EditText, dept: EditText, year: EditText, section: EditText, sem: EditText) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(user.uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    name.setText(snapshot.child("name").getValue(String::class.java))
                    dept.setText(snapshot.child("department").getValue(String::class.java))
                    year.setText(snapshot.child("year").getValue(String::class.java))
                    section.setText(snapshot.child("section").getValue(String::class.java))
                    sem.setText(snapshot.child("semester").getValue(String::class.java))

                    val photoUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this@UpdateProfileActivity).load(photoUrl).circleCrop().into(ivProfilePreview)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun openGalleryIntent() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun uploadImageToStorageAndSaveAllDetails(uid: String, imageUri: Uri, dbMap: MutableMap<String, String>) {
        progressUpload.visibility = View.VISIBLE
        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "UPLOADING PHOTO..."

        val storageRef = FirebaseStorage.getInstance().reference.child("ProfileImages").child("$uid.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    dbMap["profileImageUrl"] = uri.toString()
                    saveProfileToDatabase(uid, dbMap)
                }
            }
            .addOnFailureListener { e ->
                progressUpload.visibility = View.GONE
                btnSaveProfile.isEnabled = true
                btnSaveProfile.text = "SAVE CHANGES"
                Toast.makeText(this, "Image Upload Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfileToDatabase(uid: String, dbMap: Map<String, String>) {
        progressUpload.visibility = View.VISIBLE
        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "SAVING DETAILS..."

        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
        dbRef.updateChildren(dbMap)
            .addOnCompleteListener { task ->
                progressUpload.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close page and return to dashboard
                } else {
                    btnSaveProfile.isEnabled = true
                    btnSaveProfile.text = "SAVE CHANGES"
                    Toast.makeText(this, "Database Update Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }
}