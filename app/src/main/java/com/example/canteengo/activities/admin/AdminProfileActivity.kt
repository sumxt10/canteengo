package com.example.canteengo.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.R
import com.example.canteengo.activities.OnboardingActivity
import com.example.canteengo.databinding.ActivityAdminProfileBinding
import com.example.canteengo.models.CartManager
import com.example.canteengo.repository.AuthRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.CacheManager
import com.example.canteengo.utils.toast
import kotlinx.coroutines.launch

class AdminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminProfileBinding
    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBottomNav()

        // Show loading state initially
        showLoading(true)
        loadProfileData()

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                // Clear caches before logout
                CartManager.clear()
                CacheManager.clearAll()
                authRepo.signOut()
                toast("Logged out")
                startActivity(Intent(this@AdminProfileActivity, OnboardingActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_profile

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_orders -> {
                    startActivity(Intent(this, AdminOrdersActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_menu -> {
                    startActivity(Intent(this, AdminMenuActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun loadProfileData() {
        // First, show cached data immediately if available
        CacheManager.getAdminProfileEvenIfStale()?.let { profile ->
            binding.tvName.text = profile.name
            binding.tvEmail.text = profile.email
            binding.tvMobile.text = profile.mobile.ifEmpty { "Not provided" }
            binding.tvInitials.text = profile.name.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
            showLoading(false)
        }

        // Then refresh from Firestore in background
        lifecycleScope.launch {
            try {
                val profile = userRepo.getCurrentAdminProfile()
                profile?.let {
                    // Update cache
                    CacheManager.cacheAdminProfile(it)

                    // Update UI
                    binding.tvName.text = it.name
                    binding.tvEmail.text = it.email
                    binding.tvMobile.text = it.mobile.ifEmpty { "Not provided" }
                    binding.tvInitials.text = it.name.firstOrNull()?.uppercase() ?: "A"
                }

                showLoading(false)
            } catch (e: Exception) {
                showLoading(false)
                if (CacheManager.getAdminProfileEvenIfStale() == null) {
                    toast("Failed to load profile")
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.loadingContainer.visibility = if (loading) View.VISIBLE else View.GONE
        binding.contentScrollView.visibility = if (loading) View.GONE else View.VISIBLE
    }
}
