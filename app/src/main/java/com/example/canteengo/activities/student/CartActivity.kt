package com.example.canteengo.activities.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canteengo.adapters.CartAdapter
import com.example.canteengo.databinding.ActivityCartBinding
import com.example.canteengo.models.CartManager
import com.example.canteengo.utils.toast

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupPlaceOrder()
        updateUI()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChange = {
                refreshCartList()
                updateTotals()
            },
            onRemove = {
                refreshCartList()
                updateTotals()
                toast("Item removed")
            }
        )

        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
        }
    }

    private fun setupPlaceOrder() {
        binding.btnPlaceOrder.setOnClickListener {
            if (CartManager.isEmpty()) {
                toast("Your cart is empty")
                return@setOnClickListener
            }
            startActivity(Intent(this, PickupTimeActivity::class.java))
        }
    }

    private fun refreshCartList() {
        // Create a completely new list to force RecyclerView update
        val newList = CartManager.items.map { it.copy() }
        cartAdapter.submitList(newList)

        // Update visibility based on cart state
        if (newList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.cartContent.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.cartContent.visibility = View.VISIBLE
        }
    }

    private fun updateTotals() {
        binding.tvSubtotal.text = "₹${CartManager.subtotal.toInt()}"
        binding.tvPackingCharge.text = "₹${CartManager.handlingCharge.toInt()}"
        binding.tvTotal.text = "₹${CartManager.total.toInt()}"
        binding.btnPlaceOrder.text = "Place Order • ₹${CartManager.total.toInt()}"
    }

    private fun updateUI() {
        refreshCartList()

        if (CartManager.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.cartContent.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.cartContent.visibility = View.VISIBLE
            updateTotals()
        }
    }
}

