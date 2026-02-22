package com.example.canteengo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.canteengo.databinding.ItemCategoryBinding
import com.example.canteengo.models.FoodCategory

class CategoryAdapter(
    private val categories: List<FoodCategory>,
    private val onCategoryClick: (FoodCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: FoodCategory, isSelected: Boolean) {
            binding.tvEmoji.text = category.emoji
            binding.tvName.text = category.displayName

            binding.root.isSelected = isSelected
            binding.root.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onCategoryClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position == selectedPosition)
    }

    override fun getItemCount() = categories.size

    fun setSelectedCategory(category: FoodCategory) {
        val newPosition = categories.indexOf(category)
        if (newPosition != -1 && newPosition != selectedPosition) {
            val oldPosition = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
        }
    }
}

