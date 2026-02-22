package com.example.canteengo.activities.student

import android.content.Intent
import android.os.Bundle
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
        loadProfile()
        setupLogout()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
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
            } catch (e: Exception) {
                // Handle error
            }
        }
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

