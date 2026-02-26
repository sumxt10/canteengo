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
import com.example.canteengo.databinding.DialogRatingBinding
import com.example.canteengo.databinding.ItemOrderDetailBinding
import com.example.canteengo.models.Order
import com.example.canteengo.models.OrderItem
import com.example.canteengo.models.OrderStatus
import com.example.canteengo.repository.OrderRepository
import com.example.canteengo.repository.RatingRepository
import com.example.canteengo.utils.toast
import com.google.android.material.bottomsheet.BottomSheetDialog
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
    private val ratingRepo = RatingRepository()
    private var orderId: String = ""
    private var currentOrder: Order? = null
    private var existingRatings: Map<String, Int> = emptyMap()

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
        currentOrder = order

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

        // Load existing ratings if order is completed
        if (order.status == OrderStatus.COLLECTED) {
            loadExistingRatings(order)
        } else {
            // Items without rating support
            binding.rvItems.adapter = OrderItemsAdapter(order.items, showRating = false)
        }

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

    private fun loadExistingRatings(order: Order) {
        lifecycleScope.launch {
            try {
                existingRatings = ratingRepo.getRatingsForOrder(order.orderId)
                binding.rvItems.adapter = OrderItemsAdapter(order.items, showRating = true)
            } catch (e: Exception) {
                binding.rvItems.adapter = OrderItemsAdapter(order.items, showRating = true)
            }
        }
    }

    private fun showRatingDialog(item: OrderItem) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogRatingBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvItemName.text = item.name

        // Set existing rating if available
        val existingRating = existingRatings[item.menuItemId]
        if (existingRating != null) {
            dialogBinding.sliderRating.value = existingRating.toFloat()
            dialogBinding.tvSelectedRating.text = existingRating.toString()
            dialogBinding.btnSubmitRating.text = "Update Rating"
        }

        // Update displayed rating when slider changes
        dialogBinding.sliderRating.addOnChangeListener { _, value, _ ->
            dialogBinding.tvSelectedRating.text = value.toInt().toString()
        }

        dialogBinding.btnSubmitRating.setOnClickListener {
            val rating = dialogBinding.sliderRating.value.toInt()
            submitRating(item, rating, dialog)
        }

        dialogBinding.btnCancelRating.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun submitRating(item: OrderItem, rating: Int, dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            try {
                ratingRepo.submitRating(orderId, item.menuItemId, rating)
                toast("Rating submitted successfully!")
                dialog.dismiss()

                // Refresh existing ratings
                currentOrder?.let { loadExistingRatings(it) }
            } catch (e: Exception) {
                toast("Failed to submit rating: ${e.message}")
            }
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

    // Adapter for order items with optional rating support
    inner class OrderItemsAdapter(
        private val items: List<OrderItem>,
        private val showRating: Boolean = false
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

            // Show rating section only for completed orders
            if (showRating) {
                holder.binding.ratingSection.visibility = View.VISIBLE

                val existingRating = existingRatings[item.menuItemId]
                if (existingRating != null) {
                    holder.binding.tvExistingRating.text = "$existingRating/10"
                    holder.binding.tvExistingRating.visibility = View.VISIBLE
                    holder.binding.tvYourRating.visibility = View.VISIBLE
                    holder.binding.btnRate.text = "Edit"
                } else {
                    holder.binding.tvExistingRating.visibility = View.GONE
                    holder.binding.tvYourRating.text = "Not rated yet"
                    holder.binding.btnRate.text = "Rate"
                }

                holder.binding.btnRate.setOnClickListener {
                    showRatingDialog(item)
                }
            } else {
                holder.binding.ratingSection.visibility = View.GONE
            }
        }

        override fun getItemCount() = items.size
    }
}

