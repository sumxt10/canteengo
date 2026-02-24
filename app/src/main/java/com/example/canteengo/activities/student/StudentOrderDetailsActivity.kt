package com.example.canteengo.activities.student

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.canteengo.databinding.ActivityStudentOrderDetailsBinding
import com.example.canteengo.databinding.ItemOrderDetailBinding
import com.example.canteengo.models.Order
import com.example.canteengo.models.OrderItem
import com.example.canteengo.models.OrderStatus
import com.example.canteengo.repository.OrderRepository
import com.example.canteengo.utils.toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentOrderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentOrderDetailsBinding
    private val orderRepo = OrderRepository()
    private var orderId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getStringExtra("orderId") ?: run {
            toast("Order not found")
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        observeOrder()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        binding.rvItems.apply {
            layoutManager = LinearLayoutManager(this@StudentOrderDetailsActivity)
        }
    }

    private fun observeOrder() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            orderRepo.observeOrder(orderId).collectLatest { order ->
                binding.progressBar.visibility = View.GONE
                if (order != null) {
                    displayOrderDetails(order)
                } else {
                    toast("Order not found")
                    finish()
                }
            }
        }
    }

    private fun displayOrderDetails(order: Order) {
        // Token
        binding.tvToken.text = "#${order.token}"

        // Status
        binding.tvStatus.text = order.status.displayName
        binding.tvStatus.setBackgroundColor(Color.parseColor(order.status.colorHex))

        // Date
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        binding.tvOrderDate.text = dateFormat.format(Date(order.createdAt))

        // Pickup time
        binding.tvPickupTime.text = if (order.pickupTime == "ASAP") "Pickup: ASAP (Priority)" else "Pickup: ${order.pickupTime}"

        // Show admin phone if order is accepted and admin phone is available
        if (order.status != OrderStatus.RECEIVED && order.acceptedByAdminPhone.isNotEmpty()) {
            binding.acceptedByContainer.visibility = View.VISIBLE
            binding.tvAdminPhone.text = order.acceptedByAdminPhone

            binding.btnCallCanteen.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${order.acceptedByAdminPhone}")
                }
                startActivity(intent)
            }
        } else {
            binding.acceptedByContainer.visibility = View.GONE
        }

        // QR Code
        binding.tvQrString.text = order.qrString
        generateQrCode(order.qrString)

        // Items
        binding.rvItems.adapter = OrderItemsAdapter(order.items)

        // Bill summary
        binding.tvSubtotal.text = "₹${order.subtotal.toInt()}"
        binding.tvHandlingCharge.text = "₹${order.handlingCharge.toInt()}"
        binding.tvTotal.text = "₹${order.totalAmount.toInt()}"

        // Track order button
        binding.btnTrackOrder.setOnClickListener {
            val intent = Intent(this, OrderTrackingActivity::class.java)
            intent.putExtra("orderId", order.orderId)
            startActivity(intent)
        }
    }

    private fun generateQrCode(content: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            binding.ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // QR generation failed, leave placeholder
        }
    }

    // Simple adapter for order items
    inner class OrderItemsAdapter(
        private val items: List<OrderItem>
    ) : RecyclerView.Adapter<OrderItemsAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemOrderDetailBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemOrderDetailBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvItemName.text = item.name
            holder.binding.tvQuantity.text = "x${item.quantity}"
            holder.binding.tvPrice.text = "₹${item.totalPrice.toInt()}"
        }

        override fun getItemCount() = items.size
    }
}

