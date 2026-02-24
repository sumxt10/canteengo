package com.example.canteengo.activities.student

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.databinding.ActivityEditStudentProfileBinding
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.toast
import kotlinx.coroutines.launch

class EditStudentProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditStudentProfileBinding
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditStudentProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadCurrentProfile()
        setupSaveButton()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun loadCurrentProfile() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val profile = userRepository.getCurrentStudentProfile()
                profile?.let {
                    binding.etLibraryCard.setText(it.libraryCardNumber)
                    binding.etDepartment.setText(it.department)
                    binding.etDivision.setText(it.division)
                }
            } catch (e: Exception) {
                toast("Failed to load profile")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val libraryCard = binding.etLibraryCard.text?.toString()?.trim().orEmpty()
            val department = binding.etDepartment.text?.toString()?.trim().orEmpty()
            val division = binding.etDivision.text?.toString()?.trim().orEmpty()

            saveProfile(libraryCard, department, division)
        }
    }

    private fun saveProfile(libraryCard: String, department: String, division: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                userRepository.updateStudentProfile(
                    libraryCardNumber = libraryCard,
                    department = department,
                    division = division
                )
                toast("Profile updated successfully")
                finish()
            } catch (e: Exception) {
                toast("Failed to update profile: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }
}

