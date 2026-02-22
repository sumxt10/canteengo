package com.example.canteengo.activities.student

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.canteengo.R
import com.example.canteengo.databinding.ActivityFoodDetailsBinding
import com.example.canteengo.models.CartManager
import com.example.canteengo.models.MenuItem
import com.example.canteengo.utils.toast

class FoodDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoodDetailsBinding
    private lateinit var menuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadMenuItem()
        setupUI()
        updateCartState()
    }

    override fun onResume() {
        super.onResume()
        updateCartState()
    }

    private fun loadMenuItem() {
        menuItem = MenuItem(
            id = intent.getStringExtra("menu_item_id") ?: "",
            name = intent.getStringExtra("menu_item_name") ?: "",
            description = intent.getStringExtra("menu_item_description") ?: "",
            price = intent.getDoubleExtra("menu_item_price", 0.0),
            category = intent.getStringExtra("menu_item_category") ?: "",
            imageUrl = intent.getStringExtra("menu_item_image_url") ?: "",
            isVeg = intent.getBooleanExtra("menu_item_is_veg", true),
            isAvailable = intent.getBooleanExtra("menu_item_is_available", true)
        )
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        // Set item details
        binding.tvName.text = menuItem.name
        binding.tvDescription.text = menuItem.description
        binding.tvPrice.text = "₹${menuItem.price.toInt()}"
        binding.tvCategory.text = menuItem.category.replace("_", " ")

        // Load image if available
        if (menuItem.imageUrl.isNotEmpty()) {
            binding.ivFoodImage.load(menuItem.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_food_item_placeholder)
                error(R.drawable.ic_food_item_placeholder)
            }
        }

        // Veg badge
        binding.ivVegBadge.setImageResource(
            if (menuItem.isVeg) R.drawable.ic_veg_badge else R.drawable.ic_nonveg_badge
        )
        binding.tvVegStatus.text = if (menuItem.isVeg) "Vegetarian" else "Non-Vegetarian"

        // Availability
        if (!menuItem.isAvailable) {
            binding.btnAddToCart.isEnabled = false
            binding.btnAddToCart.text = "Currently Unavailable"
            binding.btnPlus.isEnabled = false
        }

        // Add to cart button
        binding.btnAddToCart.setOnClickListener {
            CartManager.addItem(menuItem)
            updateCartState()
            toast("${menuItem.name} added to cart")
        }

        // Quantity controls
        binding.btnMinus.setOnClickListener {
            CartManager.decrementQuantity(menuItem.id)
            updateCartState()
        }

        binding.btnPlus.setOnClickListener {
            CartManager.incrementQuantity(menuItem.id)
            updateCartState()
        }
    }

    private fun updateCartState() {
        val quantity = CartManager.getQuantity(menuItem.id)

        if (quantity > 0) {
            binding.btnAddToCart.visibility = View.GONE
            binding.quantityContainer.visibility = View.VISIBLE
            binding.tvQuantity.text = quantity.toString()

            val itemTotal = menuItem.price * quantity
            binding.tvItemTotal.text = "Item Total: ₹${itemTotal.toInt()}"
            binding.tvItemTotal.visibility = View.VISIBLE
        } else {
            binding.btnAddToCart.visibility = View.VISIBLE
            binding.quantityContainer.visibility = View.GONE
            binding.tvItemTotal.visibility = View.GONE
        }
    }
}

