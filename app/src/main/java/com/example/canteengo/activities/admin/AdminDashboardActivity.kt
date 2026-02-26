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
import com.example.canteengo.models.OrderItem
import com.example.canteengo.models.OrderStatus
import com.example.canteengo.repository.AuthRepository
import com.example.canteengo.repository.MenuRepository
import com.example.canteengo.repository.OrderRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.CacheManager
import com.example.canteengo.utils.toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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

    // Real-time listener for orders
    private var ordersListener: ListenerRegistration? = null

    // Cached admin details for order acceptance
    private var cachedAdminPhone: String = ""
    private var cachedAdminName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupBottomNav()
        setupOrdersRecyclerView()
        loadAdminData()
        startRealtimeOrdersListener()
        seedMenuIfNeeded()
    }

    private fun seedMenuIfNeeded() {
        lifecycleScope.launch {
            menuRepo.seedMenuItems()
        }
    }

    override fun onResume() {
        super.onResume()
        // Listener already handles updates, no need to manually reload
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up listener
        ordersListener?.remove()
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
        // Show cached data immediately for smooth UI
        CacheManager.getAdminProfileEvenIfStale()?.let { profile ->
            binding.tvGreeting.text = "Welcome back, ${profile.name.split(" ").first()}!"
            binding.tvProfileInitial.text = profile.name.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
            cachedAdminPhone = profile.mobile
            cachedAdminName = profile.name
        }

        // Then refresh from Firestore in background
        lifecycleScope.launch {
            try {
                val profile = userRepo.getCurrentAdminProfile()
                profile?.let {
                    // Update cache
                    CacheManager.cacheAdminProfile(it)

                    // Update UI
                    binding.tvGreeting.text = "Welcome back, ${it.name.split(" ").first()}!"
                    val initial = it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
                    binding.tvProfileInitial.text = initial
                    cachedAdminPhone = it.mobile
                    cachedAdminName = it.name
                }
            } catch (e: Exception) {
                // Use cached or default greeting
                if (CacheManager.getAdminProfileEvenIfStale() == null) {
                    binding.tvGreeting.text = "Welcome back!"
                }
            }
        }
    }

    /**
     * Start real-time listener for orders.
     * Orders will automatically update across all admin devices when changes occur.
     * Admin dashboard only shows orders from the last 7 days for data lifecycle management.
     */
    private fun startRealtimeOrdersListener() {
        ordersListener?.remove()

        try {
            val db = FirebaseFirestore.getInstance()

            // Calculate timestamp for 7 days ago
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)

            ordersListener = db.collection("orders")
                .whereGreaterThanOrEqualTo("createdAt", sevenDaysAgo)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        toast("Failed to load orders")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        allOrders = snapshot.documents.mapNotNull { doc ->
                            try {
                                if (!doc.exists()) return@mapNotNull null
                                val data = doc.data ?: return@mapNotNull null
                                parseOrderFromMap(doc.id, data)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        updateDashboardUI()
                    }
                }
        } catch (_: Exception) {
            toast("Failed to connect to orders")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseOrderFromMap(id: String, data: Map<String, Any?>): Order {
        val itemsList = (data["items"] as? List<Map<String, Any?>>) ?: emptyList()
        val items = itemsList.map { itemMap ->
            OrderItem(
                menuItemId = itemMap["menuItemId"] as? String ?: "",
                name = itemMap["name"] as? String ?: "",
                price = (itemMap["price"] as? Number)?.toDouble() ?: 0.0,
                quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                totalPrice = (itemMap["totalPrice"] as? Number)?.toDouble() ?: 0.0
            )
        }

        return Order(
            orderId = id,
            token = data["token"] as? String ?: "",
            studentId = data["studentId"] as? String ?: "",
            studentName = data["studentName"] as? String ?: "",
            items = items,
            subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
            handlingCharge = (data["handlingCharge"] as? Number)?.toDouble() ?: 0.0,
            totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            pickupTime = data["pickupTime"] as? String ?: "ASAP",
            status = OrderStatus.fromString(data["status"] as? String ?: "RECEIVED"),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            qrString = data["qrString"] as? String ?: "",
            acceptedByAdminPhone = data["acceptedByAdminPhone"] as? String ?: "",
            acceptedByAdminName = data["acceptedByAdminName"] as? String ?: ""
        )
    }

    private fun updateDashboardUI() {
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
    }

    private fun updateOrderStatus(order: Order, newStatus: OrderStatus) {
        lifecycleScope.launch {
            try {
                // If accepting, use atomic transaction to prevent race conditions
                if (newStatus == OrderStatus.ACCEPTED) {
                    if (cachedAdminPhone.isEmpty()) {
                        toast("Unable to accept: Admin phone not available")
                        return@launch
                    }
                    orderRepo.acceptOrderAtomically(order.orderId, cachedAdminPhone, cachedAdminName)
                    toast("Order ${order.token} accepted successfully!")
                } else {
                    // For other status changes, include admin phone for ownership verification
                    if (cachedAdminPhone.isNotEmpty()) {
                        orderRepo.updateOrderStatusWithAdminPhone(order.orderId, newStatus, cachedAdminPhone)
                    } else {
                        orderRepo.updateOrderStatus(order.orderId, newStatus)
                    }
                    toast("Order ${order.token} updated to ${newStatus.name}")
                }
                // Real-time listener will automatically update the UI
            } catch (e: Exception) {
                // Show user-friendly error message with admin name if available
                val errorMessage = e.message ?: "Failed to update order"
                toast(errorMessage)
            }
        }
    }
}
