package com.example.canteengo.activities.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canteengo.R
import com.example.canteengo.adapters.OrderAdapter
import com.example.canteengo.databinding.ActivityStudentOrdersBinding
import com.example.canteengo.repository.OrderRepository
import kotlinx.coroutines.launch

class StudentOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentOrdersBinding
    private val orderRepository = OrderRepository()
    private lateinit var orderAdapter: OrderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupBottomNav()
        loadOrders()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter { order ->
            val intent = Intent(this, StudentOrderDetailsActivity::class.java)
            intent.putExtra("orderId", order.orderId)
            startActivity(intent)
        }

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(this@StudentOrdersActivity)
            adapter = orderAdapter
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_orders

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, StudentHomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_orders -> true
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

    private fun loadOrders() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val orders = orderRepository.getStudentOrders()

                binding.progressBar.visibility = View.GONE

                if (orders.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.rvOrders.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.rvOrders.visibility = View.VISIBLE
                    orderAdapter.submitList(orders)
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
        }
    }
}

