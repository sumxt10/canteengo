package com.example.canteengo.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canteengo.R
import com.example.canteengo.adapters.AdminOrderAdapter
import com.example.canteengo.databinding.ActivityAdminOrdersBinding
import com.example.canteengo.models.Order
import com.example.canteengo.models.OrderItem
import com.example.canteengo.models.OrderStatus
import com.example.canteengo.repository.OrderRepository
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.CacheManager
import com.example.canteengo.utils.toast
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class AdminOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminOrdersBinding
    private val orderRepo = OrderRepository()
    private val userRepo = UserRepository()
    private lateinit var orderAdapter: AdminOrderAdapter

    private var allOrders: List<Order> = emptyList()
    private var currentTab = 0
    private var ordersListener: ListenerRegistration? = null
    private var cachedAdminPhone: String = ""
    private var cachedAdminName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTabs()
        setupRecyclerView()
        setupBottomNav()
        loadAdminPhone()
        startRealtimeOrdersListener()
    }

    override fun onResume() {
        super.onResume()
        startRealtimeOrdersListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        ordersListener?.remove()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                filterOrders()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
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

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(this@AdminOrdersActivity)
            adapter = orderAdapter
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_orders

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_orders -> true
                R.id.nav_menu -> {
                    startActivity(Intent(this, AdminMenuActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, AdminProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun startRealtimeOrdersListener() {
        ordersListener?.remove()
        binding.progressBar.visibility = View.VISIBLE

        try {
            val db = FirebaseFirestore.getInstance()

            // Listen to all orders, real-time updates will handle filtering
            // The key difference: we now check acceptedByAdminPhone for ownership
            ordersListener = db.collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    binding.progressBar.visibility = View.GONE

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
                        filterOrders()
                    }
                }
        } catch (_: Exception) {
            binding.progressBar.visibility = View.GONE
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

    private fun filterOrders() {
        // For "New Orders" tab (tab 0): Show only RECEIVED orders that have NOT been accepted by any admin
        // This ensures once an admin accepts an order, it immediately disappears from all other admins' "New" tab
        // For other tabs: Show orders assigned to this admin OR all orders for visibility

        val filtered = when (currentTab) {
            0 -> {
                // New Orders: Only truly unassigned orders (RECEIVED status AND no admin assigned)
                allOrders.filter { order ->
                    order.status == OrderStatus.RECEIVED && order.acceptedByAdminPhone.isEmpty()
                }
            }
            1 -> {
                // Preparing: Orders being prepared (this admin's orders or all for visibility)
                allOrders.filter { order ->
                    order.status == OrderStatus.ACCEPTED || order.status == OrderStatus.PREPARING
                }
            }
            2 -> {
                // Ready: Orders ready for pickup
                allOrders.filter { order ->
                    order.status == OrderStatus.READY
                }
            }
            3 -> {
                // Completed: Collected or rejected orders
                allOrders.filter { order ->
                    order.status == OrderStatus.COLLECTED || order.status == OrderStatus.REJECTED
                }
            }
            else -> allOrders
        }

        if (filtered.isEmpty()) {
            binding.rvOrders.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvOrders.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            orderAdapter.submitList(filtered)
        }
    }

    private fun loadAdminPhone() {
        // Use cached data first
        CacheManager.getAdminProfileEvenIfStale()?.let { profile ->
            cachedAdminPhone = profile.mobile
            cachedAdminName = profile.name
        }

        // Then refresh from Firestore
        lifecycleScope.launch {
            try {
                val profile = userRepo.getCurrentAdminProfile()
                profile?.let {
                    CacheManager.cacheAdminProfile(it)
                    cachedAdminPhone = it.mobile
                    cachedAdminName = it.name
                }
            } catch (_: Exception) {
                // Ignore, use cached value
            }
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
                    toast("Order ${order.token} updated to ${newStatus.displayName}")
                }
                // No need to reload - real-time listener will update automatically
            } catch (e: Exception) {
                // Show user-friendly error message with admin name if available
                val errorMessage = e.message ?: "Failed to update order"
                toast(errorMessage)
            }
        }
    }
}
