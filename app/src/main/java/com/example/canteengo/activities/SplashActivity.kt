package com.example.canteengo.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.activities.admin.AdminDashboardActivity
import com.example.canteengo.activities.student.StudentHomeActivity
import com.example.canteengo.databinding.ActivitySplashBinding
import com.example.canteengo.models.UserRole
import com.example.canteengo.repository.AuthRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.OnboardingPrefs
import com.example.canteengo.utils.toast
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    // Lazily create repositories so FirebaseAuth/Firestore aren't touched during Activity instantiation.
    private val authRepo by lazy { AuthRepository() }
    private val userRepo by lazy { UserRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Light splash with immediate routing.
        lifecycleScope.launch {
            route()
        }
    }

    private suspend fun route() {
        val user = authRepo.currentUser()
        if (user == null) {
            val next = if (OnboardingPrefs.hasSeenGetStarted(this)) {
                OnboardingActivity::class.java
            } else {
                GetStartedActivity::class.java
            }
            startActivity(Intent(this, next))
            finish()
            return
        }

        try {
            val role = userRepo.getCurrentUserRole()
            when (role) {
                UserRole.STUDENT -> {
                    startActivity(Intent(this, StudentHomeActivity::class.java))
                    finish()
                }

                UserRole.ADMIN -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                }

                null -> {
                    // Incomplete profile or missing Firestore doc.
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                    finish()
                }
            }
        } catch (e: Exception) {
            toast(getString(com.example.canteengo.R.string.something_went_wrong))
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }
}
