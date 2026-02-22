package com.example.canteengo.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.canteengo.databinding.ActivityGetStartedBinding
import com.example.canteengo.utils.OnboardingPrefs

class GetStartedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetStartedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinue.setOnClickListener {
            OnboardingPrefs.setGetStartedSeen(this, true)
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }
}

