package com.example.canteengo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.canteengo.R
import com.example.canteengo.databinding.ItemMenuFoodBinding
import com.example.canteengo.models.CartManager
import com.example.canteengo.models.MenuItem

class MenuItemAdapter(
    private val onAddClick: (MenuItem) -> Unit,
    private val onItemClick: (MenuItem) -> Unit
) : ListAdapter<MenuItem, MenuItemAdapter.MenuItemViewHolder>(MenuItemDiffCallback()) {

    inner class MenuItemViewHolder(
        private val binding: ItemMenuFoodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuItem) {
            binding.tvName.text = item.name
            binding.tvDescription.text = item.description
            binding.tvPrice.text = "₹${item.price.toInt()}"

            // Load image if available
            if (item.imageUrl.isNotEmpty()) {
                binding.ivFoodImage.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_food_item_placeholder)
                    error(R.drawable.ic_food_item_placeholder)
                }
            } else {
                binding.ivFoodImage.setImageResource(R.drawable.ic_food_item_placeholder)
            }

            // Veg/Non-veg indicator
            binding.ivVegBadge.setImageResource(
                if (item.isVeg) R.drawable.ic_veg_badge else R.drawable.ic_nonveg_badge
            )

            // Check if item is in cart
            val quantity = CartManager.getQuantity(item.id)
            if (quantity > 0) {
                binding.btnAdd.visibility = View.GONE
                binding.quantityContainer.visibility = View.VISIBLE
                binding.tvQuantity.text = quantity.toString()
            } else {
                binding.btnAdd.visibility = View.VISIBLE
                binding.quantityContainer.visibility = View.GONE
            }

            binding.btnAdd.setOnClickListener {
                onAddClick(item)
                notifyItemChanged(adapterPosition)
            }

            binding.btnMinus.setOnClickListener {
                CartManager.decrementQuantity(item.id)
                notifyItemChanged(adapterPosition)
            }

            binding.btnPlus.setOnClickListener {
                CartManager.incrementQuantity(item.id)
                notifyItemChanged(adapterPosition)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }

            // Availability
            binding.root.alpha = if (item.isAvailable) 1f else 0.5f
            binding.btnAdd.isEnabled = item.isAvailable
            binding.btnPlus.isEnabled = item.isAvailable
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val binding = ItemMenuFoodBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MenuItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun refreshCart() {
        notifyDataSetChanged()
    }
}

class MenuItemDiffCallback : DiffUtil.ItemCallback<MenuItem>() {
    override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
        return oldItem == newItem
    }
}

