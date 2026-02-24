package com.example.canteengo.activities.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canteengo.databinding.ActivityAdminOrderDetailsBinding
import com.example.canteengo.models.Order
import com.example.canteengo.models.OrderStatus
import com.example.canteengo.repository.OrderRepository
import com.example.canteengo.utils.toast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminOrderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminOrderDetailsBinding
    private val orderRepo = OrderRepository()
    private var orderId: String = ""
    private var currentOrder: Order? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getStringExtra("order_id") ?: run {
            toast("Order not found")
            finish()
            return
        }

        setupToolbar()
        setupActionButtons()
        observeOrder()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupActionButtons() {
        binding.btnAccept.setOnClickListener { updateStatus(OrderStatus.ACCEPTED) }
        binding.btnPreparing.setOnClickListener { updateStatus(OrderStatus.PREPARING) }
        binding.btnReady.setOnClickListener { updateStatus(OrderStatus.READY) }
        binding.btnComplete.setOnClickListener { updateStatus(OrderStatus.COLLECTED) }
        binding.btnReject.setOnClickListener { updateStatus(OrderStatus.REJECTED) }
    }

    private fun observeOrder() {
        lifecycleScope.launch {
            orderRepo.observeOrder(orderId).collectLatest { order ->
                if (order != null) {
                    currentOrder = order
                    displayOrderDetails(order)
                } else {
                    toast("Order not found")
                    finish()
                }
            }
        }
    }

    private fun displayOrderDetails(order: Order) {
        binding.tvToken.text = "#${order.token}"
        binding.tvStudentName.text = order.studentName
        binding.tvPickupTime.text = if (order.pickupTime == "ASAP") "ASAP (Priority)" else order.pickupTime

        // Format date
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        binding.tvOrderDate.text = dateFormat.format(Date(order.createdAt))

        // Status
        binding.tvStatus.text = order.status.displayName
        binding.tvStatus.setBackgroundColor(android.graphics.Color.parseColor(order.status.colorHex))

        // Items
        val itemsText = order.items.joinToString("\n") { item ->
            "${item.quantity}x ${item.name} - ₹${item.totalPrice.toInt()}"
        }
        binding.tvItems.text = itemsText

        // Bill summary
        binding.tvSubtotal.text = "₹${order.subtotal.toInt()}"
        binding.tvHandlingCharge.text = "₹${order.handlingCharge.toInt()}"
        binding.tvTotal.text = "₹${order.totalAmount.toInt()}"

        // Show/hide action buttons based on status
        binding.btnAccept.visibility = View.GONE
        binding.btnPreparing.visibility = View.GONE
        binding.btnReady.visibility = View.GONE
        binding.btnComplete.visibility = View.GONE
        binding.btnReject.visibility = View.GONE

        when (order.status) {
            OrderStatus.RECEIVED -> {
                binding.btnAccept.visibility = View.VISIBLE
                binding.btnReject.visibility = View.VISIBLE
            }
            OrderStatus.ACCEPTED -> {
                binding.btnPreparing.visibility = View.VISIBLE
            }
            OrderStatus.PREPARING -> {
                binding.btnReady.visibility = View.VISIBLE
            }
            OrderStatus.READY -> {
                binding.btnComplete.visibility = View.VISIBLE
            }
            else -> {
                // No actions for COLLECTED or REJECTED
            }
        }
    }

    private fun updateStatus(newStatus: OrderStatus) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                orderRepo.updateOrderStatus(orderId, newStatus)
                toast("Order updated to ${newStatus.displayName}")
            } catch (e: Exception) {
                toast("Failed to update: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}

