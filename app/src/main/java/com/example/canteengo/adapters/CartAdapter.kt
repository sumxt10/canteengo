package com.example.canteengo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.canteengo.databinding.ItemCartBinding
import com.example.canteengo.models.CartItem
import com.example.canteengo.models.CartManager

class CartAdapter(
    private val onQuantityChange: () -> Unit,
    private val onRemove: () -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.tvName.text = item.menuItem.name
            binding.tvPrice.text = "₹${item.menuItem.price.toInt()}"
            binding.tvQuantity.text = item.quantity.toString()
            binding.tvTotalPrice.text = "₹${item.totalPrice.toInt()}"

            binding.btnMinus.setOnClickListener {
                CartManager.decrementQuantity(item.menuItem.id)
                if (CartManager.getQuantity(item.menuItem.id) == 0) {
                    onRemove()
                } else {
                    onQuantityChange()
                }
            }

            binding.btnPlus.setOnClickListener {
                CartManager.incrementQuantity(item.menuItem.id)
                onQuantityChange()
            }

            binding.btnRemove.setOnClickListener {
                CartManager.removeItem(item.menuItem.id)
                onRemove()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
    override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
        return oldItem.menuItem.id == newItem.menuItem.id
    }

    override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
        return oldItem == newItem
    }
}

