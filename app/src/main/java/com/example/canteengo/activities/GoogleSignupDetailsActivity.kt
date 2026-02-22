package com.example.canteengo.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.activities.admin.AdminDashboardActivity
import com.example.canteengo.activities.student.StudentHomeActivity
import com.example.canteengo.databinding.ActivityGoogleSignupDetailsBinding
import com.example.canteengo.models.AdminProfile
import com.example.canteengo.models.StudentProfile
import com.example.canteengo.repository.AuthRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.hideKeyboard
import com.example.canteengo.utils.toast
import kotlinx.coroutines.launch

class GoogleSignupDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleSignupDetailsBinding
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private var isStudent = true
    private var isNewUser = true

    companion object {
        const val EXTRA_IS_STUDENT = "extra_is_student"
        const val EXTRA_IS_NEW_USER = "extra_is_new_user"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleSignupDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isStudent = intent.getBooleanExtra(EXTRA_IS_STUDENT, true)
        isNewUser = intent.getBooleanExtra(EXTRA_IS_NEW_USER, true)

        // Show student-specific fields only for students
        if (isStudent) {
            binding.tilLibraryCard.visibility = View.VISIBLE
            binding.tilDepartment.visibility = View.VISIBLE
            binding.tilDivision.visibility = View.VISIBLE
            binding.tvTitle.text = "Complete Your Profile"
            binding.tvSubtitle.text = "Please provide your student details to continue"
        } else {
            binding.tilLibraryCard.visibility = View.GONE
            binding.tilDepartment.visibility = View.GONE
            binding.tilDivision.visibility = View.GONE
            binding.tvTitle.text = "Complete Your Profile"
            binding.tvSubtitle.text = "Please provide your phone number to continue"
        }

        binding.btnContinue.setOnClickListener {
            hideKeyboard()
            if (isStudent) {
                saveStudentDetails()
            } else {
                saveAdminDetails()
            }
        }
    }

    private fun saveStudentDetails() {
        val mobile = binding.etMobile.text?.toString()?.trim().orEmpty()
        val libraryCard = binding.etLibraryCard.text?.toString()?.trim().orEmpty()
        val department = binding.etDepartment.text?.toString()?.trim().orEmpty()
        val division = binding.etDivision.text?.toString()?.trim().orEmpty()

        // Clear previous errors
        binding.tilMobile.error = null
        binding.tilLibraryCard.error = null
        binding.tilDepartment.error = null
        binding.tilDivision.error = null

        var hasError = false

        if (mobile.isBlank()) {
            binding.tilMobile.error = "Mobile number is required"
            hasError = true
        } else if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
            binding.tilMobile.error = "Enter a valid 10-digit mobile number"
            hasError = true
        }

        if (libraryCard.isBlank()) {
            binding.tilLibraryCard.error = "Library card number is required"
            hasError = true
        }

        if (department.isBlank()) {
            binding.tilDepartment.error = "Department is required"
            hasError = true
        }

        if (division.isBlank()) {
            binding.tilDivision.error = "Division is required"
            hasError = true
        }

        if (hasError) return

        setLoading(true)
        lifecycleScope.launch {
            try {
                val fullMobile = "+91$mobile"

                // Check if mobile number already exists for another user
                val existingUser = userRepository.getUserByMobile(fullMobile)
                val currentUid = authRepository.currentUser()?.uid

                if (existingUser != null) {
                    val existingUid = existingUser["uid"] as? String
                    if (existingUid != null && existingUid != currentUid) {
                        binding.tilMobile.error = "This mobile number is already registered with another account"
                        setLoading(false)
                        return@launch
                    }
                }

                // Update the user's profile with all details
                userRepository.updateStudentDetails(
                    mobile = fullMobile,
                    libraryCardNumber = libraryCard,
                    department = department,
                    division = division
                )

                toast("Profile completed!")

                startActivity(Intent(this@GoogleSignupDetailsActivity, StudentHomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()

            } catch (e: Exception) {
                toast("Error: ${e.message}")
                setLoading(false)
            }
        }
    }

    private fun saveAdminDetails() {
        val mobile = binding.etMobile.text?.toString()?.trim().orEmpty()

        binding.tilMobile.error = null

        if (mobile.isBlank()) {
            binding.tilMobile.error = "Mobile number is required"
            return
        }

        if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
            binding.tilMobile.error = "Enter a valid 10-digit mobile number"
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val fullMobile = "+91$mobile"

                // Check if mobile number already exists for another user
                val existingUser = userRepository.getUserByMobile(fullMobile)
                val currentUid = authRepository.currentUser()?.uid

                if (existingUser != null) {
                    val existingUid = existingUser["uid"] as? String
                    if (existingUid != null && existingUid != currentUid) {
                        binding.tilMobile.error = "This mobile number is already registered with another account"
                        setLoading(false)
                        return@launch
                    }
                }

                // Update the user's mobile number
                userRepository.updateUserMobile(fullMobile)

                toast("Profile completed!")

                startActivity(Intent(this@GoogleSignupDetailsActivity, AdminDashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()

            } catch (e: Exception) {
                toast("Error: ${e.message}")
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnContinue.isEnabled = !loading
    }
}
