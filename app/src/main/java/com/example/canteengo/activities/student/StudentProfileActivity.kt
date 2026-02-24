package com.example.canteengo.activities.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.R
import com.example.canteengo.activities.OnboardingActivity
import com.example.canteengo.databinding.ActivityStudentProfileBinding
import com.example.canteengo.models.CartManager
import com.example.canteengo.repository.AuthRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.toast
import kotlinx.coroutines.launch

class StudentProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentProfileBinding
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBottomNav()
        setupEditProfile()
        setupLogout()

        // Show loading state initially
        showLoading(true)
        loadProfile()
    }

    override fun onResume() {
        super.onResume()
        // Reload profile when returning from edit screen
        loadProfile()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupEditProfile() {
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditStudentProfileActivity::class.java))
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_profile

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, StudentHomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_orders -> {
                    startActivity(Intent(this, StudentOrdersActivity::class.java))
                    false
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    false
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val profile = userRepository.getCurrentStudentProfile()
                profile?.let {
                    // Bind all values
                    binding.tvName.text = it.name
                    binding.tvEmail.text = it.email
                    binding.tvLibraryCard.text = it.libraryCardNumber.ifEmpty { "Not set" }
                    binding.tvDepartment.text = it.department.ifEmpty { "Not set" }
                    binding.tvDivision.text = it.division.ifEmpty { "Not set" }

                    // Set initials
                    val initials = it.name.split(" ")
                        .take(2)
                        .mapNotNull { word -> word.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                    binding.tvInitials.text = initials.ifEmpty { "S" }
                }

                // Show content after data is loaded
                showLoading(false)
            } catch (e: Exception) {
                // Show content even on error, with empty state
                showLoading(false)
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.loadingContainer.visibility = if (loading) View.VISIBLE else View.GONE
        binding.contentScrollView.visibility = if (loading) View.GONE else View.VISIBLE
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                // Clear cart before logout to prevent sharing between users
                CartManager.clear()
                authRepository.signOut()
                toast("Logged out successfully")
                val intent = Intent(this@StudentProfileActivity, OnboardingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}

