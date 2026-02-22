package com.example.canteengo.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.activities.admin.AdminDashboardActivity
import com.example.canteengo.activities.student.StudentHomeActivity
import com.example.canteengo.databinding.ActivityPhoneInputBinding
import com.example.canteengo.models.UserRole
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.RolePrefs
import com.example.canteengo.utils.hideKeyboard
import com.example.canteengo.utils.toast
import kotlinx.coroutines.launch

class PhoneInputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneInputBinding
    private val userRepository = UserRepository()

    companion object {
        const val EXTRA_ROLE = "extra_role"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinue.setOnClickListener {
            hideKeyboard()
            savePhoneNumber()
        }
    }

    private fun savePhoneNumber() {
        val mobile = binding.etMobile.text?.toString()?.trim().orEmpty()

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

                // Check if mobile number already exists
                val existingUser = userRepository.getUserByMobile(fullMobile)
                if (existingUser != null) {
                    binding.tilMobile.error = "This mobile number is already registered"
                    setLoading(false)
                    return@launch
                }

                // Update the user's mobile number
                userRepository.updateUserMobile(fullMobile)

                toast("Phone number saved!")

                // Navigate to the appropriate dashboard
                val role = RolePrefs.getSelectedRole(this@PhoneInputActivity)
                val destination = if (role == UserRole.ADMIN) {
                    AdminDashboardActivity::class.java
                } else {
                    StudentHomeActivity::class.java
                }

                startActivity(Intent(this@PhoneInputActivity, destination).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } catch (e: Exception) {
                toast(e.message ?: "Something went wrong")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnContinue.isEnabled = !loading
    }

    override fun onBackPressed() {
        // Don't allow going back without entering phone number
        toast("Please enter your mobile number to continue")
    }
}

