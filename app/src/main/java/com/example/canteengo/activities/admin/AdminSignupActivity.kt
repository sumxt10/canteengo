package com.example.canteengo.activities.admin

import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.R
import com.example.canteengo.databinding.ActivityAdminSignupBinding
import com.example.canteengo.models.AdminProfile
import com.example.canteengo.repository.AuthRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.SimpleTextWatcher
import com.example.canteengo.utils.hideKeyboard
import com.example.canteengo.utils.toast
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.launch

class AdminSignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSignupBinding

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etName.addTextChangedListener(SimpleTextWatcher { binding.tilName.error = null })
        binding.etMobile.addTextChangedListener(SimpleTextWatcher { binding.tilMobile.error = null })
        binding.etEmail.addTextChangedListener(SimpleTextWatcher { binding.tilEmail.error = null })
        binding.etPassword.addTextChangedListener(SimpleTextWatcher { binding.tilPassword.error = null })

        binding.btnBackToLogin.setOnClickListener { finish() }

        binding.btnCreateAccount.setOnClickListener {
            hideKeyboard()
            signUp()
        }
    }

    private fun signUp() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val mobile = binding.etMobile.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val pass = binding.etPassword.text?.toString()?.trim().orEmpty()

        if (!validate(name, mobile, email, pass)) return

        setLoading(true)
        lifecycleScope.launch {
            try {
                val fullMobile = "+91$mobile"

                // Check if mobile number already exists
                val existingUser = userRepo.getUserByMobile(fullMobile)
                if (existingUser != null) {
                    binding.tilMobile.error = "This mobile number is already registered"
                    setLoading(false)
                    return@launch
                }

                authRepo.signUp(email, pass)
                val user = authRepo.currentUser() ?: error("Signup failed")

                userRepo.createAdminProfile(
                    AdminProfile(
                        uid = user.uid,
                        name = name,
                        email = email,
                        mobile = fullMobile,
                    )
                )

                toast("Admin account created. Please login.")
                authRepo.signOut()
                finish()
            } catch (e: Exception) {
                val msg = when (e) {
                    is FirebaseAuthUserCollisionException -> "This email is already registered. Please login."
                    else -> e.message ?: getString(R.string.something_went_wrong)
                }
                toast(msg)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun validate(name: String, mobile: String, email: String, pass: String): Boolean {
        var ok = true
        binding.tilName.error = null
        binding.tilMobile.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (name.isBlank()) {
            binding.tilName.error = "Name is required"
            ok = false
        }
        if (mobile.isBlank()) {
            binding.tilMobile.error = "Mobile number is required"
            ok = false
        } else if (mobile.length != 10) {
            binding.tilMobile.error = "Enter valid 10-digit mobile number"
            ok = false
        }
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

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnCreateAccount.isEnabled = !loading
    }
}

