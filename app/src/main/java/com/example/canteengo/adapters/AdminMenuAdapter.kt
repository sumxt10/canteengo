package com.example.canteengo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.canteengo.R
import com.example.canteengo.databinding.ItemAdminMenuBinding
import com.example.canteengo.models.MenuItem

class AdminMenuAdapter(
    private val onToggleAvailability: (MenuItem, Boolean) -> Unit,
    private val onEditClick: (MenuItem) -> Unit,
    private val onDeleteClick: (MenuItem) -> Unit
) : ListAdapter<MenuItem, AdminMenuAdapter.AdminMenuViewHolder>(AdminMenuDiffCallback()) {

    // Track which items are being toggled to prevent multiple rapid toggles
    private val togglingItems = mutableSetOf<String>()

    inner class AdminMenuViewHolder(
        private val binding: ItemAdminMenuBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuItem) {
            binding.tvName.text = item.name
            binding.tvDescription.text = item.description
            binding.tvPrice.text = "₹${item.price.toInt()}"
            binding.tvCategory.text = item.category.replace("_", " ")

            // Load image if available
            if (item.imageUrl.isNotEmpty()) {
                binding.ivItemImage.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_food_placeholder)
                    error(R.drawable.ic_food_placeholder)
                }
            } else {
                binding.ivItemImage.setImageResource(R.drawable.ic_food_placeholder)
            }

            // Set availability switch without triggering listener
            binding.switchAvailability.setOnCheckedChangeListener(null)
            binding.switchAvailability.isChecked = item.isAvailable
            binding.switchAvailability.isEnabled = !togglingItems.contains(item.id)

            binding.switchAvailability.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != item.isAvailable && !togglingItems.contains(item.id)) {
                    togglingItems.add(item.id)
                    binding.switchAvailability.isEnabled = false
                    onToggleAvailability(item, isChecked)
                }
            }

            // Veg/Non-veg badge
            if (item.isVeg) {
                binding.ivVegBadge.setImageResource(R.drawable.ic_veg_badge)
            } else {
                binding.ivVegBadge.setImageResource(R.drawable.ic_nonveg_badge)
            }

            binding.btnEdit.setOnClickListener { onEditClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    fun clearTogglingState(itemId: String) {
        togglingItems.remove(itemId)
    }

    fun clearAllTogglingStates() {
        togglingItems.clear()
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
        return oldItem.id == newItem.id &&
               oldItem.name == newItem.name &&
               oldItem.description == newItem.description &&
               oldItem.price == newItem.price &&
               oldItem.category == newItem.category &&
               oldItem.imageUrl == newItem.imageUrl &&
               oldItem.isVeg == newItem.isVeg &&
               oldItem.isAvailable == newItem.isAvailable
    }
}
