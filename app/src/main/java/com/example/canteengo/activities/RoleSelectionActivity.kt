package com.example.canteengo.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.canteengo.activities.admin.AdminLoginActivity
import com.example.canteengo.activities.student.StudentLoginActivity
import com.example.canteengo.databinding.ActivityRoleSelectionBinding
import com.example.canteengo.models.UserRole
import com.example.canteengo.utils.RolePrefs

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardStudent.setOnClickListener {
            RolePrefs.setSelectedRole(this, UserRole.STUDENT)
            startActivity(Intent(this, StudentLoginActivity::class.java))
        }

        binding.cardAdmin.setOnClickListener {
            RolePrefs.setSelectedRole(this, UserRole.ADMIN)
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }
    }
}
