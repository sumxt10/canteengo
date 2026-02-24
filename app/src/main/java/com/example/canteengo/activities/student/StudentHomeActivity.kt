package com.example.canteengo.activities.student

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canteengo.R
import com.example.canteengo.adapters.CategoryAdapter
import com.example.canteengo.adapters.MenuItemAdapter
import com.example.canteengo.databinding.ActivityStudentHomeBinding
import com.example.canteengo.models.CartManager
import com.example.canteengo.models.FoodCategory
import com.example.canteengo.models.MenuItem
import com.example.canteengo.repository.MenuRepository
import com.example.canteengo.repository.OrderRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.CacheManager
import com.example.canteengo.utils.toast
import kotlinx.coroutines.launch

class StudentHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentHomeBinding
    private val menuRepository = MenuRepository()
    private val userRepository = UserRepository()
    private val orderRepository = OrderRepository()

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var menuItemAdapter: MenuItemAdapter

    private var allMenuItems: List<MenuItem> = emptyList()
    private var selectedCategory: FoodCategory = FoodCategory.SNACKS
    private var currentSearchQuery: String = ""
    private var showAllItems: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupSearch()
        setupCategories()
        setupMenuItems()
        setupBottomNav()
        setupViewAllButtons()

        // Load cached data immediately for smooth UI, then refresh from network
        loadCachedDataFirst()
        loadUserData()
        loadMenuItems()
        loadSpendingStats()
    }

    private fun loadCachedDataFirst() {
        // Display cached profile immediately if available
        CacheManager.getStudentProfileEvenIfStale()?.let { profile ->
            binding.tvGreeting.text = "Hey ${profile.name.split(" ").first()}!"
            val initial = profile.name.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
            binding.tvProfileInitial.text = initial
        }

        // Display cached menu items immediately if available
        CacheManager.getMenuItemsEvenIfStale()?.let { items ->
            allMenuItems = items.filter { it.isAvailable }
            filterMenuItems()
        }

        // Display cached spending stats immediately if available
        CacheManager.getSpendingStatsEvenIfStale()?.let { stats ->
            binding.tvTodaySpending.text = "₹${stats.todaySpending.toInt()}"
            binding.tvWeekSpending.text = "₹${stats.weekSpending.toInt()}"
            binding.tvMonthSpending.text = "₹${stats.monthSpending.toInt()}"
            binding.tvTopCategory.text = if (stats.topCategory != "None") stats.topCategory else "No orders yet"
        }
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
        menuItemAdapter.refreshCart()
        loadSpendingStats()
    }

    private fun setupUI() {
        binding.profileContainer.setOnClickListener {
            startActivity(Intent(this, StudentProfileActivity::class.java))
        }


        binding.bannerCard.setOnClickListener {
            toast("Today's Special: Veg Thali!")
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                filterMenuItems()
            }
        })
    }

    private fun setupViewAllButtons() {
        // View all menu items button
        binding.tvViewAll.setOnClickListener {
            showAllItems = !showAllItems
            binding.tvViewAll.text = if (showAllItems) "Show Less" else "View All"
            filterMenuItems()
        }
    }

    private fun setupCategories() {
        val categories = FoodCategory.values().toList()
        categoryAdapter = CategoryAdapter(categories) { category ->
            selectedCategory = category
            showAllItems = false
            binding.tvViewAll.text = "View All"
            currentSearchQuery = ""
            binding.etSearch.setText("")
            filterMenuItems()
        }

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@StudentHomeActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }

    private fun setupMenuItems() {
        menuItemAdapter = MenuItemAdapter(
            onAddClick = { menuItem ->
                CartManager.addItem(menuItem)
                updateCartBadge()
                toast("${menuItem.name} added to cart")
            },
            onItemClick = { menuItem ->
                // Navigate to food details screen
                val intent = Intent(this, FoodDetailsActivity::class.java)
                intent.putExtra("menu_item_id", menuItem.id)
                intent.putExtra("menu_item_name", menuItem.name)
                intent.putExtra("menu_item_description", menuItem.description)
                intent.putExtra("menu_item_price", menuItem.price)
                intent.putExtra("menu_item_category", menuItem.category)
                intent.putExtra("menu_item_image_url", menuItem.imageUrl)
                intent.putExtra("menu_item_is_veg", menuItem.isVeg)
                intent.putExtra("menu_item_is_available", menuItem.isAvailable)
                startActivity(intent)
            }
        )

        binding.rvMenuItems.apply {
            layoutManager = GridLayoutManager(this@StudentHomeActivity, 2)
            adapter = menuItemAdapter
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_home

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_orders -> {
                    startActivity(Intent(this, StudentOrdersActivity::class.java))
                    false
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, StudentProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                val profile = userRepository.getCurrentStudentProfile()
                profile?.let {
                    // Cache the profile for smoother navigation
                    CacheManager.cacheStudentProfile(it)

                    binding.tvGreeting.text = "Hey ${it.name.split(" ").first()}!"
                    // Set profile initial
                    val initial = it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                    binding.tvProfileInitial.text = initial
                }
            } catch (e: Exception) {
                // Use default greeting
            }
        }
    }

    private fun loadMenuItems() {
        lifecycleScope.launch {
            try {
                val items = menuRepository.getAllMenuItems()
                // Cache menu items for smoother navigation
                CacheManager.cacheMenuItems(items)

                allMenuItems = items.filter { it.isAvailable }
                filterMenuItems()
            } catch (e: Exception) {
                toast("Failed to load menu: ${e.message}")
            }
        }
    }

    private fun filterMenuItems() {
        // Only show available items to students
        var filtered = allMenuItems.filter { it.isAvailable }

        // Apply search filter if query is not empty
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.name.contains(currentSearchQuery, ignoreCase = true) ||
                item.category.replace("_", " ").contains(currentSearchQuery, ignoreCase = true) ||
                item.description.contains(currentSearchQuery, ignoreCase = true)
            }
        } else if (!showAllItems) {
            // Apply category filter only if not searching and not showing all
            filtered = filtered.filter {
                it.category.equals(selectedCategory.name, ignoreCase = true)
            }
        }

        menuItemAdapter.submitList(filtered)
    }

    private fun updateCartBadge() {
        val count = CartManager.itemCount

        // Update bottom nav badge
        val badge = binding.bottomNav.getOrCreateBadge(R.id.nav_cart)
        if (count > 0) {
            badge.isVisible = true
            badge.number = count
        } else {
            badge.isVisible = false
        }
    }

    private fun loadSpendingStats() {
        lifecycleScope.launch {
            try {
                val stats = orderRepository.getSpendingStats()

                // Cache spending stats for smoother navigation
                CacheManager.cacheSpendingStats(stats)

                binding.tvTodaySpending.text = "₹${stats.todaySpending.toInt()}"
                binding.tvWeekSpending.text = "₹${stats.weekSpending.toInt()}"
                binding.tvMonthSpending.text = "₹${stats.monthSpending.toInt()}"
                binding.tvTopCategory.text = if (stats.topCategory != "None") stats.topCategory else "No orders yet"
                binding.tvTopCategoryCount.text = "${stats.topCategoryCount} orders"

            } catch (e: Exception) {
                // Keep defaults if there's an error
            }
        }
    }
}
