package com.example.canteengo.activities.student

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.R
import com.example.canteengo.activities.RoleSelectionActivity
import com.example.canteengo.activities.student.StudentSignupActivity
import com.example.canteengo.databinding.ActivityLoginBinding
import com.example.canteengo.models.StudentProfile
import com.example.canteengo.models.UserRole
import com.example.canteengo.repository.AuthRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.GoogleSignInHelper
import com.example.canteengo.utils.RolePrefs
import com.example.canteengo.utils.SimpleTextWatcher
import com.example.canteengo.utils.hideKeyboard
import com.example.canteengo.utils.toast
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.launch

class StudentLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()

    private val googleSignInHelper by lazy {
        GoogleSignInHelper(this, getString(R.string.default_web_client_id))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvTitle.text = getString(R.string.student)

        // Signup button
        binding.btnSignup.text = "New here? Create account"

        // Clear errors as user types.
        binding.etEmail.addTextChangedListener(SimpleTextWatcher { binding.tilEmail.error = null })
        binding.etPassword.addTextChangedListener(SimpleTextWatcher { binding.tilPassword.error = null })

        binding.btnLogin.setOnClickListener {
            hideKeyboard()
            doLogin()
        }

        // Keep btnSignup but make it open signup page.
        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this, StudentSignupActivity::class.java))
        }

        binding.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, StudentSignupActivity::class.java))
        }

        // Google Sign-In button
        val googleBtn = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGoogle)
        googleBtn.setOnClickListener {
            doGoogleSignIn()
        }

        binding.btnBack.setOnClickListener {
            RolePrefs.clear(this)
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
        }
    }

    private fun doLogin() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val pass = binding.etPassword.text?.toString()?.trim().orEmpty()
        if (!validate(email, pass)) return

        setLoading(true)
        lifecycleScope.launch {
            try {
                authRepo.signIn(email, pass)
                ensureRoleAndRoute()
            } catch (e: Exception) {
                toast(mapAuthError(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun doGoogleSignIn() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = googleSignInHelper.signIn()
                result.fold(
                    onSuccess = { _ ->
                        try {
                            // Check if user already has a complete profile with mobile number
                            val existingRole = userRepo.getCurrentUserRole()
                            val hasMobile = userRepo.hasUserMobile()

                            if (existingRole != null && hasMobile) {
                                // Existing user with complete profile - go to home
                                ensureRoleAndRoute()
                            } else {
                                // New user OR incomplete profile - redirect to collect all student details
                                // Don't create profile here - it will be created in GoogleSignupDetailsActivity
                                val intent = Intent(this@StudentLoginActivity, com.example.canteengo.activities.GoogleSignupDetailsActivity::class.java)
                                intent.putExtra(com.example.canteengo.activities.GoogleSignupDetailsActivity.EXTRA_IS_STUDENT, true)
                                intent.putExtra(com.example.canteengo.activities.GoogleSignupDetailsActivity.EXTRA_IS_NEW_USER, existingRole == null)
                                startActivity(intent)
                                finish()
                            }
                        } catch (e: Exception) {
                            toast("Error: ${e.message}")
                            setLoading(false)
                        }
                    },
                    onFailure = { e ->
                        toast(e.message ?: getString(R.string.something_went_wrong))
                        setLoading(false)
                    }
                )
            } catch (e: Exception) {
                toast("Google Sign-In error: ${e.message}")
                setLoading(false)
            }
        }
    }

    private fun validate(email: String, pass: String): Boolean {
        var ok = true
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (email.isBlank()) {
            binding.tilEmail.error = "Email is required"
            ok = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            ok = false
        }

        if (pass.isBlank()) {
            binding.tilPassword.error = "Password is required"
            ok = false
        } else if (pass.length < 6) {
            binding.tilPassword.error = "Minimum 6 characters"
            ok = false
        }

        return ok
    }

    private fun mapAuthError(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> "No account found for this email. Please sign up."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
            is FirebaseAuthUserCollisionException -> "This email is already registered. Please login instead."
            else -> e.message ?: getString(R.string.something_went_wrong)
        }
    }

    private suspend fun ensureRoleAndRoute() {
        RolePrefs.clear(this)
        startActivity(Intent(this, StudentHomeActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnSignup.isEnabled = !loading
    }
}
