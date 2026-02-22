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
import com.example.canteengo.models.OrderStatus
import com.example.canteengo.repository.OrderRepository
import com.example.canteengo.utils.toast
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class AdminOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminOrdersBinding
    private val orderRepo = OrderRepository()
    private lateinit var orderAdapter: AdminOrderAdapter

    private var allOrders: List<Order> = emptyList()
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTabs()
        setupRecyclerView()
        setupBottomNav()
        loadOrders()
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
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

    private fun loadOrders() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                allOrders = orderRepo.getAllOrders()
                filterOrders()
            } catch (e: Exception) {
                toast("Failed to load orders: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun filterOrders() {
        val filtered = when (currentTab) {
            0 -> allOrders.filter { it.status == OrderStatus.RECEIVED } // New
            1 -> allOrders.filter { it.status == OrderStatus.ACCEPTED || it.status == OrderStatus.PREPARING } // Preparing
            2 -> allOrders.filter { it.status == OrderStatus.READY } // Ready
            3 -> allOrders.filter { it.status == OrderStatus.COLLECTED || it.status == OrderStatus.REJECTED } // Completed
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

    private fun updateOrderStatus(order: Order, newStatus: OrderStatus) {
        lifecycleScope.launch {
            try {
                orderRepo.updateOrderStatus(order.orderId, newStatus)
                toast("Order ${order.token} updated to ${newStatus.displayName}")
                loadOrders()
            } catch (e: Exception) {
                toast("Failed to update order: ${e.message}")
            }
        }
    }
}
