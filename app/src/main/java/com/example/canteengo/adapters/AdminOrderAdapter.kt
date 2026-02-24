package com.example.canteengo.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.canteengo.databinding.ItemAdminOrderBinding
import com.example.canteengo.models.Order
import com.example.canteengo.models.OrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminOrderAdapter(
    private val onItemClick: (Order) -> Unit,
    private val onAcceptClick: (Order) -> Unit,
    private val onPreparingClick: (Order) -> Unit,
    private val onReadyClick: (Order) -> Unit,
    private val onCompleteClick: (Order) -> Unit,
    private val onRejectClick: (Order) -> Unit
) : ListAdapter<Order, AdminOrderAdapter.AdminOrderViewHolder>(AdminOrderDiffCallback()) {

    inner class AdminOrderViewHolder(
        private val binding: ItemAdminOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.tvToken.text = "#${order.token}"
            binding.tvStudentName.text = order.studentName
            binding.tvItemCount.text = "${order.items.size} items"
            binding.tvTotal.text = "₹${order.totalAmount.toInt()}"
            binding.tvPickupTime.text = if (order.pickupTime == "ASAP") "ASAP" else order.pickupTime

            // Format date
            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(order.createdAt))

            // Status badge
            binding.tvStatus.text = order.status.displayName
            try {
                binding.tvStatus.setBackgroundColor(Color.parseColor(order.status.colorHex))
            } catch (e: Exception) {
                binding.tvStatus.setBackgroundColor(Color.GRAY)
            }

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

            // Click listeners
            binding.root.setOnClickListener { onItemClick(order) }
            binding.btnAccept.setOnClickListener { onAcceptClick(order) }
            binding.btnPreparing.setOnClickListener { onPreparingClick(order) }
            binding.btnReady.setOnClickListener { onReadyClick(order) }
            binding.btnComplete.setOnClickListener { onCompleteClick(order) }
            binding.btnReject.setOnClickListener { onRejectClick(order) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminOrderViewHolder {
        val binding = ItemAdminOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AdminOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminOrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class AdminOrderDiffCallback : DiffUtil.ItemCallback<Order>() {
    override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
        return oldItem.orderId == newItem.orderId
    }

    override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
        return oldItem == newItem
    }
}
