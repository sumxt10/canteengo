package com.example.canteengo.activities.student

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.canteengo.R
import com.example.canteengo.databinding.ActivityOrderTrackingBinding
import com.example.canteengo.models.Order
import com.example.canteengo.models.OrderStatus
import com.example.canteengo.repository.OrderRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OrderTrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderTrackingBinding
    private val orderRepository = OrderRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val orderId = intent.getStringExtra("orderId") ?: ""

        binding.btnBack.setOnClickListener { finish() }

        if (orderId.isNotEmpty()) {
            observeOrder(orderId)
        }
    }

    private fun observeOrder(orderId: String) {
        lifecycleScope.launch {
            orderRepository.observeOrder(orderId).collectLatest { order ->
                order?.let { updateUI(it) }
            }
        }
    }

    private fun updateUI(order: Order) {
        binding.tvToken.text = "#${order.token}"
        binding.tvPickupTime.text = "Pickup: ${order.pickupTime}"
        binding.tvTotal.text = "₹${order.totalAmount.toInt()}"

        // Update timeline
        updateTimeline(order.status)
    }

    private fun updateTimeline(status: OrderStatus) {
        val statusIndex = when (status) {
            OrderStatus.RECEIVED -> 0
            OrderStatus.ACCEPTED -> 1
            OrderStatus.PREPARING -> 2
            OrderStatus.READY -> 3
            OrderStatus.COLLECTED -> 4
            OrderStatus.REJECTED -> -1
        }

        // Update status indicators
        binding.statusReceived.isActivated = statusIndex >= 0
        binding.statusAccepted.isActivated = statusIndex >= 1
        binding.statusPreparing.isActivated = statusIndex >= 2
        binding.statusReady.isActivated = statusIndex >= 3
        binding.statusCollected.isActivated = statusIndex >= 4

        // Update lines
        binding.line1.isActivated = statusIndex >= 1
        binding.line2.isActivated = statusIndex >= 2
        binding.line3.isActivated = statusIndex >= 3
        binding.line4.isActivated = statusIndex >= 4

        // Update current status text
        binding.tvCurrentStatus.text = when (status) {
            OrderStatus.RECEIVED -> "Order Received"
            OrderStatus.ACCEPTED -> "Order Accepted"
            OrderStatus.PREPARING -> "Being Prepared"
            OrderStatus.READY -> "Ready for Pickup!"
            OrderStatus.COLLECTED -> "Collected"
            OrderStatus.REJECTED -> "Order Rejected"
        }

        binding.tvStatusMessage.text = when (status) {
            OrderStatus.RECEIVED -> "Your order is being reviewed by the canteen"
            OrderStatus.ACCEPTED -> "Great! The canteen has accepted your order"
            OrderStatus.PREPARING -> "Your food is being prepared with care"
            OrderStatus.READY -> "Head to the canteen counter with your token"
            OrderStatus.COLLECTED -> "Thank you! Enjoy your meal"
            OrderStatus.REJECTED -> "Unfortunately, your order was rejected"
        }
    }
}

