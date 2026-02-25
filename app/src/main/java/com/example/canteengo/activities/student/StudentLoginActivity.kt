package com.example.canteengo.activities.student

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.R
import com.example.canteengo.activities.RoleSelectionActivity
import com.example.canteengo.databinding.ActivityLoginBinding
import com.example.canteengo.models.UserRole
import com.example.canteengo.repository.AuthRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.GoogleSignInHelper
import com.example.canteengo.utils.NetworkUtils
import com.example.canteengo.utils.RolePrefs
import com.example.canteengo.utils.SimpleTextWatcher
import com.example.canteengo.utils.hideKeyboard
import com.example.canteengo.utils.toast
import com.google.firebase.auth.FirebaseAuth
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

        // Check network connectivity first
        if (!NetworkUtils.isNetworkAvailable(this)) {
            toast(NetworkUtils.NO_INTERNET_MESSAGE)
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                authRepo.signIn(email, pass)

                // Validate role after successful authentication
                val userRole = userRepo.getCurrentUserRole()

                if (userRole == null) {
                    // No role found - sign out and show error
                    FirebaseAuth.getInstance().signOut()
                    toast("Account not found. Please sign up first.")
                    setLoading(false)
                    return@launch
                }

                if (userRole != UserRole.STUDENT) {
                    // Wrong role - sign out and show error
                    FirebaseAuth.getInstance().signOut()
                    toast("This account is not registered as a student. Please use the admin login.")
                    setLoading(false)
                    return@launch
                }

                // Role validation passed - proceed to dashboard
                navigateToHome()
            } catch (e: Exception) {
                val errorMessage = if (NetworkUtils.isNetworkError(e)) {
                    NetworkUtils.NO_INTERNET_MESSAGE
                } else {
                    mapAuthError(e)
                }
                toast(errorMessage)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun doGoogleSignIn() {
        // Check network connectivity first
        if (!NetworkUtils.isNetworkAvailable(this)) {
            toast(NetworkUtils.NO_INTERNET_MESSAGE)
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = googleSignInHelper.signIn()
                result.fold(
                    onSuccess = { _ ->
                        try {
                            // Optimized: Single Firestore read to get both role and mobile status
                            val profileStatus = userRepo.getUserProfileStatus()

                            // Validate role for existing users
                            if (profileStatus.role != null && profileStatus.role != UserRole.STUDENT) {
                                // Wrong role - sign out and show error
                                FirebaseAuth.getInstance().signOut()
                                toast("This account is not registered as a student. Please use the admin login.")
                                setLoading(false)
                                return@fold
                            }

                            if (profileStatus.role != null && profileStatus.hasCompletedProfile) {
                                // Existing user with complete profile - go directly to home
                                navigateToHome()
                            } else {
                                // New user OR incomplete profile - redirect to collect all student details
                                navigateToCompleteProfile(isNewUser = profileStatus.role == null)
                            }
                        } catch (e: Exception) {
                            val errorMessage = if (NetworkUtils.isNetworkError(e)) {
                                NetworkUtils.NO_INTERNET_MESSAGE
                            } else {
                                "Error: ${e.message}"
                            }
                            toast(errorMessage)
                            setLoading(false)
                        }
                    },
                    onFailure = { e ->
                        val errorMessage = if (NetworkUtils.isNetworkError(e)) {
                            NetworkUtils.NO_INTERNET_MESSAGE
                        } else {
                            e.message ?: getString(R.string.something_went_wrong)
                        }
                        toast(errorMessage)
                        setLoading(false)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = if (NetworkUtils.isNetworkError(e)) {
                    NetworkUtils.NO_INTERNET_MESSAGE
                } else {
                    "Google Sign-In error: ${e.message}"
                }
                toast(errorMessage)
                setLoading(false)
            }
        }
    }

    private fun navigateToHome() {
        RolePrefs.clear(this)
        startActivity(Intent(this, StudentHomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun navigateToCompleteProfile(isNewUser: Boolean) {
        val intent = Intent(this, com.example.canteengo.activities.GoogleSignupDetailsActivity::class.java)
        intent.putExtra(com.example.canteengo.activities.GoogleSignupDetailsActivity.EXTRA_IS_STUDENT, true)
        intent.putExtra(com.example.canteengo.activities.GoogleSignupDetailsActivity.EXTRA_IS_NEW_USER, isNewUser)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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


    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnSignup.isEnabled = !loading
    }
}
