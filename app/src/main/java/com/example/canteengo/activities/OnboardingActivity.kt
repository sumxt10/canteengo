package com.example.canteengo.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.canteengo.R
import com.example.canteengo.adapters.OnboardingAdapter
import com.example.canteengo.adapters.OnboardingPage
import com.example.canteengo.databinding.ActivityOnboardingBinding
import com.example.canteengo.utils.OnboardingPrefs

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var indicators: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!OnboardingPrefs.hasSeenGetStarted(this)) {
            startActivity(Intent(this, GetStartedActivity::class.java))
            finish()
            return
        }

        indicators = listOf(binding.indicator1, binding.indicator2, binding.indicator3)

        val pages = listOf(
            OnboardingPage(
                "Browse Menu",
                "Explore delicious food items from your college canteen. Filter by categories like Snacks, South Indian, Beverages & more!",
                R.drawable.ic_onboarding_browse
            ),
            OnboardingPage(
                "Quick Ordering",
                "Add items to cart, choose pickup time - ASAP or schedule for later. No payment gateway hassles, pay at counter!",
                R.drawable.ic_onboarding_order
            ),
            OnboardingPage(
                "Track & Collect",
                "Get a unique token with QR code. Track your order status in real-time: Received → Preparing → Ready → Collected",
                R.drawable.ic_onboarding_track
            )
        )

        val adapter = OnboardingAdapter(pages)
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
                updateButtonText(position, pages.size)
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.size - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                navigateToRoleSelection()
            }
        }

        binding.btnSkip.setOnClickListener {
            navigateToRoleSelection()
        }
    }

    private fun updateIndicators(position: Int) {
        indicators.forEachIndexed { index, view ->
            if (index == position) {
                view.setBackgroundResource(R.drawable.indicator_active)
                view.layoutParams.width = resources.getDimensionPixelSize(R.dimen.indicator_active_width)
            } else {
                view.setBackgroundResource(R.drawable.indicator_inactive)
                view.layoutParams.width = resources.getDimensionPixelSize(R.dimen.indicator_inactive_width)
            }
            view.requestLayout()
        }
    }

    private fun updateButtonText(position: Int, total: Int) {
        binding.btnNext.text = if (position == total - 1) "Get Started" else "Next"
    }

    private fun navigateToRoleSelection() {
        startActivity(Intent(this, RoleSelectionActivity::class.java))
        finish()
    }
}
