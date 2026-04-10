package com.example.canteengo.activities.student

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.databinding.ActivityPickupTimeBinding
import com.example.canteengo.models.CartManager
import com.example.canteengo.models.Order
import com.example.canteengo.models.OrderItem
import com.example.canteengo.repository.OrderRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.LocationGatekeeper
import com.example.canteengo.utils.toast
import kotlinx.coroutines.launch

class PickupTimeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPickupTimeBinding
    private val orderRepository = OrderRepository()
    private val userRepository = UserRepository()

    private var selectedTime = "ASAP"
    private var isLoading = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            verifyLocationAndPlaceOrder()
        } else {
            val canAskAgain = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

            if (canAskAgain) {
                toast("Location permission is required to place order near canteen")
            } else {
                toast("Location permission is permanently denied. Enable it from app settings.")
                openAppSettings()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickupTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTimeOptions()
        setupConfirmButton()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupTimeOptions() {
        binding.cardAsap.setOnClickListener {
            selectedTime = "ASAP"
            updateSelection()
        }

        binding.card15Min.setOnClickListener {
            selectedTime = "15 mins"
            updateSelection()
        }

        binding.card30Min.setOnClickListener {
            selectedTime = "30 mins"
            updateSelection()
        }

        binding.card45Min.setOnClickListener {
            selectedTime = "45 mins"
            updateSelection()
        }

        updateSelection()
    }

    private fun updateSelection() {
        binding.cardAsap.isChecked = selectedTime == "ASAP"
        binding.card15Min.isChecked = selectedTime == "15 mins"
        binding.card30Min.isChecked = selectedTime == "30 mins"
        binding.card45Min.isChecked = selectedTime == "45 mins"
    }

    private fun setupConfirmButton() {
        binding.btnConfirm.setOnClickListener {
            if (isLoading) return@setOnClickListener
            verifyLocationAndPlaceOrder()
        }
    }

    private fun verifyLocationAndPlaceOrder() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                checkOrderRadiusAndPlace()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                toast("Please allow location access to confirm order near canteen")
                requestLocationPermissions()
            }

            else -> {
                requestLocationPermissions()
            }
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun checkOrderRadiusAndPlace() {
        when (val result = LocationGatekeeper.verifyStudentWithinOrderRadius(this)) {
            is LocationGatekeeper.Result.Allowed -> placeOrder()
            is LocationGatekeeper.Result.TooFar -> {
                toast("You are ${result.distanceMeters.toInt()}m away. Order allowed only within 100m of canteen.")
            }
            LocationGatekeeper.Result.PermissionMissing -> {
                toast("Location permission is required to place order")
            }
            LocationGatekeeper.Result.LocationUnavailable -> {
                toast("Could not fetch your location. Please enable GPS and try again.")
            }
        }
    }

    private fun placeOrder() {
        isLoading = true
        binding.btnConfirm.isEnabled = false
        binding.btnConfirm.text = "Placing Order..."

        lifecycleScope.launch {
            try {
                val profile = userRepository.getCurrentStudentProfile()

                val orderItems = CartManager.items.map { cartItem ->
                    OrderItem(
                        menuItemId = cartItem.menuItem.id,
                        name = cartItem.menuItem.name,
                        price = cartItem.menuItem.price,
                        quantity = cartItem.quantity,
                        totalPrice = cartItem.totalPrice
                    )
                }

                val order = Order(
                    studentId = profile?.uid ?: "",
                    studentName = profile?.name ?: "Student",
                    items = orderItems,
                    subtotal = CartManager.subtotal,
                    handlingCharge = CartManager.handlingCharge,
                    totalAmount = CartManager.total,
                    pickupTime = selectedTime
                )

                val placedOrder = orderRepository.placeOrder(order)

                // Clear cart after successful order
                CartManager.clear()

                // Navigate to success screen
                val intent = Intent(this@PickupTimeActivity, OrderSuccessActivity::class.java)
                intent.putExtra("token", placedOrder.token)
                intent.putExtra("qrString", placedOrder.qrString)
                intent.putExtra("orderId", placedOrder.orderId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                toast("Failed to place order: ${e.message}")
                isLoading = false
                binding.btnConfirm.isEnabled = true
                binding.btnConfirm.text = "Confirm Order"
            }
        }
    }
}
