package com.example.canteengo.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canteengo.R
import com.example.canteengo.adapters.AdminMenuAdapter
import com.example.canteengo.databinding.ActivityAdminMenuBinding
import com.example.canteengo.models.MenuItem
import com.example.canteengo.repository.MenuRepository
import com.example.canteengo.utils.toast
import kotlinx.coroutines.launch

class AdminMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMenuBinding
    private val menuRepo = MenuRepository()
    private lateinit var menuAdapter: AdminMenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupBottomNav()
        setupFab()
        loadMenuItems()
    }

    override fun onResume() {
        super.onResume()
        loadMenuItems()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        menuAdapter = AdminMenuAdapter(
            onToggleAvailability = { item, isAvailable ->
                toggleItemAvailability(item, isAvailable)
            },
            onEditClick = { item ->
                // Navigate to edit screen
                val intent = Intent(this, AddEditMenuItemActivity::class.java)
                intent.putExtra("item_id", item.id)
                startActivity(intent)
            },
            onDeleteClick = { item ->
                deleteMenuItem(item)
            }
        )

        binding.rvMenuItems.apply {
            layoutManager = LinearLayoutManager(this@AdminMenuActivity)
            adapter = menuAdapter
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_menu

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_orders -> {
                    startActivity(Intent(this, AdminOrdersActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_menu -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, AdminProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFab() {
        binding.fabAddItem.setOnClickListener {
            startActivity(Intent(this, AddEditMenuItemActivity::class.java))
        }
    }

    private fun loadMenuItems() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val items = menuRepo.getAllMenuItems()

                // Clear all toggling states before updating list
                menuAdapter.clearAllTogglingStates()

                if (items.isEmpty()) {
                    binding.rvMenuItems.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                } else {
                    binding.rvMenuItems.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                    menuAdapter.submitList(items)
                }
            } catch (e: Exception) {
                toast("Failed to load menu: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun toggleItemAvailability(item: MenuItem, isAvailable: Boolean) {
        lifecycleScope.launch {
            try {
                // First update Firestore
                menuRepo.toggleAvailability(item.id, isAvailable)

                // Update local list without triggering full refresh
                val currentList = menuAdapter.currentList.toMutableList()
                val index = currentList.indexOfFirst { it.id == item.id }
                if (index != -1) {
                    currentList[index] = currentList[index].copy(isAvailable = isAvailable)
                    // Clear toggling state
                    menuAdapter.clearTogglingState(item.id)
                    // Use DiffUtil by submitting new list - this will animate smoothly
                    menuAdapter.submitList(currentList.toList())
                }

                toast("${item.name} is now ${if (isAvailable) "available" else "unavailable"}")
            } catch (e: Exception) {
                toast("Failed to update: ${e.message}")
                // Clear toggling state and reload to reset switch on error
                menuAdapter.clearTogglingState(item.id)
                loadMenuItems()
            }
        }
    }

    private fun deleteMenuItem(item: MenuItem) {
        lifecycleScope.launch {
            try {
                menuRepo.deleteMenuItem(item.id)
                toast("${item.name} deleted")
                loadMenuItems()
            } catch (e: Exception) {
                toast("Failed to delete: ${e.message}")
            }
        }
    }
}
