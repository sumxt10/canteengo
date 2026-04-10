package com.example.canteengo.activities.student

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.canteengo.R
import com.example.canteengo.databinding.ActivityFoodDetailsBinding
import com.example.canteengo.models.CartManager
import com.example.canteengo.models.MenuItem
import com.example.canteengo.repository.MenuRepository
import com.example.canteengo.utils.toast
import com.google.firebase.firestore.ListenerRegistration
import java.text.DecimalFormat

class FoodDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoodDetailsBinding
    private lateinit var menuItem: MenuItem
    private val menuRepository = MenuRepository()
    private var menuItemListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadMenuItem()
        setupUI()
        bindMenuItemDetails()
        updateCartState()
        startRealtimeMenuItemListener()
    }

    override fun onResume() {
        super.onResume()
        updateCartState()
    }

    override fun onDestroy() {
        super.onDestroy()
        menuItemListener?.remove()
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
            isAvailable = intent.getBooleanExtra("menu_item_is_available", true),
            totalRatingSum = intent.getDoubleExtra("menu_item_rating_sum", 0.0),
            totalRatingCount = intent.getIntExtra("menu_item_rating_count", 0)
        )
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

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

    private fun bindMenuItemDetails() {
        binding.tvName.text = menuItem.name
        binding.tvDescription.text = menuItem.description
        binding.tvPrice.text = "₹${menuItem.price.toInt()}"
        binding.tvCategory.text = menuItem.category.replace("_", " ")

        if (menuItem.imageUrl.isNotEmpty()) {
            binding.ivFoodImage.load(menuItem.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_food_item_placeholder)
                error(R.drawable.ic_food_item_placeholder)
            }
        } else {
            binding.ivFoodImage.setImageResource(R.drawable.ic_food_item_placeholder)
        }

        binding.ivVegBadge.setImageResource(
            if (menuItem.isVeg) R.drawable.ic_veg_badge else R.drawable.ic_nonveg_badge
        )
        binding.tvVegStatus.text = if (menuItem.isVeg) "Vegetarian" else "Non-Vegetarian"

        val isAvailable = menuItem.isAvailable
        binding.btnAddToCart.isEnabled = isAvailable
        binding.btnAddToCart.text = if (isAvailable) "Add to Cart" else "Currently Unavailable"
        binding.btnPlus.isEnabled = isAvailable

        setupRatingDisplay()
        updateCartState()
    }

    private fun startRealtimeMenuItemListener() {
        menuItemListener?.remove()
        menuItemListener = menuRepository.observeMenuItemById(
            itemId = menuItem.id,
            onUpdate = { latestItem ->
                if (latestItem == null) {
                    toast("This menu item is no longer available")
                    finish()
                    return@observeMenuItemById
                }

                menuItem = latestItem
                bindMenuItemDetails()
            }
        )
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
            binding.btnPlus.isEnabled = menuItem.isAvailable
        } else {
            binding.btnAddToCart.visibility = View.VISIBLE
            binding.quantityContainer.visibility = View.GONE
            binding.tvItemTotal.visibility = View.GONE
        }
    }

    /**
     * Display rating information for this menu item.
     * Shows average rating and count if ratings exist, otherwise shows "no ratings" message.
     */
    private fun setupRatingDisplay() {
        if (menuItem.totalRatingCount > 0) {
            binding.ratingDisplaySection.visibility = View.VISIBLE
            binding.noRatingSection.visibility = View.GONE

            // Format average rating to 1 decimal place
            val avgRating = menuItem.averageRating
            val df = DecimalFormat("#.#")
            binding.tvAverageRating.text = df.format(avgRating)

            // Display rating count
            val countText = if (menuItem.totalRatingCount == 1) {
                "(1 rating)"
            } else {
                "(${menuItem.totalRatingCount} ratings)"
            }
            binding.tvRatingCount.text = countText
        } else {
            binding.ratingDisplaySection.visibility = View.GONE
            binding.noRatingSection.visibility = View.VISIBLE
        }
    }
}

