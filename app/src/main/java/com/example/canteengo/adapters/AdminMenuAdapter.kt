package com.example.canteengo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.canteengo.databinding.ItemAdminMenuBinding
import com.example.canteengo.models.MenuItem

class AdminMenuAdapter(
    private val onToggleAvailability: (MenuItem, Boolean) -> Unit,
    private val onEditClick: (MenuItem) -> Unit,
    private val onDeleteClick: (MenuItem) -> Unit
) : ListAdapter<MenuItem, AdminMenuAdapter.AdminMenuViewHolder>(AdminMenuDiffCallback()) {

    inner class AdminMenuViewHolder(
        private val binding: ItemAdminMenuBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuItem) {
            binding.tvName.text = item.name
            binding.tvDescription.text = item.description
            binding.tvPrice.text = "₹${item.price.toInt()}"
            binding.tvCategory.text = item.category.replace("_", " ")

            // Set availability switch without triggering listener
            binding.switchAvailability.setOnCheckedChangeListener(null)
            binding.switchAvailability.isChecked = item.isAvailable
            binding.switchAvailability.setOnCheckedChangeListener { _, isChecked ->
                onToggleAvailability(item, isChecked)
            }

            // Veg/Non-veg badge
            if (item.isVeg) {
                binding.ivVegBadge.setImageResource(com.example.canteengo.R.drawable.ic_veg_badge)
            } else {
                binding.ivVegBadge.setImageResource(com.example.canteengo.R.drawable.ic_nonveg_badge)
            }

            binding.btnEdit.setOnClickListener { onEditClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminMenuViewHolder {
        val binding = ItemAdminMenuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AdminMenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminMenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class AdminMenuDiffCallback : DiffUtil.ItemCallback<MenuItem>() {
    override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
        return oldItem == newItem
    }
}
