package com.example.canteengo.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canteengo.R
import com.example.canteengo.activities.OnboardingActivity
import com.example.canteengo.adapters.AdminOrderAdapter
import com.example.canteengo.databinding.ActivityAdminDashboardBinding
import com.example.canteengo.models.Order
import com.example.canteengo.models.OrderStatus
import com.example.canteengo.repository.AuthRepository
import com.example.canteengo.repository.MenuRepository
import com.example.canteengo.repository.OrderRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.toast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()
    private val orderRepo = OrderRepository()
    private val menuRepo = MenuRepository()

    private lateinit var orderAdapter: AdminOrderAdapter
    private var allOrders: List<Order> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupBottomNav()
        setupOrdersRecyclerView()
        loadAdminData()
        loadOrders()
        seedMenuIfNeeded()
    }

    private fun seedMenuIfNeeded() {
        lifecycleScope.launch {
            menuRepo.seedMenuItems()
        }
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun setupUI() {
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }

        binding.cardViewOrders.setOnClickListener {
            startActivity(Intent(this, AdminOrdersActivity::class.java))
        }

        binding.cardManageMenu.setOnClickListener {
            startActivity(Intent(this, AdminMenuActivity::class.java))
        }

        binding.tvViewAllOrders.setOnClickListener {
            startActivity(Intent(this, AdminOrdersActivity::class.java))
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_dashboard

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_orders -> {
                    startActivity(Intent(this, AdminOrdersActivity::class.java))
                    false
                }
                R.id.nav_menu -> {
                    startActivity(Intent(this, AdminMenuActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, AdminProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun setupOrdersRecyclerView() {
        orderAdapter = AdminOrderAdapter(
            onItemClick = { order ->
                val intent = Intent(this, AdminOrderDetailsActivity::class.java)
                intent.putExtra("order_id", order.orderId)
                startActivity(intent)
            },
            onAcceptClick = { order -> updateOrderStatus(order, OrderStatus.ACCEPTED) },
            onPreparingClick = { order -> updateOrderStatus(order, OrderStatus.PREPARING) },
            onReadyClick = { order -> updateOrderStatus(order, OrderStatus.READY) },
            onCompleteClick = { order -> updateOrderStatus(order, OrderStatus.COLLECTED) },
            onRejectClick = { order -> updateOrderStatus(order, OrderStatus.REJECTED) }
        )

        binding.rvRecentOrders.apply {
            layoutManager = LinearLayoutManager(this@AdminDashboardActivity)
            adapter = orderAdapter
        }
    }

    private fun loadAdminData() {
        lifecycleScope.launch {
            try {
                val profile = userRepo.getCurrentAdminProfile()
                profile?.let {
                    binding.tvGreeting.text = "Welcome back, ${it.name.split(" ").first()}! 👋"
                    // Set profile initial
                    val initial = it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
                    binding.tvProfileInitial.text = initial
                }
            } catch (e: Exception) {
                // Use default greeting
            }
        }
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            try {
                allOrders = orderRepo.getAllOrders()

                // Get today's date
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Filter today's orders
                val todayOrders = allOrders.filter { order ->
                    val orderDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date(order.createdAt))
                    orderDate == today
                }

                // Calculate stats
                val pendingOrders = allOrders.filter {
                    it.status == OrderStatus.RECEIVED || it.status == OrderStatus.ACCEPTED
                }
                val completedTodayOrders = todayOrders.filter {
                    it.status == OrderStatus.COLLECTED
                }
                val todayEarnings = completedTodayOrders.sumOf { it.totalAmount }

                // Update UI
                binding.tvTodayOrders.text = todayOrders.size.toString()
                binding.tvTodayEarnings.text = "₹${todayEarnings.toInt()}"
                binding.tvPendingOrders.text = pendingOrders.size.toString()

                // Show recent orders (last 5)
                val recentOrders = allOrders.sortedByDescending { it.createdAt }.take(5)

                if (recentOrders.isEmpty()) {
                    binding.rvRecentOrders.visibility = View.GONE
                    binding.emptyOrdersState.visibility = View.VISIBLE
                } else {
                    binding.rvRecentOrders.visibility = View.VISIBLE
                    binding.emptyOrdersState.visibility = View.GONE
                    orderAdapter.submitList(recentOrders)
                }

            } catch (e: Exception) {
                toast("Failed to load orders: ${e.message}")
            }
        }
    }

    private fun updateOrderStatus(order: Order, newStatus: OrderStatus) {
        lifecycleScope.launch {
            try {
                orderRepo.updateOrderStatus(order.orderId, newStatus)
                toast("Order ${order.token} updated to ${newStatus.name}")
                loadOrders()
            } catch (e: Exception) {
                toast("Failed to update order: ${e.message}")
            }
        }
    }
}
