package com.example.canteengo.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.canteengo.databinding.ItemOrderBinding
import com.example.canteengo.models.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private val onOrderClick: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    inner class OrderViewHolder(
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.tvToken.text = "#${order.token}"
            binding.tvItemCount.text = "${order.items.size} items"
            binding.tvTotal.text = "₹${order.totalAmount.toInt()}"

            // Format date
            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(order.createdAt))

            // Status badge
            binding.tvStatus.text = order.status.displayName
            binding.tvStatus.setBackgroundColor(Color.parseColor(order.status.colorHex))

            binding.root.setOnClickListener {
                onOrderClick(order)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
    override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
        return oldItem.orderId == newItem.orderId
    }

    override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
        return oldItem == newItem
    }
}

